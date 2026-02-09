package ch.climbd.newsfeed;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Inline;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.page.TargetElement;
import com.vaadin.flow.server.AppShellSettings;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;

@Theme(themeClass = Lumo.class, variant = Lumo.DARK)
@PWA(name = "Climbd Cycling News - Latest News", shortName = "Climbd News")
@Push
public class AppShell implements AppShellConfigurator {

    @Override
    public void configurePage(AppShellSettings settings) {
        settings.addMetaTag("titel", "Climbd Cycling News - Latest News");
        settings.addMetaTag("author", "climbd");
        settings.addMetaTag("description", "Finding the most popular and freshest news on road bike cycling with the Climbd news aggregator. All your favorite content on a single page.");
        settings.addMetaTag("robots", "index, follow");
        settings.addMetaTag("viewport", "width=device-width, initial-scale=1.0");
        settings.addMetaTag("charset", "UTF-8");

        String noscript = "<noscript><meta http-equiv=\"refresh\" content=\"0; url=/seo\">"
                + "<div style=\"padding:16px;font-family:Arial,sans-serif;\">"
                + "JavaScript is disabled. Visit <a href=\"/seo\">the SEO view</a> for the latest news."
                + "</div></noscript>";
        settings.addInlineWithContents(TargetElement.BODY, Inline.Position.APPEND, noscript, Inline.Wrapping.NONE);

        settings.addFavIcon("icon", "icon/favicon-192x192.png", "192x192");
        settings.addFavIcon("icon", "icon/favicon-96x96.png", "96x96");
        settings.addFavIcon("icon", "icon/favicon-32x32.png", "32x32");
        settings.addFavIcon("icon", "icon/favicon-16x16.png", "16x16");
        //settings.addLink("shortcut icon", "icon/favicon.ico");
    }
}
