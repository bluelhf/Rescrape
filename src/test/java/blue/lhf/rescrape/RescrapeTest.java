package blue.lhf.rescrape;

import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

import static blue.lhf.rescrape.SpliteratorUtil.*;
import static blue.lhf.rescrape.api.RescrapeAPI.rescrape;
import static blue.lhf.rescrape.api.query.Query.*;
import static blue.lhf.rescrape.api.search.Search.search;
import static blue.lhf.rescrape.api.search.Sort.*;

public class RescrapeTest {

    public static boolean notThumbnail(final URL url) {
        return !url.getFile().endsWith("?play");
    }

    private static boolean notRemoved(final URL url) {
        return !url.getFile().endsWith("removed.png");
    }

    public static void main(final String[] args) {
        final ExecutorService downloadPool = Executors.newFixedThreadPool(8);
        System.err.println("Downloading data...");


        rescrape().scrape(search(r("ProgrammingAnimemes")).limit(100).sort(HOT))
                  .thenApply(Collection::spliterator)
                  .thenApply(filter(RescrapeTest::notThumbnail))
                  .thenApply(filter(RescrapeTest::notExternalPreview))
                  .thenApply(filter(RescrapeTest::notRemoved))
                  .thenApply(filter(RescrapeTest::notSizeRestricted))
                  .thenAccept(bindR(Spliterator<URL>::forEachRemaining, (url) -> {
                      final Handlers handlers = Handlers.getHandler(url);
                      if (handlers == null) return;
                      final String filename = handlers.getName(url);

                      final Path target = Path.of("download", filename);
                      try {
                          Files.createDirectories(target.getParent());
                      } catch (IOException e) {
                          throw new UncheckedIOException(e);
                      }
                      if (Files.exists(target)) return;
                      CompletableFuture.runAsync(() -> {
                          try (final InputStream stream = handlers.handle(url)) {
                              if (stream == null) return;
                              System.err.println("Downloading " + url + " to " + filename);
                              Files.copy(stream, target);
                          } catch (IOException e) {
                              System.err.println("Failed to save " + url);
                              e.printStackTrace();
                          }
                      }, downloadPool);
                  })).join();
        downloadPool.shutdown();
    }

    private static boolean notSizeRestricted(final URL url) {
        return !url.getFile().contains("size_restricted");
    }

    private static boolean notMobile(final URL url) {
        return !url.getFile().contains("mobile");
    }

    private static boolean notExternalPreview(final URL url) {
        return !url.getHost().startsWith("external-preview.redd.it");
    }
}
