package com.example.demo.Models;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Aviso de la bolsa de trabajo: búsqueda de músicos, técnicos o docentes. */
@Entity
@Table(name = "job_postings", indexes = @Index(name = "ix_jobs_open", columnList = "open,deleted,created_at"))
public class JobPosting extends BaseEntity {

    @Column(nullable = false, length = 120)
    private String title;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private JobType type;

    @Column(length = 80)
    private String instrument;

    @Column(nullable = false, length = 120)
    private String location;

    @Column(nullable = false)
    private boolean remote;

    /** Remuneración en texto libre ("$ 25.000 por sesión", "A convenir"). */
    @Column(nullable = false, length = 60)
    private String pay;

    @Column(nullable = false)
    private boolean open = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id")
    private User author;

    protected JobPosting() {
    }

    public JobPosting(String title, String description, JobType type, String instrument, String location, boolean remote, String pay, User author) {
        this.title = title;
        this.description = description;
        this.type = type;
        this.instrument = instrument;
        this.location = location;
        this.remote = remote;
        this.pay = pay;
        this.author = author;
    }

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public JobType getType() { return type; }
    public String getInstrument() { return instrument; }
    public String getLocation() { return location; }
    public boolean isRemote() { return remote; }
    public String getPay() { return pay; }
    public boolean isOpen() { return open; }
    public void setOpen(boolean open) { this.open = open; }
    public User getAuthor() { return author; }
}
