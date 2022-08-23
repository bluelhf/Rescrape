package blue.lhf.rescrape.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class MediaExtractor {
    private static final WeakHashMap<String, String> mimeCache = new WeakHashMap<>();
    private final URLConnection connection;
    private final Consumer<Exception> exceptionHandler;

    public MediaExtractor(final URLConnection connection,
                          final Consumer<Exception> exceptionHandler) {
        this.connection = connection;
        this.exceptionHandler = exceptionHandler;
    }

    protected static String extractMimeType(final URLConnection connection) throws IOException {
        final String cached = mimeCache.get(connection.getURL().toString());
        if (cached != null)
            return cached;

        String mime;
        mime = URLConnection.guessContentTypeFromStream(connection.getInputStream());
        if (mime == null) mime = connection.getContentType();
        if (mime == null) mime = URLConnection.guessContentTypeFromName(
            connection.getURL().getFile());

        mimeCache.put(connection.getURL().toString(), mime);

        return mime;
    }

    /**
     * @return All og:video and og:image elements in the HTML document
     */
    protected static Elements getOGMedia(final URLConnection connection) throws IOException {
        connection.connect();

        final Document document = Jsoup.parse(
            connection.getInputStream(),
            connection.getContentEncoding(),
            connection.getURL().toString());

        final Elements elements = document.select("meta[property=\"og:video\"]");
        elements.addAll(document.select("meta[property=\"og:image\"]"));
        return elements;
    }

    public CompletableFuture<Collection<URL>> extractURLs() {
        return CompletableFuture.supplyAsync(this::extractURLs0);
    }

    public CompletableFuture<Collection<URL>> extractURLs(final Executor executor) {
        return CompletableFuture.supplyAsync(this::extractURLs0, executor);
    }

    protected Collection<URL> extractURLs0() {
        final List<URL> set = new ArrayList<>();
        final String type;
        try {
            type = extractMimeType(connection);
            if (type != null && !type.contains("html")) {
                set.add(connection.getURL());
            } else {
                final Elements elements = getOGMedia(connection);
                for (final Element element : elements) {
                    set.add(new URL(element.attr("content")));
                }
            }

        } catch (IOException ex) {
            exceptionHandler.accept(ex);
        }

        return set;
    }
}
