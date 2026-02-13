package ch.climbd.newsfeed.views;

import ch.climbd.newsfeed.controller.MongoChangeStreamService;
import ch.climbd.newsfeed.controller.MongoController;
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

import java.util.concurrent.atomic.AtomicBoolean;

@Route("views")
@PageTitle("Climbd Cycling News - Most Popular News")
public class MostPopularView extends VerticalLayout {

    private static final Logger LOG = LoggerFactory.getLogger(MostPopularView.class);

    @Autowired
    private MongoController mongo;

    @Autowired
    private CommonSessionComponents commonSessionComponents;

    @Autowired
    private SearchComponent searchComponent;

    @Autowired
    private NewsItemComponent newsItemComponent;

    @Autowired
    private MongoChangeStreamService mongoChangeStreamService;

    @Value("${baseurl}")
    private String baseUrl;

    private VerticalLayout newsItemsContainer;
    private AutoCloseable mongoSubscription;
    private final AtomicBoolean refreshQueued = new AtomicBoolean(false);

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

        newsItemsContainer = new VerticalLayout();
        newsItemsContainer.setPadding(false);
        newsItemsContainer.setSpacing(false);
        newsItemsContainer.setWidthFull();
        refreshNewsItems();

        var searchBar = searchComponent.createSearchBar(newsItemsContainer);
        add(searchBar);
        add(newsItemsContainer);

        addAttachListener(attachEvent -> subscribeForRealtimeUpdates(attachEvent.getUI()));
        addDetachListener(detachEvent -> unsubscribeFromRealtimeUpdates());
    }

    private void refreshNewsItems() {
        newsItemsContainer.removeAll();
        newsItemsContainer.add(newsItemComponent.createNewsItem(
                mongo.findAllOrderedByViews(commonSessionComponents.getSelectedLanguages())
        ));
    }

    private void subscribeForRealtimeUpdates(UI ui) {
        unsubscribeFromRealtimeUpdates();
        mongoSubscription = mongoChangeStreamService.subscribe(event -> {
            if (!"__rss_batch__".equals(event.link()) && !"__language_change__".equals(event.link())) {
                return;
            }
            if (refreshQueued.compareAndSet(false, true)) {
                ui.access(() -> {
                    try {
                        if (ui.isAttached()) {
                            refreshNewsItems();
                        }
                    } finally {
                        refreshQueued.set(false);
                    }
                });
            }
        });
    }

    private void unsubscribeFromRealtimeUpdates() {
        if (mongoSubscription == null) {
            return;
        }

        try {
            mongoSubscription.close();
        } catch (Exception e) {
            LOG.debug("Failed to close MongoDB realtime subscription", e);
        } finally {
            mongoSubscription = null;
            refreshQueued.set(false);
        }
    }
}
