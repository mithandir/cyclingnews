package ch.climbd.newsfeed.views;

import ch.climbd.newsfeed.controller.MongoController;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;


@Route("views")
@PageTitle("Climbd Cycling News - Most Popular News")
public class MostPopularView extends VerticalLayout {

    @Autowired
    private MongoController mongo;

    @Autowired
    private CommonSessionComponents commonSessionComponents;

    @Autowired
    private SearchComponent searchComponent;

    @Autowired
    private NewsItemComponent newsItemComponent;

    @Value("${baseurl}")
    private String baseUrl;

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

        VerticalLayout newsItems = newsItemComponent.createNewsItem(mongo.findAllOrderedByViews(commonSessionComponents.getSelectedLanguages()));

        var searchBar = searchComponent.createSearchBar(newsItems);
        add(searchBar);
        add(newsItems);
    }

}
