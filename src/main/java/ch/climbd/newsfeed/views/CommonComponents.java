package ch.climbd.newsfeed.views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.icon.Icon;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class CommonComponents {
    private final Map<String, String> iconCache = new HashMap<>();

    @Autowired
    Environment env;

    public void writeLocalStorage(String id, String value) {
        UI.getCurrent().getPage().executeJs("debugger; window.localStorage.setItem($0, $1);", id, value);
    }

    public void checkIcon(Icon icon, String id) {
        try {
            UI.getCurrent().getPage().executeJs("debugger; return window.localStorage.getItem($0);", id).then(String.class, result -> {
                if (result == null || result.equals("false")) {
                    icon.setColor(null);
                } else {
                    icon.setColor("green");
                }
            });
        } catch (Exception e) {
            //NOTHING
        }
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

    private String findIcon(String pageUrl) {
        if (env.getActiveProfiles() != null && env.getActiveProfiles().length > 0 && env.getActiveProfiles()[0].contains("local")) {
            return null;
        }

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
