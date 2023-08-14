package blue.lhf.rescrape.util.query;

import java.net.*;
import java.util.*;

public class RedditQuery {

    public List<URL> getChildURLs() {
        final List<URL> urls = new ArrayList<>();
        for (final QueryPost child : data.children) {
            for (final String url : child.data.getURLs()) {
                try {
                    urls.add(new URL(url));
                } catch (MalformedURLException e) {
                }
            }
        }

        return urls;
    }

    public QueryData data = new QueryData();
}
