package ch.climbd.newsfeed.views;

import ch.climbd.newsfeed.controller.MongoController;
import com.rometools.rome.feed.rss.Channel;
import com.rometools.rome.feed.rss.Description;
import com.rometools.rome.feed.rss.Item;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Date;
import java.util.Set;

@RestController
public class RssFeed {

    @Autowired
    private MongoController mongo;

    @GetMapping(path = "/feed")
    public Channel rss() {
        Channel channel = new Channel();
        channel.setFeedType("rss_2.0");
        channel.setTitle("Climbd Cycling News Feed");
        channel.setDescription("Recent posts");
        channel.setLink("https://news.qfotografie.de");
        channel.setUri("https://news.qfotografie.de");
        channel.setGenerator("Climbd Cycling News Feed");
        Date postDate = new Date();
        channel.setPubDate(postDate);

        var rssItems = new ArrayList<Item>();

        var mongoData = mongo.findAllOrderedByDate(Set.of("en"));

        for (var entry : mongoData) {
            if (!entry.getSummary().isBlank() || !entry.getContent().isBlank()) {

                Item item = new Item();
                item.setAuthor(entry.getDomainOnly());
                item.setLink(entry.getLink());
                item.setTitle(entry.getTitle());
                item.setUri(entry.getLink());
                item.setPubDate(Date.from(entry.getPublishedDateTime().toInstant()));

                Description descr = new Description();
                descr.setValue(entry.getSummary().isBlank() ? entry.getContent() : entry.getSummary());

                item.setDescription(descr);

                rssItems.add(item);
            }
        }

        channel.setItems(rssItems);

        return channel;
    }
}
