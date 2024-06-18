package ch.climbd.newsfeed.controller;

import ch.climbd.newsfeed.data.NewsEntry;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;

import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

@Controller
public class MlController {

    private static final Logger LOG = LoggerFactory.getLogger(MlController.class);

    private final ChatClient chatClient;
    private final MongoController mongo;
    private final LinkedList<NewsEntry> queue = new LinkedList<>();

    MlController(ChatClient.Builder chatClientBuilder, MongoController mongoController) {
        this.chatClient = chatClientBuilder.build();
        this.mongo = mongoController;
    }

    @PostConstruct
    public void fixQueueAfterRestart() {
        var todaysNews = mongo.findAllPostedToday();
        todaysNews.stream()
                .filter(news -> news.getSummary() == null || news.getSummary().isBlank())
                .filter(news -> news.getContent() != null && !news.getContent().isBlank() && news.getContent().length() > 1000)
                .forEach(this::queueSummarize);
    }

    public void queueSummarize(NewsEntry news) {
        LOG.info("Queued article for summarization: {}", news.getTitle());
        queue.add(news);
    }

    @Scheduled(fixedDelay = 15, initialDelay = 30, timeUnit = TimeUnit.SECONDS)
    public void summarize() {
        NewsEntry news;
        try {
            news = queue.poll();

            if (news == null) {
                return;
            }
            LOG.info("Processing summarization queue of length: {}", queue.size());
            news.setSummary(chatClient.prompt()
                    .system("You are a news reporter that summarizes news articles")
                    .user("Summarize the following text, in a maximum of 3 paragraphs: " + news.getContent())
                    .call()
                    .content());
            LOG.debug("Summary: {}", news.getSummary());
            mongo.update(news);
            LOG.info("Summarized the article: {}", news.getTitle());
        } catch (Exception e) {
            LOG.error("Error summarizing article", e);
        }
    }
}
