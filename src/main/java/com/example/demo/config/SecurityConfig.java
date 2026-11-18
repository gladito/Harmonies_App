package com.example.demo.config;
import com.example.demo.Repositories.UserRepository;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
@Configuration
public class SecurityConfig {
    @Bean public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    @Bean public UserDetailsService userDetailsService(UserRepository users) {
        return email -> users.findByEmailAndDeletedFalse(email)
            .map(u -> {
                boolean isAdmin = "gladito".equalsIgnoreCase(u.getUsername())
                    || u.getRoles().stream().anyMatch(r -> "ADMIN".equalsIgnoreCase(r.getName()) || "ROLE_ADMIN".equalsIgnoreCase(r.getName()));
                return org.springframework.security.core.userdetails.User.withUsername(u.getEmail())
                    .password(u.getPassword()).roles(isAdmin ? new String[]{"USER", "ADMIN"} : new String[]{"USER"}).disabled(!u.isEnabled()).build();
            })
            .orElseThrow(() -> new UsernameNotFoundException("Credenciales inválidas"));
    }
    @Bean public SecurityFilterChain securityFilterChain(HttpSecurity http, UserRepository users) throws Exception {
        http.authorizeHttpRequests(a -> a.requestMatchers("/", "/login", "/register", "/forgot-password", "/forgot-password/**", "/css/**", "/js/**", "/images/**", "/avatars/**", "/error").permitAll().anyRequest().authenticated())
            .formLogin(f -> f.loginPage("/login").usernameParameter("email").failureUrl("/login?error")
                .successHandler((req,res,auth) -> {
                    var u = users.findByEmailAndDeletedFalse(auth.getName()).orElseThrow();
                    boolean isAdmin = "gladito".equalsIgnoreCase(u.getUsername())
                        || u.getRoles().stream().anyMatch(r -> "ADMIN".equalsIgnoreCase(r.getName()) || "ROLE_ADMIN".equalsIgnoreCase(r.getName()));
                    req.getSession().setAttribute("userId",u.getId());
                    req.getSession().setAttribute("username",u.getUsername());
                    req.getSession().setAttribute("isAdmin",isAdmin);
                    res.sendRedirect(req.getContextPath()+"/home");
                }))
            .logout(l -> l.logoutSuccessUrl("/login?logout"))
            .sessionManagement(s -> s.sessionFixation(f -> f.changeSessionId()))
            .addFilterAfter(new ActiveAccountFilter(users),org.springframework.security.web.authentication.AnonymousAuthenticationFilter.class);
        return http.build();
    }
}
