package ch.climbd.newsfeed.views.components;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriUtils;
import org.vaadin.addon.browserstorage.LocalStorage;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

@Component
public class CommonComponents {
    private final Map<String, String> iconCache = new HashMap<>();
    private final static DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_DATE_TIME;
    private final ZoneId zoneId = ZoneId.of("Europe/Berlin");

    @Autowired
    Environment env;

    public void writeLocalStorage(String id, String value) {
        UI currentUI = UI.getCurrent();
        LocalStorage localStorage = new LocalStorage(currentUI);
        localStorage.setItem(id, value);
    }

    public void checkIconStatus(Icon icon, String id) {
        UI currentUI = UI.getCurrent();
        LocalStorage localStorage = new LocalStorage(currentUI);
        localStorage.getItem(id).thenAccept(result -> {
            if (result == null || result.equals("false")) {
                icon.setColor(null);
            } else {
                icon.setColor("green");
            }
        });
    }

    public void updateLastVisit() {
        UI currentUI = UI.getCurrent();
        LocalStorage localStorage = new LocalStorage(currentUI);
        localStorage.getItem("LAST-VISIT").thenAccept(lastVisit -> {
            if (lastVisit != null) {
                ZonedDateTime lastVisitDate = ZonedDateTime.parse(lastVisit, FORMATTER);
                if (lastVisitDate.plusSeconds(10).isAfter(ZonedDateTime.ofInstant(Instant.now(), zoneId))) {
                    // Was already saved a few seconds ago (Redirect for Admin etc.)
                }
            }

            writeLocalStorage("LAST-VISIT", ZonedDateTime.ofInstant(Instant.now(), zoneId).format(FORMATTER));
        });
    }

    public void isItemUnRead(ZonedDateTime itemPublishDate, HorizontalLayout horizontalLayout, Avatar avatar) {
        final ZonedDateTime itemDate;
        if (itemPublishDate.isAfter(ZonedDateTime.ofInstant(Instant.now(), zoneId))) {
            itemDate = ZonedDateTime.ofInstant(Instant.now().minus(1, ChronoUnit.MINUTES), zoneId);
        } else {
            itemDate = itemPublishDate;
        }

        UI currentUI = UI.getCurrent();
        LocalStorage localStorage = new LocalStorage(currentUI);
        localStorage.getItem("LAST-VISIT").thenAccept(lastVisit -> {
            if (lastVisit != null) {
                ZonedDateTime lastVisitDate = ZonedDateTime.parse(lastVisit, FORMATTER);
                if (itemDate.isAfter(lastVisitDate)) {
                    horizontalLayout.getStyle().set("opacity", "100%");
                    avatar.getStyle().set("opacity", "100%");
                } else {
                    horizontalLayout.getStyle().set("opacity", "50%");
                    avatar.getStyle().set("opacity", "50%");
                }
            }
        });

        horizontalLayout.getStyle().set("opacity", "100%");
    }

    public Avatar buildSiteIcon(String pageUrl, String domainOnly) {
        Avatar avatar = new Avatar();
        avatar.setName(domainOnly);
        avatar.setImage(findIcon(pageUrl));

        return avatar;
    }

    public boolean isMobile() {
        var browser = UI.getCurrent().getSession().getBrowser();
        return browser.isAndroid() || browser.isIPhone() || browser.isWindowsPhone();
    }

    public String createLinkWithStats(String url) {
        var base = env.getProperty("baseurl");

        String encUrl = UriUtils.encodePath(url, "UTF-8");

        if (base != null && !base.isBlank()) {
            return base + "/" + "redirect?url=" + encUrl;
        }

        return url;
    }

    public String findIcon(String pageUrl) {
        if (iconCache.containsKey(pageUrl)) {
            return iconCache.get(pageUrl);
        }

        try {
            Connection con = Jsoup.connect(pageUrl);
            Document doc = con.get();

            Element e3 = doc.head().select("link[rel~=icon]").first();
            if (e3 != null && !e3.attr("href").isBlank()) {
                String url = e3.attr("href");
                if (!url.startsWith("http")) {
                    url = pageUrl + url;
                }
                iconCache.put(pageUrl, url);
                return url;
            }

            Element e1 = doc.head().select("link[href~=.*\\.(ico|png)]").first();
            if (e1 != null && !e1.attr("href").isBlank()) {
                String url = e1.attr("href");
                if (!url.startsWith("http")) {
                    url = pageUrl + url;
                }
                iconCache.put(pageUrl, url);
                return url;
            }

            Element e2 = doc.head().select("meta[itemprop=image]").first();
            if (e2 != null && !e2.attr("itemprop").isBlank()) {
                String url = null;
                if (e1 != null) {
                    url = e1.attr("itemprop");
                }
                if (url != null && !url.startsWith("http")) {
                    url = pageUrl + url;
                }
                iconCache.put(pageUrl, url);
                return url;
            }
        } catch (Exception e) {
            // DO nothing
        }

        iconCache.put(pageUrl, null);
        return null;
    }
}
