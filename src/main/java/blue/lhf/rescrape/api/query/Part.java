package blue.lhf.rescrape.api.query;

/**
 * Represents part of a search query, or potentially the entire query.
 * @see Query
 * */
public interface Part {
    /**
     * @return The non-URL-encoded query string for this {@link Part}
     * */
    String toQueryString();

    /**
     * @return A part which matches either this part OR the given part.
     * @param other The other part, which may also match.
     * */
    default Part or(final Part other) {
        return new OrPart(this, other);
    }

    /**
     * @return A part which matches this part AND the given part.
     * @param other The other part, which must also match.
     * */
    default Part and(final Part other) {
        return new AndPart(this, other);
    }
}
