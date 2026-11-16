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
            .map(u -> org.springframework.security.core.userdetails.User.withUsername(u.getEmail())
                .password(u.getPassword()).roles("USER").disabled(!u.isEnabled()).build())
            .orElseThrow(() -> new UsernameNotFoundException("Credenciales inválidas"));
    }
    @Bean public SecurityFilterChain securityFilterChain(HttpSecurity http, UserRepository users) throws Exception {
        http.authorizeHttpRequests(a -> a.requestMatchers("/", "/login", "/register", "/css/**", "/js/**", "/images/**", "/avatars/**", "/error").permitAll().anyRequest().authenticated())
            .formLogin(f -> f.loginPage("/login").usernameParameter("email").failureUrl("/login?error")
                .successHandler((req,res,auth) -> {
                    var u = users.findByEmailAndDeletedFalse(auth.getName()).orElseThrow();
                    req.getSession().setAttribute("userId",u.getId());
                    req.getSession().setAttribute("username",u.getUsername());
                    res.sendRedirect(req.getContextPath()+"/home");
                }))
            .logout(l -> l.logoutSuccessUrl("/login?logout"))
            .sessionManagement(s -> s.sessionFixation(f -> f.changeSessionId()))
            .addFilterAfter(new ActiveAccountFilter(users),org.springframework.security.web.authentication.AnonymousAuthenticationFilter.class);
        return http.build();
    }
}
