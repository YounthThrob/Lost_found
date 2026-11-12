package com.lostfound.security;

import com.lostfound.account.InMemoryUserStore;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final InMemoryUserStore userStore;

    public JwtAuthFilter(JwtService jwtService, InMemoryUserStore userStore) {
        this.jwtService = jwtService;
        this.userStore = userStore;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                String subject = jwtService.parseSubject(header.substring(7));
                // 会话有效性：除 JWT 签名与有效期外，还需确认用户仍存在；否则不建立认证，
                // 后续由 Spring Security 的 AuthenticationEntryPoint 返回 401 SESSION_EXPIRED。
                if (userStore.findById(subject) != null) {
                    var authentication = new UsernamePasswordAuthenticationToken(subject, null,
                        AuthorityUtils.NO_AUTHORITIES);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } else {
                    SecurityContextHolder.clearContext();
                }
            } catch (JwtException | IllegalArgumentException e) {
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }
}
