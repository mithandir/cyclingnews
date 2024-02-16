package ch.climbd.newsfeed.controller;

import ch.climbd.newsfeed.data.NewsEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

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
        var startTime = LocalDateTime.now();
        int currentPage = 0;
        List<NewsEntry> newsEntries = new ArrayList<>();

        while (newsEntries.isEmpty()
                || (newsEntries.size() == currentPage * 10
                && newsEntries.size() <= 90
                && Duration.between(startTime, LocalDateTime.now()).toSeconds() < 1)) {

            var paging = PageRequest.of(currentPage, 10);
            var query = new Query()
                    .addCriteria(Criteria.where("publishedAt").gte(ZonedDateTime.now().minusDays(2).toInstant()))
                    .addCriteria(Criteria.where("language").in(language))
                    .with(Sort.by(Sort.Direction.DESC, "publishedAt"))
                    .with(paging);

            newsEntries.addAll(template.find(query, NewsEntry.class));
            currentPage++;
        }

        return newsEntries;
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

        var startTime = LocalDateTime.now();
        int currentPage = 0;
        List<NewsEntry> newsEntries = new ArrayList<>();

        while (newsEntries.isEmpty()
                || (newsEntries.size() == currentPage * 10
                && newsEntries.size() <= 90
                && Duration.between(startTime, LocalDateTime.now()).toSeconds() < 1)) {

            var paging = PageRequest.of(currentPage, 10);
            var query = new Query()
                    .addCriteria(Criteria.where("language").in(language))
                    .addCriteria(Criteria.where("votes").gte(1))
                    .with(Sort.by(Sort.Direction.DESC, "votes"))
                    .with(paging);

            newsEntries.addAll(template.find(query, NewsEntry.class));
            currentPage++;
        }

        newsEntries.sort(compareByVotePerDay);
        return newsEntries;
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

        var startTime = LocalDateTime.now();
        int currentPage = 0;
        List<NewsEntry> newsEntries = new ArrayList<>();

        while (newsEntries.isEmpty()
                || (newsEntries.size() == currentPage * 10
                && newsEntries.size() <= 90
                && Duration.between(startTime, LocalDateTime.now()).toSeconds() < 1)) {

            var paging = PageRequest.of(currentPage, 10);


            var query = new Query()
                    .addCriteria(Criteria.where("language").in(language))
                    .addCriteria(Criteria.where("views").gte(1))
                    .with(Sort.by(Sort.Direction.DESC, "views"))
                    .with(paging);


            newsEntries.addAll(template.find(query, NewsEntry.class));
            currentPage++;
        }

        newsEntries.sort(compareByViewPerDay);
        return newsEntries;
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

    public List<NewsEntry> searchEntries(String searchString, Set<String> language) {
        var startTime = LocalDateTime.now();
        var splitted = searchString.strip().split(" ");

        StringBuilder searchQuery = new StringBuilder();
        Arrays.stream(splitted).map(e -> "(?=.*" + e + ")").forEach(searchQuery::append);

        Criteria regex = Criteria.where("title").regex(searchQuery.toString(), "i");

        int currentPage = 0;
        List<NewsEntry> newsEntries = new ArrayList<>();

        while (newsEntries.isEmpty()
                || (newsEntries.size() == currentPage * 10
                && newsEntries.size() <= 90
                && Duration.between(startTime, LocalDateTime.now()).toSeconds() < 1)) {

            var paging = PageRequest.of(currentPage, 10);
            var query = new Query()
                    .addCriteria(regex)
                    .addCriteria(Criteria.where("language").in(language))
                    .with(Sort.by(Sort.Direction.DESC, "publishedAt"))
                    .with(paging);

            newsEntries.addAll(template.find(query, NewsEntry.class));
            currentPage++;
        }

        return newsEntries;
    }
}
