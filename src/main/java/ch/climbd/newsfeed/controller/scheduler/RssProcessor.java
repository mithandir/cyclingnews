package ch.climbd.newsfeed.controller.scheduler;

import ch.climbd.newsfeed.controller.MlController;
import ch.climbd.newsfeed.controller.MongoController;
import ch.climbd.newsfeed.controller.PushoverController;
import ch.climbd.newsfeed.data.NewsEntry;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import io.github.thoroldvix.api.TranscriptFormatters;
import io.github.thoroldvix.api.TranscriptList;
import io.github.thoroldvix.api.TranscriptRetrievalException;
import io.github.thoroldvix.api.YoutubeTranscriptApi;
import io.github.thoroldvix.internal.TranscriptApiFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.net.ssl.*;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

@Component
public class RssProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(RssProcessor.class);
    private final ZoneId zoneId = ZoneId.of("Europe/Berlin");
    private final YoutubeTranscriptApi youtubeTranscriptApi = TranscriptApiFactory.createWithClient(new DefaultYoutubeClientCopy());

    @Autowired
    private MongoController mongo;

    @Autowired
    private PushoverController pushover;

    @Autowired
    private Filter filter;

    @Autowired
    MlController mlController;

    public void processRss(String url, String language) {
        try {
            HttpsURLConnection.setDefaultHostnameVerifier(initTrustAll());
            SyndFeed feed = new SyndFeedInput().build(new XmlReader(new URL(url)));
            feed.getEntries().stream().map(this::map)
                    .filter(item -> item.getLink() != null && item.getTitle() != null)
                    .filter(item -> !item.getLink().isBlank() && !item.getTitle().isBlank())
                    .filter(item -> item.getLink().startsWith("http"))
                    .filter(item -> !filter.isSpam(item.getTitle()))
                    .filter(item -> !mongo.exists(item))
                    .forEach(item -> Thread.startVirtualThread(() -> {
                        item.setLanguage(language);
                        mongo.save(item);
                        pushover.sendNotification(item);
                        LOG.debug("New entry: {}", item.getTitle());

                        processYoutubeTranscription(item);
                        if (item.getContent() != null) {
                            if (!item.getLink().startsWith("https://www.youtube.com")) { // don't HTML process youtube videos
                                item.setContent(processHtmlContent(item.getContent()));
                            }

                            mongo.update(item);
                            if (item.getContent().length() > 1000) {
                                mlController.queueSummarize(item);
                            }
                        }
                    }));

        } catch (Exception e) {
            LOG.error("Error reading RSS feed: {}", url);
        }
    }

    private void processYoutubeTranscription(NewsEntry item) {

        if (item.getLink().startsWith("https://www.youtube.com/watch?v=")) {
            var videoId = item.getLink().substring(32);
            try {
                TranscriptList transcriptList = youtubeTranscriptApi.listTranscripts(videoId);
                var fragments = transcriptList.findTranscript("en").fetch();
                var content = TranscriptFormatters.textFormatter().format(fragments);
                LOG.info("Transcript found for video: {}", item.getTitle());
                item.setContent(content);
            } catch (TranscriptRetrievalException e) {
                LOG.warn("No transcript found for video: {}", videoId);
            }
        }
    }

    private NewsEntry map(SyndEntry item) {
        NewsEntry result = new NewsEntry();
        String title = item.getTitle().strip();
        result.setTitle(filter.replaceHtml(title));

        StringBuilder content = new StringBuilder();
        if (item.getContents() != null) {
            for (var itemContent : item.getContents()) {
                content.append(itemContent.getValue());
            }
            result.setContent(content.toString());
        }
        if ((result.getContent() == null || result.getContent().isBlank())
                && item.getDescription() != null) {
            result.setContent(item.getDescription().getValue());
        }

        result.setLink(item.getLink().strip());

        if (item.getPublishedDate() == null || Date.from(Instant.now()).equals(item.getPublishedDate())) {
            result.setPublishedAt(ZonedDateTime.ofInstant(Instant.now(), zoneId));
        } else {
            result.setPublishedAt(ZonedDateTime.ofInstant(item.getPublishedDate().toInstant(), zoneId));
        }

        return result;
    }

    private String processHtmlContent(String content) {
        Document jsoupDoc = Jsoup.parse(content);
        Document.OutputSettings outputSettings = new Document.OutputSettings();
        outputSettings.prettyPrint(false);
        jsoupDoc.outputSettings(outputSettings);
        jsoupDoc.select("br").before("\\br");
        jsoupDoc.select("p").before("\\p");
        String strWithNewLines = Jsoup.clean(jsoupDoc.html(), "", Safelist.none(), outputSettings);
        String str = strWithNewLines.replaceAll("\\\\br", "<br>")
                .replaceAll("\\\\p", "<br><br>")
                .replaceAll("\n", "");

        str = removeLeadingLineBreaks(str);
        str = handleMultipleLineBreaks(str);

        return str;
    }

    private String removeLeadingLineBreaks(String str) {
        for (int i = 0; i < 5; i++) {
            if (str.startsWith("<br>")) {
                str = str.substring(4);
            } else if (str.startsWith("<br><br>")) {
                str = str.substring(8);
            }
        }
        return str;
    }

    private String handleMultipleLineBreaks(String str) {
        str = str.replaceAll("<br><br><br><br>", "<br><br>");
        str = str.replaceAll("<br><br><br>", "<br><br>");
        return str;
    }

    private HostnameVerifier initTrustAll() {
        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }

            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
        }};
        // Install the all-trusting trust manager
        try {
            final SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            // Create all-trusting host name verifier
            return (hostname, session) -> true;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}
