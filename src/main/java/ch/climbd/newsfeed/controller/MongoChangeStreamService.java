package ch.climbd.newsfeed.controller;

import ch.climbd.newsfeed.data.NewsEntry;
import com.mongodb.MongoCommandException;
import com.mongodb.MongoException;
import com.mongodb.client.MongoChangeStreamCursor;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.mongodb.client.model.changestream.FullDocument;
import com.mongodb.client.model.changestream.OperationType;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

@Service
public class MongoChangeStreamService {

    private static final Logger LOG = LoggerFactory.getLogger(MongoChangeStreamService.class);
    private static final long RETRY_DELAY_MS = 3000;
    private static final long FALLBACK_POLL_DELAY_MS = 5000;

    private final MongoTemplate mongoTemplate;
    private final Set<Consumer<NewsChangeEvent>> listeners = new CopyOnWriteArraySet<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor(runnable -> {
        var thread = new Thread(runnable);
        thread.setName("mongodb-change-stream-watcher");
        thread.setDaemon(true);
        return thread;
    });

    private volatile boolean running = true;

    public MongoChangeStreamService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @PostConstruct
    void startWatcher() {
        executorService.submit(this::watchLoop);
    }

    @PreDestroy
    void shutdownWatcher() {
        running = false;
        executorService.shutdownNow();
    }

    public AutoCloseable subscribe(Consumer<NewsChangeEvent> listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    public void publishSyntheticChange(OperationType operationType, String link) {
        notifyListeners(new NewsChangeEvent(operationType, link));
    }

    private void watchLoop() {
        while (running) {
            try {
                var collectionName = mongoTemplate.getCollectionName(NewsEntry.class);
                var collection = mongoTemplate.getCollection(collectionName);

                try (MongoChangeStreamCursor<ChangeStreamDocument<Document>> cursor = collection.watch()
                        .fullDocument(FullDocument.UPDATE_LOOKUP)
                        .cursor()) {

                    LOG.info("MongoDB change stream active for collection: {}", collectionName);
                    while (running && cursor.hasNext()) {
                        var change = cursor.next();
                        notifyListeners(toEvent(change));
                    }
                }
            } catch (MongoCommandException e) {
                if (e.getErrorCode() == 40573) {
                    LOG.warn("MongoDB change streams require a replica set. Falling back to MongoDB polling.");
                    runPollingFallback();
                    return;
                }
                LOG.warn("MongoDB command error in change stream watcher. Retrying in {}ms", RETRY_DELAY_MS, e);
                sleepBeforeRetry();
            } catch (MongoException e) {
                LOG.warn("MongoDB change stream watcher disconnected. Retrying in {}ms", RETRY_DELAY_MS, e);
                sleepBeforeRetry();
            } catch (Exception e) {
                LOG.warn("Unexpected error in MongoDB change stream watcher. Retrying in {}ms", RETRY_DELAY_MS, e);
                sleepBeforeRetry();
            }
        }
    }

    private void sleepBeforeRetry() {
        try {
            Thread.sleep(RETRY_DELAY_MS);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
        }
    }

    private void runPollingFallback() {
        Set<String> previousRecentLinks = new HashSet<>(fetchRecentLinks());
        LOG.info("MongoDB polling fallback active. Poll interval={}ms", FALLBACK_POLL_DELAY_MS);

        while (running) {
            try {
                Thread.sleep(FALLBACK_POLL_DELAY_MS);
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
                return;
            }

            Set<String> currentRecentLinks = new HashSet<>(fetchRecentLinks());
            if (!currentRecentLinks.equals(previousRecentLinks)) {
                notifyListeners(new NewsChangeEvent(OperationType.INSERT, null));
                previousRecentLinks = currentRecentLinks;
            }
        }
    }

    private List<String> fetchRecentLinks() {
        var query = new Query();
        query.addCriteria(Criteria.where("publishedAt").gte(ZonedDateTime.now().minusDays(2).toInstant()));
        query.with(Sort.by(Sort.Direction.DESC, "publishedAt"));
        query.limit(100);

        return mongoTemplate.find(query, NewsEntry.class)
                .stream()
                .map(NewsEntry::getLink)
                .filter(link -> link != null && !link.isBlank())
                .toList();
    }

    private NewsChangeEvent toEvent(ChangeStreamDocument<Document> change) {
        String link = null;
        if (change.getDocumentKey() != null) {
            var id = change.getDocumentKey().get("_id");
            link = id == null ? null : id.toString();
        }

        OperationType operationType = change.getOperationType();
        return new NewsChangeEvent(operationType, link);
    }

    private void notifyListeners(NewsChangeEvent event) {
        for (var listener : listeners) {
            try {
                listener.accept(event);
            } catch (Exception e) {
                LOG.debug("Error delivering change stream event to a listener", e);
            }
        }
    }

    public record NewsChangeEvent(OperationType operationType, String link) {
    }
}
