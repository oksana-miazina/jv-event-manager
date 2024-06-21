package mate.academy;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class EventManager {
    public static final int THREAD_POOL_MIN_SIZE = 1;
    public static final int SHUTDOWN_TIMEOUT_MINUTES = 2;
    private final ThreadPoolExecutor executor;
    private final List<EventListener> listeners;

    public EventManager() {
        this.executor = new ThreadPoolExecutor(
                THREAD_POOL_MIN_SIZE, THREAD_POOL_MIN_SIZE,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>());
        this.listeners = new CopyOnWriteArrayList<>();
    }

    public void registerListener(EventListener listener) {
        listeners.add(listener);
        adjustThreadPoolSize();
    }

    public void deregisterListener(EventListener listener) {
        listeners.remove(listener);
        adjustThreadPoolSize();
    }

    public void notifyEvent(Event event) {
        Iterator<EventListener> iterator = listeners.iterator();
        while (iterator.hasNext()) {
            EventListener listener = iterator.next();
            executor.submit(() -> listener.onEvent(event));
        }
    }

    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(SHUTDOWN_TIMEOUT_MINUTES, TimeUnit.MINUTES)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private void adjustThreadPoolSize() {
        int newSize = Math.max(listeners.size(), THREAD_POOL_MIN_SIZE);
        if (executor.getPoolSize() > newSize) {
            executor.setCorePoolSize(newSize);
            executor.setMaximumPoolSize(newSize);
        } else {
            executor.setMaximumPoolSize(newSize);
            executor.setCorePoolSize(newSize);
        }
    }
}
