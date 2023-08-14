package blue.lhf.rescrape.api.search;

public abstract class SearchBound implements Parameter {
    private final String parameter;
    private final String postName;

    public static final class Before extends SearchBound {
        public Before(final String postName) {
            super("before", postName);
        }
    }

    public static final class After extends SearchBound {
        public After(final String postName) {
            super("after", postName);
        }
    }

    public SearchBound(final String parameter, final String postName) {
        this.parameter = parameter;
        this.postName = postName;
    }

    public String getQueryParameter() {
        return parameter;
    }

    public String getPostName() {
        return postName;
    }

    public static SearchBound before(final String post) {
        return new SearchBound.Before(post);
    }

    public static SearchBound after(final String post) {
        return new SearchBound.After(post);
    }

    public String toQuery() {
        return getQueryParameter() + "=" + getPostName();
    }
}
