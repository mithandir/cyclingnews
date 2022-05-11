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
        for (String entry : spam) {
            if (title.toLowerCase().contains(entry)) {
                return true;
            }
        }

        return false;
    }

    public String replaceHtml(String title) {
        for (String search : replace.keySet()) {
            title = title.replaceAll(search, replace.get(search));
        }

        return title;
    }
}
