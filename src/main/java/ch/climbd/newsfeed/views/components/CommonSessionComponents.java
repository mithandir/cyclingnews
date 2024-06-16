package ch.climbd.newsfeed.views.components;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.contextmenu.HasMenuItems;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;
import org.vaadin.addon.browserstorage.LocalStorage;

import java.util.HashSet;
import java.util.Set;

@Component
@SessionScope
public class CommonSessionComponents {

    @Value("${api-key}")
    private String apiKey;

    @Autowired
    private CommonComponents commonComponents;

    private boolean isAdmin = false;
    private boolean adminChecked = false;
    private final Set<String> selectedLanguages = new HashSet<>();

    @PostConstruct
    public void init() {
        checkIsAdmin(true);

        var locale = UI.getCurrent().getSession().getBrowser().getLocale();
        selectedLanguages.add(locale.getLanguage());

        // Add english anyway
        selectedLanguages.add("en");
    }

    public Set<String> getSelectedLanguages() {
        return selectedLanguages;
    }

    public MenuBar createMenu() {
        var menu = new MenuBar();
        menu.setOpenOnHover(true);

        createIconItem(menu, VaadinIcon.NEWSPAPER, "Recent", null).addClickListener(listenerNewest);
        createIconItem(menu, VaadinIcon.BAR_CHART, "Most views", null).addClickListener(listenerViews);
        createIconItem(menu, VaadinIcon.THUMBS_UP, "Most liked", null).addClickListener(listenerLiked);

        if (isAdmin) {
            createIconItem(menu, VaadinIcon.MEGAPHONE, "Report News", null).addClickListener(listenerAddNews);
        }

        var languages = createIconItem(menu, VaadinIcon.COG, "Languages", null);
        SubMenu shareSubMenu = languages.getSubMenu();
        MenuItem english = shareSubMenu.addItem("English", listenerLanguageEn);
        english.setCheckable(true);
        english.setChecked(selectedLanguages.contains("en"));
        MenuItem german = shareSubMenu.addItem("German", listenerLanguageDe);
        german.setCheckable(true);
        german.setChecked(selectedLanguages.contains("de"));

        return menu;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public void checkIsAdmin(boolean forceReload) {
        UI currentUI = UI.getCurrent();
        LocalStorage localStorage = new LocalStorage(currentUI);
        localStorage.getItem("API-KEY").thenAccept(result -> {
            adminChecked = true;
            if (apiKey.equals(result)) {
                isAdmin = true;
                if (forceReload) {
                    UI.getCurrent().getPage().reload();
                }
            } else {
                isAdmin = false;
                commonComponents.updateLastVisit();
            }
        });
    }

    private final ComponentEventListener<ClickEvent<MenuItem>> listenerLiked = e -> e.getSource().getUI().ifPresent(ui ->
            ui.navigate("liked"));
    private final ComponentEventListener<ClickEvent<MenuItem>> listenerViews = e -> e.getSource().getUI().ifPresent(ui ->
            ui.navigate("views"));
    private final ComponentEventListener<ClickEvent<MenuItem>> listenerNewest = e -> e.getSource().getUI().ifPresent(ui ->
            ui.navigate("latest"));
    private final ComponentEventListener<ClickEvent<MenuItem>> listenerAddNews = e -> e.getSource().getUI().ifPresent(ui ->
            ui.navigate("add-news"));

    private final ComponentEventListener<ClickEvent<MenuItem>> listenerLanguageEn = e -> {
        if (e.getSource().isChecked()) {
            selectedLanguages.add("en");
        } else {
            selectedLanguages.remove("en");
        }

        e.getSource().getUI().ifPresent(ui -> ui.getPage().reload());
    };

    private final ComponentEventListener<ClickEvent<MenuItem>> listenerLanguageDe = e -> {
        if (e.getSource().isChecked()) {
            selectedLanguages.add("de");
        } else {
            selectedLanguages.remove("de");
        }

        e.getSource().getUI().ifPresent(ui -> ui.getPage().reload());
    };

    private MenuItem createIconItem(HasMenuItems menu, VaadinIcon iconName, String label, String ariaLabel) {
        return createIconItem(menu, iconName, label, ariaLabel, false);
    }

    private MenuItem createIconItem(HasMenuItems menu, VaadinIcon iconName, String label, String ariaLabel, boolean isChild) {
        Icon icon = new Icon(iconName);

        if (isChild) {
            icon.getStyle().set("width", "var(--lumo-icon-size-s)");
            icon.getStyle().set("height", "var(--lumo-icon-size-s)");
            icon.getStyle().set("marginRight", "var(--lumo-space-s)");
        }

        MenuItem item = menu.addItem(icon, e -> {
        });

        if (ariaLabel != null) {
            item.getElement().setAttribute("aria-label", ariaLabel);
        }

        if (label != null) {
            item.add(new Text(label));
        }

        return item;
    }

    public boolean isAdminChecked() {
        return adminChecked;
    }
}
