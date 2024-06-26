package ch.climbd.newsfeed.views;

import ch.climbd.newsfeed.controller.MongoController;
import ch.climbd.newsfeed.data.NewsEntry;
import ch.climbd.newsfeed.views.components.CommonComponents;
import ch.climbd.newsfeed.views.components.CommonSessionComponents;
import ch.climbd.newsfeed.views.components.NewsItemComponent;
import ch.climbd.newsfeed.views.components.SearchComponent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.LinkedList;

@Route("")
@PageTitle("Climbd Cycling News - Latest News")
public class LatestView extends VerticalLayout {

    private static final Logger LOG = LoggerFactory.getLogger(LatestView.class);

    @Autowired
    private MongoController mongo;

    @Autowired
    private CommonComponents commonComponents;

    @Autowired
    private CommonSessionComponents commonSessionComponents;

    @Autowired
    private SearchComponent searchComponent;

    @Autowired
    private NewsItemComponent newsItemComponent;

    @Value("${baseurl}")
    private String baseUrl;

    private LinkedList<NewsEntry> sourceData;
    private VerticalLayout newsItems;

    @PostConstruct
    public void init() {
        super.getStyle().set("width", "inherit");

        checkIfSmallScreenAndAdjustStyle();
        listenAndAdjustOnResize();

        var image = new Image(baseUrl + "/logo.svg", "Title");
        image.setWidth("8em");
        var heading = new H1("cycling news");
        var header = new HorizontalLayout(image, heading);
        header.setAlignItems(Alignment.CENTER);

        add(header);

        add(commonSessionComponents.createMenu());

        sourceData = new LinkedList<>(mongo.findAllOrderedByDate(commonSessionComponents.getSelectedLanguages()));
        newsItems = newsItemComponent.createNewsItem(sourceData);

        var searchBar = searchComponent.createSearchBar(newsItems);

        add(searchBar);

        add(newsItems);

        if (commonSessionComponents.isAdminChecked()) {
            commonComponents.updateLastVisit();
        }
    }

    private void listenAndAdjustOnResize() {
        UI.getCurrent().getPage().addBrowserWindowResizeListener(event -> {
            if (event.getWidth() < 1200) {
                super.getStyle().set("margin-left", "0");
            } else {
                super.getStyle().set("margin-left", "10em");
            }
        });
    }


    private void checkIfSmallScreenAndAdjustStyle() {
        if (!commonComponents.isMobile()) {
            UI.getCurrent().getPage().retrieveExtendedClientDetails(details -> {
                if (details.getBodyClientWidth() < 1200) {
                    super.getStyle().set("margin-left", "0");
                } else {
                    super.getStyle().set("margin-left", "10em");
                }
            });
        } else {
            super.getStyle().set("margin-left", "0");
        }
    }
}
