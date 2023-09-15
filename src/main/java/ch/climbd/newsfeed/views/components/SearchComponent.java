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

        var searchButton = new Button("Search");
        var clearButton = new Button("Clear");
        clearButton.setVisible(false);


        searchButton.addClickListener(event -> {
            var searchValue = textField.getValue().strip();
            if (searchValue.length() >= 3) {
                List<NewsEntry> newsEntries = mongoController.searchEntries(searchValue);
                newsItems.getUI().get().access(() -> {
                    newsItems.removeAll();
                    newsItems.add(newsItemComponent.createNewsItem(newsEntries));
                    clearButton.setVisible(true);
                });
            }
        });

        clearButton.addClickListener(event -> {
            textField.setValue("");
            clearButton.setVisible(false);

            var ui = newsItems.getUI().get();

            ui.getPage().fetchCurrentURL(currentUrl -> {
                var path = currentUrl.getPath().split("/");
                var page = path[path.length - 1];

                ui.access(() -> {
                    newsItems.removeAll();
                    List<NewsEntry> newsEntries;

                    if ("views".equals(page)) {
                        newsEntries = mongoController.findAllOrderedByViews(commonSessionComponents.getSelectedLanguages());
                    } else if ("liked".equals(page)) {
                        newsEntries = mongoController.findAllOrderedByVotes(commonSessionComponents.getSelectedLanguages());
                    } else {
                        newsEntries = mongoController.findAllOrderedByDate(commonSessionComponents.getSelectedLanguages());
                    }

                    newsItems.add(newsItemComponent.createNewsItem(newsEntries));
                });
            });


        });

        var searchbar = new HorizontalLayout(textField, searchButton, clearButton);
        return new VerticalLayout(searchbar);
    }
}
