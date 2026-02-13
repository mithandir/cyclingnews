package ch.climbd.newsfeed.views.components;

import ch.climbd.newsfeed.controller.MongoController;
import ch.climbd.newsfeed.controller.scheduler.Filter;
import ch.climbd.newsfeed.data.NewsEntry;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.vaadin.flow.component.ComponentUtil.fireEvent;

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
        // Start before the first item so the first "j" selects index 0.
        commonSessionComponents.setFocusKeyIndex(-1);
        var verticalLayout = new VerticalLayout();
        verticalLayout.addClassName("news-list");
        verticalLayout.setPadding(false);
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

        renderMobileNavigation(verticalLayout);

        return verticalLayout;
    }

    private void renderMobileNavigation(VerticalLayout verticalLayout) {
        if (commonComponents.isMobile()) {
            HorizontalLayout mobileNavBar = new HorizontalLayout();
            var nextBtn = new Button(new Icon(VaadinIcon.ARROW_DOWN));
            nextBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            nextBtn.addClickListener(event -> {
                commonSessionComponents.setFocusCurrentIndex(0);
                var sizeNewsItems = verticalLayout.getChildren()
                        .filter(component -> component instanceof VerticalLayout)
                        .count();

                if (commonSessionComponents.getFocusKeyIndex() == sizeNewsItems) {
                    return;
                }
                commonSessionComponents.setFocusKeyIndex(commonSessionComponents.getFocusKeyIndex() + 1);

                verticalLayout.getChildren().forEach(component -> {
                    if (component instanceof VerticalLayout) {
                        boolean isExpanded = component.getElement().getProperty("isExpanded", false);

                        if (commonSessionComponents.getFocusCurrentIndex() == commonSessionComponents.getFocusKeyIndex()) {
                            component.scrollIntoView();

                            if (!isExpanded) {
                                fireEvent(component, new ClickEvent<VerticalLayout>(component));
                            }
                        } else if (isExpanded) {
                            fireEvent(component, new ClickEvent<VerticalLayout>(component));
                        }
                        commonSessionComponents.setFocusCurrentIndex(commonSessionComponents.getFocusCurrentIndex() + 1);
                    }
                });
            });
            var prevBtn = new Button(new Icon(VaadinIcon.ARROW_UP));
            prevBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            prevBtn.addClickListener(event -> {
                commonSessionComponents.setFocusCurrentIndex(0);
                var sizeNewsItems = verticalLayout.getChildren()
                        .filter(component -> component instanceof VerticalLayout)
                        .count();

                if (commonSessionComponents.getFocusKeyIndex() <= 0) {
                    return;
                }
                commonSessionComponents.setFocusKeyIndex(commonSessionComponents.getFocusKeyIndex() - 1);

                verticalLayout.getChildren().forEach(component -> {
                    if (component instanceof VerticalLayout) {
                        boolean isExpanded = component.getElement().getProperty("isExpanded", false);

                        if (commonSessionComponents.getFocusCurrentIndex() == commonSessionComponents.getFocusKeyIndex()) {
                            component.scrollIntoView();

                            if (!isExpanded) {
                                fireEvent(component, new ClickEvent<VerticalLayout>(component));
                            }
                        } else if (isExpanded) {
                            fireEvent(component, new ClickEvent<VerticalLayout>(component));
                        }
                        commonSessionComponents.setFocusCurrentIndex(commonSessionComponents.getFocusCurrentIndex() + 1);
                    }
                });
            });

            mobileNavBar.add(nextBtn, prevBtn);
            mobileNavBar.addClassName("mobile-nav");

            verticalLayout.add(mobileNavBar);
        }
    }

    public VerticalLayout buildNewsItem(int index, NewsEntry item, VerticalLayout sourceLayout) {
        if (filter.isSpam(item.getTitle())) {
            return null;
        }

        VerticalLayout cardLayout = new VerticalLayout();
        cardLayout.addClassName("news-card");
        cardLayout.setWidthFull();
        cardLayout.setSpacing(false);
        cardLayout.setPadding(false);

        HorizontalLayout row = new HorizontalLayout();
        row.addClassName("news-row");
        row.setAlignItems(FlexComponent.Alignment.START);
        row.setSpacing(true);

        Div avatarDiv = new Div();
        avatarDiv.addClassName("news-avatar");
        Avatar avatar = commonComponents.buildSiteIcon(item.getDomainWithProtocol(), item.getDomainOnly());
        avatarDiv.add(avatar);
        avatarDiv.addClickListener(e -> UI.getCurrent().access(() -> {
            sourceLayout.removeAll();
            List<NewsEntry> newsEntries = mongo.findAllFilterdBySite(item.getDomainWithProtocol());
            sourceLayout.add(createNewsItem(newsEntries));
        }));

        HorizontalLayout rowTitle = new HorizontalLayout();
        rowTitle.setAlignItems(FlexComponent.Alignment.BASELINE);
        rowTitle.addClassName("news-title");
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
        rowDateAndLinks.addClassName("news-meta");

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
        column.addClassName("news-column");
        column.setSpacing(false);
        column.setPadding(false);

        if (commonComponents.isMobile()) {
            row.add(new Span(String.valueOf(index)), column);
        } else {
            row.add(new Span(String.valueOf(index)), avatarDiv, column);
        }
        cardLayout.add(row);

        // Base styles for animation
        String transitionStyle = "max-height 0.5s ease-in-out";
        String collapsedHeight = "5em"; // Example for a few lines
        String expandedHeight = "1000px"; // Increased height for full content

        // Store both content versions and apply styles
        Html excerptContent = formatHtml(item, true);
        excerptContent.getStyle().set("overflow", "hidden");
        excerptContent.getStyle().set("transition", transitionStyle);
        excerptContent.getStyle().set("box-sizing", "border-box");
        excerptContent.getStyle().set("max-height", collapsedHeight); // Initial state: collapsed
        excerptContent.addClassName("news-content");

        Html fullContent = formatHtml(item, false);
        fullContent.getStyle().set("overflow-y", "auto"); // Allow vertical scroll on fullContent
        fullContent.getStyle().set("transition", transitionStyle);
        fullContent.getStyle().set("box-sizing", "border-box");
        fullContent.getStyle().set("max-height", "0px"); // Initial state: hidden collapsed
        fullContent.addClassName("news-content");

        // Initial display & state
        cardLayout.getElement().setProperty("isExpanded", false);
        if (item.getContent() != null && !item.getContent().isBlank()) {
            cardLayout.add(excerptContent);
        }

        // "Read more..." / "Show less..." indicator
        Span expandIndicator = new Span("Read more...");
        expandIndicator.addClassName("news-expand");
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

        str = sanitizeHtml(str);

        // Remove all <div> and </div> tags to avoid nested/multiple top-level elements
        str = str.replaceAll("(?i)</?div>", "");
        // Remove all <p> and </p> tags, replace with <br> for line breaks
        str = str.replaceAll("(?i)</p>", "<br>");
        str = str.replaceAll("(?i)<p>", "");
        // Remove leading/trailing whitespace and <br>
        str = str.trim().replaceAll("^(<br>)+", "").replaceAll("(<br>)+$", "");

        return createHtmlElement(str);
    }

    private String createExcerpt(String str) {
        if (str == null || str.isBlank()) {
            return "";
        }
        // Remove <br> for excerpt length calculation
        str = str.replaceAll("(?i)<br>", " ");
        str = str.substring(0, Math.min(str.length(), commonComponents.isMobile() ? 25 : 100)) + "...";
        return str;
    }

    private String sanitizeHtml(String str) {
        if (str == null || str.isBlank()) {
            return "";
        }
        return Jsoup.clean(str, Safelist.basic().addTags("br"));
    }

    private Html createHtmlElement(String str) {
        // Always wrap in a single <div> to ensure only one top-level element
        var html = new Html("<div>" + str + "</div>");
        return html;
    }

    private void handleKeyEvents(VerticalLayout verticalLayout, boolean goDown) {
        commonSessionComponents.setFocusCurrentIndex(0);

        if (!goDown) {
            if (commonSessionComponents.getFocusKeyIndex() <= 0) {
                return;
            }
            commonSessionComponents.setFocusKeyIndex(commonSessionComponents.getFocusKeyIndex() - 1);
        }

        if (goDown) {
            var sizeNewsItems = verticalLayout.getChildren()
                    .filter(component -> component instanceof VerticalLayout)
                    .count();

            if (commonSessionComponents.getFocusKeyIndex() == sizeNewsItems) {
                return;
            }
            commonSessionComponents.setFocusKeyIndex(commonSessionComponents.getFocusKeyIndex() + 1);
        }

        verticalLayout.getChildren().forEach(component -> {
            if (component instanceof VerticalLayout) {
                boolean isExpanded = component.getElement().getProperty("isExpanded", false);

                if (commonSessionComponents.getFocusCurrentIndex() == commonSessionComponents.getFocusKeyIndex()) {
                    component.scrollIntoView();
                    if (!isExpanded) {
                        fireEvent(component, new ClickEvent<VerticalLayout>(component));
                    }
                } else if (isExpanded) {
                    fireEvent(component, new ClickEvent<VerticalLayout>(component));
                }
                commonSessionComponents.setFocusCurrentIndex(commonSessionComponents.getFocusCurrentIndex() + 1);
            }
        });

    }
}
