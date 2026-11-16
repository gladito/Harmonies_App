package com.example.demo.Services;

import com.example.demo.Models.MovementEntityType;
import com.example.demo.Models.StateMovement;
import com.example.demo.Models.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class StateMovementService {

    @PersistenceContext
    private EntityManager em;
    private final CommunityService community;

    public StateMovementService(CommunityService community) {
        this.community = community;
    }

    public StateMovement record(MovementEntityType entityType,
                                Long entityId,
                                String entityLabel,
                                String previousStatus,
                                String newStatus,
                                User actor,
                                String note) {
        String safeLabel = entityLabel == null || entityLabel.isBlank()
                ? entityType.name() + " #" + entityId
                : (entityLabel.length() > 160 ? entityLabel.substring(0, 160) : entityLabel);
        String safeNote = note != null && note.length() > 255 ? note.substring(0, 255) : note;
        StateMovement movement = new StateMovement(
                entityType,
                entityId,
                safeLabel,
                previousStatus,
                newStatus,
                actor,
                safeNote
        );
        em.persist(movement);
        return movement;
    }

    @Transactional(readOnly = true)
    public List<StateMovement> list(Long viewer, MovementEntityType type, boolean onlyMine) {
        community.user(viewer);
        StringBuilder hql = new StringBuilder(
                "select m from StateMovement m join fetch m.actor a left join fetch a.profile " +
                "where m.deleted=false and a.deleted=false and a.enabled=true " +
                "and not exists (select b.id from UserBlock b where b.owner=a and b.target.id=:v)");
        if (type != null) {
            hql.append(" and m.entityType=:type");
        }
        if (onlyMine) {
            hql.append(" and a.id=:v");
        }
        hql.append(" order by m.createdAt desc, m.id desc");

        var query = em.createQuery(hql.toString(), StateMovement.class)
                .setParameter("v", viewer);
        if (type != null) {
            query.setParameter("type", type);
        }
        return query.setMaxResults(100).getResultList();
    }

    @Transactional(readOnly = true)
    public List<StateMovement> historyForEntity(MovementEntityType type, Long entityId) {
        return em.createQuery(
                "select m from StateMovement m join fetch m.actor a left join fetch a.profile " +
                "where m.deleted=false and m.entityType=:type and m.entityId=:id " +
                "order by m.createdAt asc, m.id asc", StateMovement.class)
                .setParameter("type", type)
                .setParameter("id", entityId)
                .getResultList();
    }
}
