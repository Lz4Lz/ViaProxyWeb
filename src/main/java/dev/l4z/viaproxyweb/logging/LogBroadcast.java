package dev.l4z.viaproxyweb.logging;

import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Consumer;

public final class LogBroadcast {
    private static final Set<Consumer<String>> LISTENERS = ConcurrentHashMap.newKeySet();
    private static final Deque<String> RECENT = new ConcurrentLinkedDeque<>();
    private static final int MAX = 300;

    private LogBroadcast() {
    }

    public static void subscribe(Consumer<String> listener) {
        LISTENERS.add(listener);
    }

    public static void unsubscribe(Consumer<String> listener) {
        LISTENERS.remove(listener);
    }

    public static List<String> recent() {
        return List.copyOf(RECENT);
    }

    public static void publish(String line) {
        RECENT.addLast(line);
        while (RECENT.size() > MAX) RECENT.pollFirst();
        for (Consumer<String> listener : LISTENERS) {
            try {
                listener.accept(line);
            } catch (Exception ignored) {
            }
        }
    }
}
