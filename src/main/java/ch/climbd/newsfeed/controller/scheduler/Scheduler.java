package ch.climbd.newsfeed.controller.scheduler;

import ch.climbd.newsfeed.views.components.CommonComponents;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class Scheduler {

    private static final Logger LOG = LoggerFactory.getLogger(Scheduler.class);

    @Autowired
    private RssProcessor processor;

    @Autowired
    private CommonComponents commonComponents;

    @Autowired
    Environment env;

    private final Map<String, String> rssFeeds = new HashMap<>();

    @PostConstruct
    public void init() {
        LOG.info("Initialising Feed List");
        rssFeeds.put("https://forum.xertonline.com/latest.rss", "en");
        rssFeeds.put("http://feeds.feedburner.com/ilovecyclingde", "de");
        rssFeeds.put("http://feeds.feedburner.com/inrng/inrng0", "en");
        rssFeeds.put("http://feeds.feedburner.com/shutuplegsde", "de");
        rssFeeds.put("http://fetchrss.com/rss/6002dbb8135789796c3d9b526002dc80a9582662d205cdf2.xml", "en");
        rssFeeds.put("http://www.uaeteamemirates.com/news-and-media/feed/", "en");
        rssFeeds.put("https://anchor.fm/s/1311c8b8/podcast/rss", "en");
        rssFeeds.put("https://audioboom.com/channels/1405050.rss", "en");
        rssFeeds.put("https://bahraincyclingteam.com/news/feed/", "en");
        rssFeeds.put("https://bike.bikegremlin.com/feed", "en");
        rssFeeds.put("https://bikerumor.com/feed/", "en");
        rssFeeds.put("https://bikesnobnyc.com/feed/", "en");
        rssFeeds.put("https://challenge-magazin.com/feed/", "de");
        rssFeeds.put("https://classic.rad-net.de/html/feed.xml", "de");
        rssFeeds.put("https://cycling.today/feed/", "en");
        rssFeeds.put("https://escapecollective.com/feed/", "en");
        rssFeeds.put("https://feeds.sbs.com.au/cycling-central", "en");
        rssFeeds.put("https://forum.intervals.icu/latest.rss", "en");
        rssFeeds.put("https://granfondo-cycling.com/feed/", "en");
        rssFeeds.put("https://gruppetto-magazin.ch/api/rss-feed", "de");
        rssFeeds.put("https://intheknowcycling.com/feed/", "en");
        rssFeeds.put("https://joefrieltraining.com/feed/", "en");
        rssFeeds.put("https://marginalgainspodcast.libsyn.com/rss", "en");
        rssFeeds.put("https://pezcyclingnews.com/feed/", "en");
        rssFeeds.put("https://podcastd69f1e.podigee.io/feed/mp3", "de");
        rssFeeds.put("https://procyclinguk.com/feed/", "en");
        rssFeeds.put("https://radamring.de/feed/", "de");
        rssFeeds.put("https://radsportverband-nrw.de/feed/", "de");
        rssFeeds.put("https://roadcycling.de/feed", "de");
        rssFeeds.put("https://stages.libsyn.com/rss", "en");
        rssFeeds.put("https://tdaglobalcycling.com/blog/all/feed/", "en");
        rssFeeds.put("https://torstenfrank.wordpress.com/feed/", "de");
        rssFeeds.put("https://trainerroad.libsyn.com/rss", "en");
        rssFeeds.put("https://velomobil.blog/feed/", "de");
        rssFeeds.put("https://www.bike-x.de/rss/rennrad/", "de");
        rssFeeds.put("https://www.bikehugger.com/feed/", "en");
        rssFeeds.put("https://www.cycling-challenge.com/feed/", "en");
        rssFeeds.put("https://www.cyclingapps.net/feed/", "en");
        rssFeeds.put("https://www.cyclingmagazine.de/feed/", "de");
        rssFeeds.put("https://www.cyclingnews.com/feeds.xml", "en");
        rssFeeds.put("https://www.cyclingtips.com/category/news/feed/", "en");
        rssFeeds.put("https://www.cyclist.co.uk/feeds/all", "en");
        rssFeeds.put("https://www.cyclist.co.uk/type/news/feed", "en");
        rssFeeds.put("https://www.cyclistshub.com/feed/", "en");
        rssFeeds.put("https://www.dcrainmaker.com/feed/", "en");
        rssFeeds.put("https://www.dirtracingseries.com/feed/", "en");
        rssFeeds.put("https://www.hambini.com/feed/", "en");
        rssFeeds.put("https://www.podiumcafe.com/rss/current.xml", "en");
        rssFeeds.put("https://www.radsport-events.de/rss", "de");
        rssFeeds.put("https://www.radsport-news.com/rss.xml", "de");
        rssFeeds.put("https://www.radsport-rennrad.de/feed/", "de");
        rssFeeds.put("https://www.rennrad-news.de/news/feed/", "de");
        rssFeeds.put("https://www.roadbike-holidays.com/magazin/feed", "de");
        rssFeeds.put("https://www.rouleur.cc/blogs/desire-journal.atom", "en");
        rssFeeds.put("https://www.rouleur.cc/blogs/rouleur-performance.atom", "en");
        rssFeeds.put("https://www.rouleur.cc/blogs/the-rouleur-journal.atom", "en");
        rssFeeds.put("https://www.sportivecyclist.com/feed/", "en");
        rssFeeds.put("https://www.stickybottle.com/feed/", "en");
        rssFeeds.put("https://www.swiss-cycling.ch/de/feed", "de");
        rssFeeds.put("https://www.trainerroad.com/blog/feed/", "en");
        rssFeeds.put("https://www.trainerroad.com/forum/latest.rss", "en");
        rssFeeds.put("https://www.velomotion.de/magazin/feed", "de");
        rssFeeds.put("https://www.velonews.com/feed/", "en");
        rssFeeds.put("https://www.youtube.com/feeds/videos.xml?channel_id=UC77UtoyivVHkpApL0wGfH5w", "en");
        rssFeeds.put("https://www.youtube.com/feeds/videos.xml?channel_id=UCH9263dSaOHFe25dkyGAu3Q", "en");
        rssFeeds.put("https://www.youtube.com/feeds/videos.xml?channel_id=UCIf1xvRN8pzyd_VfLgj_dow", "en");
        rssFeeds.put("https://www.youtube.com/feeds/videos.xml?channel_id=UCYuKCZ35_lrDmFj2gNuAwZw", "en");
        rssFeeds.put("https://www.youtube.com/feeds/videos.xml?channel_id=UCuTaETsuCOkJ0H_GAztWt0Q", "en");
        rssFeeds.put("https://israelpremiertech.com/feed", "en");
        rssFeeds.put("https://zwiftinsider.com/feed/", "en");
        rssFeeds.put("https://bahraincyclingteam.com/feed", "en");
        rssFeeds.put("https://www.wmncycling.com/feed", "en");
        rssFeeds.put("https://racing.trekbikes.com/feed", "en");
//      rssFeeds.put("https://road.cc/rss", "en"); Mostly AD articles

        initIconCache();
    }

    private void initIconCache() {
        rssFeeds.keySet().forEach(url -> Thread.startVirtualThread(() -> {
            if (!url.isEmpty()) {
                var start = url.indexOf("://") + 3;

                try {
                    var end = url.indexOf("/", start);
                    String subUrl = url.substring(0, end);
                    commonComponents.findIcon(subUrl);
                } catch (IndexOutOfBoundsException e) {
                    // DO NOTHING
                }
            }
        }));
        LOG.info("Icon cache initialised");
    }

    @Scheduled(fixedDelay = 15, initialDelay = 1, timeUnit = TimeUnit.MINUTES)
    public void scheduleFeedProcessing() {
        LOG.info("Running RSS scheduler");

        rssFeeds.keySet()
                .forEach(feedId -> Thread.startVirtualThread(
                        () -> processor.processRss(feedId, rssFeeds.get(feedId)))
                );
    }
}
