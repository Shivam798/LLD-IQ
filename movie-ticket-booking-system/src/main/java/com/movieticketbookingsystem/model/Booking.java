package com.movieticketbookingsystem.model;

import com.movieticketbookingsystem.enums.BookingStatus;
import com.movieticketbookingsystem.enums.SeatStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class Booking {
    private final String id;
    private final User user;
    private final Show show;
    private final List<Seat> seats;
    private final double totalAmount;
    private final Payment payment;
    private final LocalDateTime bookedAt;
    private BookingStatus status;

    private Booking(String id, User user, Show show, List<Seat> seats,
                    double totalAmount, Payment payment) {
        this.id = id;
        this.user = user;
        this.show = show;
        this.seats = seats;
        this.totalAmount = totalAmount;
        this.payment = payment;
        this.bookedAt = LocalDateTime.now();
        this.status = BookingStatus.PENDING;
    }

    public void confirmBooking() {
        for (Seat seat : seats) {
            seat.setStatus(SeatStatus.BOOKED);
        }
        this.status = BookingStatus.CONFIRMED;
    }

    public void cancelBooking() {
        for (Seat seat : seats) {
            seat.setStatus(SeatStatus.AVAILABLE);
        }
        this.status = BookingStatus.CANCELLED;
    }

    public String getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Show getShow() {
        return show;
    }

    public List<Seat> getSeats() {
        return seats;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public Payment getPayment() {
        return payment;
    }

    public LocalDateTime getBookedAt() {
        return bookedAt;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public static class Builder {
        private String id = UUID.randomUUID().toString();
        private User user;
        private Show show;
        private List<Seat> seats;
        private double totalAmount;
        private Payment payment;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder user(User user) {
            this.user = user;
            return this;
        }

        public Builder show(Show show) {
            this.show = show;
            return this;
        }

        public Builder seats(List<Seat> seats) {
            this.seats = seats;
            return this;
        }

        public Builder totalAmount(double totalAmount) {
            this.totalAmount = totalAmount;
            return this;
        }

        public Builder payment(Payment payment) {
            this.payment = payment;
            return this;
        }

        public Booking build() {
            if (user == null || show == null || seats == null || seats.isEmpty() || payment == null) {
                throw new IllegalStateException("Booking is missing required fields");
            }
            return new Booking(id, user, show, seats, totalAmount, payment);
        }
    }
}
