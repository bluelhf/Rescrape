package blue.lhf.rescrape.api.query;

/**
 * Represents the binary logical NOT operator for search queries.
 * @see Query
 * @see <a href="https://www.reddit.com/wiki/search/#wiki_manual_filtering">Reddit Search — Manual Filtering</a>
 * */
public record NotPart(Part sub) implements Part {
    @Override
    public String toQueryString() {
        return "NOT " + sub;
    }
}
