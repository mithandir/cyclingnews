package ch.climbd.newsfeed.views.components;

import ch.climbd.newsfeed.data.NewsEntry;
import com.vaadin.flow.shared.Registration;

import java.util.LinkedList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class Broadcaster {
    static final Executor executor = Executors.newSingleThreadExecutor();

    static final LinkedList<Consumer<NewsEntry>> listeners = new LinkedList<>();

    public static synchronized Registration register(Consumer<NewsEntry> listener) {
        listeners.add(listener);

        return () -> {
            synchronized (Broadcaster.class) {
                listeners.remove(listener);
            }
        };
    }

    public static synchronized void broadcast(NewsEntry message) {
        for (Consumer<NewsEntry> listener : listeners) {
            executor.execute(() -> listener.accept(message));
        }
    }
}
