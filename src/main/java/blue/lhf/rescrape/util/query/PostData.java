package blue.lhf.rescrape.util.query;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class PostData {
    public String url = "";
    public PostMedia media = null;

    public List<String> getURLs() {
        final List<String> list = new ArrayList<>();
        list.add(url);
        if (media != null && media.video != null && media.video.hls != null) {
            list.add(media.video.hls);
        }

        return list;
    }
}
