package com.meetingroombooking.model;

import java.time.LocalDateTime;

/**
 * A half-open time interval {@code [start, end)} — the single smallest idea
 * the whole system is built on.
 *
 * <p><b>Why half-open?</b> A meeting from 09:00–10:00 and one from 10:00–11:00
 * are back-to-back, NOT a conflict. If the interval were closed on both ends
 * ([start, end]) the shared 10:00 instant would count as an overlap and you
 * could never schedule consecutive meetings. Making {@code end} exclusive is
 * what encodes "the room is free again the instant the meeting ends".
 *
 * <p>Immutable and self-validating: {@code start} must be strictly before
 * {@code end}, so an illegal slot can never exist (fail fast at construction).
 */
public final class TimeSlot {

    private final LocalDateTime start;
    private final LocalDateTime end;

    public TimeSlot(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("start and end are required");
        }
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException(
                    "start must be strictly before end: " + start + " .. " + end);
        }
        this.start = start;
        this.end = end;
    }

    /**
     * Two half-open intervals overlap iff each starts before the other ends.
     * Because the ends are exclusive, touching intervals (09–10, 10–11) return
     * false — exactly the back-to-back case we want to allow. This is the single
     * source of truth for the overlap rule; callers must not re-derive it.
     */
    public boolean overlaps(TimeSlot other) {
        return this.start.isBefore(other.end) && other.start.isBefore(this.end);
    }

    public LocalDateTime getStart() {
        return start;
    }

    public LocalDateTime getEnd() {
        return end;
    }

    @Override
    public String toString() {
        return "[" + start.toLocalTime() + " -> " + end.toLocalTime() + ")";
    }
}
