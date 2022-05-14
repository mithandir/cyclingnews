package ch.climbd.newsfeed.controller;

import ch.climbd.newsfeed.data.NewsEntry;
import de.svenkubiak.jpushover.JPushover;
import de.svenkubiak.jpushover.exceptions.JPushoverException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PushoverController {

    private static final Logger LOG = LoggerFactory.getLogger(PushoverController.class);

    @Value("${pushover.enabled:false}")
    private boolean enabled;

    @Value("${pushover.api-key}")
    private String apiKey;

    @Value("${pushover.user-key}")
    private String userKey;

    public void sendNotification(NewsEntry newsEntry) {
        if (enabled) {
            try {
                JPushover.messageAPI()
                        .withToken(apiKey)
                        .withUser(userKey)
                        .withTitle("Climbd: "+ newsEntry.getTitle())
                        .withMessage(newsEntry.getLink())
                        .push();
            } catch (JPushoverException e) {
                LOG.error("Error sending PushOver notification", e);
            }
        }
    }
}
