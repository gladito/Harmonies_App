package com.example.demo.Models;

import jakarta.persistence.*;
import java.util.Objects;

/**
 * Tabla unificada de movimientos de estado para trazabilidad y auditoría.
 * Registra cada transición (previous_status -> new_status), fecha/hora, actor y motivo.
 */
@Entity
@Table(name = "state_movements")
public class StateMovement extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 32)
    private MovementEntityType entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(name = "entity_label", nullable = false, length = 160)
    private String entityLabel;

    @Column(name = "previous_status", length = 32)
    private String previousStatus;

    @Column(name = "new_status", nullable = false, length = 32)
    private String newStatus;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_user_id", nullable = false)
    private User actor;

    @Column(name = "note", length = 255)
    private String note;

    public StateMovement() {
    }

    public StateMovement(MovementEntityType entityType,
                         Long entityId,
                         String entityLabel,
                         String previousStatus,
                         String newStatus,
                         User actor,
                         String note) {
        this.entityType = Objects.requireNonNull(entityType, "entityType es obligatorio");
        this.entityId = Objects.requireNonNull(entityId, "entityId es obligatorio");
        this.entityLabel = Objects.requireNonNull(entityLabel, "entityLabel es obligatorio");
        this.previousStatus = previousStatus;
        this.newStatus = Objects.requireNonNull(newStatus, "newStatus es obligatorio");
        this.actor = Objects.requireNonNull(actor, "actor es obligatorio");
        this.note = note;
    }

    public MovementEntityType getEntityType() {
        return entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public String getEntityLabel() {
        return entityLabel;
    }

    public String getPreviousStatus() {
        return previousStatus;
    }

    public String getNewStatus() {
        return newStatus;
    }

    public User getActor() {
        return actor;
    }

    public String getNote() {
        return note;
    }
}
