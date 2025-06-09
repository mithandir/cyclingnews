package ch.climbd.newsfeed.views;

import ch.climbd.newsfeed.controller.MongoController;
import ch.climbd.newsfeed.data.NewsEntry;
import ch.climbd.newsfeed.views.components.CommonComponents;
import ch.climbd.newsfeed.views.components.CommonSessionComponents;
import ch.climbd.newsfeed.views.components.NewsItemComponent;
import ch.climbd.newsfeed.views.components.SearchComponent;
import ch.climbd.newsfeed.data.NewsEntry;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;

@Route("")
@PageTitle("Climbd Cycling News - Latest News")
public class LatestView extends VerticalLayout {

    private int currentPage = 0;
    private int pageSize = 20; // Default page size

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

    private Page<NewsEntry> sourceData;
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

        // Fetch initial page
        sourceData = mongo.findAllOrderedByDate(commonSessionComponents.getSelectedLanguages(), currentPage, pageSize);
        newsItems = newsItemComponent.createNewsItem(sourceData.getContent());

        var searchBar = searchComponent.createSearchBar(newsItems); // Assuming search might need adjustments later
        add(searchBar);
        add(newsItems);

        // Add pagination controls
        paginationControls = createPaginationControls();
        add(paginationControls);
        updatePaginationState(); // Initial state update

        if (commonSessionComponents.isAdminChecked()) {
            commonComponents.updateLastVisit();
        }
    }

    private HorizontalLayout paginationControls; // Field to hold pagination controls
    private Button prevButton;
    private Button nextButton;
    private Label paginationLabel;

    private HorizontalLayout createPaginationControls() {
        prevButton = new Button("Previous");
        prevButton.addClickListener(e -> {
            if (currentPage > 0) {
                currentPage--;
                refreshNewsItems();
            }
        });

        nextButton = new Button("Next");
        nextButton.addClickListener(e -> {
            if (currentPage < sourceData.getTotalPages() - 1) {
                currentPage++;
                refreshNewsItems();
            }
        });

        paginationLabel = new Label(); // Text will be set in updatePaginationState

        HorizontalLayout layout = new HorizontalLayout(prevButton, paginationLabel, nextButton);
        layout.setAlignItems(Alignment.CENTER);
        return layout;
    }

    private void refreshNewsItems() {
        // Fetch the new page data
        sourceData = mongo.findAllOrderedByDate(commonSessionComponents.getSelectedLanguages(), currentPage, pageSize);

        // Clear existing items
        newsItems.removeAll();

        // Add new items. Assuming createNewsItem returns a layout/component whose children can be added.
        // If createNewsItem modifies newsItems directly or returns a single component to be added, this needs adjustment.
        newsItemComponent.createNewsItem(sourceData.getContent()).getChildren()
                .forEach(newsItems::add);

        // Update pagination controls state
        updatePaginationState();
    }

    private void updatePaginationState() {
        if (sourceData != null) {
            paginationLabel.setText("Page " + (currentPage + 1) + " of " + sourceData.getTotalPages());
            prevButton.setEnabled(!sourceData.isFirst());
            nextButton.setEnabled(!sourceData.isLast());
        } else {
            // Handle case where sourceData might be null initially or after an error
            paginationLabel.setText("Page 0 of 0");
            prevButton.setEnabled(false);
            nextButton.setEnabled(false);
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
