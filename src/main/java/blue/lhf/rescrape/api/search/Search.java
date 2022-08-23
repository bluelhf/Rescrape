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
    private Query query;
    private Sort sort;
    private Time time;
    private SearchType type;
    private int limit;

    private Search(Query query, Sort sort,
                  Time time, SearchType type, int limit) {
        this.query = query;
        this.sort = sort;
        this.time = time;
        this.type = type;
        this.limit = limit;
    }

    @Contract(value = "_ -> new", pure = true)
    public static @NotNull Search search(final Query query) {
        return new Search(query, RELEVANCE, ALL, POSTS, 25);
    }

    @Contract("_ -> new")
    public static @NotNull Search search(final Part... parts) {
        return search(Query.query(parts));
    }

    @Contract(value = "_ -> this", mutates = "this")
    public Search query(Query query) {
        this.query = query;
        return this;
    }

    @Contract(value = "_ -> this", mutates = "this")
    public Search sort(Sort sort) {
        this.sort = sort;
        return this;
    }

    @Contract(value = "_ -> this", mutates = "this")
    public Search time(Time time) {
        this.time = time;
        return this;
    }

    @Contract(value = "_ -> this", mutates = "this")
    public Search type(SearchType type) {
        this.type = type;
        return this;
    }

    @Range(from = 1, to = 100)
    @Contract(value = "_ -> this", mutates = "this")
    public Search limit(int limit) {
        this.limit = limit;
        return this;
    }


    public @NotNull String toQuery() {
        return "?q=" + encode(query.toQueryString(), UTF_8) +
            "&include_over_18=true" +
            "&sort=" + sort.toQuery() +
            "&t=" + time.toQuery() +
            "&type=" + type.toQuery() +
            "&limit=" + limit;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Search) obj;
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
