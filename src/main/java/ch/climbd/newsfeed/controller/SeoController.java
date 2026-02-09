package ch.climbd.newsfeed.controller;

import ch.climbd.newsfeed.data.NewsEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

@Controller
public class SeoController {

    private static final int SUMMARY_LIMIT = 220;
    private static final Set<String> DEFAULT_LANGUAGES = Set.of("en", "de");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    @Autowired
    private MongoController mongo;

    @Value("${baseurl:https://news.qfotografie.de}")
    private String baseUrl;

    @GetMapping(value = "/seo", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String latest() {
        return renderPage(
                "Climbd Cycling News - Latest News",
                "Latest cycling news curated by Climbd.",
                baseUrl + "/seo",
                mongo.findLast100PostsPostedInTheLast48h()
        );
    }

    @GetMapping(value = "/seo/views", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String mostViewed() {
        return renderPage(
                "Climbd Cycling News - Most Viewed",
                "Most viewed cycling news curated by Climbd.",
                baseUrl + "/seo/views",
                mongo.findAllOrderedByViews(DEFAULT_LANGUAGES)
        );
    }

    @GetMapping(value = "/seo/liked", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String mostLiked() {
        return renderPage(
                "Climbd Cycling News - Most Liked",
                "Most liked cycling news curated by Climbd.",
                baseUrl + "/seo/liked",
                mongo.findAllOrderedByVotes(DEFAULT_LANGUAGES)
        );
    }

    private String renderPage(String title, String description, String canonicalUrl, List<NewsEntry> entries) {
        StringBuilder html = new StringBuilder(16384);
        html.append("<!doctype html>");
        html.append("<html lang=\"en\">");
        html.append("<head>");
        html.append("<meta charset=\"utf-8\">");
        html.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">");
        html.append("<title>").append(escape(title)).append("</title>");
        html.append("<meta name=\"description\" content=\"").append(escape(description)).append("\">");
        html.append("<meta name=\"robots\" content=\"index, follow\">");
        html.append("<link rel=\"canonical\" href=\"").append(escape(canonicalUrl)).append("\">");
        html.append("<link rel=\"alternate\" type=\"application/rss+xml\" title=\"Climbd Cycling News Feed\" href=\"")
                .append(escape(baseUrl)).append("/feed\">");
        html.append("<meta property=\"og:title\" content=\"").append(escape(title)).append("\">");
        html.append("<meta property=\"og:description\" content=\"").append(escape(description)).append("\">");
        html.append("<meta property=\"og:type\" content=\"website\">");
        html.append("<meta property=\"og:url\" content=\"").append(escape(canonicalUrl)).append("\">");
        html.append("<style>");
        html.append("body{font-family:Arial,sans-serif;margin:0;background:#f7f7f5;color:#1b1b1b;line-height:1.6}");
        html.append("header{background:#0f172a;color:#fff;padding:24px 20px}");
        html.append("header a{color:#fff;text-decoration:none}");
        html.append("main{max-width:960px;margin:0 auto;padding:20px}");
        html.append("nav a{margin-right:16px;color:#0f172a;text-decoration:none;font-weight:bold}");
        html.append("article{border-bottom:1px solid #ddd;padding:16px 0}");
        html.append("h1{margin:0 0 8px;font-size:28px}");
        html.append("h2{margin:0 0 8px;font-size:20px}");
        html.append(".meta{font-size:12px;color:#4b5563}");
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");
        html.append("<header>");
        html.append("<h1><a href=\"").append(escape(baseUrl)).append("\">Climbd Cycling News</a></h1>");
        html.append("<div>").append(escape(description)).append("</div>");
        html.append("</header>");
        html.append("<main>");
        html.append("<nav>");
        html.append("<a href=\"").append(escape(baseUrl)).append("/seo\">Latest</a>");
        html.append("<a href=\"").append(escape(baseUrl)).append("/seo/views\">Most views</a>");
        html.append("<a href=\"").append(escape(baseUrl)).append("/seo/liked\">Most liked</a>");
        html.append("</nav>");
        html.append("<section>");

        if (entries == null || entries.isEmpty()) {
            html.append("<p>No entries available.</p>");
        } else {
            for (NewsEntry entry : entries) {
                appendEntry(html, entry);
            }
        }

        html.append("</section>");
        html.append("</main>");
        html.append("</body>");
        html.append("</html>");
        return html.toString();
    }

    private void appendEntry(StringBuilder html, NewsEntry entry) {
        String title = entry.getTitle() == null ? "" : entry.getTitle();
        String link = entry.getLink() == null ? "" : entry.getLink();
        String summary = entry.getSummary();
        String content = entry.getContent();
        String excerptSource = (summary == null || summary.isBlank()) ? content : summary;
        String excerpt = excerptSource == null ? "" : trimToLimit(excerptSource, SUMMARY_LIMIT);
        ZonedDateTime published = entry.getPublishedDateTime();

        html.append("<article>");
        html.append("<h2><a href=\"").append(escape(link)).append("\">").append(escape(title)).append("</a></h2>");
        if (!excerpt.isBlank()) {
            html.append("<p>").append(escape(excerpt)).append("</p>");
        }
        html.append("<div class=\"meta\">");
        if (published != null) {
            html.append("<time datetime=\"").append(escape(DATETIME_FORMAT.format(published))).append("\">")
                    .append(escape(DATE_FORMAT.format(published))).append("</time>");
        }
        String domain = entry.getDomainOnly();
        if (domain != null && !domain.isBlank()) {
            html.append(" • ").append(escape(domain));
        }
        if (entry.getLanguage() != null && !entry.getLanguage().isBlank()) {
            html.append(" • ").append(escape(entry.getLanguage()));
        }
        html.append("</div>");
        html.append("</article>");
    }

    private String trimToLimit(String value, int limit) {
        String clean = value.replaceAll("\\s+", " ").strip();
        if (clean.length() <= limit) {
            return clean;
        }
        return clean.substring(0, limit - 1) + "...";
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
