package com.example.demo.Models;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Sala de ensayo, estudio de grabación o cabina publicada por un usuario. */
@Entity
@Table(name = "studios")
public class Studio extends BaseEntity {

    @Column(nullable = false, length = 80)
    private String name;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private StudioType type;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, length = 120)
    private String location;

    @Column(length = 255)
    private String amenities;

    /** Precio por hora en pesos, sin decimales. */
    @org.hibernate.annotations.Check(constraints = "hourly_rate > 0")
    @Column(name = "hourly_rate", nullable = false)
    private int hourlyRate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id")
    private User owner;

    protected Studio() {
    }

    public Studio(String name, StudioType type, String description, String location, String amenities, int hourlyRate, User owner) {
        this.name = name;
        this.type = type;
        this.description = description;
        this.location = location;
        this.amenities = amenities;
        this.hourlyRate = hourlyRate;
        this.owner = owner;
    }

    public String getName() { return name; }
    public StudioType getType() { return type; }
    public String getDescription() { return description; }
    public String getLocation() { return location; }
    public String getAmenities() { return amenities; }
    public int getHourlyRate() { return hourlyRate; }
    public User getOwner() { return owner; }
}
