package blue.lhf.rescrape.api.query;

/**
 * Represents the binary logical AND operator for search queries.
 * @see Query
 * @see <a href="https://www.reddit.com/wiki/search/#wiki_manual_filtering">Reddit Search — Manual Filtering</a>
 * */
public record AndPart(Part one, Part other) implements Part {
    @Override
    public String toQueryString() {
        return one.toQueryString() + " AND " + other.toQueryString();
    }
}
