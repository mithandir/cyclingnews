package ch.climbd.newsfeed.scheduler;

import ch.climbd.newsfeed.controller.MongoController;
import ch.climbd.newsfeed.data.NewsEntry;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

@Component
public class RssProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(RssProcessor.class);

    @Autowired
    private MongoController mongo;

    @Autowired
    private Filter filter;

    public void processRss(String url, String language) {
        try {
            SyndFeed feed = new SyndFeedInput().build(new XmlReader(new URL(url)));
            feed.getEntries().stream().map(this::map)
                    .filter(item -> item.getLink() != null && item.getTitle() != null)
                    .filter(item -> !item.getLink().isBlank() && !item.getTitle().isBlank())
                    .filter(item -> !filter.isSpam(item.getTitle()))
                    .filter(item -> !mongo.exists(item))
                    .forEach(item -> {
                        item.setLanguage(language);
                        mongo.save(item);
                    });

        } catch (Exception e) {
            LOG.error("Error reading RSS feed: " + url);
        }
    }

    private NewsEntry map(SyndEntry item) {
        NewsEntry result = new NewsEntry();
        String title = item.getTitle().strip();
        if (title == null) {
            result.setTitle(null);
        } else {
            result.setTitle(filter.replaceHtml(title));
        }

        result.setLink(item.getLink().strip());

        if (Date.from(Instant.now()).equals(item.getPublishedDate())) {
            result.setPublishedAt(ZonedDateTime.now());
        } else {
            result.setPublishedAt(ZonedDateTime.ofInstant(item.getPublishedDate().toInstant(), ZoneId.systemDefault()));
        }

        return result;
    }
}
