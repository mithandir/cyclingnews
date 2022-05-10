package ch.climbd.newsfeed.views;

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
import org.vaadin.addon.browserstorage.LocalStorage;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Component
public class CommonComponents {
    private final Map<String, String> iconCache = new HashMap<>();
    private static DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
    private ZoneId zoneId = ZoneId.of("Europe/Berlin");

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
        writeLocalStorage("LAST-VISIT", ZonedDateTime.ofInstant(Instant.now(), zoneId).format(formatter));
    }

    public void isItemUnRead(ZonedDateTime itemPublishDate, HorizontalLayout horizontalLayout) {
        UI currentUI = UI.getCurrent();
        LocalStorage localStorage = new LocalStorage(currentUI);
        localStorage.getItem("LAST-VISIT").thenAccept(lastVisit -> {
            if (lastVisit != null) {
                ZonedDateTime lastVisitDate = ZonedDateTime.parse(lastVisit, formatter);
                if (itemPublishDate.isAfter(lastVisitDate)) {
                    horizontalLayout.getStyle().set("opacity", "100%");
                } else {
                    horizontalLayout.getStyle().set("opacity", "60%");
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
                String url = e1.attr("itemprop");
                if (!url.startsWith("http")) {
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
