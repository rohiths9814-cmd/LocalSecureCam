package com.localsecurecam.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Central configuration for LocalSecureCam.
 * Bound from the "app.*" keys in application.properties so camera URLs,
 * the recordings directory, stream tuning and credentials live in ONE place
 * instead of being hardcoded across multiple classes.
 */
@Component
@ConfigurationProperties(prefix = "app")
public class CameraProperties {

    /** Directory where recordings are written (e.g. a mounted pendrive/SSD). */
    private String recordingsDir = "/mnt/cctv/recordings";

    /** Map of cameraId -> RTSP URL. */
    private Map<String, String> cameras = new LinkedHashMap<>();

    private Stream stream = new Stream();
    private Auth auth = new Auth();

    // ---- Live preview tuning ----
    public static class Stream {
        private int fps = 5;
        private int scaleWidth = 640;
        private int quality = 8; // ffmpeg -q:v (lower = better quality, larger frames)

        public int getFps() { return fps; }
        public void setFps(int fps) { this.fps = fps; }
        public int getScaleWidth() { return scaleWidth; }
        public void setScaleWidth(int scaleWidth) { this.scaleWidth = scaleWidth; }
        public int getQuality() { return quality; }
        public void setQuality(int quality) { this.quality = quality; }
    }

    // ---- Dashboard / API credentials ----
    public static class Auth {
        private String username = "admin";
        private String password = "changeme";

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public String getRecordingsDir() { return recordingsDir; }
    public void setRecordingsDir(String recordingsDir) { this.recordingsDir = recordingsDir; }

    public Map<String, String> getCameras() { return cameras; }
    public void setCameras(Map<String, String> cameras) { this.cameras = cameras; }

    public Stream getStream() { return stream; }
    public void setStream(Stream stream) { this.stream = stream; }

    public Auth getAuth() { return auth; }
    public void setAuth(Auth auth) { this.auth = auth; }
}
