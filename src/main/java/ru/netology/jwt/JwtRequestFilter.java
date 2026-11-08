package ru.netology.jwt;

import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.netology.service.UserService;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@Slf4j
public class JwtRequestFilter extends OncePerRequestFilter {

    private final UserService userService;
    private final JwtTokenUtil jwtTokenUtil;

    public JwtRequestFilter(UserService userService, JwtTokenUtil jwtTokenUtil) {
        this.userService = userService;
        this.jwtTokenUtil = jwtTokenUtil;
    }


    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {

        final String requestTokenHeader = request.getHeader("auth-token");

        if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {
            final String jwtToken = requestTokenHeader.substring(7);

            try {
                String username = jwtTokenUtil.getUsernameFromToken(jwtToken);

                if (username != null
                        && SecurityContextHolder.getContext().getAuthentication() == null) {

                    UserDetails userDetails = userService.loadUserByUsername(username);

                    if (jwtTokenUtil.validateToken(jwtToken, userDetails)) {
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails,
                                        null,
                                        userDetails.getAuthorities()
                                );

                        authentication.setDetails(
                                new WebAuthenticationDetailsSource().buildDetails(request)
                        );

                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            } catch (JwtException | IllegalArgumentException e) {
                log.warn("JWT authentication failed: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            } catch (UsernameNotFoundException e) {
                log.warn("JWT user not found");
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }

}