package com.luairan.security;

import org.springframework.context.annotation.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;

@Configuration
public class SecurityConfig {
    @Bean SecurityFilterChain security(HttpSecurity http, SecurityTokens tokens) throws Exception {
        return http.csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .requestCache(c -> c.disable())
            .authorizeHttpRequests(a -> a.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll().requestMatchers("/paxos").hasRole("NODE")
                .requestMatchers("/show/**", "/business/**").hasRole("ADMIN").anyRequest().denyAll())
            .exceptionHandling(e -> e.authenticationEntryPoint((req, res, error) -> {
                    res.setHeader("WWW-Authenticate", "Bearer"); res.sendError(401);
                })
                .accessDeniedHandler((req, res, error) -> res.sendError(403)))
            .addFilterBefore(new OncePerRequestFilter() {
                @Override protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
                        throws ServletException, IOException {
                    String role = tokens.role(req.getHeader("Authorization"));
                    if (role != null) SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(role, null, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
                    chain.doFilter(req, res);
                }
            }, UsernamePasswordAuthenticationFilter.class).build();
    }
}
