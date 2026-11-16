package com.example.demo.Services;

import com.example.demo.Models.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Salas de ensayo y estudios: publicación, reservas por bloques de horas y cancelación.
 * No procesa pagos; el total de la reserva es informativo.
 */
@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class StudioService {

    public static final int MAX_HOURS = 8;
    public static final int MAX_DAYS_AHEAD = 180;

    @PersistenceContext
    private EntityManager em;
    private final CommunityService community;
    @Autowired
    private StateMovementService movementService;

    public StudioService(CommunityService community) {
        this.community = community;
    }

    public List<Studio> list(Long viewer, StudioType type) {
        community.user(viewer);
        var query = em.createQuery("select s from Studio s join fetch s.owner o where s.deleted=false and o.deleted=false and o.enabled=true"
                + (type == null ? "" : " and s.type=:type")
                + " and not exists (select b.id from UserBlock b where b.owner=o and b.target.id=:v) order by s.name", Studio.class)
                .setParameter("v", viewer);
        if (type != null) query.setParameter("type", type);
        return query.setMaxResults(100).getResultList();
    }

    public Studio studio(Long id, Long viewer) {
        community.user(viewer);
        Studio s = id == null ? null : em.find(Studio.class, id);
        if (s == null || s.isDeleted() || s.getOwner().isDeleted() || !community.canSeeUser(viewer, s.getOwner().getId()))
            throw new IllegalArgumentException("Sala no disponible");
        return s;
    }

    public Studio create(Long actor, String name, StudioType type, String description, String location, String amenities, Integer hourlyRate) {
        User owner = community.user(actor);
        name = required(name, 80, "Indicá un nombre de hasta 80 caracteres");
        location = required(location, 120, "Indicá una ubicación de hasta 120 caracteres");
        description = optional(description, 500, "La descripción admite hasta 500 caracteres");
        amenities = optional(amenities, 255, "El equipamiento admite hasta 255 caracteres");
        if (type == null) throw new IllegalArgumentException("Elegí el tipo de espacio");
        if (hourlyRate == null || hourlyRate < 1 || hourlyRate > 1_000_000)
            throw new IllegalArgumentException("El precio por hora debe estar entre 1 y 1.000.000");
        Studio studio = new Studio(name, type, description, location, amenities, hourlyRate, owner);
        em.persist(studio);
        return studio;
    }

    /** Despublica la sala y cancela sus reservas futuras. Solo el dueño. */
    public void remove(Long actor, Long studioId) {
        User owner = community.user(actor);
        Studio s = lock(studioId);
        if (!s.getOwner().getId().equals(actor)) throw new IllegalArgumentException("Solo quien publicó la sala puede quitarla");
        for (StudioBooking b : upcoming(studioId)) {
            String prev = b.getStatus().name();
            b.cancel();
            if (movementService != null) {
                movementService.record(
                        MovementEntityType.STUDIO_BOOKING,
                        b.getId(),
                        b.getUser().getUsername() + " → " + s.getName(),
                        prev,
                        b.getStatus().name(),
                        owner,
                        "Reserva cancelada por baja de la sala"
                );
            }
        }
        s.markAsDeleted();
    }

    public StudioBooking book(Long actor, Long studioId, String start, Integer hours) {
        User user = community.user(actor);
        Studio s = lock(studioId);
        studio(studioId, actor);
        if (s.getOwner().getId().equals(actor)) throw new IllegalArgumentException("No podés reservar tu propia sala");
        if (community.blocked(actor, s.getOwner().getId())) throw new IllegalArgumentException("Sala no disponible");
        LocalDateTime startAt = parseStart(start);
        if (hours == null || hours < 1 || hours > MAX_HOURS)
            throw new IllegalArgumentException("Las reservas son de 1 a " + MAX_HOURS + " horas");
        LocalDateTime now = LocalDateTime.now();
        if (!startAt.isAfter(now)) throw new IllegalArgumentException("Elegí un horario futuro");
        if (startAt.isAfter(now.plusDays(MAX_DAYS_AHEAD)))
            throw new IllegalArgumentException("Solo se puede reservar hasta " + MAX_DAYS_AHEAD + " días adelante");
        LocalDateTime endAt = startAt.plusHours(hours);
        long overlapping = em.createQuery("select count(b) from StudioBooking b where b.studio.id=:s and b.status=:st and b.startAt<:end and b.endAt>:start", Long.class)
                .setParameter("s", studioId).setParameter("st", BookingStatus.CONFIRMED)
                .setParameter("start", startAt).setParameter("end", endAt).getSingleResult();
        if (overlapping > 0) throw new IllegalArgumentException("Ese horario ya está reservado. Elegí otro.");
        StudioBooking booking = new StudioBooking(s, user, startAt, hours);
        em.persist(booking);
        em.flush();
        if (movementService != null) {
            movementService.record(
                    MovementEntityType.STUDIO_BOOKING,
                    booking.getId(),
                    user.getUsername() + " → " + s.getName(),
                    null,
                    booking.getStatus().name(),
                    user,
                    "Reserva confirmada por " + hours + " h"
            );
        }
        return booking;
    }

    /** Cancela una reserva futura. Puede hacerlo quien reservó o el dueño de la sala. */
    public void cancel(Long actor, Long bookingId) {
        User actorUser = community.user(actor);
        StudioBooking b = bookingId == null ? null : em.find(StudioBooking.class, bookingId);
        if (b == null) throw new IllegalArgumentException("Reserva no disponible");
        lock(b.getStudio().getId());
        boolean allowed = b.getUser().getId().equals(actor) || b.getStudio().getOwner().getId().equals(actor);
        if (!allowed) throw new IllegalArgumentException("No podés cancelar esta reserva");
        if (!b.isUpcoming()) throw new IllegalArgumentException("Solo se pueden cancelar reservas futuras confirmadas");
        String prev = b.getStatus().name();
        b.cancel();
        if (movementService != null) {
            String note = b.getUser().getId().equals(actor)
                    ? "Reserva cancelada por quien reservó"
                    : "Reserva cancelada por el responsable de la sala";
            movementService.record(
                    MovementEntityType.STUDIO_BOOKING,
                    b.getId(),
                    b.getUser().getUsername() + " → " + b.getStudio().getName(),
                    prev,
                    b.getStatus().name(),
                    actorUser,
                    note
            );
        }
    }

    public List<StudioBooking> upcoming(Long studioId) {
        return em.createQuery("select b from StudioBooking b join fetch b.user where b.studio.id=:s and b.status=:st and b.endAt>:now order by b.startAt", StudioBooking.class)
                .setParameter("s", studioId).setParameter("st", BookingStatus.CONFIRMED).setParameter("now", LocalDateTime.now())
                .setMaxResults(50).getResultList();
    }

    public List<StudioBooking> myBookings(Long actor) {
        return em.createQuery("select b from StudioBooking b join fetch b.studio where b.user.id=:u order by b.startAt desc", StudioBooking.class)
                .setParameter("u", actor).setMaxResults(50).getResultList();
    }

    public List<Studio> owned(Long actor) {
        return em.createQuery("from Studio s where s.owner.id=:u and s.deleted=false order by s.name", Studio.class)
                .setParameter("u", actor).getResultList();
    }

    private Studio lock(Long id) {
        Studio s = id == null ? null : em.find(Studio.class, id);
        if (s == null || s.isDeleted()) throw new IllegalArgumentException("Sala no disponible");
        em.lock(s, LockModeType.PESSIMISTIC_WRITE);
        return s;
    }

    private LocalDateTime parseStart(String raw) {
        if (raw == null || raw.isBlank()) throw new IllegalArgumentException("Elegí fecha y hora de inicio");
        try {
            LocalDateTime value = LocalDateTime.parse(raw.trim());
            if (value.getMinute() != 0 || value.getSecond() != 0 || value.getNano() != 0)
                throw new IllegalArgumentException("Las reservas comienzan en horas exactas (por ejemplo 18:00)");
            return value;
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Fecha u hora inválida");
        }
    }

    static String required(String value, int max, String error) {
        if (value == null || value.isBlank() || value.trim().length() > max) throw new IllegalArgumentException(error);
        return value.trim();
    }

    static String optional(String value, int max, String error) {
        if (value == null || value.isBlank()) return null;
        if (value.trim().length() > max) throw new IllegalArgumentException(error);
        return value.trim();
    }
}
