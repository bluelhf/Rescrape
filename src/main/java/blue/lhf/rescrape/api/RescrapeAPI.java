package blue.lhf.rescrape.api;

import blue.lhf.rescrape.api.query.Part;
import blue.lhf.rescrape.api.search.Search;
import blue.lhf.rescrape.util.MediaExtractor;
import blue.lhf.rescrape.util.query.RedditQuery;
import mx.kenzie.argo.Json;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

import static blue.lhf.rescrape.api.search.Search.search;
import static java.util.concurrent.ThreadLocalRandom.current;

public class RescrapeAPI {
    private Consumer<HttpsURLConnection> connectionConfigurator = (c) -> {
        c.setConnectTimeout(5000);
        c.setReadTimeout(5000);

        StringBuilder uab = new StringBuilder();
        for (int i = 0; i < 16; ++i) {
            uab.append((char) current().nextInt('a', 'z'));
        }

        c.setRequestProperty("User-Agent", uab.toString());

    };
    private Executor extractionExecutor = Executors.newCachedThreadPool();

    private RescrapeAPI() {

    }

    /**
     * Creates a Rescrape API with the default connection and extraction handling.
     * This means using a random User-Agent request header
     * matching <code>/[a-z]{16}/</code> and a cached thread pool executor for extracting URLs.
     * */
    public static RescrapeAPI rescrape() {
        return new RescrapeAPI();
    }


    /**
     * Creates a Rescrape API with the given
     * <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/User-Agent">User-Agent</a> request header.
     * @param userAgent The User-Agent property to use.
     * @return The constructed Rescrape API
     * */
    public static RescrapeAPI withUserAgent(final String userAgent) {
        return rescrape().connects(c -> c.setRequestProperty("User-Agent", userAgent));
    }

    /**
     * Sets the connection configurator of this Rescrape API to the given value.
     * The connection configurator is called after creating the base connection for scraping
     * to configure values such as the User-Agent request header and relevant timeouts.
     * */
    public RescrapeAPI connects(final Consumer<HttpsURLConnection> connectionConfigurator) {
        this.connectionConfigurator = connectionConfigurator;
        return this;
    }

    /**
     * Sets the extracting executor of this Rescrape API to the given value.
     * The extracting executor is responsible for handling all media extraction tasks,
     * and can be used to control the thread count of the scrapers, for example.
     * */
    public RescrapeAPI extracts(final Executor extractionExecutor) {
        this.extractionExecutor = extractionExecutor;
        return this;
    }

    /**
     * Runs a scrape of a search constructed from the given query parts with the default search options.
     * */
    public CompletableFuture<Collection<URL>> scrape(final Part... parts) {
        return scrape(search(parts));
    }

    /**
     * Runs a scrape of the given search with non-operative url and exception consumers.
     * */
    public CompletableFuture<Collection<URL>> scrape(final Search search) {
        return scrape(search, url -> {
        }, e -> {
        });
    }

    /**
     * Runs a scrape of the given search with the given url consumer and a non-operative exception consumer.
     * The URL consumer is called for each extracted URL as they are retrieved.
     * */
    public CompletableFuture<Collection<URL>> scrape(
        final Search search, final Consumer<URL> urlConsumer) {
        return scrape(search, urlConsumer, e -> {
        });
    }

    /**
     * Runs a scrape of the given search with the given url and exception consumers.
     * The URL consumer is called for each extracted URL as they are retrieved.
     * The exception consumer is called for each exception as they happen.
     * */
    public CompletableFuture<Collection<URL>> scrape(
        final Search search, final Consumer<? super URL> urlConsumer,
        final Consumer<Exception> exceptionHandler) {

        final String base = "https://reddit.com/search.json";
        HttpsURLConnection connection;
        try {
            final URL url = new URL(base + search.toQuery());
            connection = (HttpsURLConnection) url.openConnection();
            connectionConfigurator.accept(connection);
            connection.connect();
        } catch (IOException e) {
            exceptionHandler.accept(e);
            return CompletableFuture.failedFuture(e);
        }

        final List<URL> urls = new CopyOnWriteArrayList<>();
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        try (Json json = new Json(connection.getInputStream())) {
            final RedditQuery query = json.toObject(new RedditQuery());
            final List<URL> childURLs = query.getChildURLs();
            for (final URL child : childURLs) {
                futures.add(new MediaExtractor(child.openConnection(),
                    exceptionHandler).extractURLs().thenAccept(list -> {
                    list.forEach(urlConsumer);
                    urls.addAll(list);
                }));
            }
        } catch (IOException e) {
            exceptionHandler.accept(e);
            return CompletableFuture.failedFuture(e);
        }

        return CompletableFuture.allOf(futures
            .toArray(CompletableFuture[]::new)
        ).thenApply(v -> urls);
    }
}
