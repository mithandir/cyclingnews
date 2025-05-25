package ch.climbd.newsfeed.views;

import ch.climbd.newsfeed.views.components.CommonComponents;
import ch.climbd.newsfeed.views.components.CommonSessionComponents;
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


@Route("logout")
@PageTitle("Climbd Cycling News - Login")
public class LogoutView extends VerticalLayout {
    private static final Logger LOG = LoggerFactory.getLogger(LogoutView.class);

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
            commonComponents.deleteLocalStorage("API-KEY");
            commonSessionComponents.checkIsAdmin(true);
            LOG.info("Logout successful");

            UI.getCurrent().navigate("");
        } else {
            LOG.info("Logout: User was not logged in");
            UI.getCurrent().navigate("");
        }
    }
}