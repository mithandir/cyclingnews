package ch.climbd.newsfeed.scheduler;

import ch.climbd.newsfeed.controller.MongoController;
import ch.climbd.newsfeed.data.NewsEntry;
import com.apptastic.rssreader.Item;
import com.apptastic.rssreader.RssReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.stream.Stream;

@Component
public class RssProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(RssProcessor.class);

    @Autowired
    private MongoController mongo;

    @Autowired
    private Filter filter;

    public void processRss(String url, String language) {
        RssReader reader = new RssReader();
        Stream<Item> rssFeed;
        try {
            rssFeed = reader.read(url);
            rssFeed.map(this::map)
                    .filter(item -> item.getLink() != null && item.getTitle() != null)
                    .filter(item -> !filter.isSpam(item.getTitle()))
                    .filter(item -> !mongo.exists(item))
                    .forEach(item -> {
                        item.setLanguage(language);
                        mongo.save(item);
                    });

        } catch (IOException e) {
            LOG.error("Error reading RSS feed: " + url);
        }
    }

    private NewsEntry map(Item item) {
        NewsEntry result = new NewsEntry();
        String title = item.getTitle().orElse(null);
        if (title == null) {
            result.setTitle(null);
        } else {
            result.setTitle(filter.replaceHtml(title));
        }
        result.setLink(item.getLink().orElse(null));
        result.setPublishedAt(item.getPubDateZonedDateTime().orElse(ZonedDateTime.now()));

        return result;
    }
}
