package blue.lhf.rescrape;

import java.io.Closeable;
import java.io.PrintStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class Logger implements Closeable {
    private static final AtomicInteger counter = new AtomicInteger(1);
    private static final String FLUSH = "\n".repeat(600) + "\033[2J\033[H";

    private final AtomicInteger id = new AtomicInteger(0);
    private final Map<Integer, String> map = new ConcurrentSkipListMap<>();
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

                    final int maxWidth = map.size() == 0 ? 1 : (int) Math.log10(map.size()) + 1;
                    final String format = "%0" + maxWidth + "d %s%n";
                    final Collection<String> values = map.values();
                    int counter = 0;
                    for (final String line : values) {
                        stream.printf(format, counter++, line);
                    }

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
        final int index = id.getAndIncrement();
        map.put(index, message);
        return index;
    }

    public void println(final int index, final String message) {
        map.put(index, message);
    }

    public void delete(final int index) {
        map.remove(index);
    }

    @Override
    public void close() {
        logThread.interrupt();
    }
}
