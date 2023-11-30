package ch.climbd.newsfeed.views;

import ch.climbd.newsfeed.controller.MongoController;
import ch.climbd.newsfeed.data.NewsEntry;
import ch.climbd.newsfeed.views.components.*;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.LinkedList;
import java.util.concurrent.Executors;

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
        super.setWidthFull();
        super.getStyle().set("margin-left", commonComponents.isMobile() ? "0" : "20%");

        var image = new Image(baseUrl + "/logo.svg", "Title");
        image.setWidth("8em");
        var heading = new H1("cycling news");
        var header = new HorizontalLayout(image, heading);
        header.setWidthFull();
        header.setAlignItems(Alignment.CENTER);
        add(header);

        add(commonSessionComponents.createMenu());

        sourceData = new LinkedList<>(mongo.findAllOrderedByDate(commonSessionComponents.getSelectedLanguages()));
        newsItems = newsItemComponent.createNewsItem(sourceData);
        newsItems.setWidthFull();

        var searchBar = searchComponent.createSearchBar(newsItems);
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
            var notification = new Notification();
            var div = new Div(new Text("New story: "), new Anchor(newsEntry.getLink(), newsEntry.getTitle(), AnchorTarget.BLANK));

            Button closeButton = new Button(new Icon("lumo", "cross"));
            closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
            closeButton.setAriaLabel("Close");
            closeButton.addClickListener(event -> notification.close());

            HorizontalLayout layout = new HorizontalLayout(div, closeButton);
            layout.setAlignItems(Alignment.CENTER);

            notification.add(layout);
            notification.addThemeVariants(NotificationVariant.LUMO_PRIMARY);
            notification.setDuration(30000);
            notification.setPosition(Notification.Position.TOP_END);
            notification.open();

            Executors.newVirtualThreadPerTaskExecutor().execute(() -> {
                try {
                    Thread.sleep(10000);
                    ui.getPage().reload();
                } catch (Exception e) {
                    // DO NOTHING
                }
            });
        }));
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        broadcasterRegistration.remove();
        broadcasterRegistration = null;
    }
}