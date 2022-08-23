package blue.lhf.rescrape.api.query;

import static blue.lhf.rescrape.api.query.Query.query;

/**
 * Represents an option part in a search query.
 * Any option part constructed via methods in {@link Query} must have a valid, recognised key.
 * */
public record OptionPart<T>(String key, T value) implements Part {
    @Override
    public String toQueryString() {
        return key + ":" + value;
    }

    /**
     * Shorthand for a part which matches for several possible values for this part's key.
     * @return A part which matches for several possible values for this part's key.
     * */
    @SafeVarargs
    public final Part or(T... values) {
        Part current = this;
        for (T t : values) {
            current = current.or(new OptionPart<>(key, t));
        }
        return query(current);
    }
}
