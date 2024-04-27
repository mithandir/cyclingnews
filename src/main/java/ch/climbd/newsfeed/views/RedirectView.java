package ch.climbd.newsfeed.views;

import ch.climbd.newsfeed.controller.MongoController;
import ch.climbd.newsfeed.views.components.CommonComponents;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

@Route("redirect")
@PageTitle("Climbd Cycling News - Add your news")
public class RedirectView extends Div
        implements HasUrlParameter<String> {

    @Autowired
    private CommonComponents commonComponents;

    @Autowired
    private MongoController mongoController;

    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter String parameter) {

        Location location = event.getLocation();
        QueryParameters queryParameters = location.getQueryParameters();

        Map<String, List<String>> parametersMap = queryParameters.getParameters();
        var url = parametersMap.get("url").getFirst();

        mongoController.increaseViews(url);

        UI.getCurrent().getPage().executeJs("location.href = '" + url + "';");
    }
}