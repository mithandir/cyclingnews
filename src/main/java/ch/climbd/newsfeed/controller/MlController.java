package ch.climbd.newsfeed.controller;

import ch.climbd.newsfeed.data.NewsEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

@Controller
public class MlController {

    private static final Logger LOG = LoggerFactory.getLogger(MlController.class);

    private final ChatClient chatClient;
    private final MongoController mongo;
    private final SynchronousQueue<NewsEntry> queue = new SynchronousQueue<>();
    private final CountDownLatch countDownLatch = new CountDownLatch(1);
    private int queueSize = 0;

    MlController(ChatClient.Builder chatClientBuilder, MongoController mongoController) {
        this.chatClient = chatClientBuilder.build();
        this.mongo = mongoController;
    }

    public void queueSummarize(NewsEntry news) {
        LOG.info("Queued article for summarization: {}", news.getTitle());
        try {
            queue.put(news);
            queueSize++;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Scheduled(fixedDelay = 1, initialDelay = 2, timeUnit = TimeUnit.MINUTES)
    public void summarize() {
        LOG.info("Processing summarization queue of length: {}", queueSize);
        if (queueSize >= 0) {
            try {
                countDownLatch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        NewsEntry news = null;
        try {
            news = queue.take();

            if (news == null) {
                return;
            }

            news.setContent(chatClient.prompt()
                    .user("You are a news reporter that summarizes news articles in maximum 1000 characters. Summarize the following text: " + news.getContent())
                    .call()
                    .content());
            LOG.debug("Summary: {}", news.getSummary());
            mongo.update(news);
            LOG.info("Summarized the article: {}", news.getTitle());
            queueSize--;
            countDownLatch.countDown();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
