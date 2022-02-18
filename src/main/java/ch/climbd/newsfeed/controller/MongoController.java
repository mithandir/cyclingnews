package ch.climbd.newsfeed.controller;

import ch.climbd.newsfeed.data.NewsEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.Set;

@Service
public class MongoController {

    private static final Logger LOG = LoggerFactory.getLogger(MongoController.class);

    @Autowired
    private ReactiveMongoTemplate template;

    public Mono<NewsEntry> findByLink(String link) {
        return template.findById(link, NewsEntry.class);
    }

    public Flux<NewsEntry> findAllOrderedByDate(Set<String> language) {
        Query query = new Query();
        query.addCriteria(Criteria.where("publishedAt").gte(ZonedDateTime.now().minusDays(2).toInstant()));
        query.addCriteria(Criteria.where("language").in(language));

        return template.find(query, NewsEntry.class)
                .sort(Comparator.comparing(NewsEntry::getPublishedDateTime).reversed());
    }

    public Flux<NewsEntry> findAllOrderedByVotes(Set<String> language) {
        Comparator<NewsEntry> compareByVote = Comparator
                .comparingInt(NewsEntry::getVotes).reversed()
                .thenComparing((o1, o2) -> o2.getPublishedDateTime().compareTo(o1.getPublishedDateTime()));

        Query query = new Query();
        query.addCriteria(Criteria.where("language").in(language));
        query.addCriteria(Criteria.where("votes").gte(1));

        return template.find(query, NewsEntry.class)
                .sort(compareByVote)
                .take(50);
    }

    public boolean exists(NewsEntry newsEntry) {
        return findByLink(newsEntry.getLink()).hasElement().block();
    }

    public void save(NewsEntry newsEntry) {
        template.save(Mono.just(newsEntry)).subscribe(
                success -> LOG.info("SavedEntry: " + success.toString()),
                error -> LOG.error("Could not save entry: " + error)
        );
    }

    public void update(NewsEntry newsEntry) {
        template.save(Mono.just(newsEntry)).subscribe(
                success -> LOG.info("Updated entry: " + success.toString()),
                error -> LOG.error("Could not update entry: " + error)
        );
    }

    public void increaseVote(NewsEntry newsEntry) {
        template.findById(newsEntry.getLink(), NewsEntry.class).subscribe(
                success -> {
                    success.setVotes(success.getVotes() + 1);
                    update(success);
                },
                error -> LOG.error("Error: " + error)
        );
    }

    public void decreaseVote(NewsEntry newsEntry) {
        template.findById(newsEntry.getLink(), NewsEntry.class).subscribe(
                success -> {
                    success.setVotes(success.getVotes() - 1);
                    update(success);
                },
                error -> LOG.error("Error: " + error)
        );
    }
}
