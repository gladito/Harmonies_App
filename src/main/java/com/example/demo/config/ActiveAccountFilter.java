package com.example.demo.config;

import com.example.demo.Repositories.UserRepository;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** Keeps existing MVC session attributes consistent with the authenticated account. */
public class ActiveAccountFilter extends OncePerRequestFilter {
    private final UserRepository users;
    public ActiveAccountFilter(UserRepository users) { this.users=users; }
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
        var auth=SecurityContextHolder.getContext().getAuthentication();
        if(auth!=null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            var user=users.findByEmailAndDeletedFalse(auth.getName());
            if(user.isEmpty() || !user.get().isEnabled()) {
                SecurityContextHolder.clearContext();
                if(request.getSession(false)!=null)request.getSession(false).invalidate();
                response.sendRedirect(request.getContextPath()+"/login");return;
            }
            request.getSession().setAttribute("userId",user.get().getId());
            request.getSession().setAttribute("username",user.get().getUsername());
        }
        chain.doFilter(request,response);
    }
}
