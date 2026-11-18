package com.example.demo.Models;

import jakarta.persistence.*;
import java.util.Objects;
import java.util.UUID;

/**
 * Segunda tabla para el almacenamiento particionado de contraseñas cifradas.
 * La primera mitad del hash BCrypt reside en users.password y la segunda mitad
 * reside en user_password_parts.password_fragment junto con la semilla key_salt.
 */
@Entity
@Table(name = "user_password_parts")
public class UserPasswordPart extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "password_fragment", nullable = false, length = 255)
    private String passwordFragment;

    @Column(name = "key_salt", nullable = false, length = 64)
    private String keySalt;

    public UserPasswordPart() {
    }

    public UserPasswordPart(User user, String passwordFragment) {
        this.user = Objects.requireNonNull(user, "user es obligatorio");
        this.passwordFragment = Objects.requireNonNull(passwordFragment, "passwordFragment es obligatorio");
        this.keySalt = UUID.randomUUID().toString().replace("-", "");
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getPasswordFragment() {
        return passwordFragment;
    }

    public void setPasswordFragment(String passwordFragment) {
        this.passwordFragment = Objects.requireNonNull(passwordFragment, "passwordFragment es obligatorio");
        this.keySalt = UUID.randomUUID().toString().replace("-", "");
    }

    public String getKeySalt() {
        return keySalt;
    }
}
