package com.localsecurecam.backend.service;

import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RecordingService {

    private static final String FFMPEG = "/usr/bin/ffmpeg";
    private static final String BASE_DIR = "/home/pi/LocalSecureCam/recordings";

    private static final long STALL_TIMEOUT_SEC = 45;
    private static final long FORCED_RESTART_SEC = 2 * 60 * 60;

    private final Map<String, Process> processes = new ConcurrentHashMap<>();
    private final Map<String, Boolean> autoRestart = new ConcurrentHashMap<>();
    private final Map<String, Instant> lastStart = new ConcurrentHashMap<>();
    private final Map<String, Path> activeFile = new ConcurrentHashMap<>();

    private final Map<String, String> cameraUrls = Map.of(
            "camera1", "rtsp://192.168.31.196:554/",
            "camera2", "rtsp://192.168.31.107:554/"
    );

    public RecordingService() {
        new Thread(this::fileHeartbeatMonitor, "file-heartbeat").start();
        new Thread(this::scheduledRestartMonitor, "periodic-restart").start();
    }

    // ===================== START =====================
    public synchronized void startRecording(String cameraId) {
        String rtspUrl = cameraUrls.get(cameraId);
        if (rtspUrl == null) throw new RuntimeException("Unknown camera");

        autoRestart.put(cameraId, true);

        // ALWAYS clear stale state before start
        cleanupState(cameraId);

        try {
            Path dir = Paths.get(BASE_DIR, cameraId, LocalDate.now().toString());
            Files.createDirectories(dir);

            Path outputPattern = dir.resolve("%Y-%m-%d_%H-%M-%S.mp4");

            ProcessBuilder pb = new ProcessBuilder(
                    FFMPEG,

                    "-rtsp_transport", "tcp",
                    "-probesize", "10M",
                    "-analyzeduration", "10M",

                    "-fflags", "+genpts",
                    "-use_wallclock_as_timestamps", "1",
                    "-avoid_negative_ts", "make_zero",

                    "-i", rtspUrl,

                    "-map", "0:v:0",
                    "-c:v", "copy",

                    "-movflags", "+frag_keyframe+empty_moov",

                    "-f", "segment",
                    "-segment_time", "300",
                    "-reset_timestamps", "1",
                    "-strftime", "1",

                    outputPattern.toString()
            );

            Process p = pb.redirectErrorStream(true).start();

            processes.put(cameraId, p);
            lastStart.put(cameraId, Instant.now());

            detectActiveFile(cameraId, dir);
            new Thread(() -> waitForExit(cameraId, p)).start();

            System.out.println("✅ Recording started: " + cameraId);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ===================== STOP =====================
    public synchronized void stopRecording(String cameraId) {
        autoRestart.put(cameraId, false);
        cleanupState(cameraId);
    }

    // ===================== HARD CLEANUP =====================
    private synchronized void cleanupState(String cam) {
        Process p = processes.remove(cam);
        if (p != null) p.destroyForcibly();

        activeFile.remove(cam);
        lastStart.remove(cam);
    }

    // ===================== EXIT WATCH =====================
    private void waitForExit(String cam, Process p) {
        try {
            p.waitFor();
            if (!Boolean.TRUE.equals(autoRestart.get(cam))) return;

            System.err.println("🔁 FFmpeg exited for " + cam);
            Thread.sleep(5000);
            startRecording(cam);

        } catch (InterruptedException ignored) {}
    }

    // ===================== FILE HEARTBEAT =====================
    private void fileHeartbeatMonitor() {
        while (true) {
            try {
                Thread.sleep(10_000);

                for (String cam : processes.keySet()) {
                    Path f = activeFile.get(cam);
                    if (f == null || !Files.exists(f)) continue;

                    long silent =
                            Instant.now().getEpochSecond() -
                            Files.getLastModifiedTime(f).toInstant().getEpochSecond();

                    if (silent > STALL_TIMEOUT_SEC) {
                        System.err.println("⚠ File stall detected for " + cam);
                        startRecording(cam); // HARD restart
                    }
                }
            } catch (Exception ignored) {}
        }
    }

    // ===================== PERIODIC RESTART =====================
    private void scheduledRestartMonitor() {
        while (true) {
            try {
                Thread.sleep(60_000);

                for (String cam : processes.keySet()) {
                    Instant started = lastStart.get(cam);
                    if (started == null) continue;

                    if (Instant.now().getEpochSecond() - started.getEpochSecond()
                            > FORCED_RESTART_SEC) {

                        System.out.println("♻ Periodic restart for " + cam);
                        startRecording(cam);
                    }
                }
            } catch (Exception ignored) {}
        }
    }

    // ===================== ACTIVE FILE DETECTION =====================
    private void detectActiveFile(String cam, Path dir) {
        new Thread(() -> {
            try {
                while (!activeFile.containsKey(cam)) {
                    Files.list(dir)
                            .filter(p -> p.toString().endsWith(".mp4"))
                            .max((a, b) -> {
                                try {
                                    return Files.getLastModifiedTime(a)
                                            .compareTo(Files.getLastModifiedTime(b));
                                } catch (Exception e) {
                                    return 0;
                                }
                            })
                            .ifPresent(p -> activeFile.put(cam, p));

                    Thread.sleep(2000);
                }
            } catch (Exception ignored) {}
        }).start();
    }
}
