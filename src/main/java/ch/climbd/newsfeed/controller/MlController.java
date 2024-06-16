package ch.climbd.newsfeed.controller;

import ch.climbd.newsfeed.data.NewsEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;

import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

@Controller
public class MlController {

    private static final Logger LOG = LoggerFactory.getLogger(MlController.class);

    private final ChatClient chatClient;
    private final MongoController mongo;
    private final Queue<NewsEntry> queue = new SynchronousQueue<>();
    private final CountDownLatch countDownLatch = new CountDownLatch(1);

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
        LOG.info("Processing summarization queue");
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        var news = queue.poll();
        if (news == null) {
            return;
        }
        news.setContent(chatClient.prompt()
                .system("You are a news reporter that summarizes news articles in maximum 1000 characters.")
                .user("Summarize the following text: " + news.getContent())
                .call()
                .content());
        LOG.debug("Summary: {}", news.getSummary());
        mongo.update(news);
        LOG.info("Summarized the article: {}", news.getTitle());
        countDownLatch.countDown();
    }

}
