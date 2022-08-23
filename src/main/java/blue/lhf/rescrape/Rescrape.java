package blue.lhf.rescrape;

import blue.lhf.rescrape.api.RescrapeAPI;
import blue.lhf.rescrape.api.search.*;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.HttpURLConnection;
import java.nio.file.*;
import java.util.DoubleSummaryStatistics;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.LockSupport;

import static blue.lhf.rescrape.api.RescrapeAPI.rescrape;
import static blue.lhf.rescrape.api.query.Query.*;
import static blue.lhf.rescrape.api.search.Search.search;
import static blue.lhf.rescrape.api.search.SearchType.POSTS;
import static blue.lhf.rescrape.api.search.Sort.TOP;
import static blue.lhf.rescrape.api.search.Time.ALL;
import static java.lang.Math.round;
import static java.lang.System.err;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import java.util.Locale;

public class Rescrape {
    /*public static void main(String[] args) {
        RescrapeAPI.withUserAgent("R34LU53R493N7")
            .scrape(search(r("aww"), keyword("dog")).limit(100))
            .thenAccept(urls -> urls.forEach(err::println)).join();
    }*/


    public static final class Bytes {

        private Bytes() {
        }

        public static String format(double value, Locale locale) {
            if (value < 1024) {
                return value + " B";
            }
            int z = (63 - Long.numberOfLeadingZeros((long) value)) / 10;
            return String.format(locale, "%.3f %siB", value / (1L << (z * 10)), " KMGTPE".charAt(z));
        }
    }

    public static class MovingAvgLastN{
        int maxTotal;
        int total;
        double lastN[];
        double avg;
        int head;

        public MovingAvgLastN(int N){
            maxTotal = N;
            lastN = new double[N];
            avg = 0;
            head = 0;
            total = 0;
        }

        public void add(double num){
            double prevSum = total*avg;

            if(total == maxTotal){
                prevSum-=lastN[head];
                total--;
            }

            head = (head+1)%maxTotal;
            int emptyPos = (maxTotal+head-1)%maxTotal;
            lastN[emptyPos] = num;

            double newSum = prevSum+num;
            total++;
            avg = newSum/total;
        }

        public double getAvg(){
            return avg;
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {

        final Path dir = Path.of("down");
        Files.createDirectories(dir);
        final ExecutorService urlHandlerService = Executors.newFixedThreadPool(128);

        final LogView view = new LogView();
        final LogView.Slot exceptionSlot = view.slot();
        final LogView.Slot overallSlot = view.slot();

        AtomicInteger maxLengthSize = new AtomicInteger(9);
        AtomicLong max = new AtomicLong(0);
        AtomicLong complete = new AtomicLong(0);
        AtomicLong bytes = new AtomicLong(0);

        CompletableFuture.runAsync(() -> {
            final long period = (long) 5E8;
            long pastBytes = 0;
            final MovingAvgLastN stats = new MovingAvgLastN((int) ((1E9 / (1.0 * period)) * 10));
            while (true) {
                stats.add((bytes.get() - pastBytes) / (1E9 / (period * 1.0)));
                pastBytes = bytes.get();
                overallSlot.set("Overall progress: %d/%d%n%s/s".formatted(complete.get(), max.get(), Bytes.format((long) stats.getAvg(), Locale.ROOT)));
                view.print(err);
                LockSupport.parkNanos(period);
            }
        });

        rescrape().extracts(urlHandlerService).scrape(search(query(
            nsfw()
                .and(subreddit("AraAra").or("HENTAI_GIF", "MinecraftPorn2", "BigAnimeTiddies"))
        )).sort(TOP).time(ALL).type(POSTS).limit(100), url -> {
            max.getAndIncrement();
            CompletableFuture.runAsync(() -> {
                final LogView.Slot slot = view.slot();

                boolean success = false;
                attempt:
                try {
                    String name = url.getPath();
                    name = name.substring(Math.max(name.lastIndexOf('/') + 1, 0));
                    final Path target = dir.resolve(name);

                    if (name.contains("-mobile")) break attempt;
                    if ("play".equals(url.getQuery())) break attempt;

                    final HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                    final long length = connection.getContentLengthLong();

                    if (Files.isRegularFile(target) && Files.size(target) == length) {
                        success = true;
                        break attempt;
                    }

                    final int lengthSize = String.valueOf(length).length();

                    if (maxLengthSize.get() < lengthSize) {
                        maxLengthSize.set(lengthSize);
                    }

                    final String lbp = "%0" + maxLengthSize.get() + "d";

                    try (final var input = connection.getInputStream()) {
                        try (final var output = Files.newOutputStream(target, TRUNCATE_EXISTING, CREATE)) {
                            // Initial guess
                            int bufferSize = 131072;
                            double learningRate = 0.1;
                            byte[] buf = new byte[bufferSize];
                            long[] nanos = new long[]{System.nanoTime() - (long) 1E7, System.nanoTime()};

                            int read, total = 0;
                            while ((read = input.read(buf)) != -1) {

                                nanos[0] = nanos[1];
                                nanos[1] = System.nanoTime() - nanos[0];
                                final double F = nanos[1] / (bufferSize * 1.0);

                                total += read;
                                bytes.getAndAdd(read);

                                final int width = 30;
                                final int filled = (int) (total * 1.0 / length * width);
                                slot.set(("[" + lbp + "/" + lbp + "] [%07.3f %%] [%-" + width + "s] %s").formatted(total, length, (total * 1.0 / length * 100), "=".repeat(filled), url));

                                output.write(buf, 0, read);

                                final int sizeCandidate = (int) round(bufferSize - learningRate * F);
                                if (sizeCandidate > 64 && sizeCandidate < 16777216) {
                                    bufferSize = (int) round(bufferSize - learningRate * F);
                                }
                                buf = new byte[bufferSize];
                            }
                        }
                    }
                    success = true;

                    view.free(slot);
                    connection.disconnect();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (!success) max.getAndDecrement();
                if (success) complete.getAndIncrement();

            }, urlHandlerService).exceptionally(t -> {
                final StringWriter sw = new StringWriter();
                final PrintWriter pw = new PrintWriter(sw);
                t.printStackTrace(pw);
                exceptionSlot.set(sw.toString());
                pw.close();
                return null;
            });
        }, (t) -> {
            final StringWriter sw = new StringWriter();
            final PrintWriter pw = new PrintWriter(sw);
            t.printStackTrace(pw);
            exceptionSlot.set(sw.toString());
            pw.close();
        }).join();

        urlHandlerService.shutdown();
        while (!urlHandlerService.awaitTermination(Integer.MAX_VALUE, TimeUnit.MILLISECONDS)) ;
    }
}
