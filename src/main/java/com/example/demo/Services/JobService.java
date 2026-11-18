package com.example.demo.Services;

import com.example.demo.Models.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/** Bolsa de trabajo: avisos, postulaciones y decisión del autor. */
@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class JobService {

    @PersistenceContext
    private EntityManager em;
    private final CommunityService community;
    @Autowired
    private StateMovementService movementService;
    @Autowired
    private PaymentService paymentService;

    public JobService(CommunityService community) {
        this.community = community;
    }

    public List<JobPosting> list(Long viewer, JobType type) {
        community.user(viewer);
        var query = em.createQuery("select j from JobPosting j join fetch j.author a where j.deleted=false and j.open=true and a.deleted=false and a.enabled=true"
                + (type == null ? "" : " and j.type=:type")
                + " and not exists (select b.id from UserBlock b where b.owner=a and b.target.id=:v) order by j.createdAt desc, j.id desc", JobPosting.class)
                .setParameter("v", viewer);
        if (type != null) query.setParameter("type", type);
        return query.setMaxResults(100).getResultList();
    }

    public JobPosting job(Long id, Long viewer) {
        community.user(viewer);
        JobPosting j = id == null ? null : em.find(JobPosting.class, id);
        if (j == null || j.isDeleted() || j.getAuthor().isDeleted() || !community.canSeeUser(viewer, j.getAuthor().getId()))
            throw new IllegalArgumentException("Aviso no disponible");
        if (!j.isOpen() && !j.getAuthor().getId().equals(viewer) && myApplication(id, viewer).isEmpty())
            throw new IllegalArgumentException("Este aviso ya está cerrado");
        return j;
    }

    public JobPosting create(Long actor, String title, JobType type, String description, String instrument, String location, boolean remote, String pay) {
        User author = community.user(actor);
        title = StudioService.required(title, 120, "Indicá un título de hasta 120 caracteres");
        location = StudioService.required(location, 120, "Indicá una ubicación de hasta 120 caracteres");
        pay = StudioService.required(pay, 60, "Indicá la remuneración (hasta 60 caracteres), por ejemplo \"A convenir\"");
        description = StudioService.optional(description, 1000, "La descripción admite hasta 1000 caracteres");
        instrument = StudioService.optional(instrument, 80, "El instrumento o rol admite hasta 80 caracteres");
        if (type == null) throw new IllegalArgumentException("Elegí el tipo de trabajo");
        JobPosting job = new JobPosting(title, description, type, instrument, location, remote, pay, author);
        em.persist(job);
        em.flush();
        if (movementService != null) {
            movementService.record(
                    MovementEntityType.JOB_POSTING,
                    job.getId(),
                    job.getTitle(),
                    null,
                    "OPEN",
                    author,
                    "Aviso publicado en la bolsa de trabajo"
            );
        }
        return job;
    }

    public JobApplication apply(Long actor, Long jobId, String message) {
        User applicant = community.user(actor);
        JobPosting job = lock(jobId);
        job(jobId, actor);
        if (job.getAuthor().getId().equals(actor)) throw new IllegalArgumentException("No podés postularte a tu propio aviso");
        if (!job.isOpen()) throw new IllegalArgumentException("Este aviso ya está cerrado");
        if (community.blocked(actor, job.getAuthor().getId())) throw new IllegalArgumentException("Aviso no disponible");
        if (myApplication(jobId, actor).isPresent()) throw new IllegalArgumentException("Ya te postulaste a este aviso");
        message = StudioService.optional(message, 500, "El mensaje admite hasta 500 caracteres");
        JobApplication application = new JobApplication(job, applicant, message);
        em.persist(application);
        em.flush();
        if (movementService != null) {
            String note = (message != null && !message.isBlank())
                    ? "Postulación enviada: " + message
                    : "Postulación enviada";
            movementService.record(
                    MovementEntityType.JOB_APPLICATION,
                    application.getId(),
                    applicant.getUsername() + " → " + job.getTitle(),
                    null,
                    application.getStatus().name(),
                    applicant,
                    note
            );
        }
        return application;
    }

    /** El autor del aviso acepta o rechaza una postulación pendiente. */
    public void decide(Long actor, Long applicationId, boolean accept) {
        User author = community.user(actor);
        JobApplication a = applicationId == null ? null : em.find(JobApplication.class, applicationId);
        if (a == null) throw new IllegalArgumentException("Postulación no disponible");
        lock(a.getJob().getId());
        if (!a.getJob().getAuthor().getId().equals(actor)) throw new IllegalArgumentException("Solo el autor del aviso puede responder postulaciones");
        if (a.getStatus() != ApplicationStatus.PENDING) throw new IllegalArgumentException("Esta postulación ya fue respondida");
        String prev = a.getStatus().name();
        a.setStatus(accept ? ApplicationStatus.ACCEPTED : ApplicationStatus.REJECTED);
        if (movementService != null) {
            movementService.record(
                    MovementEntityType.JOB_APPLICATION,
                    a.getId(),
                    a.getApplicant().getUsername() + " → " + a.getJob().getTitle(),
                    prev,
                    a.getStatus().name(),
                    author,
                    accept ? "Postulación aceptada por el autor del aviso" : "Postulación rechazada por el autor del aviso"
            );
        }
        if (accept && paymentService != null) {
            int estimatedAmount = PaymentService.parseAmountFromPayText(a.getJob().getPay());
            paymentService.createOrder(
                    PaymentConceptType.JOB_CONTRACT,
                    a.getId(),
                    "Contratación: " + a.getJob().getTitle() + " (" + a.getApplicant().getUsername() + ")",
                    estimatedAmount,
                    PaymentMethod.MERCADO_PAGO,
                    false,
                    author,
                    a.getApplicant()
            );
        }
    }

    /** Permite al autor del aviso cancelar una contratación ya aceptada (anula el pago pendiente o lo reembolsa). */
    public void cancelAcceptance(Long actor, Long applicationId) {
        User author = community.user(actor);
        JobApplication a = applicationId == null ? null : em.find(JobApplication.class, applicationId);
        if (a == null) throw new IllegalArgumentException("Postulación no disponible");
        lock(a.getJob().getId());
        if (!a.getJob().getAuthor().getId().equals(actor)) throw new IllegalArgumentException("Solo el autor del aviso puede cancelar la contratación");
        if (a.getStatus() != ApplicationStatus.ACCEPTED) throw new IllegalArgumentException("Solo podés cancelar una postulación aceptada");

        String prev = a.getStatus().name();
        a.setStatus(ApplicationStatus.REJECTED);
        if (movementService != null) {
            movementService.record(
                    MovementEntityType.JOB_APPLICATION,
                    a.getId(),
                    a.getApplicant().getUsername() + " → " + a.getJob().getTitle(),
                    prev,
                    a.getStatus().name(),
                    author,
                    "Contratación cancelada por el autor del aviso"
            );
        }
        if (paymentService != null) {
            paymentService.refundOrCancelForReference(
                    PaymentConceptType.JOB_CONTRACT,
                    a.getId(),
                    author,
                    "Cancelación de contratación"
            );
        }
    }

    public void setOpen(Long actor, Long jobId, boolean open) {
        User author = community.user(actor);
        JobPosting job = lock(jobId);
        if (!job.getAuthor().getId().equals(actor)) throw new IllegalArgumentException("Solo el autor puede cerrar o reabrir el aviso");
        boolean wasOpen = job.isOpen();
        job.setOpen(open);
        if (wasOpen != open && movementService != null) {
            movementService.record(
                    MovementEntityType.JOB_POSTING,
                    job.getId(),
                    job.getTitle(),
                    wasOpen ? "OPEN" : "CLOSED",
                    open ? "OPEN" : "CLOSED",
                    author,
                    open ? "Aviso reabierto por el autor" : "Aviso cerrado por el autor"
            );
        }
    }

    public List<JobApplication> applications(Long jobId, Long actor) {
        JobPosting job = job(jobId, actor);
        if (!job.getAuthor().getId().equals(actor)) return List.of();
        return em.createQuery("select a from JobApplication a join fetch a.applicant where a.job.id=:j order by a.createdAt", JobApplication.class)
                .setParameter("j", jobId).getResultList();
    }

    public Optional<JobApplication> myApplication(Long jobId, Long actor) {
        return em.createQuery("from JobApplication a where a.job.id=:j and a.applicant.id=:u", JobApplication.class)
                .setParameter("j", jobId).setParameter("u", actor).getResultStream().findFirst();
    }

    public List<JobApplication> myApplications(Long actor) {
        return em.createQuery("select a from JobApplication a join fetch a.job j where a.applicant.id=:u and j.deleted=false order by a.createdAt desc", JobApplication.class)
                .setParameter("u", actor).setMaxResults(50).getResultList();
    }

    public List<JobPosting> myPostings(Long actor) {
        return em.createQuery("from JobPosting j where j.author.id=:u and j.deleted=false order by j.createdAt desc", JobPosting.class)
                .setParameter("u", actor).setMaxResults(50).getResultList();
    }

    private JobPosting lock(Long id) {
        JobPosting j = id == null ? null : em.find(JobPosting.class, id);
        if (j == null || j.isDeleted()) throw new IllegalArgumentException("Aviso no disponible");
        em.lock(j, LockModeType.PESSIMISTIC_WRITE);
        return j;
    }
}
