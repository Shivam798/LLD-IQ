package com.movieticketbookingsystem;

import com.movieticketbookingsystem.enums.SeatType;
import com.movieticketbookingsystem.model.Booking;
import com.movieticketbookingsystem.model.Cinema;
import com.movieticketbookingsystem.model.City;
import com.movieticketbookingsystem.model.Movie;
import com.movieticketbookingsystem.model.Screen;
import com.movieticketbookingsystem.model.Seat;
import com.movieticketbookingsystem.model.Show;
import com.movieticketbookingsystem.model.User;
import com.movieticketbookingsystem.observer.UserObserver;
import com.movieticketbookingsystem.strategy.payment.CreditCardPaymentStrategy;
import com.movieticketbookingsystem.strategy.payment.UpiPaymentStrategy;
import com.movieticketbookingsystem.strategy.pricing.WeekdayPricingStrategy;
import com.movieticketbookingsystem.strategy.pricing.WeekendPricingStrategy;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class MovieTicketBookingDemo {
    public static void main(String[] args) throws InterruptedException {
        MovieBookingSystem system = MovieBookingSystem.getInstance();

        // 1. Cities
        City nyc = system.addCity("city1", "New York");
        system.addCity("city2", "Los Angeles");

        // 2. Movies
        Movie matrix = system.addMovie(new Movie("M1", "The Matrix", "Sci-Fi", "English", 136));
        Movie inception = system.addMovie(new Movie("M2", "Inception", "Sci-Fi", "English", 148));

        // 3. Screen with mixed seat types
        Screen screen1 = new Screen("SC1", "Screen 1");
        for (int row = 1; row <= 3; row++) {
            for (int col = 1; col <= 5; col++) {
                SeatType type = (row == 1) ? SeatType.RECLINER
                        : (row == 2 ? SeatType.PREMIUM : SeatType.REGULAR);
                screen1.addSeat(new Seat("R" + row + "C" + col, row, col, type));
            }
        }

        // 4. Cinema and shows
        Cinema pvr = system.addCinema("CIN1", "PVR Times Square", nyc.getId(), List.of(screen1));
        Show matrixShow = system.addShow("SH1", matrix.getId(), pvr.getId(), screen1.getId(),
                LocalDateTime.now().plusHours(3), new WeekdayPricingStrategy());
        system.addShow("SH2", inception.getId(), pvr.getId(), screen1.getId(),
                LocalDateTime.now().plusDays(1), new WeekendPricingStrategy());

        // 5. Users
        User alice = system.registerUser("Alice", "alice@example.com");
        User bob = system.registerUser("Bob", "bob@example.com");

        // 6. Observer — Alice subscribes to Matrix
        matrix.addObserver(new UserObserver(alice));
        System.out.println("--- Notify subscribers about Matrix release ---");
        matrix.notifyObservers();

        // 7. Search and view available seats
        System.out.println("\n--- Search for shows in New York ---");
        List<Show> hits = system.findShows("The Matrix", "New York");
        Show pick = hits.get(0);
        List<Seat> available = system.getAvailableSeats(pick.getId());
        System.out.println("Available seats: " + available.stream().map(Seat::getId).toList());

        // 8. Alice books two seats with credit card
        System.out.println("\n--- Alice books 2 seats ---");
        List<Seat> aliceSeats = List.of(available.get(0), available.get(1));
        Optional<Booking> aliceBooking = system.bookTickets(
                alice.getId(), pick.getId(), aliceSeats,
                new CreditCardPaymentStrategy("4111111111111234", "123"));
        aliceBooking.ifPresent(b -> System.out.printf("Booking %s | seats %s | ₹%.2f | %s%n",
                b.getId(), b.getSeats().stream().map(Seat::getId).toList(),
                b.getTotalAmount(), b.getStatus()));

        // 9. Bob tries to book a seat Alice already booked — should fail
        System.out.println("\n--- Bob tries to book one of Alice's seats ---");
        Optional<Booking> clash = system.bookTickets(
                bob.getId(), pick.getId(),
                List.of(available.get(0), available.get(2)),
                new UpiPaymentStrategy("bob@upi"));
        if (clash.isEmpty()) System.out.println("Bob's booking rejected (seat clash) — expected.");

        // 10. Bob books a different pair with UPI
        System.out.println("\n--- Bob books fresh seats with UPI ---");
        Optional<Booking> bobBooking = system.bookTickets(
                bob.getId(), pick.getId(),
                List.of(available.get(2), available.get(3)),
                new UpiPaymentStrategy("bob@upi"));
        bobBooking.ifPresent(b -> System.out.printf("Booking %s | seats %s | ₹%.2f | %s%n",
                b.getId(), b.getSeats().stream().map(Seat::getId).toList(),
                b.getTotalAmount(), b.getStatus()));

        // 11. Snapshot of seat statuses
        System.out.println("\n--- Final seat statuses (first row) ---");
        pick.getScreen().getSeats().stream()
                .filter(s -> s.getRow() == 1)
                .forEach(s -> System.out.printf("%s [%s] -> %s%n",
                        s.getId(), s.getType(), s.getStatus()));

        system.shutdown();
    }
}
