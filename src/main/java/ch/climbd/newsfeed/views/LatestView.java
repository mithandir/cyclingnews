package ch.climbd.newsfeed.views;

import ch.climbd.newsfeed.controller.MongoController;
import ch.climbd.newsfeed.data.NewsEntry;
import ch.climbd.newsfeed.views.components.CommonComponents;
import ch.climbd.newsfeed.views.components.CommonSessionComponents;
import ch.climbd.newsfeed.views.components.NewsItemComponent;
import ch.climbd.newsfeed.views.components.SearchComponent;
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
        addClassName("page-layout");
        setWidthFull();
        setPadding(false);

        var image = new Image(baseUrl + "/logo.svg", "Title");
        image.setWidth("8em");
        var heading = new H1("cycling news");
        heading.addClassName("app-title");
        var brand = new HorizontalLayout(image, heading);
        brand.addClassName("app-brand");
        brand.setAlignItems(Alignment.CENTER);

        var header = new HorizontalLayout(brand);
        header.addClassName("app-header");
        header.setWidthFull();

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
}
