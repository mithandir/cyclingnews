package ch.climbd.newsfeed.views.components;

import ch.climbd.newsfeed.controller.MongoController;
import ch.climbd.newsfeed.controller.scheduler.Filter;
import ch.climbd.newsfeed.data.NewsEntry;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class NewsItemComponent {

    @Autowired
    private Filter filter;

    @Autowired
    private CommonComponents commonComponents;

    @Autowired
    private CommonSessionComponents commonSessionComponents;

    @Autowired
    private MongoController mongo;

    public VerticalLayout createNewsItem(List<NewsEntry> items) {
        var verticalLayout = new VerticalLayout();
        var index = 0;
        for (var item : items) {
            index++;
            HorizontalLayout row = buildNewsItem(index, item);
            if (row == null) continue;
            verticalLayout.add(row);
        }

        return verticalLayout;
    }

    public HorizontalLayout buildNewsItem(int index, NewsEntry item) {
        if (filter.isSpam(item.getTitle())) {
            return null;
        }
        HorizontalLayout row = new HorizontalLayout();
        row.setAlignItems(FlexComponent.Alignment.CENTER);

        Avatar avatar = commonComponents.buildSiteIcon(item.getDomainWithProtocol(), item.getDomainOnly());

        HorizontalLayout rowTitle = new HorizontalLayout();
        commonComponents.isItemUnRead(item.getPublishedDateTime(), rowTitle, avatar);

        // build dialog
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(item.getTitle());

        Button closeButton = new Button("Close", e -> dialog.close());
        dialog.getFooter().add(closeButton);

        Anchor title = new Anchor(commonComponents.createLinkWithStats(item.getLink()), item.getTitle(), AnchorTarget.BLANK);
        if (commonComponents.isMobile()) {
            rowTitle.add(title, dialog);
        } else {
            Span source = new Span("(" + item.getDomainOnly() + ")");
            rowTitle.add(title, source, dialog);
        }

        HorizontalLayout rowDateAndLinks = new HorizontalLayout();
        rowDateAndLinks.setAlignItems(FlexComponent.Alignment.CENTER);
        Span date = new Span(item.getPublishedDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE));
        date.getStyle().set("font-size", "small");


        var voteSum = new Span(String.valueOf(item.getVotes()));
        voteSum.getStyle().set("font-size", "small");

        var viewSum = new Span(String.valueOf(item.getViews()));
        viewSum.getStyle().set("font-size", "small");

        var viewIcon = VaadinIcon.BAR_CHART.create();
        viewIcon.setSize("15px");
        viewIcon.setTooltipText("Views");

        Icon vote = VaadinIcon.THUMBS_UP.create();
        vote.setSize("15px");
        vote.setTooltipText("Likes");
        vote.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> handleVotes(item, voteSum, vote));

        Icon viewContent = VaadinIcon.SEARCH.create();
        viewContent.setSize("15px");
        viewContent.addClickListener(e -> dialog.open());

        commonComponents.checkIconStatus(vote, item.getLink());

        if (commonSessionComponents.isAdmin()) {
            rowDateAndLinks.add(date, viewSum, viewIcon, voteSum, vote);
        } else {
            rowDateAndLinks.add(date, voteSum, vote);
        }

        VerticalLayout column = new VerticalLayout();
        column.add(rowTitle, rowDateAndLinks);
        column.setSpacing(false);

        row.add(new Span(String.valueOf(index)), avatar, column);
        return row;
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
