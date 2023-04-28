package blue.lhf.rescrape;

import mx.kenzie.argo.Json;
import org.jetbrains.annotations.NotNull;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.Map;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.Locale.ROOT;

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
            return url.getFile().substring(1, url.getFile().indexOf('-'));
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
            return getId(url) + ".mp4";
        }

        @NotNull
        private String getId(URL url) {
            return url.getPath().substring(1, url.getPath().indexOf('/', 1));
        }

        @Override
        protected InputStream handle(URL url) {
            try {
                final String id = getId(url);
                final Path tmp = Files.createTempDirectory("rescrape");

                for (final int resolution : VIDEO_RESOLUTIONS) {
                    final URL videoSource = new URL("https://v.redd.it/" + id + "/HLS_" + resolution + ".ts");
                    final HttpsURLConnection connection = (HttpsURLConnection) videoSource.openConnection();
                    if (connection.getResponseCode() == 200) {
                        final Path path = tmp.resolve("video.ts");
                        Files.copy(connection.getInputStream(), path, REPLACE_EXISTING);
                        break;
                    }
                }

                for (final int bitrate : AUDIO_BITRATES) {
                    final URL audioSource = new URL("https://v.redd.it/" + id + "/HLS_AUDIO_" + bitrate + "_K.aac");
                    final HttpsURLConnection connection = (HttpsURLConnection) audioSource.openConnection();
                    if (connection.getResponseCode() == 200) {
                        final Path path = tmp.resolve("audio.aac");
                        Files.copy(connection.getInputStream(), path, REPLACE_EXISTING);
                        break;
                    }
                }

                // Combine video.ts and audio.aac with ffmpeg
                final Process process = new ProcessBuilder("ffmpeg", "-i", "video.ts", "-i", "audio.aac", "-c", "copy", "output.mp4")
                    .directory(tmp.toFile())
                    .start();

                if (process.waitFor() != 0) {
                    process.getErrorStream().transferTo(System.err);
                }
                return Files.newInputStream(tmp.resolve("output.mp4"));
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
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
