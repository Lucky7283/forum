package az.animeazerbaycan.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.*;
import org.springframework.http.HttpMethod;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.util.List;

import az.animeazerbaycan.security.*;
import az.animeazerbaycan.repository.UserRepository;

@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
public class SecurityConfig {
        private final JwtFilter jwtFilter;

        @Bean
        PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        SecurityFilterChain security(HttpSecurity http, JwtService jwt, UserRepository users,
                        @Value("${app.client-origin}") String origin) throws Exception {
                CorsConfiguration cors = new CorsConfiguration();
                cors.setAllowedOriginPatterns(List.of(origin.split(",")));
                cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                cors.setAllowedHeaders(List.of("Content-Type"));
                cors.setAllowCredentials(true);
                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", cors);

                return http.cors(c -> c.configurationSource(source)).csrf(c -> c.disable())
                                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .formLogin(f -> f.disable()).httpBasic(b -> b.disable()).logout(l -> l.disable())
                                .authorizeHttpRequests(a -> a.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                                                .requestMatchers(HttpMethod.POST, "/api/v1/auth/register",
                                                                "/api/v1/auth/login")
                                                .permitAll()
                                                .requestMatchers("/api/v1/users/me", "/api/v1/users/me/**",
                                                                "/api/v1/auth/me",
                                                                "/api/v1/auth/logout")
                                                .authenticated()
                                                .requestMatchers(HttpMethod.GET, "/api/v1/anime", "/api/v1/anime/*",
                                                                "/api/v1/anime/*/comments",
                                                                "/api/v1/comments/*/replies",
                                                                "/api/v1/genres", "/api/v1/users/*", "/v3/api-docs/**",
                                                                "/swagger-ui/**",
                                                                "/swagger-ui.html")
                                                .permitAll()
                                                .anyRequest().authenticated())
                                .exceptionHandling(
                                                e -> e.authenticationEntryPoint((req, res, ex) -> json(res, 401,
                                                                "Authentication required"))
                                                                .accessDeniedHandler((req, res, ex) -> json(res, 403,
                                                                                "Access denied")))
                                .addFilterBefore(new JwtFilter(jwt, users), UsernamePasswordAuthenticationFilter.class)
                                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class).build();
        }

        private static void json(HttpServletResponse response, int status, String message) throws IOException {
                response.setStatus(status);
                response.setContentType("application/json");
                response.getWriter().write("{\"status\":" + status + ",\"message\":\"" + message + "\",\"errors\":{}}");
        }
}
