package ch.climbd.newsfeed.views;

import ch.climbd.newsfeed.views.components.CommonComponents;
import ch.climbd.newsfeed.views.components.CommonSessionComponents;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;


@Route("login")
@PageTitle("Climbd Cycling News - Login")
public class LoginView extends VerticalLayout {
    private static final Logger LOG = LoggerFactory.getLogger(LoginView.class);

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
            add(new Span("Already logged in!"));
        } else {
            VerticalLayout form = buildForm();
            add(form);
        }
    }

    private VerticalLayout buildForm() {
        TextField apiKey = new TextField("API Key");
        apiKey.setWidth("22em");
        apiKey.setClearButtonVisible(true);
        apiKey.getStyle().set("margin-left", commonComponents.isMobile() ? "2%" : "10%");

        Button button = new Button("Save");
        button.setDisableOnClick(true);
        button.addClickListener(event -> clickSaveAction(apiKey));
        button.getStyle().set("margin-left", commonComponents.isMobile() ? "2%" : "10%");

        return new VerticalLayout(apiKey, button);
    }

    private void clickSaveAction(TextField apiKey) {
        if (!apiKey.getValue().isBlank()) {
            commonComponents.writeLocalStorage("API-KEY", apiKey.getValue());
            commonSessionComponents.checkIsAdmin(false);
            LOG.info("Login successful");

            UI.getCurrent().navigate("latest");
        } else {
            LOG.warn("Login attempt failed!");
        }
    }
}