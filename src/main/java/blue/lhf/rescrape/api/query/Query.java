package blue.lhf.rescrape.api.query;

import java.util.*;

import static java.util.stream.Collectors.joining;

/**
 * Represents a search query or one of its sub-queries within parentheses.
 * @see <a href="https://www.reddit.com/wiki/search/#wiki_manual_filtering">Reddit Search — Manual Filtering</a>
 * */
@SuppressWarnings("unused")
public class Query extends ArrayList<Part> implements Part {

    private Query(List<Part> parts) {
        super(parts);
    }

    /**
     * @return A new query, constructed by appending each given {@link Part}.
     * @param parts The parts to use for the {@link Query}.
     * */
    public static Query query(Part... parts) {
        return new Query(List.of(parts));
    }

    /**
     * @return An option part which requires the subreddit name to match the given input.
     * @param subreddit The subreddit name to match.
     * */
    public static OptionPart<String> subreddit(final String subreddit) {
        return new OptionPart<>("subreddit", subreddit);
    }

    /**
     * @return A keyword part for the given input string.
     * @param word The input string.
     * */
    public static KeywordPart keyword(final String word) {
        return new KeywordPart(word);
    }

    /**
     * Shorthand for {@link Query#subreddit(String)}.
     * @see Query#subreddit(String)
     * */
    public static OptionPart<String> r(final String subreddit) {
        return subreddit(subreddit);
    }

    /**
     * Shorthand for {@link Query#author(String)}.
     * @see Query#author(String)
     * */
    public static OptionPart<String> u(final String author) {
        return author(author);
    }

    /**
     * Shorthand for {@link Query#nsfw(boolean)} with the value <code>true</code>.
     * @see Query#nsfw(boolean)
     * */
    public static OptionPart<Boolean> nsfw() {
        return nsfw(true);
    }

    /**
     * @return An option part which requires the NSFW state of the given post to match the input.
     * @param nsfw The input to match.
     * */
    public static OptionPart<Boolean> nsfw(final boolean nsfw) {
        return new OptionPart<>("nsfw", nsfw);
    }

    /**
     * @return An option part which requires the author of the given post to match the input.
     * @param author The input to match.
     * */
    public static OptionPart<String> author(final String author) {
        return new OptionPart<>("author", author);
    }

    /**
     * @return An option part which requires any flair of the given post to match the input.
     * @param flair The input to match.
     * */
    public static OptionPart<String> flair(final String flair) {
        return new OptionPart<>("flair", flair);
    }

    /**
     * Shorthand for {@link Query#self(boolean)}.
     * @deprecated Setting this to <code>true</code> will always cause Rescrape to return nothing at all.
     * @see Query#self(boolean)
     * */
    public static OptionPart<Boolean> self() {
        return self(true);
    }

    /**
     * Reddit calls text-only posts 'self-posts'; when this option is set, no media is returned.
     * This is included for compatibility — using it will always cause the Rescrape output to be empty.
     * @return An option part which requires the text-only state of the post to match the input.
     * @deprecated Setting this to <code>true</code> will always cause Rescrape to return nothing at all.
     * @param self The input to match.
     * */
    public static OptionPart<Boolean> self(final boolean self) {
        return new OptionPart<>("self", self);
    }

    /**
     * @return An option part which requires the text content of the post to match the input.
     * @deprecated Setting this to any value will always cause Rescrape to return nothing at all.
     * @param selftext The input to match
     * */
    public static OptionPart<String> selftext(final String selftext) {
        return new OptionPart<>("selftext", selftext);
    }

    /**
     * @return An option part which requires the origin domain of the post to match the input.
     * @param site The input to match
     * */
    public static OptionPart<String> site(final String site) {
        return new OptionPart<>("site", site);
    }

    /**
     * @return An option part which requires the title of the post to match the input.
     * @param title The input to match
     * */
    public static OptionPart<String> title(final String title) {
        return new OptionPart<>("title", title);
    }

    /**
     * @return An option part which requires the URL of the post content to match the input.
     * @param url The input to match
     * */
    public static OptionPart<String> url(final String url) {
        return new OptionPart<>("url", url);
    }

    /**
     * @return A part which requires that the given part does <i>not</i> match.
     * @param part The part to <i>not</i> match.
     * */
    public static NotPart not(final Part part) {
        return new NotPart(part);
    }

    @Override
    public String toQueryString() {
        return "(" + stream()
            .map(Part::toQueryString)
            .collect(joining(" ")) + ")";
    }
}
