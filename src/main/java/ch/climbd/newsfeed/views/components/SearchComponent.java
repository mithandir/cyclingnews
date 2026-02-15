package ch.climbd.newsfeed.views.components;

import ch.climbd.newsfeed.controller.MongoController;
import ch.climbd.newsfeed.data.NewsEntry;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SearchComponent {

    @Autowired
    private MongoController mongoController;

    @Autowired
    private NewsItemComponent newsItemComponent;

    @Autowired
    private CommonSessionComponents commonSessionComponents;

    public VerticalLayout createSearchBar(VerticalLayout newsItems) {
        TextField textField = new TextField();
        textField.setPlaceholder("Search");
        textField.setPrefixComponent(VaadinIcon.SEARCH.create());
        textField.setClearButtonVisible(true);
        textField.addClassName("search-input");

        var searchButton = new Button("Search");
        searchButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        var clearButton = new Button("Clear");
        clearButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        clearButton.setVisible(false);

        textField.addKeyPressListener(Key.ENTER, event -> searchEventHandler(newsItems, textField, clearButton));

        searchButton.addClickListener(event -> searchEventHandler(newsItems, textField, clearButton));

        clearButton.addClickListener(event -> clearEventHandler(newsItems, textField, clearButton));

        var searchbar = new HorizontalLayout(textField, searchButton, clearButton);
        searchbar.addClassName("search-bar");
        searchbar.setWidthFull();

        var verticalLayout = new VerticalLayout(searchbar);
        verticalLayout.addClassName("search-shell");
        verticalLayout.setWidthFull();

        return verticalLayout;
    }

    private void clearEventHandler(VerticalLayout newsItems, TextField textField, Button clearButton) {
        textField.setValue("");
        clearButton.setVisible(false);

        newsItems.getUI().ifPresent(ui -> ui.getPage().fetchCurrentURL(currentUrl -> {
            String route = currentUrl.getPath() == null ? "" : currentUrl.getPath();
            if (route.startsWith("/")) {
                route = route.substring(1);
            }
            final String targetRoute = route;
            ui.access(() -> ui.navigate(targetRoute));
        }));
    }

    private void searchEventHandler(VerticalLayout newsItems, TextField textField, Button clearButton) {
        var searchValue = textField.getValue().strip();
        if (searchValue.length() >= 3) {
            List<NewsEntry> newsEntries = mongoController.searchEntries(searchValue, commonSessionComponents.getSelectedLanguages());
            newsItems.getUI().get().access(() -> {
                newsItems.removeAll();
                newsItems.add(newsItemComponent.createNewsItem(newsEntries));
                clearButton.setVisible(true);
            });
        }
    }
}
