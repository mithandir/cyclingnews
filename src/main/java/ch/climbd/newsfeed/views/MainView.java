package ch.climbd.newsfeed.views;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route
@PageTitle("Climbd Cycling News - Latest News")
public class MainView extends Div implements BeforeEnterObserver {

    public MainView() {
        add("Nothing here");
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        beforeEnterEvent.rerouteTo(LatestView.class);
    }
}