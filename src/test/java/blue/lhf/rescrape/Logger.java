package blue.lhf.rescrape;

import java.io.Closeable;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class Logger implements Closeable {
    private static final AtomicInteger counter = new AtomicInteger(1);
    private static final String FLUSH = "\n".repeat(600) + "\033[2J\033[H";
    private final List<String> map = new CopyOnWriteArrayList<>();
    private final List<String> ephemeralMap = new CopyOnWriteArrayList<>();
    public static Logger LOGGER = new Logger(System.err);
    private final Thread logThread;

    public Logger(final PrintStream stream) {
        this.logThread = new Thread(() -> {
            while (true) {
                try {
                    stream.print(FLUSH); stream.flush();
                    ephemeralMap.forEach(stream::println);
                    ephemeralMap.clear();
                    map.forEach(stream::println);
                    Thread.sleep(1000);
                } catch (final InterruptedException interrupt) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        this.logThread.setName("Rescrape Logger #" + counter.getAndIncrement());
        this.logThread.start();
    }

    public void printlnEphemeral(final String message) {
        ephemeralMap.add(message);
    }

    public int println(final String message) {
        synchronized (map) {
            final int index = map.size();
            map.add(message);
            return index;
        }
    }

    public void println(final int index, final String message) {
        map.set(index, message);
    }

    public void delete(final int index) {
        synchronized (map) {
            map.remove(index);
        }
    }

    @Override
    public void close() {
        logThread.interrupt();
    }
}
