package com.taskscheduler.strategy;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * A minute-resolution cron expression with the classic 5 fields:
 *
 * <pre>
 *   minute(0-59)  hour(0-23)  dayOfMonth(1-31)  month(1-12)  dayOfWeek(0-6, 0=Sun)
 * </pre>
 *
 * <p>Each field supports {@code *}, single values, comma lists {@code a,b}, ranges
 * {@code a-b}, and steps written with a slash — {@code star/n} (i.e. {@code *} then
 * {@code /n}) or {@code a-b/n}. Day-of-week also accepts {@code 7} as Sunday.
 *
 * <p><b>Parsing is separated from scheduling on purpose (SRP):</b> this class only
 * knows how to answer "does this instant match?" and "what is the next matching
 * instant after T?". {@link CronSchedule} adapts it to the {@link Schedule} contract.
 *
 * <p><b>Day-of-month vs day-of-week:</b> standard cron semantics — if <i>both</i>
 * fields are restricted (neither is {@code *}), an instant matches when <i>either</i>
 * matches (OR). If only one is restricted, only that one must match.
 *
 * <p><b>Algorithm:</b> "next after" walks forward minute by minute from the next
 * whole minute, testing {@link #matches}. It is O(minutes-until-match) rather than a
 * closed-form jump — simple, obviously correct, and bounded to ~4 years so a never-
 * matching expression fails loudly instead of looping forever. A production cron
 * would field-jump for speed; for an LLD the clarity is the point.
 */
public final class CronExpression {

    private static final int SAFETY_LIMIT_MINUTES = 4 * 366 * 24 * 60; // ~4 years

    private final boolean[] minutes;     // index 0..59
    private final boolean[] hours;       // index 0..23
    private final boolean[] daysOfMonth; // index 1..31
    private final boolean[] months;      // index 1..12
    private final boolean[] daysOfWeek;  // index 0..6 (0 = Sunday)
    private final boolean domRestricted;
    private final boolean dowRestricted;
    private final ZoneId zone;
    private final String raw;

    private CronExpression(boolean[] minutes, boolean[] hours, boolean[] daysOfMonth,
                           boolean[] months, boolean[] daysOfWeek,
                           boolean domRestricted, boolean dowRestricted,
                           ZoneId zone, String raw) {
        this.minutes = minutes;
        this.hours = hours;
        this.daysOfMonth = daysOfMonth;
        this.months = months;
        this.daysOfWeek = daysOfWeek;
        this.domRestricted = domRestricted;
        this.dowRestricted = dowRestricted;
        this.zone = zone;
        this.raw = raw;
    }

    public static CronExpression parse(String expression) {
        return parse(expression, ZoneId.systemDefault());
    }

    public static CronExpression parse(String expression, ZoneId zone) {
        if (expression == null) {
            throw new IllegalArgumentException("cron expression is required");
        }
        String[] f = expression.trim().split("\\s+");
        if (f.length != 5) {
            throw new IllegalArgumentException(
                    "cron must have 5 fields (min hour dom month dow): '" + expression + "'");
        }
        boolean[] minutes = parseField(f[0], 0, 59);
        boolean[] hours = parseField(f[1], 0, 23);
        boolean[] dom = parseField(f[2], 1, 31);
        boolean[] months = parseField(f[3], 1, 12);

        // Day-of-week: parse over 0..7, then fold 7 (Sunday) into 0.
        boolean[] dow8 = parseField(f[4], 0, 7);
        boolean[] dow = new boolean[7];
        for (int i = 0; i <= 6; i++) {
            dow[i] = dow8[i];
        }
        if (dow8[7]) {
            dow[0] = true;
        }

        boolean domRestricted = !f[2].equals("*");
        boolean dowRestricted = !f[4].equals("*");
        return new CronExpression(minutes, hours, dom, months, dow,
                domRestricted, dowRestricted, zone, expression.trim());
    }

    /** Parse one field into an inclusion table of size {@code max + 1}. */
    private static boolean[] parseField(String field, int min, int max) {
        boolean[] allowed = new boolean[max + 1];
        for (String token : field.split(",")) {
            int step = 1;
            String range = token;
            int slash = token.indexOf('/');
            if (slash >= 0) {
                range = token.substring(0, slash);
                step = Integer.parseInt(token.substring(slash + 1));
                if (step <= 0) {
                    throw new IllegalArgumentException("step must be positive: " + token);
                }
            }

            int lo;
            int hi;
            if (range.equals("*")) {
                lo = min;
                hi = max;
            } else if (range.contains("-")) {
                String[] bounds = range.split("-");
                lo = Integer.parseInt(bounds[0]);
                hi = Integer.parseInt(bounds[1]);
            } else {
                lo = Integer.parseInt(range);
                // "5/15" means 5,20,35,... up to max; a bare "5" means just 5.
                hi = (slash >= 0) ? max : lo;
            }

            if (lo < min || hi > max || lo > hi) {
                throw new IllegalArgumentException(
                        "field '" + field + "' out of range [" + min + "," + max + "]");
            }
            for (int i = lo; i <= hi; i += step) {
                allowed[i] = true;
            }
        }
        return allowed;
    }

    /** True if the given wall-clock time matches every field of this expression. */
    public boolean matches(ZonedDateTime t) {
        if (!months[t.getMonthValue()]) {
            return false;
        }
        if (!hours[t.getHour()]) {
            return false;
        }
        if (!minutes[t.getMinute()]) {
            return false;
        }
        boolean domMatch = daysOfMonth[t.getDayOfMonth()];
        // ISO: MONDAY=1 .. SUNDAY=7  ->  cron: SUNDAY=0 .. SATURDAY=6
        int cronDow = t.getDayOfWeek().getValue() % 7;
        boolean dowMatch = daysOfWeek[cronDow];

        if (domRestricted && dowRestricted) {
            return domMatch || dowMatch; // classic OR semantics
        }
        if (domRestricted) {
            return domMatch;
        }
        if (dowRestricted) {
            return dowMatch;
        }
        return true; // both fields are '*'
    }

    /** The first matching instant strictly after {@code after} (minute resolution). */
    public Instant nextAfter(Instant after) {
        ZonedDateTime t = after.atZone(zone)
                .withSecond(0)
                .withNano(0)
                .plusMinutes(1); // strictly after, snapped to a whole minute
        for (int i = 0; i < SAFETY_LIMIT_MINUTES; i++) {
            if (matches(t)) {
                return t.toInstant();
            }
            t = t.plusMinutes(1);
        }
        throw new IllegalStateException("no matching time within ~4 years for '" + raw + "'");
    }

    @Override
    public String toString() {
        return raw;
    }
}
