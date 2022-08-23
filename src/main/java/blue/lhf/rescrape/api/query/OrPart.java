package blue.lhf.rescrape.api.query;

/**
 * Represents the binary logical OR operator for search queries.
 * @see Query
 * @see <a href="https://www.reddit.com/wiki/search/#wiki_manual_filtering">Reddit Search — Manual Filtering</a>
 * */
public record OrPart(Part a, Part b) implements Part {
    @Override
    public String toQueryString() {
        return a.toQueryString() + " OR " + b.toQueryString();
    }
}
