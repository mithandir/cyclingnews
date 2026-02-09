package ch.climbd.newsfeed.controller;

import ch.climbd.newsfeed.data.NewsEntry;
import io.github.thoroldvix.api.*;
import jakarta.annotation.PostConstruct;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Controller
public class MlController {

    private static final Logger LOG = LoggerFactory.getLogger(MlController.class);
    private final YoutubeTranscriptApi youtubeTranscriptApi = TranscriptApiFactory.createWithClient(new DefaultYoutubeClientCopy());

    private final ChatClient chatClient;
    private final MongoController mongo;
    private final BlockingQueue<NewsEntry> queue = new LinkedBlockingQueue<>();

    MlController(ChatClient.Builder chatClientBuilder, MongoController mongoController) {
        this.chatClient = chatClientBuilder.build();
        this.mongo = mongoController;
    }

    @PostConstruct
    public void fixQueueAfterRestart() {
        var todaysNews = mongo.findLast100PostsPostedInTheLast48h();
        todaysNews.stream()
                .filter(news -> news.getSummary() == null || news.getSummary().isBlank())
                .filter(news -> news.getContent() != null && !news.getContent().isBlank() && news.getContent().length() > 1000)
                .forEach(this::queueSummarize);
    }

    public void queueSummarize(NewsEntry news) {
        LOG.info("Queued article for summarization: {}", news.getTitle());
        queue.offer(news);
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

            processYoutubeTranscription(news);

            summarizeNormalText(news);
        } catch (Exception e) {
            LOG.error("Error summarizing article", e);
        }
    }

    private void summarizeNormalText(NewsEntry news) {
        if (!news.getLink().startsWith("https://www.youtube.com/watch?v=")) {
            var content = chatClient.prompt()
                    .system("As a professional summarizer, create a concise and comprehensive summary of the provided text, be it an article, post, conversation, or passage, while adhering to these guidelines:\n" +
                            "* Craft a summary that is detailed, thorough, in-depth, and complex, while maintaining clarity and conciseness.\n" +
                            "* Rely strictly on the provided text, without including external information.\n" +
                            "* Format the summary in paragraph form for easy understanding.")
                    .user("Summaries the following:\n" + news.getContent())
                    .call()
                    .content();

            content = handleO1Reasoning(content);
            content = convertMarkdownToHtml(content);

            news.setSummary(content);
            LOG.debug("Summary: {}", content);
            mongo.update(news);
            LOG.info("Summarized the article: {}", news.getTitle());
        }
    }

    private void processYoutubeTranscription(NewsEntry item) {
        if (item.getLink().startsWith("https://www.youtube.com/watch?v=")) {

            // Fix potential double summarization
            if (item.getContent() != null && !item.getContent().isBlank()) {
                return;
            }

            var videoId = item.getLink().substring(32);
            try {
                TranscriptList transcriptList = youtubeTranscriptApi.listTranscripts(videoId);
                var fragments = transcriptList.findTranscript("en").fetch();
                var content = TranscriptFormatters.textFormatter().format(fragments);
                LOG.info("Transcript found for video: {}", item.getTitle());

                // 32k token limit
//                if (content.length() > 100000) {
//                    content = content.substring(0, 100000);
//                }

                var summary = chatClient.prompt()
                        .system("As a professional summarizer, create a concise and comprehensive summary of the provided text, be it an article, post, conversation, or passage, while adhering to these guidelines:\n" +
                                "* Craft a summary that is detailed, thorough, in-depth, and complex, while maintaining clarity and conciseness.\n" +
                                "* Rely strictly on the provided text, without including external information.\n" +
                                "* Format the summary in paragraph form for easy understanding.")
                        .user("Summaries the following:\n" + content)
                        .call()
                        .content();

                summary = handleO1Reasoning(summary);
                summary = convertMarkdownToHtml(summary);

                item.setContent(summary);
                mongo.update(item);
                LOG.info("Summarized the article: {}", item.getTitle());

            } catch (TranscriptRetrievalException e) {
                LOG.warn("No transcript found for video: {}", videoId);
            }
        }
    }

    private static String convertMarkdownToHtml(String markdown) {
        try {
            Parser parser = Parser.builder().build();
            Node document = parser.parse(markdown);
            HtmlRenderer renderer = HtmlRenderer.builder().build();
            var html = renderer.render(document);
            return Jsoup.clean(html, Safelist.basic().addTags("br"));
        } catch (Exception e) {
            LOG.error("Error converting markdown to html", e);
            return markdown;
        }
    }

    private static String handleO1Reasoning(String content) {
        var thinkEnd = content.indexOf("</think>");

        if (thinkEnd > 0) {
            return content.substring(thinkEnd + 8);
        }

        return content;
    }
}
