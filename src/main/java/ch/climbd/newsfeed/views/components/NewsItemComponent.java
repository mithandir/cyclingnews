package ch.climbd.newsfeed.views.components;

import ch.climbd.newsfeed.controller.MongoController;
import ch.climbd.newsfeed.controller.scheduler.Filter;
import ch.climbd.newsfeed.data.NewsEntry;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class NewsItemComponent {

    private static final Logger LOG = LoggerFactory.getLogger(NewsItemComponent.class);

    @Autowired
    private Filter filter;

    @Autowired
    private CommonComponents commonComponents;

    @Autowired
    private CommonSessionComponents commonSessionComponents;

    @Autowired
    private MongoController mongo;

    public VerticalLayout createNewsItem(List<NewsEntry> items) {
        commonSessionComponents.setFocusKeyIndex(0);
        var verticalLayout = new VerticalLayout();
        var index = 0;
        for (var item : items) {
            if (item.isDeleted()) {
                continue;
            }
            index++;
            var row = buildNewsItem(index, item, verticalLayout);
            if (row == null) continue;
            verticalLayout.add(row);

        }
        if (!commonSessionComponents.getRegistration().isEmpty()) {
            commonSessionComponents.getRegistration().forEach(ShortcutRegistration::remove);
            commonSessionComponents.getRegistration().clear();
        }

        commonSessionComponents.getRegistration().add(UI.getCurrent().addShortcutListener(
                () -> handleKeyEvents(verticalLayout, true), Key.KEY_J));

        commonSessionComponents.getRegistration().add(UI.getCurrent().addShortcutListener(
                () -> handleKeyEvents(verticalLayout, false), Key.KEY_K));

        return verticalLayout;
    }

    public VerticalLayout buildNewsItem(int index, NewsEntry item, VerticalLayout sourceLayout) {
        if (filter.isSpam(item.getTitle())) {
            return null;
        }

        VerticalLayout cardLayout = new VerticalLayout();
        cardLayout.getStyle().set("border", "1px solid #e0e0e0");
        cardLayout.getStyle().set("border-radius", "8px");
        cardLayout.getStyle().set("padding", "16px");
        cardLayout.getStyle().set("margin-bottom", "16px");
        cardLayout.getStyle().set("margin-right", "10%");
        cardLayout.setWidth("90%");
        cardLayout.setSpacing(false); // Ensure no default spacing from VerticalLayout itself
        // cardLayout.setPadding(true); // Padding is set via direct style "padding: 16px"

        HorizontalLayout row = new HorizontalLayout();
        // Align items to the top for better alignment of index, avatar, and text column
        row.setAlignItems(FlexComponent.Alignment.START); 
        row.setSpacing(true); // Add spacing between index, avatar, and column

        Div avatarDiv = new Div();
        // Add margin to the right of the avatar for spacing from the text column
        avatarDiv.getStyle().set("margin-right", "var(--lumo-space-s)"); 
        Avatar avatar = commonComponents.buildSiteIcon(item.getDomainWithProtocol(), item.getDomainOnly());
        avatarDiv.add(avatar);
        avatarDiv.addClickListener(e -> UI.getCurrent().access(() -> {
            sourceLayout.removeAll();
            List<NewsEntry> newsEntries = mongo.findAllFilterdBySite(item.getDomainWithProtocol());
            sourceLayout.add(createNewsItem(newsEntries));
        }));

        HorizontalLayout rowTitle = new HorizontalLayout();
        // Align items on baseline for better visual consistency of text
        rowTitle.setAlignItems(FlexComponent.Alignment.BASELINE);
        //TODO rowTitle.setGap("var(--lumo-space-s)"); // Add gap between title and source
        commonComponents.isItemUnRead(item.getPublishedDateTime(), rowTitle, avatar);

        Anchor title = new Anchor(commonComponents.createLinkWithStats(item.getLink()), item.getTitle(), AnchorTarget.BLANK);
        title.getStyle().set("word-break", "break-word"); // Ensure title wraps if too long
        if (commonComponents.isMobile()) {
            rowTitle.add(title);
        } else {
            Span source = new Span("(" + item.getDomainOnly() + ")");
            source.getStyle().set("flex-shrink", "0"); // Prevent source from shrinking
            rowTitle.add(title, source);
        }

        HorizontalLayout rowDateAndLinks = new HorizontalLayout();
        rowDateAndLinks.setAlignItems(FlexComponent.Alignment.CENTER);
        //TODO rowDateAndLinks.setGap("var(--lumo-space-s)"); // Add gaps between date/icon items

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

        Icon delete = VaadinIcon.TRASH.create();
        delete.setSize("15px");
        delete.setTooltipText("Delete");
        delete.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
            LOG.info("Delete: {}", item.getTitle());
            item.delete();
            mongo.update(item);
            UI.getCurrent().getPage().reload();
        });

        commonComponents.checkIconStatus(vote, item.getLink());

        if (commonSessionComponents.isAdmin()) {
            rowDateAndLinks.add(date, viewSum, viewIcon, voteSum, vote, delete);
        } else {
            rowDateAndLinks.add(date, voteSum, vote);
        }

        VerticalLayout column = new VerticalLayout();
        column.add(rowTitle, rowDateAndLinks);
        //TODO column.setGap("var(--lumo-space-xs)"); // Small gap between title row and date/links row
        column.setSpacing(false); // Explicitly false, gap is used instead
        column.setPadding(false); // No padding for the column itself

        row.add(new Span(String.valueOf(index)), avatarDiv, column);
        cardLayout.add(row);

        // Base styles for animation
        String transitionStyle = "max-height 0.5s ease-in-out";
        String collapsedHeight = "5em"; // Example for a few lines
        String expandedHeight = "1000px"; // Increased height for full content

        // Store both content versions and apply styles
        Html excerptContent = formatHtml(item, true);
        excerptContent.getStyle().set("margin-top", "10px");
        excerptContent.getStyle().set("overflow", "hidden");
        excerptContent.getStyle().set("transition", transitionStyle);
        excerptContent.getStyle().set("box-sizing", "border-box");
        excerptContent.getStyle().set("max-height", collapsedHeight); // Initial state: collapsed

        Html fullContent = formatHtml(item, false);
        fullContent.getStyle().set("margin-top", "10px");
        fullContent.getStyle().set("overflow-y", "auto"); // Allow vertical scroll on fullContent
        fullContent.getStyle().set("transition", transitionStyle);
        fullContent.getStyle().set("box-sizing", "border-box");
        fullContent.getStyle().set("max-height", "0px"); // Initial state: hidden collapsed

        // Initial display & state
        cardLayout.getElement().setProperty("isExpanded", false);
        if (item.getContent() != null && !item.getContent().isBlank()) {
            cardLayout.add(excerptContent);
        }

        // "Read more..." / "Show less..." indicator
        Span expandIndicator = new Span("Read more...");
        expandIndicator.getStyle().set("cursor", "pointer");
        expandIndicator.getStyle().set("color", "var(--lumo-primary-text-color)");
        expandIndicator.getStyle().set("font-size", "var(--lumo-font-size-s)");
        expandIndicator.getStyle().set("margin-top", "var(--lumo-space-s)");
        cardLayout.add(expandIndicator); // Add it to the layout

        boolean isContentDifferent = !fullContent.getInnerHtml().equals(excerptContent.getInnerHtml());
        expandIndicator.setVisible(isContentDifferent && item.getContent() != null && !item.getContent().isBlank());

        // Click listener on card
        cardLayout.addClickListener(event -> {
            boolean isExpanded = cardLayout.getElement().getProperty("isExpanded", false);
            if (item.getContent() != null && !item.getContent().isBlank() && isContentDifferent) {
                if (!isExpanded) {
                    excerptContent.getStyle().set("max-height", "0px"); // Collapse current
                    fullContent.getStyle().set("max-height", expandedHeight); // Expand new
                    cardLayout.replace(excerptContent, fullContent);
                    cardLayout.getElement().setProperty("isExpanded", true);
                    expandIndicator.setText("Show less...");
                } else {
                    fullContent.getStyle().set("max-height", "0px"); // Collapse current
                    excerptContent.getStyle().set("max-height", collapsedHeight); // Expand new
                    cardLayout.replace(fullContent, excerptContent);
                    cardLayout.getElement().setProperty("isExpanded", false);
                    expandIndicator.setText("Read more...");
                }
            }
        });

        return cardLayout;
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

    private Html formatHtml(NewsEntry item, boolean excerpt) {
        var str = item.getContent();

        if (excerpt) {
            str = createExcerpt(str);
        } else {
            str = !item.getSummary().isBlank() ? item.getSummary() : str;
        }

        return createHtmlElement(str);
    }

    private String createExcerpt(String str) {
        str = str.replaceAll("<br>", " ");
        str = str.substring(0, Math.min(str.length(), commonComponents.isMobile() ? 25 : 100)) + "...";
        return str;
    }

    private Html createHtmlElement(String str) {
        var html = new Html("<div>" + str + "</div>");
        html.getStyle().set("text-wrap", "wrap");
        html.getStyle().set("text-align", "justify");
        html.getStyle().set("font-size", "small");
        return html;
    }

    private void handleKeyEvents(VerticalLayout verticalLayout, boolean goDown) {
        commonSessionComponents.setFocusCurrentIndex(0);

        if (!goDown) {
            if (commonSessionComponents.getFocusKeyIndex() == 0) {
                return;
            }
            commonSessionComponents.setFocusKeyIndex(commonSessionComponents.getFocusKeyIndex() - 1);
        }

        verticalLayout.getChildren().forEach(component -> {
            if (component instanceof VerticalLayout) {
                if (commonSessionComponents.getFocusCurrentIndex() == commonSessionComponents.getFocusKeyIndex()) {
                    component.scrollIntoView();
                }
                commonSessionComponents.setFocusCurrentIndex(commonSessionComponents.getFocusCurrentIndex() + 1);
            }
        });

        if (goDown) {
            var sizeNewsItems = verticalLayout.getChildren()
                    .filter(component -> component instanceof VerticalLayout)
                    .count();

            if (commonSessionComponents.getFocusKeyIndex() == sizeNewsItems) {
                return;
            }
            commonSessionComponents.setFocusKeyIndex(commonSessionComponents.getFocusKeyIndex() + 1);
        }
    }
}
