package ch.climbd.newsfeed.views;

import ch.climbd.newsfeed.controller.MongoController;
import ch.climbd.newsfeed.data.NewsEntry;
import ch.climbd.newsfeed.views.components.*;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.LinkedList;

@Route("latest")
@PageTitle("Climbd Cycling News - Latest News")
public class LatestView extends VerticalLayout {

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

    private Registration broadcasterRegistration;
    private LinkedList<NewsEntry> sourceData;
    private VerticalLayout newsItems;

    @PostConstruct
    public void init() {
        var image = new Image(baseUrl + "/logo.svg", "Title");
        image.setWidth("8em");
        image.getStyle().set("margin-left", commonComponents.isMobile() ? "2%" : "10%");
        var heading = new H1("cycling news");
        var header = new HorizontalLayout(image, heading);
        header.setWidthFull();
        header.setAlignItems(Alignment.CENTER);
        add(header);

        add(commonSessionComponents.createMenu());

        sourceData = new LinkedList<>(mongo.findAllOrderedByDate(commonSessionComponents.getSelectedLanguages()).collectList().block());
        newsItems = newsItemComponent.createNewsItem(sourceData);
        newsItems.setWidthFull();
        newsItems.getStyle().set("margin-left", commonComponents.isMobile() ? "2%" : "10%");

        var searchBar = searchComponent.createSearchBar(newsItems);
        searchBar.getStyle().set("margin-left", commonComponents.isMobile() ? "2%" : "10%");
        add(searchBar);
        add(newsItems);

        if (commonSessionComponents.isAdminChecked()) {
            commonComponents.updateLastVisit();
        }
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        UI ui = attachEvent.getUI();
        broadcasterRegistration = Broadcaster.register(newsEntry -> ui.access(() -> {
            sourceData.add(0, newsEntry);
            newsItems.removeAll();

            for (int i = 0; i < 100; i++) {
                newsItems.add(newsItemComponent.buildNewsItem(i + 1, sourceData.get(i)));
            }
            var notification = Notification.show(newsEntry.getTitle());
            notification.setDuration(15000);
            notification.setPosition(Notification.Position.TOP_END);
        }));
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        broadcasterRegistration.remove();
        broadcasterRegistration = null;
    }
}