package ch.climbd.newsfeed.controller;

import ch.climbd.newsfeed.data.NewsEntry;
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
    private boolean processing = false;

    MlController(ChatClient.Builder chatClientBuilder, MongoController mongoController) {
        this.chatClient = chatClientBuilder.build();
        this.mongo = mongoController;
    }

    public void queueSummarize(NewsEntry news) {
        LOG.info("Queued article for summarization: {}", news.getTitle());
        queue.add(news);
    }

    @Scheduled(fixedDelay = 1, initialDelay = 2, timeUnit = TimeUnit.MINUTES)
    public void summarize() {
        LOG.info("Processing summarization queue of length: {}", queue.size());
        if (processing) {
            return;
        }

        processing = true;
        NewsEntry news = null;
        try {
            news = queue.poll();

            if (news == null) {
                processing = false;
                return;
            }

            news.setSummary(chatClient.prompt()
                    .user("You are a news reporter that summarizes news articles in maximum 800 characters. Summarize the following text: " + news.getContent())
                    .call()
                    .content());
            LOG.debug("Summary: {}", news.getSummary());
            mongo.update(news);
            LOG.info("Summarized the article: {}", news.getTitle());
        } catch (Exception e) {
            LOG.error("Error summarizing article", e);
        } finally {
            processing = false;
        }
    }
}
