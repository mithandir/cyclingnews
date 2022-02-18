package ch.climbd.newsfeed.views;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.menubar.MenuBar;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.Set;

@Component
@SessionScope
public class CommonSessionComponents {
    private final Set<String> selectedLanguages = new HashSet<String>();

    @Autowired
    CommonComponents commonComponents;

    @PostConstruct
    public void init() {
        selectedLanguages.add("en");
    }

    public Set<String> getSelectedLanguages() {
        return selectedLanguages;
    }

    public MenuBar createMenu() {
        var menu = new MenuBar();
        menu.setOpenOnHover(true);
        menu.addItem("Newest", listenerNewest);
        menu.addItem("Popular", listenerPopular);

        var languages = menu.addItem("Languages");
        SubMenu shareSubMenu = languages.getSubMenu();
        MenuItem english = shareSubMenu.addItem("English", listenerLanguageEn);
        english.setCheckable(true);
        english.setChecked(selectedLanguages.contains("en"));
        MenuItem german = shareSubMenu.addItem("German", listenerLanguageDe);
        german.setCheckable(true);
        german.setChecked(selectedLanguages.contains("de"));

        menu.getStyle().set("padding-left", commonComponents.isMobile() ? "2%" : "10%");

        return menu;
    }

    private final ComponentEventListener<ClickEvent<MenuItem>> listenerPopular = e -> e.getSource().getUI().ifPresent(ui ->
            ui.navigate("popular"));
    private final ComponentEventListener<ClickEvent<MenuItem>> listenerNewest = e -> e.getSource().getUI().ifPresent(ui ->
            ui.navigate("latest"));

    private final ComponentEventListener<ClickEvent<MenuItem>> listenerLanguageEn = e -> {
        if (e.getSource().isChecked()) {
            selectedLanguages.add("en");
        } else if (selectedLanguages.contains("en")) {
            selectedLanguages.remove("en");
        }

        e.getSource().getUI().ifPresent(ui -> ui.getPage().reload());
    };

    private final ComponentEventListener<ClickEvent<MenuItem>> listenerLanguageDe = e -> {
        if (e.getSource().isChecked()) {
            selectedLanguages.add("de");

        } else if (selectedLanguages.contains("de")) {
            selectedLanguages.remove("de");
        }

        e.getSource().getUI().ifPresent(ui -> ui.getPage().reload());
    };
}
