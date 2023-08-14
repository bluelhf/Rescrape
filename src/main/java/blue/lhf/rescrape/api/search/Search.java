package blue.lhf.rescrape.api.search;

import blue.lhf.rescrape.api.query.*;
import org.jetbrains.annotations.*;

import java.util.Objects;

import static blue.lhf.rescrape.api.search.SearchType.POSTS;
import static blue.lhf.rescrape.api.search.Sort.RELEVANCE;
import static blue.lhf.rescrape.api.search.Time.ALL;
import static java.net.URLEncoder.encode;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Represents a Reddit search, including parameters and a query.
 * */
public final class Search {
    private @NotNull Query query;
    private @NotNull Sort sort;
    private @NotNull Time time;
    private @NotNull SearchType type;
    private @Nullable SearchBound bound;
    private int limit;

    private Search(@NotNull final Query query, @NotNull final Sort sort,
                   @NotNull final Time time, @NotNull final SearchType type,
                   final int limit, @Nullable final SearchBound bound) {
        this.query = query;
        this.sort = sort;
        this.time = time;
        this.type = type;
        this.limit = limit;
        this.bound = bound;
    }

    @Contract(value = "_ -> new", pure = true)
    public static @NotNull Search search(final Query query) {
        return new Search(query, RELEVANCE, ALL, POSTS, 25, null);
    }

    @Contract("_ -> new")
    public static @NotNull Search search(final Part... parts) {
        return search(Query.query(parts));
    }

    @Contract(value = "_ -> this", mutates = "this")
    public Search query(final Query query) {
        this.query = query;
        return this;
    }

    @Contract(value = "_ -> this", mutates = "this")
    public Search sort(final Sort sort) {
        this.sort = sort;
        return this;
    }

    @Contract(value = "_ -> this", mutates = "this")
    public Search time(final Time time) {
        this.time = time;
        return this;
    }

    @Contract(value = "_ -> this", mutates = "this")
    public Search type(final SearchType type) {
        this.type = type;
        return this;
    }

    @Contract(value = "_ -> this", mutates = "this")
    public Search bound(final SearchBound bound) {
        this.bound = bound;
        return this;
    }

    @Range(from = 1, to = 100)
    @Contract(value = "_ -> this", mutates = "this")
    public Search limit(final int limit) {
        this.limit = limit;
        return this;
    }

    public int limit() {
        return this.limit;
    }

    public @NotNull String toQuery() {
        return "?q=" + encode(query.toQueryString(), UTF_8) +
            "&include_over_18=true" +
            "&" + sort.toQuery() +
            "&" + time.toQuery() +
            "&" + type.toQuery() +
            (bound != null ? "&" + bound.toQuery() : "") +
            "&limit=" + limit;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        final var that = (Search) obj;
        return Objects.equals(this.query, that.query) &&
            Objects.equals(this.sort, that.sort) &&
            Objects.equals(this.time, that.time) &&
            Objects.equals(this.type, that.type) &&
            this.limit == that.limit;
    }

    @Override
    public int hashCode() {
        return Objects.hash(query, sort, time, type, limit);
    }

    @Override
    public String toString() {
        return "Search[" +
            "query=" + query + ", " +
            "sort=" + sort + ", " +
            "time=" + time + ", " +
            "type=" + type + ", " +
            "limit=" + limit + ']';
    }
}
