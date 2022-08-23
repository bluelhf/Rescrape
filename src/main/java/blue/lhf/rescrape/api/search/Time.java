package blue.lhf.rescrape.api.search;

import java.util.Locale;

/**
 * Represents the possible time bounds for Reddit results: an otherwise
 * valid result must be posted at most the given value from now in order
 * to be shown.
 * */
public enum Time {
    HOUR, DAY, WEEK, MONTH, ALL;

    public String toQuery() {
        return name().toLowerCase(Locale.ROOT);
    }
}
