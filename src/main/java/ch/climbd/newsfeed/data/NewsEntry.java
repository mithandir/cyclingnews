package ch.climbd.newsfeed.data;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

@Document
public class NewsEntry {

    @Id
    private String link;

    private String title;
    private String content = "";
    private String summary = "";
    private LocalDateTime publishedAt;
    private Integer votes = 0;

    private Integer views = 0;
    private String language = "undefined";

    public NewsEntry() {
    }

    public NewsEntry(String title, String link, ZonedDateTime publishedAt) {
        this.setTitle(title);
        this.setLink(link);
        this.setPublishedAt(publishedAt);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public ZonedDateTime getPublishedDateTime() {
        ZoneId zId = ZoneId.of("Europe/Berlin");

        if (publishedAt.isAfter(LocalDateTime.now())) {
            return ZonedDateTime.now().minusSeconds(1);
        }

        return ZonedDateTime.ofInstant(publishedAt.toInstant(ZoneOffset.UTC), zId);
    }

    public void setPublishedAt(ZonedDateTime publishedAt) {
        ZoneId zId = ZoneId.of("Europe/Berlin");
        LocalDateTime itemDateTime = LocalDateTime.ofInstant(publishedAt.toInstant(), zId);

        if (itemDateTime.isAfter(LocalDateTime.now())) {
            this.publishedAt = LocalDateTime.now().minusSeconds(10);
        } else {
            this.publishedAt = itemDateTime;
        }
    }

    public Integer getVotes() {
        return votes;
    }

    public void setVotes(Integer votes) {
        this.votes = votes;
    }

    public Integer getViews() {
        return views;
    }

    public void setViews(Integer views) {
        this.views = views;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getDomainWithProtocol() {
        if (!link.isEmpty()) {
            var start = link.indexOf("://") + 3;

            try {
                var end = link.indexOf("/", start);
                return link.substring(0, end);
            } catch (IndexOutOfBoundsException e) {
                return link;
            }

        }
        return null;
    }

    public String getDomainOnly() {
        if (!link.isEmpty()) {
            var start = link.indexOf("://") + 3;

            try {
                var end = link.indexOf("/", start);
                return link.substring(start, end).replace("www.", "");
            } catch (IndexOutOfBoundsException e) {
                return link.substring(start).replace("www.", "");
            }

        }
        return "";
    }

    @Override
    public String toString() {
        return String.format(
                "NewsEntry[link=%s, title='%s', votes='%s']",
                link, title, votes);
    }
}
