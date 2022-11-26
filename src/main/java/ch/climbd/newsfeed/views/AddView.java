package ch.climbd.newsfeed.views;

import ch.climbd.newsfeed.controller.MongoController;
import ch.climbd.newsfeed.data.NewsEntry;
import ch.climbd.newsfeed.views.components.CommonComponents;
import ch.climbd.newsfeed.views.components.CommonSessionComponents;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.time.ZonedDateTime;

@Route("add-news")
@PageTitle("Climbd Cycling News - Add your news")
public class AddView extends VerticalLayout {

    @Autowired
    private MongoController mongo;

    @Autowired
    private CommonComponents commonComponents;

    @Autowired
    private CommonSessionComponents commonSessionComponents;

    @Value("${baseurl}")
    private String baseUrl;

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

        if (commonSessionComponents.isAdmin()) {
            VerticalLayout form = buildForm();
            add(form);
        } else {
            add(new Span("Not authorized"));
        }
    }

    private VerticalLayout buildForm() {
        VerticalLayout verticalLayout = new VerticalLayout();

        TextField title = new TextField("Title");
        title.setClearButtonVisible(true);
        TextField link = new TextField("Link");
        link.setClearButtonVisible(true);

        CheckboxGroup<String> languageGroup = new CheckboxGroup<>();
        languageGroup.setLabel("Language");
        languageGroup.setItems("English", "German");

        FormLayout formLayout = new FormLayout();
        handleFormLayout(title, link, languageGroup, formLayout);
        verticalLayout.add(formLayout);

        Button button = new Button("Save");
        button.setDisableOnClick(true);
        button.addClickListener(event -> clickSaveAction(title, link, languageGroup, button));
        button.getStyle().set("margin-left", commonComponents.isMobile() ? "2%" : "10%");

        verticalLayout.add(button);

        return verticalLayout;
    }

    private void clickSaveAction(TextField title, TextField link, CheckboxGroup<String> languageGroup, Button button) {
        setErrorMessages(title, link, languageGroup);

        if (!title.getValue().isBlank() && !link.getValue().isBlank() && languageGroup.getSelectedItems().size() == 1) {
            var newsEntry = new NewsEntry(title.getValue(), link.getValue(), ZonedDateTime.now());

            if (languageGroup.getSelectedItems().contains("German")) {
                newsEntry.setLanguage("de");
            } else {
                newsEntry.setLanguage("en");
            }

            mongo.save(newsEntry);
            title.clear();
            link.clear();
            languageGroup.clear();
        }

        button.setEnabled(true);
    }

    private void setErrorMessages(TextField title, TextField link, CheckboxGroup<String> languageGroup) {
        title.setInvalid(false);
        title.setHelperText(null);
        link.setInvalid(false);
        link.setHelperText(null);
        languageGroup.setInvalid(false);
        languageGroup.setHelperText(null);

        if (title.getValue().isBlank()) {
            title.setHelperText("Field should not be empty");
            title.setInvalid(true);
        }
        if (link.getValue().isBlank()) {
            link.setHelperText("Field should not be empty");
            link.setInvalid(true);
        }
        if (languageGroup.getSelectedItems().size() == 0) {
            languageGroup.setHelperText("Please select a language");
            languageGroup.setInvalid(true);
        }
        if (languageGroup.getSelectedItems().size() > 1) {
            languageGroup.setHelperText("Please select only 1 language");
            languageGroup.setInvalid(true);
        }
    }

    private void handleFormLayout(TextField title, TextField link, CheckboxGroup<String> languageGroup, FormLayout formLayout) {
        if (commonComponents.isMobile()) {
            formLayout.add(
                    title,
                    link,
                    languageGroup
            );
            formLayout.setColspan(title, 2);
            formLayout.setColspan(link, 2);
        } else {
            formLayout.add(
                    title,
                    languageGroup,
                    link
            );
            formLayout.setColspan(title, 1);
            formLayout.setColspan(link, 1);
        }

        formLayout.getStyle().set("margin-left", commonComponents.isMobile() ? "2%" : "10%");
        formLayout.getStyle().set("margin-right", commonComponents.isMobile() ? "2%" : "10%");
    }
}