package ch.climbd.newsfeed.views.components;

import ch.climbd.newsfeed.controller.MongoController;
import ch.climbd.newsfeed.data.NewsEntry;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class SearchComponent {

    @Autowired
    private MongoController mongoController;

    @Autowired
    private NewsItemComponent newsItemComponent;

    public VerticalLayout createSearchBar(VerticalLayout newsItems) {
        TextField textField = new TextField();
        textField.setPlaceholder("Search");
        textField.setPrefixComponent(VaadinIcon.SEARCH.create());
        textField.setClearButtonVisible(true);

        var searchButton = new Button("Search");

        searchButton.addClickListener(event -> {
            if (!textField.getValue().isEmpty()) {
                Flux<NewsEntry> newsEntries = mongoController.searchEntries(textField.getValue());
                newsItems.getUI().get().access(() -> {
                    newsItems.removeAll();
                    newsItems.add(newsItemComponent.createNewsItem(newsEntries.collectList().block()));
                });
            }
        });

        var searchbar = new HorizontalLayout(textField, searchButton);
        return new VerticalLayout(searchbar);
    }
}
