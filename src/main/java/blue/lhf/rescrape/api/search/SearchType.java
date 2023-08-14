package blue.lhf.rescrape.api.search;

import java.util.Locale;

/**
 * Represents the different types of results to look for.
 * Types other than {@link SearchType#POSTS} are included for compatibility — they will not work.
 * */
public enum SearchType implements Parameter {
    SUBREDDITS, POSTS, USERS;

    public String toQuery() {
        return "type=" + switch (this) {
            case POSTS -> "link";
            case SUBREDDITS -> "sr";
            default -> name().toLowerCase(Locale.ROOT);
        };
    }
}
