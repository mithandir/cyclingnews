package ch.climbd.newsfeed.views;

import ch.climbd.newsfeed.controller.MongoController;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

@Route("redirect")
@PageTitle("Climbd Cycling News - Add your news")
public class RedirectView extends Div
        implements HasUrlParameter<String> {

    @Autowired
    private MongoController mongoController;

    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter String parameter) {

        Location location = event.getLocation();
        QueryParameters queryParameters = location.getQueryParameters();

        Map<String, List<String>> parametersMap = queryParameters.getParameters();
        var urls = parametersMap.get("url");
        if (urls == null || urls.isEmpty()) {
            UI.getCurrent().navigate("");
            return;
        }

        var url = urls.getFirst();
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            UI.getCurrent().navigate("");
            return;
        }

        var scheme = uri.getScheme();
        if (scheme == null || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
            UI.getCurrent().navigate("");
            return;
        }

        if (uri.getHost() == null || uri.getHost().isBlank()) {
            UI.getCurrent().navigate("");
            return;
        }

        mongoController.increaseViews(url);

        UI.getCurrent().getPage().setLocation(uri.toString());
    }
}
