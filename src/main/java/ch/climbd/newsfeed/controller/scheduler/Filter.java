package ch.climbd.newsfeed.controller.scheduler;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class Filter {
    private final Set<String> spam = Set.of("deal", "&#");

    private final Map<String, String> replace = Map.of(
            "&quot;", "\"",
            "&#039;", "'");

    public boolean isSpam(String title) {
        return spam.stream().anyMatch(title.toLowerCase()::contains);
    }

    public String replaceHtml(String title) {
        for (Map.Entry<String, String> entry : replace.entrySet()) {
            title = title.replace(entry.getKey(), entry.getValue());
        }

        return title;
    }
}
