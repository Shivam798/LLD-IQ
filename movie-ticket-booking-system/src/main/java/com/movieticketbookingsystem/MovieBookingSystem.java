package com.movieticketbookingsystem;

import com.movieticketbookingsystem.enums.SeatStatus;
import com.movieticketbookingsystem.model.Booking;
import com.movieticketbookingsystem.model.Cinema;
import com.movieticketbookingsystem.model.City;
import com.movieticketbookingsystem.model.Movie;
import com.movieticketbookingsystem.model.Screen;
import com.movieticketbookingsystem.model.Seat;
import com.movieticketbookingsystem.model.Show;
import com.movieticketbookingsystem.model.User;
import com.movieticketbookingsystem.strategy.payment.PaymentStrategy;
import com.movieticketbookingsystem.strategy.pricing.PricingStrategy;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Top-level facade for the whole booking system — the single entry point that
 * client code (the demo, a CLI, a future REST controller) talks to. It owns the
 * in-memory catalogs (cities, cinemas, movies, shows, users) and delegates the
 * actual booking flow to BookingManager / SeatLockManager.
 *
 * Implemented as a Singleton because the whole application needs a single shared
 * view of state — multiple instances would mean fragmented catalogs and locks that
 * don't see each other.
 */
public class MovieBookingSystem {
    // 'volatile' is the key piece of double-checked locking — without it, the JVM
    // could publish a partially-constructed instance to another thread (instance ref
    // visible but fields not yet initialized). volatile guarantees a happens-before
    // edge so other threads see the fully-built object.
    private static volatile MovieBookingSystem instance;

    // All catalogs are ConcurrentHashMap because admin operations (addCity, addShow)
    // and user operations (findShows, bookTickets) can hit the system concurrently.
    // Keyed by id for O(1) lookup — the typical access pattern is "fetch by id".
    private final Map<String, City> cities = new ConcurrentHashMap<>();
    private final Map<String, Cinema> cinemas = new ConcurrentHashMap<>();
    private final Map<String, Movie> movies = new ConcurrentHashMap<>();
    private final Map<String, Show> shows = new ConcurrentHashMap<>();
    private final Map<String, User> users = new ConcurrentHashMap<>();

    // Composition over inheritance — the system "has a" lock manager and a booking
    // manager rather than "being one". Each collaborator has a single responsibility
    // (SRP); this class only orchestrates and exposes the public API.
    private final SeatLockManager seatLockManager;
    private final BookingManager bookingManager;

    // Private constructor enforces the Singleton — outside code cannot do `new`.
    private MovieBookingSystem() {
        this.seatLockManager = new SeatLockManager();
        // BookingManager takes the lock manager as a constructor arg (DIP — depend on
        // abstractions / inject dependencies rather than letting it construct its own).
        this.bookingManager = new BookingManager(seatLockManager);
    }

    /**
     * Double-checked locking Singleton accessor. The outer null-check avoids the
     * synchronized cost on every call after init; the inner null-check inside the
     * synchronized block handles the race where two threads pass the outer check
     * before either has constructed the instance.
     */
    public static MovieBookingSystem getInstance() {
        if (instance == null) {
            synchronized (MovieBookingSystem.class) {
                if (instance == null) {
                    instance = new MovieBookingSystem();
                }
            }
        }
        return instance;
    }

    // ---------- Catalog management (admin-side operations) ----------

    /** Register a city where cinemas can operate. */
    public City addCity(String id, String name) {
        City city = new City(id, name);
        cities.put(city.getId(), city);
        return city;
    }

    /**
     * Register a cinema inside a known city. Fails fast if the city isn't registered
     * — better to surface a programming error here than to silently create an orphan
     * cinema that no user can ever find via city-based search.
     */
    public Cinema addCinema(String id, String name, String cityId, List<Screen> screens) {
        City city = cities.get(cityId);
        if (city == null) throw new IllegalArgumentException("Unknown city: " + cityId);
        Cinema cinema = new Cinema(id, name, city, screens);
        cinemas.put(cinema.getId(), cinema);
        return cinema;
    }

    /** Register a movie in the catalog so it can later be scheduled as a Show. */
    public Movie addMovie(Movie movie) {
        movies.put(movie.getId(), movie);
        return movie;
    }

    /**
     * Schedule a show: a (movie, cinema, screen, start-time) tuple with its own
     * pricing strategy. Pricing is injected per-show so different shows can use
     * different rules (flat, dynamic, weekend surge) without changing this class —
     * Strategy pattern, OCP-friendly.
     */
    public Show addShow(String id, String movieId, String cinemaId, String screenId,
                        LocalDateTime startTime, PricingStrategy pricingStrategy) {
        Movie movie = movies.get(movieId);
        Cinema cinema = cinemas.get(cinemaId);
        if (movie == null || cinema == null) {
            throw new IllegalArgumentException("Unknown movie or cinema");
        }
        // Validate that the requested screen actually belongs to the given cinema —
        // prevents creating a show pointing at a screen owned by a different cinema.
        Screen screen = cinema.getScreens().stream()
                .filter(s -> s.getId().equals(screenId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Screen not in cinema: " + screenId));
        Show show = new Show(id, movie, screen, cinema, startTime, pricingStrategy);
        shows.put(show.getId(), show);
        return show;
    }

    /** Register a new end-user who can later book tickets. */
    public User registerUser(String name, String email) {
        User user = new User(name, email);
        users.put(user.getId(), user);
        return user;
    }

    // ---------- Discovery (user-side read operations) ----------

    /**
     * Find all shows of a given movie playing in a given city. Streams over the
     * in-memory show map — fine for a demo. A real system would back this with an
     * index (movieId+cityId → shows) since the unfiltered scan is O(N) in show count.
     */
    public List<Show> findShows(String movieTitle, String cityName) {
        return shows.values().stream()
                .filter(s -> s.getMovie().getTitle().equalsIgnoreCase(movieTitle))
                .filter(s -> s.getCinema().getCity().getName().equalsIgnoreCase(cityName))
                .collect(Collectors.toList());
    }

    /**
     * Snapshot of seats currently AVAILABLE for a show. Note: this is a point-in-time
     * read — a seat shown here can be locked or booked by another user microseconds
     * later, which is exactly why BookingManager re-checks under a lock before charging.
     */
    public List<Seat> getAvailableSeats(String showId) {
        Show show = shows.get(showId);
        if (show == null) return List.of();
        return show.getScreen().getSeats().stream()
                .filter(seat -> seat.getStatus() == SeatStatus.AVAILABLE)
                .collect(Collectors.toList());
    }

    // ---------- Booking (user-side write operations) ----------

    /**
     * Thin wrapper that resolves ids into domain objects and then hands off to
     * BookingManager for the real workflow. Returning Optional.empty() for an
     * unknown user/show keeps the API uniform with "booking couldn't be created".
     */
    public Optional<Booking> bookTickets(String userId, String showId, List<Seat> seats,
                                         PaymentStrategy paymentStrategy) {
        User user = users.get(userId);
        Show show = shows.get(showId);
        if (user == null || show == null) return Optional.empty();
        return bookingManager.createBooking(user, show, seats, paymentStrategy);
    }

    /** Lookup an existing booking — pass-through to BookingManager's store. */
    public Optional<Booking> getBooking(String bookingId) {
        return bookingManager.getBooking(bookingId);
    }

    /**
     * Cleanly shut down background resources (the lock manager's scheduler thread).
     * Without this, the JVM would hang on exit waiting for non-daemon scheduler threads.
     */
    public void shutdown() {
        seatLockManager.shutdown();
        System.out.println("MovieBookingSystem shut down.");
    }
}
