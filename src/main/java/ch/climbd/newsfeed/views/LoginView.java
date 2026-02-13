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

        if (commonSessionComponents.isAdmin()) {
            add(new Span("Already logged in!"));
        } else {
            VerticalLayout form = buildForm();
            add(form);
        }
    }

    private VerticalLayout buildForm() {
        TextField apiKey = new TextField("API Key");
        apiKey.setWidthFull();
        apiKey.setClearButtonVisible(true);

        Button button = new Button("Save");
        button.setDisableOnClick(true);
        button.addClickListener(event -> clickSaveAction(apiKey));

        var form = new VerticalLayout(apiKey, button);
        form.addClassName("form-shell");
        form.setWidthFull();
        return form;
    }

    private void clickSaveAction(TextField apiKey) {
        if (!apiKey.getValue().isBlank()) {
            LOG.info("Login successful");
            commonComponents.writeLocalStorage("API-KEY", apiKey.getValue());
            commonSessionComponents.checkIsAdmin(true);
            UI.getCurrent().navigate("");
        } else {
            LOG.warn("Login attempt failed!");
        }
    }
}
