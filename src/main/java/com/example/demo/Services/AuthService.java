package com.example.demo.Services;

import com.example.demo.Dtos.Auth.AuthLoginRequestDto;
import com.example.demo.Dtos.Auth.AuthRegisterRequestDto;
import com.example.demo.Models.User;
import com.example.demo.Repositories.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Objects;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(AuthRegisterRequestDto registerRequest) {
        validateRegisterRequest(registerRequest);

        if (userRepository.existsByEmailAndDeletedFalse(registerRequest.getEmail())) {
            throw new IllegalArgumentException("El email ya está registrado");
        }

        if (userRepository.existsByUsernameAndDeletedFalse(registerRequest.getUsername())) {
            throw new IllegalArgumentException("El nombre de usuario ya está en uso");
        }

        validatePasswordPolicy(registerRequest.getPassword());
        String encodedPassword = passwordEncoder.encode(registerRequest.getPassword());

        User newUser = new User(
                registerRequest.getFirstName(),
                registerRequest.getLastName(),
                registerRequest.getBirthDate(),
                registerRequest.getUsername(),
                registerRequest.getEmail(),
                encodedPassword
        );

        return userRepository.save(newUser);
    }

    @Transactional(readOnly = true)
    public User authenticate(AuthLoginRequestDto loginRequest) {
        if (loginRequest.getEmail() == null || loginRequest.getEmail().isBlank()) {
            throw new IllegalArgumentException("Debes ingresar un email");
        }
        if (loginRequest.getPassword() == null || loginRequest.getPassword().isBlank()) {
            throw new IllegalArgumentException("Debes ingresar una contraseña");
        }

        User user = userRepository.findByEmailAndDeletedFalse(loginRequest.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Las credenciales no son válidas"));

        if (!user.isEnabled()) {
            throw new IllegalStateException("La cuenta está deshabilitada");
        }

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Las credenciales no son válidas");
        }

        return user;
    }

    @Transactional
    public User updateProfile(Long userId, String newUsername) {
        User user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el usuario."));

        if (isBlank(newUsername) || newUsername.length() > 40) {
            throw new IllegalArgumentException("El nombre de usuario no puede estar vacío.");
        }
        if (!newUsername.equals(user.getUsername())
                && userRepository.existsByUsernameAndDeletedFalse(newUsername)) {
            throw new IllegalArgumentException("Ese nombre de usuario ya está en uso.");
        }

        user.setUsername(newUsername);
        return userRepository.save(user);
    }

    private void validateRegisterRequest(AuthRegisterRequestDto registerRequest) {
        Objects.requireNonNull(registerRequest, "La solicitud de registro es obligatoria");

        if (isBlank(registerRequest.getFirstName()) || registerRequest.getFirstName().length() > 80) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
        if (isBlank(registerRequest.getLastName()) || registerRequest.getLastName().length() > 80) {
            throw new IllegalArgumentException("El apellido es obligatorio");
        }
        LocalDate birthDate = registerRequest.getBirthDate();
        if (birthDate == null || birthDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de nacimiento es obligatoria");
        }
        if (isBlank(registerRequest.getUsername()) || registerRequest.getUsername().length() > 40) {
            throw new IllegalArgumentException("El nombre de usuario es obligatorio");
        }
        if (isBlank(registerRequest.getEmail()) || !registerRequest.getEmail().matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+") || registerRequest.getEmail().length() > 254) {
            throw new IllegalArgumentException("El email es obligatorio");
        }
        if (isBlank(registerRequest.getPassword())) {
            throw new IllegalArgumentException("La contraseña es obligatoria");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    @Transactional
    public void updatePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el usuario."));

        if (currentPassword == null || currentPassword.isBlank()) {
            throw new IllegalArgumentException("Debes ingresar tu contraseña actual.");
        }
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("La contraseña actual no es correcta.");
        }
        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("Debes ingresar la nueva contraseña.");
        }

        validatePasswordPolicy(newPassword);

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public java.util.Optional<User> getUserById(Long userId) {
        return userRepository.findByIdAndDeletedFalse(userId);
    }

    @Transactional(readOnly = true)
    public User verifyEmailForPasswordReset(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Debes ingresar el email asociado a tu cuenta.");
        }
        User user = userRepository.findByEmailAndDeletedFalse(email.trim())
                .orElseThrow(() -> new IllegalArgumentException("No encontramos ninguna cuenta asociada a ese email."));
        if (!user.isEnabled()) {
            throw new IllegalStateException("La cuenta asociada a ese email está deshabilitada.");
        }
        return user;
    }

    @Transactional
    public User resetPasswordByVerifiedEmail(String verifiedEmail, String newPassword, String confirmPassword) {
        if (verifiedEmail == null || verifiedEmail.isBlank()) {
            throw new IllegalStateException("Primero debes verificar el email de tu cuenta.");
        }
        User user = verifyEmailForPasswordReset(verifiedEmail);
        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("Debes ingresar la nueva contraseña.");
        }
        if (confirmPassword == null || !newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("Las contraseñas ingresadas no coinciden.");
        }
        validatePasswordPolicy(newPassword);
        user.setPassword(passwordEncoder.encode(newPassword));
        return userRepository.save(user);
    }

    private void validatePasswordPolicy(String newPassword) {
        boolean hasUppercase = newPassword.chars().anyMatch(Character::isUpperCase);
        boolean hasDigit = newPassword.chars().anyMatch(Character::isDigit);
        if (newPassword.length() < 8 || newPassword.length() > 72 || !hasUppercase || !hasDigit) {
            throw new IllegalArgumentException("La contraseña debe tener de 8 a 72 caracteres, una mayúscula y un número.");
        }
    }
}

