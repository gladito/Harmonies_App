package com.example.demo.Models;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/** Reserva de un bloque de horas en una sala. Sin pagos reales: el total es informativo. */
@Entity
@Table(name = "studio_bookings", indexes = @Index(name = "ix_bookings_slot", columnList = "studio_id,status,start_at"))
public class StudioBooking extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "studio_id")
    private Studio studio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Column(nullable = false)
    private int hours;

    @Column(name = "total_price", nullable = false)
    private int totalPrice;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 15)
    private BookingStatus status = BookingStatus.CONFIRMED;

    protected StudioBooking() {
    }

    public StudioBooking(Studio studio, User user, LocalDateTime startAt, int hours) {
        this.studio = studio;
        this.user = user;
        this.startAt = startAt;
        this.hours = hours;
        this.endAt = startAt.plusHours(hours);
        this.totalPrice = studio.getHourlyRate() * hours;
    }

    public Studio getStudio() { return studio; }
    public User getUser() { return user; }
    public LocalDateTime getStartAt() { return startAt; }
    public LocalDateTime getEndAt() { return endAt; }
    public int getHours() { return hours; }
    public int getTotalPrice() { return totalPrice; }
    public BookingStatus getStatus() { return status; }
    public void cancel() { this.status = BookingStatus.CANCELLED; }
    public boolean isUpcoming() { return status == BookingStatus.CONFIRMED && startAt.isAfter(LocalDateTime.now()); }
}
