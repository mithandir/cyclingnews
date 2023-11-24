package ch.climbd.newsfeed.controller;

import ch.climbd.newsfeed.data.NewsEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
public class MongoController {

    private static final Logger LOG = LoggerFactory.getLogger(MongoController.class);

    @Autowired
    private MongoTemplate template;

    public boolean exists(String link) {
        if (link != null && link.startsWith("http")) {
            Query query = new Query();
            query.addCriteria(Criteria.where("link").in(link));
            return template.exists(query, NewsEntry.class);
        }

        LOG.warn("Not a link: " + link);
        return false;
    }

    public boolean exists(NewsEntry newsEntry) {
        return exists(newsEntry.getLink());
    }

    public List<NewsEntry> findAllOrderedByDate(Set<String> language) {
        Query query = new Query();
        query.addCriteria(Criteria.where("publishedAt").gte(ZonedDateTime.now().minusDays(2).toInstant()));
        query.addCriteria(Criteria.where("language").in(language));
        query.with(Sort.by(Sort.Direction.DESC, "publishedAt"));
        query.limit(100);

        return template.find(query, NewsEntry.class);
    }

    public List<NewsEntry> findAllOrderedByVotes(Set<String> language) {
        Comparator<NewsEntry> compareByVotePerDay = (NewsEntry o1, NewsEntry o2) -> {
            // Cut down to just plain day without hours, minutes and seconds.
            // Then add the amount of votes as seconds to have sorting inside a day.
            var obj1 = o1.getPublishedDateTime()
                    .truncatedTo(ChronoUnit.DAYS)
                    .plusSeconds(o1.getVotes());
            var obj2 = o2.getPublishedDateTime()
                    .truncatedTo(ChronoUnit.DAYS)
                    .plusSeconds(o2.getVotes());

            return obj2.compareTo(obj1);
        };

        Query query = new Query();
        query.addCriteria(Criteria.where("language").in(language));
        query.addCriteria(Criteria.where("votes").gte(1));
        query.with(Sort.by(Sort.Direction.DESC, "votes"));
        query.limit(100);

        var result = template.find(query, NewsEntry.class);
        result.sort(compareByVotePerDay);
        return result;
    }

    public List<NewsEntry> findAllOrderedByViews(Set<String> language) {
        Comparator<NewsEntry> compareByViewPerDay = (NewsEntry o1, NewsEntry o2) -> {
            // Cut down to just plain day without hours, minutes and seconds.
            // Then add the amount of votes as seconds to have sorting inside a day.
            var obj1 = o1.getPublishedDateTime()
                    .truncatedTo(ChronoUnit.DAYS)
                    .plusSeconds(o1.getViews());
            var obj2 = o2.getPublishedDateTime()
                    .truncatedTo(ChronoUnit.DAYS)
                    .plusSeconds(o2.getViews());

            return obj2.compareTo(obj1);
        };

        Query query = new Query();
        query.addCriteria(Criteria.where("language").in(language));
        query.addCriteria(Criteria.where("views").gte(1));
        query.with(Sort.by(Sort.Direction.DESC, "views"));
        query.limit(100);

        var result = template.find(query, NewsEntry.class);
        result.sort(compareByViewPerDay);
        return result;
    }

    public void save(NewsEntry newsEntry) {
        template.save(newsEntry);
    }

    public void update(NewsEntry newsEntry) {
        template.save(newsEntry);
    }

    public void increaseVote(NewsEntry newsEntry) {
        var result = template.findById(newsEntry.getLink(), NewsEntry.class);
        if (result != null) {
            result.setVotes(result.getVotes() + 1);
            update(result);
        }
    }

    public void decreaseVote(NewsEntry newsEntry) {
        var result = template.findById(newsEntry.getLink(), NewsEntry.class);
        if (result != null) {
            result.setVotes(result.getVotes() - 1);
            update(result);
        }
    }

    public void increaseViews(String url) {
        var result = template.findById(url, NewsEntry.class);
        if (result != null) {
            if (result.getViews() != null) {
                result.setViews(result.getViews() + 1);
                update(result);
                LOG.info(result.getViews() + " views for: " + url);
            } else {
                result.setViews(1);
                update(result);
            }
        }
    }

    public List<NewsEntry> searchEntries(String searchString) {
        var splitted = searchString.strip().split(" ");

        StringBuilder searchQuery = new StringBuilder();
        Arrays.stream(splitted).map(e -> "(?=.*" + e + ")").forEach(searchQuery::append);

        Criteria regex = Criteria.where("title").regex(searchQuery.toString(), "i");

        var query = new Query()
                .addCriteria(regex)
                .with(Sort.by(Sort.Direction.DESC, "publishedAt"))
                .limit(100);

        return template.find(query, NewsEntry.class);
    }
}
