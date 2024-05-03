package ch.climbd.newsfeed.controller.scheduler;

import ch.climbd.newsfeed.controller.MongoController;
import ch.climbd.newsfeed.controller.PushoverController;
import ch.climbd.newsfeed.data.NewsEntry;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
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

    @Autowired
    private MongoController mongo;

    @Autowired
    private PushoverController pushover;

    @Autowired
    private Filter filter;

    public void processRss(String url, String language) {
        try {
            HttpsURLConnection.setDefaultHostnameVerifier(initTrustAll());
            SyndFeed feed = new SyndFeedInput().build(new XmlReader(new URL(url)));
            feed.getEntries().stream().map(this::map)
                    .filter(item -> item.getLink() != null && item.getTitle() != null)
                    .filter(item -> !item.getLink().isBlank() && !item.getTitle().isBlank())
                    .filter(item -> !filter.isSpam(item.getTitle()))
                    .forEach(item -> Thread.startVirtualThread(() -> {
                        if (!mongo.exists(item)) {
                            item.setLanguage(language);
                            mongo.save(item);
                            pushover.sendNotification(item);
                            LOG.debug("New entry: {}", item.getTitle());
                        }
                    }));

        } catch (Exception e) {
            LOG.error("Error reading RSS feed: {}", url);
        }
    }

    private NewsEntry map(SyndEntry item) {
        NewsEntry result = new NewsEntry();
        String title = item.getTitle().strip();
        result.setTitle(filter.replaceHtml(title));

        result.setLink(item.getLink().strip());

        if (item.getPublishedDate() == null || Date.from(Instant.now()).equals(item.getPublishedDate())) {
            result.setPublishedAt(ZonedDateTime.ofInstant(Instant.now(), zoneId));
        } else {
            result.setPublishedAt(ZonedDateTime.ofInstant(item.getPublishedDate().toInstant(), zoneId));
        }

        return result;
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
