package blue.lhf.rescrape.api.search;

import java.util.Locale;

/**
 * Represents different sorts for Reddit results.
 * */
public enum Sort implements Parameter {
    RELEVANCE,
    HOT,
    TOP,
    NEW,
    COMMENTS;

    public String toQuery() {
        return "sort=" + name().toLowerCase(Locale.ROOT);
    }
}
