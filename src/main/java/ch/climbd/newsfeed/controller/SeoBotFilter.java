package ch.climbd.newsfeed.controller;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Pattern;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SeoBotFilter extends OncePerRequestFilter {

    private static final Pattern BOT_PATTERN = Pattern.compile(
            "bot|crawler|spider|slurp|facebookexternalhit|bingpreview|pinterest|discordbot|twitterbot|linkedinbot|embedly|"
                    + "quora link preview|slackbot|telegrambot|whatsapp|googlebot|bingbot|yandex|duckduckbot|baiduspider",
            Pattern.CASE_INSENSITIVE
    );

    private static final Map<String, String> SEO_ROUTES = Map.of(
            "/", "/seo",
            "/views", "/seo/views",
            "/liked", "/seo/liked"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = getPath(request);

        if (shouldForward(request, path)) {
            String target = SEO_ROUTES.get(path);
            if (target != null) {
                request.getRequestDispatcher(target).forward(request, response);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean shouldForward(HttpServletRequest request, String path) {
        if (path == null || path.isBlank()) {
            return false;
        }
        if (path.startsWith("/seo") || path.startsWith("/VAADIN") || path.startsWith("/frontend")) {
            return false;
        }
        String userAgent = request.getHeader("User-Agent");
        return userAgent != null && BOT_PATTERN.matcher(userAgent).find() && SEO_ROUTES.containsKey(path);
    }

    private String getPath(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        String uri = request.getRequestURI();
        if (uri != null && uri.length() > 1 && uri.endsWith("/")) {
            uri = uri.substring(0, uri.length() - 1);
        }
        if (contextPath == null || contextPath.isBlank()) {
            return uri;
        }
        if (uri.startsWith(contextPath)) {
            return uri.substring(contextPath.length());
        }
        return uri;
    }
}
