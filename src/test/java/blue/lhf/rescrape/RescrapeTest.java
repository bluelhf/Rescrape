package blue.lhf.rescrape;

import blue.lhf.rescrape.api.search.Search;

import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;

import static blue.lhf.rescrape.Logger.LOGGER;
import static blue.lhf.rescrape.SpliteratorUtil.*;
import static blue.lhf.rescrape.api.RescrapeAPI.rescrape;
import static blue.lhf.rescrape.api.query.Query.*;
import static blue.lhf.rescrape.api.search.Search.search;
import static blue.lhf.rescrape.api.search.Sort.*;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class RescrapeTest {

    public static boolean notThumbnail(final URL url) {
        return !url.getFile().endsWith("?play");
    }

    private static boolean notRemoved(final URL url) {
        return !url.getFile().endsWith("removed.png");
    }

    public static void main(final String[] args) {
        final ExecutorService downloadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        final int status = LOGGER.println("Downloading data...");

        final String subreddit = "ProgrammingAnimemes";
        rescrape().connects(conf -> {
            LOGGER.printlnEphemeral("Connect to " + conf.getURL().toString().replaceAll("(?<=^.{49}).*$", "…"));
            conf.setReadTimeout(30_000);
            conf.setConnectTimeout(30_000);
        }).scrape(search(r(subreddit)).limit(1000).sort(TOP), url -> {
            CompletableFuture.runAsync(() -> {

                final int index = LOGGER.println("Handling " + url.toString().replaceAll("(?<=^.{49}).*$", "…"));
                if (!notThumbnail(url)
                        || !notExternalPreview(url)
                        || !notSizeRestricted(url)
                        || !notRemoved(url)) {
                    LOGGER.delete(index);
                    return;
                }

                final Handlers handlers = Handlers.getHandler(url);
                if (handlers == null) {
                    LOGGER.delete(index);
                    return;
                }

                final String filename = handlers.getName(url);

                final Path target = Path.of("download", subreddit, filename);
                try {
                    Files.createDirectories(target.getParent());
                } catch (final IOException e) {
                    throw new UncheckedIOException(e);
                }
                if (Files.exists(target)) {
                    LOGGER.delete(index);
                    return;
                }
                try {
                    Files.createFile(target);
                } catch (final IOException e) {
                    throw new UncheckedIOException(e);
                }

                try (final InputStream stream = handlers.handle(url);
                     final OutputStream output = Files.newOutputStream(target)) {
                    LOGGER.println(index, "Downloading %s...".formatted(url.toString().replaceAll("(?<=^.{49}).*$", "…")));
                    if (stream == null) {
                        LOGGER.delete(index);
                        return;
                    }
                    final byte[] buffer = new byte[3072];
                    int read = 0, progress = 0;
                    while ((read = stream.read(buffer, 0, buffer.length)) != -1) {
                        progress += read;
                        LOGGER.println(index, "Downloaded %d bytes of %s".formatted(progress, url.toString().replaceAll("(?<=^.{49}).*$", "…")));
                        output.write(buffer, 0, read);
                    }
                    LOGGER.delete(index);
                } catch (final Exception e) {
                    LOGGER.printlnEphemeral("Failed to save " + url);
                    e.printStackTrace();
                    LOGGER.delete(index);
                }
            }, downloadPool);
        });
        final Scanner scanner = new Scanner(System.in);
        while (!scanner.nextLine().isEmpty()) {

        }
        downloadPool.shutdown();
    }

    private static boolean notSizeRestricted(final URL url) {
        return !url.getFile().contains("size_restricted");
    }

    private static boolean notExternalPreview(final URL url) {
        return !url.getHost().startsWith("external-preview.redd.it");
    }
}
