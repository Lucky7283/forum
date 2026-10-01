package az.animeazerbaycan.security;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import az.animeazerbaycan.repository.UserRepository;
import jakarta.servlet.*;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.util.List;
@Component
public class JwtFilter extends OncePerRequestFilter {
    private final JwtService jwt;
    private final UserRepository users;

    public JwtFilter(JwtService jwt, UserRepository users) {
        this.jwt = jwt;
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        if (request.getCookies() != null) for (Cookie c : request.getCookies())
            if (c.getName().equals("auth")) {
                Long id = null;
                try {
                    id = jwt.userId(c.getValue());
                } catch (org.springframework.security.oauth2.jwt.JwtException | IllegalArgumentException ignored) {
                }
                if (id != null)
                    users.findById(id).ifPresent(u -> SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(u.getId(), null, List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole())))));
                break;
            }
        chain.doFilter(request, response);
    }
}
