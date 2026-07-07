package com.haloclient.client.gui.click;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class WindowsMediaStatusProvider {

    private static final long POLL_INTERVAL_MS = 1_500L;
    private static final Pattern STRING_FIELD = Pattern.compile("\"%s\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"");
    private static final Pattern NUMBER_FIELD = Pattern.compile("\"%s\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)");

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "Halo Windows Media Poller");
        thread.setDaemon(true);
        return thread;
    });

    private static volatile MediaStatus status = MediaStatus.EMPTY;
    private static volatile long lastPollMs;
    private static volatile boolean polling;

    private WindowsMediaStatusProvider() {
    }

    static MediaStatus getStatus() {
        long now = System.currentTimeMillis();
        if (!polling && now - lastPollMs > POLL_INTERVAL_MS && isWindows()) {
            polling = true;
            lastPollMs = now;
            EXECUTOR.execute(() -> {
                try {
                    status = queryWindowsMedia();
                } catch (Exception ignored) {
                    status = MediaStatus.EMPTY;
                } finally {
                    polling = false;
                }
            });
        }
        return status;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    private static MediaStatus queryWindowsMedia() throws Exception {
        String script = """
                $ErrorActionPreference = 'SilentlyContinue';
                function Await($operation) {
                    [System.WindowsRuntimeSystemExtensions]::AsTask($operation).GetAwaiter().GetResult()
                }
                $manager = Await ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager, Windows.Media.Control, ContentType=WindowsRuntime]::RequestAsync());
                $session = $null;
                foreach ($candidate in $manager.GetSessions()) {
                    $playbackInfo = $candidate.GetPlaybackInfo();
                    if ($null -ne $playbackInfo -and $playbackInfo.PlaybackStatus.ToString() -eq 'Playing') {
                        $session = $candidate;
                        break;
                    }
                }
                if ($null -eq $session) {
                    $session = $manager.GetCurrentSession();
                }
                if ($null -eq $session) { '{}' ; exit }
                $source = [string]$session.SourceAppUserModelId;
                $playback = $session.GetPlaybackInfo();
                $playbackStatus = if ($null -ne $playback) { [string]$playback.PlaybackStatus } else { '' };
                $props = Await ($session.TryGetMediaPropertiesAsync());
                $timeline = $session.GetTimelineProperties();
                $safeName = (($props.Title + '-' + $props.Artist) -replace '[^a-zA-Z0-9._-]', '_');
                if ([string]::IsNullOrWhiteSpace($safeName)) { $safeName = 'current-media-art'; }
                $artDir = Join-Path $env:TEMP 'halo-media-art';
                [void][System.IO.Directory]::CreateDirectory($artDir);
                $artPath = Join-Path $artDir ($safeName + '.bin');
                $artWritten = '';
                if ($null -ne $props.Thumbnail) {
                    $stream = Await ($props.Thumbnail.OpenReadAsync());
                    if ($null -ne $stream -and $stream.Size -gt 0) {
                        $reader = [Windows.Storage.Streams.DataReader, Windows.Storage.Streams, ContentType=WindowsRuntime]::new($stream);
                        [void](Await ($reader.LoadAsync([uint32]$stream.Size)));
                        $bytes = New-Object byte[] ([int]$stream.Size);
                        $reader.ReadBytes($bytes);
                        [System.IO.File]::WriteAllBytes($artPath, $bytes);
                        $artWritten = $artPath;
                    }
                }
                [pscustomobject]@{
                    Title = [string]$props.Title;
                    Artist = [string]$props.Artist;
                    Position = [double]$timeline.Position.TotalSeconds;
                    Duration = [double]$timeline.EndTime.TotalSeconds;
                    Artwork = [string]$artWritten;
                    Source = [string]$source;
                    PlaybackStatus = [string]$playbackStatus;
                    Provider = 'SMTC'
                } | ConvertTo-Json -Compress
                """;

        Process process = new ProcessBuilder(
                "powershell.exe",
                "-NoProfile",
                "-ExecutionPolicy", "Bypass",
                "-Command", script
        ).redirectErrorStream(true).start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
        }
        process.waitFor();

        String json = output.toString();
        String title = readString(json, "Title");
        String artist = readString(json, "Artist");
        String artwork = readString(json, "Artwork");
        double position = readNumber(json, "Position");
        double duration = readNumber(json, "Duration");

        if (title.isBlank() && artist.isBlank()) {
            return queryWindowTitleFallback();
        }

        if (!artwork.isBlank() && !new File(artwork).isFile()) {
            artwork = "";
        }

        return new MediaStatus(title, artist, position, duration, artwork);
    }

    private static MediaStatus queryWindowTitleFallback() throws Exception {
        String script = """
                $ErrorActionPreference = 'SilentlyContinue';
                $preferred = @(
                    'Spotify',
                    'Chrome',
                    'msedge',
                    'firefox',
                    'brave',
                    'opera',
                    'vlc',
                    'Music.UI',
                    'iTunes',
                    'wmplayer'
                );
                $items = Get-Process |
                    Where-Object { -not [string]::IsNullOrWhiteSpace($_.MainWindowTitle) } |
                    ForEach-Object {
                        [pscustomobject]@{
                            Process = $_.ProcessName;
                            Title = $_.MainWindowTitle
                        }
                    };
                $chosen = $null;
                foreach ($name in $preferred) {
                    $pattern = '*' + $name + '*';
                    $chosen = $items | Where-Object { $_.Process -like $pattern -and $_.Title -notmatch 'Minecraft|Codex|IntelliJ|Visual Studio|Discord' } | Select-Object -First 1;
                    if ($null -ne $chosen) { break; }
                }
                if ($null -eq $chosen) {
                    $chosen = $items | Where-Object { $_.Title -match ' - | — | – ' -and $_.Title -notmatch 'Minecraft|Codex|IntelliJ|Visual Studio|Discord' } | Select-Object -First 1;
                }
                if ($null -eq $chosen) { '{}' ; exit }
                $title = [string]$chosen.Title;
                $artist = [string]$chosen.Process;
                $parts = $title -split '\\s+[-—–]\\s+', 2;
                if ($parts.Length -eq 2) {
                    $artist = $parts[0];
                    $title = $parts[1];
                }
                [pscustomobject]@{
                    Title = [string]$title;
                    Artist = [string]$artist;
                    Position = 0;
                    Duration = 0;
                    Artwork = '';
                    Provider = 'WindowTitle'
                } | ConvertTo-Json -Compress
                """;

        Process process = new ProcessBuilder(
                "powershell.exe",
                "-NoProfile",
                "-ExecutionPolicy", "Bypass",
                "-Command", script
        ).redirectErrorStream(true).start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
        }
        process.waitFor();

        String json = output.toString();
        String title = readString(json, "Title");
        String artist = readString(json, "Artist");
        if (title.isBlank() && artist.isBlank()) {
            return MediaStatus.EMPTY;
        }
        return new MediaStatus(title, artist, 0.0d, 0.0d, "");
    }

    private static String readString(String json, String field) {
        Matcher matcher = Pattern.compile(STRING_FIELD.pattern().formatted(field)).matcher(json);
        if (!matcher.find()) {
            return "";
        }
        return matcher.group(1)
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    private static double readNumber(String json, String field) {
        Matcher matcher = Pattern.compile(NUMBER_FIELD.pattern().formatted(field)).matcher(json);
        if (!matcher.find()) {
            return 0.0d;
        }
        try {
            return Double.parseDouble(matcher.group(1));
        } catch (NumberFormatException ignored) {
            return 0.0d;
        }
    }

    record MediaStatus(String title, String artist, double positionSeconds, double durationSeconds, String artworkPath) {
        static final MediaStatus EMPTY = new MediaStatus("", "", 0.0d, 0.0d, "");

        boolean hasMedia() {
            return !title.isBlank() || !artist.isBlank();
        }

        float progress() {
            if (durationSeconds <= 0.0d) {
                return 0.0f;
            }
            return Math.max(0.0f, Math.min(1.0f, (float) (positionSeconds / durationSeconds)));
        }
    }
}
