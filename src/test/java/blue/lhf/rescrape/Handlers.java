package blue.lhf.rescrape;

import mx.kenzie.argo.Json;
import org.jetbrains.annotations.NotNull;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.lang.ProcessBuilder.Redirect.INHERIT;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.Locale.ROOT;
import static java.util.concurrent.TimeUnit.SECONDS;

public enum Handlers {
    IMGUR() {
        @Override
        public boolean accept(URL url) {
            return url.getHost().endsWith("imgur.com");
        }

        @Override
        protected InputStream handle(URL url) {
            try {
                return url.openStream();
            } catch (IOException e) {
                System.err.println("Failed to download " + url);
                e.printStackTrace();
                return null;
            }
        }
    },

    REDGIFS() {
        private String token;

        private String getToken() throws IOException {
            if (token != null) return token;
            return (token = regenerateToken());
        }

        private String regenerateToken() throws IOException {
            try {
                final URL url = new URL("https://api.redgifs.com/v2/auth/temporary");
                try (final Json json = new Json(url.openStream())) {
                    return (String) json.toMap().get("token");
                }
            } catch (MalformedURLException e) {
                throw new AssertionError("Predefined URL is malformed", e);
            }
        }

        @Override
        public boolean accept(URL url) {
            return url.getHost().endsWith("redgifs.com");
        }

        @Override
        public String getName(URL url) {
            return getId(url) + ".mp4";
        }

        @Override
        protected InputStream handle(URL url) {
            final String id = getId(url);

            final URL downloadURL;
            try {
                downloadURL = getDownloadURL(id);
                return downloadURL.openStream();
            } catch (Exception e) {
                System.err.println("Failed to download " + url);
                e.printStackTrace();
            }

            return null;
        }

        @NotNull
        private String getId(URL url) {
            final int files = Math.max(url.getFile().indexOf("/files/") + 7, 0);
            int dash = url.getFile().indexOf('-');
            if (dash == -1) dash = url.getFile().length() - 4;
            return url.getFile().substring(files, dash);
        }

        private URL getDownloadURL(final String id) throws Exception {
            final String token = getToken();
            final URL url = new URL("https://api.redgifs.com/v2/gifs/" + id.toLowerCase(ROOT));
            final HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestProperty("Authorization", "Bearer " + token);
            try (final Json json = new Json(connection.getInputStream())) {
                final Map<String, Object> map = json.toMap();
                final Map<String, Object> gif = (Map<String, Object>) map.get("gif");
                final Map<String, Object> urls = (Map<String, Object>) gif.get("urls");
                return new URL((String) urls.get("hd"));
            }
        }
    },

    REDDIT() {
        private static final int[] VIDEO_RESOLUTIONS = new int[]{1080, 720, 480, 360, 240, 144};
        private static final int[] AUDIO_BITRATES = new int[]{192, 160, 128, 96, 64, 32};

        @Override
        public boolean accept(URL url) {
            return url.getHost().startsWith("v.redd.it") && url.getPath().endsWith(".m3u8");
        }

        @Override
        public String getName(URL url) {
            return getId(url) + ".avi";
        }

        @NotNull
        private String getId(URL url) {
            return url.getPath().substring(1, url.getPath().indexOf('/', 1));
        }

        @Override
        protected InputStream handle(URL url) {
            try {
                final String id = getId(url);

                URL videoSource = null;
                for (final int resolution : VIDEO_RESOLUTIONS) {
                    final URL candidate = new URL("https://v.redd.it/" + id + "/HLS_" + resolution + ".ts");
                    final HttpsURLConnection connection = (HttpsURLConnection) candidate.openConnection();
                    if (connection.getResponseCode() == 200) {
                        videoSource = candidate;
                        break;
                    }
                }

                if (videoSource == null)
                    throw new IllegalStateException("No video source for " + url);

                URL audioSource = null;
                for (final int bitrate : AUDIO_BITRATES) {
                    final URL candidate = new URL("https://v.redd.it/" + id + "/HLS_AUDIO_" + bitrate + "_K.aac");
                    final HttpsURLConnection connection = (HttpsURLConnection) candidate.openConnection();
                    if (connection.getResponseCode() == 200) {
                        audioSource = candidate;
                        break;
                    }
                }

                final List<String> command = new ArrayList<>(Arrays.asList(
                        "ffmpeg", "-i", videoSource.toString()
                ));
                if (audioSource != null) command.addAll(Arrays.asList("-i", audioSource.toString()));
                command.addAll(Arrays.asList("-loglevel", "error", "-f", "avi", "-qscale", "0", "-"));

                // Combine video.ts and audio.aac with ffmpeg
                final Process process = new ProcessBuilder(command.toArray(new String[0]))
                        .redirectError(INHERIT).start();

                return process.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }
    },

    FALLBACK() {
        @Override
        public boolean accept(URL url) {
            return true;
        }

        @Override
        protected InputStream handle(URL url) {
            try {
                return url.openStream();
            } catch (IOException e) {
                System.err.println("Failed to download " + url);
                e.printStackTrace();
            }

            return null;
        }
    };

    public String getName(final URL url) {
        return url.getFile().substring(url.getFile().lastIndexOf('/') + 1);
    }

    public boolean accept(final URL url) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    protected InputStream handle(final URL url) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public static Handlers getHandler(final URL url) {
        for (final Handlers handler : values()) {
            if (handler.accept(url)) {
                return handler;
            }
        }

        return null;
    }

    public static InputStream route(final URL url) {
        for (final Handlers handler : values()) {
            if (handler.accept(url)) {
                return handler.handle(url);
            }
        }

        return null;
    }
}
