package ch.climbd.newsfeed.views;

import ch.climbd.newsfeed.controller.MongoController;
import ch.climbd.newsfeed.data.NewsEntry;
import ch.climbd.newsfeed.scheduler.Filter;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Route("latest")
@PageTitle("Climbd Cycling News - Latest News")
public class LatestView extends VerticalLayout {

    @Autowired
    private MongoController mongo;

    @Autowired
    private CommonComponents commonComponents;

    @Autowired
    private CommonSessionComponents commonSessionComponents;

    @Autowired
    private Filter filter;

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

        VerticalLayout newsItems = createNewsItem(mongo.findAllOrderedByDate(commonSessionComponents.getSelectedLanguages()).collectList().block());
        newsItems.setWidthFull();
        newsItems.getStyle().set("margin-left", commonComponents.isMobile() ? "2%" : "10%");
        add(newsItems);
    }

    private VerticalLayout createNewsItem(List<NewsEntry> items) {
        var verticalLayout = new VerticalLayout();
        var index = 0;
        for (var item : items) {
            index++;
            if (filter.isSpam(item.getTitle())) {
                continue;
            }
            HorizontalLayout row = new HorizontalLayout();
            row.setAlignItems(Alignment.CENTER);

            Avatar avatar = commonComponents.buildSiteIcon(item.getDomainWithProtocol(), item.getDomainOnly());

            HorizontalLayout rowTitle = new HorizontalLayout();
            Anchor title = new Anchor(item.getLink(), item.getTitle(), AnchorTarget.BLANK);
            if (commonComponents.isMobile()) {
                rowTitle.add(title);
            } else {
                Span source = new Span("(" + item.getDomainOnly() + ")");
                rowTitle.add(title, source);
            }

            HorizontalLayout rowDateAndLinks = new HorizontalLayout();
            rowDateAndLinks.setAlignItems(Alignment.CENTER);
            Span date = new Span(item.getPublishedDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE));
            date.getStyle().set("font-size", "small");

            Span voteSum = new Span(String.valueOf(item.getVotes()));
            voteSum.getStyle().set("font-size", "small");

            Icon vote = VaadinIcon.THUMBS_UP.create();
            vote.setSize("15px");
            vote.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> handleVotes(item, voteSum, vote));
            commonComponents.checkIcon(vote, item.getLink());

            rowDateAndLinks.add(date, voteSum, vote);


            VerticalLayout column = new VerticalLayout();
            column.add(rowTitle, rowDateAndLinks);
            column.setSpacing(false);

            row.add(new Span(String.valueOf(index)), avatar, column);
            verticalLayout.add(row);
        }

        return verticalLayout;
    }

    private void handleVotes(NewsEntry item, Span voteSum, Icon vote) {
        if (vote.getColor() == null) {
            vote.setColor("green");
            mongo.increaseVote(item);
            item.setVotes(item.getVotes() + 1);
            commonComponents.writeLocalStorage(item.getLink(), "true");
        } else {
            vote.setColor(null);
            mongo.decreaseVote(item);
            item.setVotes(item.getVotes() - 1);
            commonComponents.writeLocalStorage(item.getLink(), "false");
        }
        voteSum.setText(String.valueOf(item.getVotes()));
    }
}