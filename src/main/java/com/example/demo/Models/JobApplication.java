package com.example.demo.Models;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Postulación de un músico a un aviso. Una por aviso y usuario. */
@Entity
@Table(name = "job_applications", uniqueConstraints = @UniqueConstraint(columnNames = {"job_id", "applicant_id"}))
public class JobApplication extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id")
    private JobPosting job;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "applicant_id")
    private User applicant;

    @Column(length = 500)
    private String message;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 15)
    private ApplicationStatus status = ApplicationStatus.PENDING;

    protected JobApplication() {
    }

    public JobApplication(JobPosting job, User applicant, String message) {
        this.job = job;
        this.applicant = applicant;
        this.message = message;
    }

    public JobPosting getJob() { return job; }
    public User getApplicant() { return applicant; }
    public String getMessage() { return message; }
    public ApplicationStatus getStatus() { return status; }
    public void setStatus(ApplicationStatus status) { this.status = status; }
}
