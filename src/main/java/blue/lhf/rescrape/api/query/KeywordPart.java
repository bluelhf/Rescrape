package blue.lhf.rescrape.api.query;

/**
 * Represents search query keywords.
 * @see Query
 * @see <a href="https://www.reddit.com/wiki/search/#wiki_manual_filtering">Reddit Search — Manual Filtering</a>
 * */
public record KeywordPart(String word) implements Part {

    @Override
    public String toQueryString() {
        return word;
    }
}
