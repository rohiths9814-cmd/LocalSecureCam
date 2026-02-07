package com.localsecurecam.backend.service;

import org.springframework.stereotype.Service;

import java.nio.file.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RecordingService {

    private static final String FFMPEG = "/usr/bin/ffmpeg";
    private static final String BASE_DIR = "/home/pi/LocalSecureCam/recordings";

    // ===== TUNING =====
    private static final long STALL_TIMEOUT_SEC = 45;
    private static final long FORCED_RESTART_SEC = 2 * 60 * 60; // 2 hours
    // ==================

    private final Map<String, Process> processes = new ConcurrentHashMap<>();
    private final Map<String, Instant> lastStart = new ConcurrentHashMap<>();
    private final Map<String, Path> activeFile = new ConcurrentHashMap<>();
    private final Map<String, Boolean> autoRestart = new ConcurrentHashMap<>();

    private final Map<String, String> cameraUrls = Map.of(
            "camera1", "rtsp://192.168.31.196:554/",
            "camera2", "rtsp://192.168.31.107:554/"
    );

    private final HealthService healthService;

    // ===================== CONSTRUCTOR =====================
    public RecordingService(HealthService healthService) {
        this.healthService = healthService;

        new Thread(this::fileHeartbeatMonitor, "ffmpeg-file-heartbeat").start();
        new Thread(this::scheduledRestartMonitor, "ffmpeg-periodic-restart").start();
    }

    // ===================== START =====================
    public synchronized void startRecording(String cameraId) {

        String rtspUrl = cameraUrls.get(cameraId);
        if (rtspUrl == null) {
            throw new RuntimeException("Unknown camera: " + cameraId);
        }

        autoRestart.put(cameraId, true);

        // HARD RESET STATE (important)
        cleanupState(cameraId);

        try {
            Path dir = Paths.get(
                    BASE_DIR,
                    cameraId,
                    LocalDate.now().toString()
            );
            Files.createDirectories(dir);

            Path outputPattern =
                    dir.resolve("%Y-%m-%d_%H-%M-%S.mp4");

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

            Process process = pb
                    .redirectErrorStream(true)
                    .start();

            processes.put(cameraId, process);
            lastStart.put(cameraId, Instant.now());

            healthService.setState(
                    cameraId,
                    HealthService.CameraState.RECORDING
            );

            detectActiveFile(cameraId, dir);

            new Thread(
                    () -> waitForExit(cameraId, process),
                    "ffmpeg-exit-" + cameraId
            ).start();

            System.out.println("✅ Recording started: " + cameraId);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ===================== STOP =====================
    public synchronized void stopRecording(String cameraId) {
        autoRestart.put(cameraId, false);
        cleanupState(cameraId);

        healthService.setState(
                cameraId,
                HealthService.CameraState.STOPPED
        );

        System.out.println("🛑 Recording stopped: " + cameraId);
    }

    // ===================== CLEANUP =====================
    private synchronized void cleanupState(String cameraId) {
        Process p = processes.remove(cameraId);
        if (p != null) {
            p.destroyForcibly();
        }

        activeFile.remove(cameraId);
        lastStart.remove(cameraId);
    }

    // ===================== EXIT WATCHDOG =====================
    private void waitForExit(String cameraId, Process process) {
        try {
            process.waitFor();

            if (!Boolean.TRUE.equals(autoRestart.get(cameraId))) {
                return;
            }

            System.err.println("🔁 FFmpeg exited for " + cameraId);

            healthService.setState(
                    cameraId,
                    HealthService.CameraState.RESTARTING
            );

            Thread.sleep(5000);
            startRecording(cameraId);

        } catch (InterruptedException ignored) {}
    }

    // ===================== FILE HEARTBEAT =====================
    private void fileHeartbeatMonitor() {
        while (true) {
            try {
                Thread.sleep(10_000);

                for (String cam : processes.keySet()) {
                    Path file = activeFile.get(cam);
                    if (file == null || !Files.exists(file)) continue;

                    long silent =
                            Instant.now().getEpochSecond()
                                    - Files.getLastModifiedTime(file)
                                    .toInstant()
                                    .getEpochSecond();

                    if (silent > STALL_TIMEOUT_SEC) {
                        System.err.println("⚠ File stall detected for " + cam);

                        healthService.setState(
                                cam,
                                HealthService.CameraState.RESTARTING
                        );

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

                    long uptime =
                            Instant.now().getEpochSecond()
                                    - started.getEpochSecond();

                    if (uptime > FORCED_RESTART_SEC) {
                        System.out.println("♻ Periodic restart for " + cam);

                        healthService.setState(
                                cam,
                                HealthService.CameraState.RESTARTING
                        );

                        startRecording(cam);
                    }
                }
            } catch (InterruptedException ignored) {}
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
                                            .compareTo(
                                                    Files.getLastModifiedTime(b)
                                            );
                                } catch (Exception e) {
                                    return 0;
                                }
                            })
                            .ifPresent(p -> activeFile.put(cam, p));

                    Thread.sleep(2000);
                }
            } catch (Exception ignored) {}
        }, "active-file-detector-" + cam).start();
    }
}
