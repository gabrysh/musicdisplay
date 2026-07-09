package com.haloclient.client.gui.click;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Subsonic / OpenSubsonic music source. Reads the currently playing track for the
 * configured user from the server ("getNowPlaying") and exposes it through the same
 * {@link SpotifyManager.MediaStatus} record used by the overlay, so it can be plugged
 * in as an alternative source next to Spotify.
 *
 * Subsonic does not report a live playback position, so the progress bar is estimated
 * client-side from the moment a track first appears in "now playing".
 */
public final class SubsonicManager {

    private static final int PORT = 8889;
    private static final String API_VERSION = "1.16.1";
    private static final String CLIENT_NAME = "halo";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final SubsonicManager INSTANCE = new SubsonicManager();
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "Halo Subsonic Poller");
        thread.setDaemon(true);
        return thread;
    });

    private String baseUrl = "";
    private String username = "";
    private String password = "";
    private volatile boolean authorized = false;

    private HttpServer server;
    private boolean serverRunning = false;

    private volatile SpotifyManager.MediaStatus currentStatus = SpotifyManager.MediaStatus.EMPTY;
    private volatile boolean polling = false;

    // Client-side progress estimation (server fallback mode)
    private String currentSongId = "";
    private long currentSongStartMs = 0L;

    // Local MPRIS player (real position/pause/controls) — preferred over the server API when present.
    private final LinuxMediaProvider mpris = new LinuxMediaProvider();
    private volatile boolean mprisActive = false;
    private volatile String mprisPlayer = null;
    private volatile long mprisBaseTimeMs = 0L;

    private SubsonicManager() {
    }

    public static SubsonicManager getInstance() {
        return INSTANCE;
    }

    public SpotifyManager.MediaStatus getStatus() {
        SpotifyManager.MediaStatus s = currentStatus;
        // In MPRIS mode, extrapolate the position between polls for a smooth, accurate bar.
        if (mprisActive && s.isPlaying() && s.durationSeconds() > 0.0) {
            double pos = Math.min(s.durationSeconds(),
                    s.positionSeconds() + (System.currentTimeMillis() - mprisBaseTimeMs) / 1000.0);
            return withPosition(s, pos);
        }
        return s;
    }

    private static SpotifyManager.MediaStatus withPosition(SpotifyManager.MediaStatus s, double pos) {
        return new SpotifyManager.MediaStatus(s.title(), s.artist(), pos, s.durationSeconds(),
                s.artworkPath(), s.trackId(), s.isPlaying(), s.shuffleState(), s.volumePercent(),
                s.liked(), s.repeatState());
    }

    public boolean isConfigured() {
        // Configured either via the server credentials or an active local MPRIS player.
        return (authorized && !baseUrl.isBlank() && !username.isBlank()) || mprisActive;
    }

    /** Whether playback can actually be controlled (only true when a local MPRIS player is driving). */
    public boolean isMprisControllable() {
        return mprisActive && mprisPlayer != null;
    }

    // ------------------------------------------------------------------
    // MPRIS playback controls (Linux local player)
    // ------------------------------------------------------------------

    public void mprisTogglePlayPause() {
        if (!isMprisControllable()) return;
        String player = mprisPlayer;
        // Optimistic UI flip until the next poll confirms it.
        SpotifyManager.MediaStatus s = getStatus();
        currentStatus = withPlaying(s, !s.isPlaying());
        mprisBaseTimeMs = System.currentTimeMillis();
        mpris.playPause(player);
    }

    public void mprisNext() {
        if (isMprisControllable()) mpris.next(mprisPlayer);
    }

    public void mprisPrevious() {
        if (isMprisControllable()) mpris.previous(mprisPlayer);
    }

    public void mprisSetVolume(int percent) {
        if (!isMprisControllable()) return;
        SpotifyManager.MediaStatus s = currentStatus;
        currentStatus = new SpotifyManager.MediaStatus(s.title(), s.artist(), s.positionSeconds(),
                s.durationSeconds(), s.artworkPath(), s.trackId(), s.isPlaying(), s.shuffleState(),
                Math.max(0, Math.min(100, percent)), s.liked(), s.repeatState());
        mpris.setVolume(mprisPlayer, percent);
    }

    private static SpotifyManager.MediaStatus withPlaying(SpotifyManager.MediaStatus s, boolean playing) {
        return new SpotifyManager.MediaStatus(s.title(), s.artist(), s.positionSeconds(), s.durationSeconds(),
                s.artworkPath(), s.trackId(), playing, s.shuffleState(), s.volumePercent(),
                s.liked(), s.repeatState());
    }

    // ------------------------------------------------------------------
    // Config persistence
    // ------------------------------------------------------------------

    private File getConfigFile() {
        return new File(Minecraft.getInstance().gameDirectory, "config/subsonic-config.json");
    }

    public synchronized void load() {
        File file = getConfigFile();
        if (!file.exists()) {
            return;
        }
        try (Reader reader = new FileReader(file, StandardCharsets.UTF_8)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            if (json.has("baseUrl")) this.baseUrl = normalizeBaseUrl(json.get("baseUrl").getAsString());
            if (json.has("username")) this.username = json.get("username").getAsString();
            if (json.has("password")) this.password = json.get("password").getAsString();
            if (json.has("authorized")) this.authorized = json.get("authorized").getAsBoolean();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void save() {
        File file = getConfigFile();
        File dir = file.getParentFile();
        if (dir != null && !dir.exists()) {
            dir.mkdirs();
        }
        JsonObject json = new JsonObject();
        json.addProperty("baseUrl", baseUrl);
        json.addProperty("username", username);
        json.addProperty("password", password);
        json.addProperty("authorized", authorized);
        try (Writer writer = new FileWriter(file, StandardCharsets.UTF_8)) {
            GSON.toJson(json, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String normalizeBaseUrl(String url) {
        if (url == null) return "";
        String trimmed = url.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        // Strip a trailing /rest if the user pasted the API root.
        if (trimmed.toLowerCase().endsWith("/rest")) {
            trimmed = trimmed.substring(0, trimmed.length() - "/rest".length());
        }
        return trimmed;
    }

    // ------------------------------------------------------------------
    // Auth helpers (token = md5(password + salt))
    // ------------------------------------------------------------------

    private static String md5Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String randomSalt() {
        String chars = "abcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        java.util.concurrent.ThreadLocalRandom rnd = java.util.concurrent.ThreadLocalRandom.current();
        for (int i = 0; i < 10; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }

    /** Builds a full REST URL for the given endpoint (without the leading '?'), appending auth params. */
    private String buildUrl(String endpoint, String extraQuery, String user, String pass, String base) {
        String salt = randomSalt();
        String token = md5Hex(pass + salt);
        StringBuilder sb = new StringBuilder();
        sb.append(base).append("/rest/").append(endpoint).append(".view?");
        sb.append("u=").append(URLEncoder.encode(user, StandardCharsets.UTF_8));
        sb.append("&t=").append(token);
        sb.append("&s=").append(salt);
        sb.append("&v=").append(API_VERSION);
        sb.append("&c=").append(CLIENT_NAME);
        sb.append("&f=json");
        if (extraQuery != null && !extraQuery.isBlank()) {
            sb.append('&').append(extraQuery);
        }
        return sb.toString();
    }

    /** Verifies credentials against the server via ping.view. */
    private boolean ping(String base, String user, String pass) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(buildUrl("ping", null, user, pass, base)))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return false;
            JsonObject resp = JsonParser.parseString(response.body()).getAsJsonObject()
                    .getAsJsonObject("subsonic-response");
            return resp != null && "ok".equals(resp.get("status").getAsString());
        } catch (Exception e) {
            System.err.println("[Halo/Subsonic] Ping failed: " + e.getMessage());
            return false;
        }
    }

    // ------------------------------------------------------------------
    // Polling
    // ------------------------------------------------------------------

    public void startPolling() {
        if (polling) return;
        polling = true;
        EXECUTOR.execute(() -> {
            while (polling) {
                try {
                    poll();
                    Thread.sleep(1000L);
                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void poll() {
        // 1) Prefer a local MPRIS player (real position, pause state, controls).
        SpotifyManager.MediaStatus mp = mpris.poll();
        if (mp != null) {
            mprisActive = true;
            mprisPlayer = mpris.lastPlayer();
            // Base the extrapolation on when Position was actually sampled (not "now", which
            // trails by the cost of the metadata/art calls) so the bar stays in sync.
            mprisBaseTimeMs = mpris.positionSampleMs();
            currentStatus = mp;
            return;
        }
        mprisActive = false;
        mprisPlayer = null;

        // 2) Fall back to the Subsonic server's getNowPlaying (position estimated).
        if (!(authorized && !baseUrl.isBlank() && !username.isBlank())) {
            currentStatus = SpotifyManager.MediaStatus.EMPTY;
            return;
        }
        pollServer();
    }

    private void pollServer() {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(buildUrl("getNowPlaying", null, username, password, baseUrl)))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                currentStatus = SpotifyManager.MediaStatus.EMPTY;
                return;
            }

            JsonObject resp = JsonParser.parseString(response.body()).getAsJsonObject()
                    .getAsJsonObject("subsonic-response");
            if (resp == null || !"ok".equals(getString(resp, "status", "failed"))) {
                currentStatus = SpotifyManager.MediaStatus.EMPTY;
                return;
            }

            JsonObject nowPlaying = resp.has("nowPlaying") && resp.get("nowPlaying").isJsonObject()
                    ? resp.getAsJsonObject("nowPlaying") : null;
            if (nowPlaying == null || !nowPlaying.has("entry")) {
                currentStatus = SpotifyManager.MediaStatus.EMPTY;
                return;
            }

            JsonArray entries = asArray(nowPlaying.get("entry"));
            JsonObject chosen = pickEntryForUser(entries);
            if (chosen == null) {
                currentStatus = SpotifyManager.MediaStatus.EMPTY;
                return;
            }

            String title = getString(chosen, "title", "");
            String artist = getString(chosen, "artist", "");
            double duration = chosen.has("duration") ? chosen.get("duration").getAsDouble() : 0.0;
            String songId = getString(chosen, "id", "");
            int minutesAgo = chosen.has("minutesAgo") ? chosen.get("minutesAgo").getAsInt() : 0;

            // Estimate playback position client-side.
            long now = System.currentTimeMillis();
            if (!songId.equals(currentSongId)) {
                currentSongId = songId;
                double elapsedGuess = Math.min(Math.max(minutesAgo * 60.0, 0.0), duration > 0 ? duration : 0.0);
                currentSongStartMs = now - (long) (elapsedGuess * 1000.0);
            }
            double position = (now - currentSongStartMs) / 1000.0;
            if (duration > 0) {
                position = Math.min(position, duration);
            }
            position = Math.max(0.0, position);

            String artPath = "";
            String coverArtId = getString(chosen, "coverArt", "");
            if (!coverArtId.isBlank()) {
                artPath = downloadCoverArt(coverArtId);
            }

            currentStatus = new SpotifyManager.MediaStatus(
                    title,
                    artist,
                    position,
                    duration,
                    artPath,
                    songId,
                    true,   // isPlaying (Subsonic has no live play/pause state)
                    false,  // shuffleState
                    100,    // volumePercent
                    false,  // liked
                    "off"   // repeatState
            );
        } catch (Exception e) {
            e.printStackTrace();
            currentStatus = SpotifyManager.MediaStatus.EMPTY;
        }
    }

    /** Prefer the entry belonging to the configured user; otherwise the most recently reported one. */
    private JsonObject pickEntryForUser(JsonArray entries) {
        JsonObject best = null;
        int bestMinutesAgo = Integer.MAX_VALUE;
        for (JsonElement el : entries) {
            if (el == null || !el.isJsonObject()) continue;
            JsonObject entry = el.getAsJsonObject();
            String user = getString(entry, "username", "");
            int minutesAgo = entry.has("minutesAgo") ? entry.get("minutesAgo").getAsInt() : 0;
            if (!username.isBlank() && username.equalsIgnoreCase(user)) {
                // Among the configured user's entries, take the freshest.
                if (minutesAgo <= bestMinutesAgo) {
                    bestMinutesAgo = minutesAgo;
                    best = entry;
                }
            } else if (best == null && username.isBlank()) {
                if (minutesAgo < bestMinutesAgo) {
                    bestMinutesAgo = minutesAgo;
                    best = entry;
                }
            }
        }
        return best;
    }

    private final Map<String, String> coverArtCache = new HashMap<>();

    private String downloadCoverArt(String coverArtId) {
        String hash = String.valueOf(Math.abs(coverArtId.hashCode()));
        synchronized (coverArtCache) {
            String cached = coverArtCache.get(coverArtId);
            if (cached != null && new File(cached).exists()) {
                return cached;
            }
        }
        File artFile = new File(Minecraft.getInstance().gameDirectory, "config/subsonic-art-" + hash + ".png");
        if (artFile.exists() && artFile.length() > 0) {
            synchronized (coverArtCache) {
                coverArtCache.put(coverArtId, artFile.getAbsolutePath());
            }
            return artFile.getAbsolutePath();
        }
        try {
            HttpClient client = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
            // NOTE: do not send "size" — with a size param some servers (e.g. Navidrome) return
            // WebP, which the STB image loader cannot decode. The original is JPEG/PNG.
            String url = buildUrl("getCoverArt", "id=" + URLEncoder.encode(coverArtId, StandardCharsets.UTF_8),
                    username, password, baseUrl);
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
            File tempFile = new File(Minecraft.getInstance().gameDirectory, "config/subsonic-art-temp-" + hash + ".png");
            if (!tempFile.getParentFile().exists()) {
                tempFile.getParentFile().mkdirs();
            }
            if (tempFile.exists()) {
                tempFile.delete();
            }
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() == 200 && response.body() != null && response.body().length > 0) {
                Files.write(tempFile.toPath(), response.body());
                Files.move(tempFile.toPath(), artFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                synchronized (coverArtCache) {
                    coverArtCache.put(coverArtId, artFile.getAbsolutePath());
                }
                return artFile.getAbsolutePath();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public void cleanArtworkCache() {
        File configDir = new File(Minecraft.getInstance().gameDirectory, "config");
        if (configDir.exists() && configDir.isDirectory()) {
            File[] files = configDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().startsWith("subsonic-art")) {
                        file.delete();
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Local setup web form (mirrors the Spotify setup flow)
    // ------------------------------------------------------------------

    public synchronized void startSetupServer() {
        if (serverRunning) {
            return;
        }
        try {
            server = HttpServer.create(new InetSocketAddress(PORT), 0);
            server.createContext("/setup", exchange -> {
                byte[] bytes = getSetupHtml(null).getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            });

            server.createContext("/submit", exchange -> {
                if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(405, -1);
                    return;
                }
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                Map<String, String> params = parseFormData(body);
                String url = normalizeBaseUrl(params.getOrDefault("baseUrl", ""));
                String user = params.getOrDefault("username", "").trim();
                String pass = params.getOrDefault("password", "");

                String error = null;
                if (url.isBlank() || user.isBlank() || pass.isBlank()) {
                    error = "Server URL, username and password are required.";
                } else if (!ping(url, user, pass)) {
                    error = "Could not authenticate with the Subsonic server. Check URL, username and password.";
                }

                if (error != null) {
                    byte[] bytes = getSetupHtml(error).getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
                    exchange.sendResponseHeaders(400, bytes.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(bytes);
                    }
                    return;
                }

                synchronized (SubsonicManager.this) {
                    this.baseUrl = url;
                    this.username = user;
                    this.password = pass;
                    this.authorized = true;
                    this.currentSongId = "";
                    save();
                }
                MusicManager.setActiveSource(MusicManager.Source.SUBSONIC);

                byte[] bytes = getSuccessHtml().getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }

                new Thread(() -> {
                    try {
                        Thread.sleep(1000);
                    } catch (Exception ignored) {
                    }
                    stopSetupServer();
                }).start();
            });

            server.start();
            serverRunning = true;
            System.out.println("[Halo/Subsonic] Setup server started on port " + PORT);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void stopSetupServer() {
        if (!serverRunning) {
            return;
        }
        try {
            server.stop(0);
            serverRunning = false;
            System.out.println("[Halo/Subsonic] Setup server stopped.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Map<String, String> parseFormData(String query) {
        Map<String, String> result = new HashMap<>();
        if (query == null || query.isBlank()) {
            return result;
        }
        for (String pair : query.split("&")) {
            int idx = pair.indexOf("=");
            try {
                if (idx > 0 && pair.length() > idx + 1) {
                    String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
                    String value = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
                    result.put(key, value);
                } else if (idx > 0) {
                    result.put(URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8), "");
                }
            } catch (Exception ignored) {
            }
        }
        return result;
    }

    private String getSetupHtml(String error) {
        String errorBlock = error == null ? "" :
                "<div class=\"bg-red-500/10 border border-red-500/40 text-red-300 text-xs rounded-md p-3 mb-4\">" + error + "</div>";
        return ("""
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <title>Subsonic Setup</title>
                    <script src="https://cdn.tailwindcss.com"></script>
                    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
                    <style> body { font-family: 'Inter', sans-serif; } </style>
                </head>
                <body class="bg-zinc-950 text-zinc-50 flex items-center justify-center min-h-screen antialiased px-4">
                    <div class="w-full max-w-md bg-zinc-900 border border-zinc-800 rounded-lg p-6 shadow-2xl relative overflow-hidden">
                        <div class="absolute top-0 left-0 w-full h-[2px] bg-gradient-to-r from-sky-500 to-indigo-500"></div>
                        <div class="flex items-center space-x-2 mb-4">
                            <h2 class="text-xl font-bold tracking-tight text-white">Subsonic Integration</h2>
                        </div>
                        <p class="text-sm text-zinc-400 mb-6 leading-relaxed">
                            Connect Minecraft to your Subsonic / Navidrome / Airsonic server. The overlay will show what
                            you are currently playing.
                        </p>
                        %ERROR%
                        <form action="/submit" method="POST" class="space-y-4">
                            <div>
                                <label class="block text-xs font-semibold uppercase tracking-wider text-zinc-400 mb-1">Server URL</label>
                                <input type="text" name="baseUrl" value="%URL%" required placeholder="https://music.example.com"
                                    class="w-full px-3 py-2 bg-zinc-950 border border-zinc-800 rounded-md text-zinc-100 placeholder-zinc-700 focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-sky-500 transition">
                            </div>
                            <div>
                                <label class="block text-xs font-semibold uppercase tracking-wider text-zinc-400 mb-1">Username</label>
                                <input type="text" name="username" value="%USER%" required placeholder="Enter username"
                                    class="w-full px-3 py-2 bg-zinc-950 border border-zinc-800 rounded-md text-zinc-100 placeholder-zinc-700 focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-sky-500 transition">
                            </div>
                            <div>
                                <label class="block text-xs font-semibold uppercase tracking-wider text-zinc-400 mb-1">Password</label>
                                <input type="password" name="password" required placeholder="Enter password"
                                    class="w-full px-3 py-2 bg-zinc-950 border border-zinc-800 rounded-md text-zinc-100 placeholder-zinc-700 focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-sky-500 transition">
                            </div>
                            <button type="submit" class="w-full py-2 bg-white hover:bg-zinc-200 text-zinc-950 font-bold rounded-md transition shadow-md">
                                Connect &amp; use Subsonic
                            </button>
                        </form>
                    </div>
                </body>
                </html>
                """)
                .replace("%ERROR%", errorBlock)
                .replace("%URL%", baseUrl == null ? "" : baseUrl)
                .replace("%USER%", username == null ? "" : username);
    }

    private String getSuccessHtml() {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <title>Subsonic Connected</title>
                    <script src="https://cdn.tailwindcss.com"></script>
                    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
                    <style> body { font-family: 'Inter', sans-serif; } </style>
                </head>
                <body class="bg-zinc-950 text-zinc-50 flex items-center justify-center min-h-screen antialiased px-4">
                    <div class="w-full max-w-md bg-zinc-900 border border-zinc-800 rounded-lg p-6 shadow-2xl relative overflow-hidden text-center">
                        <div class="absolute top-0 left-0 w-full h-[2px] bg-sky-500"></div>
                        <h2 class="text-xl font-bold tracking-tight text-white mb-2">Subsonic Connected</h2>
                        <p class="text-sm text-zinc-400 mb-4 leading-relaxed">
                            Subsonic is now the active music source. You can close this tab and return to the game.
                        </p>
                    </div>
                </body>
                </html>
                """;
    }

    // ------------------------------------------------------------------
    // JSON helpers
    // ------------------------------------------------------------------

    private static String getString(JsonObject obj, String key, String fallback) {
        if (obj != null && obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsString();
        }
        return fallback;
    }

    private static JsonArray asArray(JsonElement el) {
        if (el == null || el.isJsonNull()) return new JsonArray();
        if (el.isJsonArray()) return el.getAsJsonArray();
        // Some servers return a single object instead of an array.
        JsonArray arr = new JsonArray();
        if (el.isJsonObject()) arr.add(el);
        return arr;
    }
}
