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
    private static final long FORCED_RESTART_SEC = 2 * 60 * 60; // 2 hours
    // ==================

    private final Map<String, Process> processes = new ConcurrentHashMap<>();
    private final Map<String, Instant> lastStart = new ConcurrentHashMap<>();
    private final Map<String, Boolean> autoRestart = new ConcurrentHashMap<>();

    private final Map<String, String> cameraUrls = Map.of(
        "camera1", "rtsp://192.168.31.196:554/",
        "camera2", "rtsp://192.168.31.107:554/"
    );

    private final HealthService healthService;

    // ===================== CONSTRUCTOR =====================
    public RecordingService(HealthService healthService) {
        this.healthService = healthService;
        new Thread(this::scheduledRestartMonitor, "ffmpeg-periodic-restart").start();
    }

    // ===================== START =====================
    public synchronized void startRecording(String cameraId) {

        if (processes.containsKey(cameraId)) return;

        String rtspUrl = cameraUrls.get(cameraId);
        if (rtspUrl == null) {
            throw new RuntimeException("Unknown camera: " + cameraId);
        }

        autoRestart.put(cameraId, true);

        try {
            Path dir = Paths.get(
                BASE_DIR,
                cameraId,
                LocalDate.now().toString()
            );
            Files.createDirectories(dir);

            String output =
                dir.resolve("%Y-%m-%d_%H-%M-%S.mp4").toString();

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

                output
            );

            pb.redirectErrorStream(true);
            Process process = pb.start();

            processes.put(cameraId, process);
            lastStart.put(cameraId, Instant.now());

            healthService.setState(
                cameraId,
                HealthService.CameraState.RECORDING
            );

            // heartbeat when start succeeds
            healthService.updateLastSegment(cameraId);

            new Thread(
                () -> exitWatchdog(cameraId, process),
                "ffmpeg-exit-" + cameraId
            ).start();

            System.out.println("✅ Recording started: " + cameraId);

        } catch (Exception e) {
            throw new RuntimeException("FFmpeg failed for " + cameraId, e);
        }
    }

    // ===================== STOP =====================
    public synchronized void stopRecording(String cameraId) {
        autoRestart.put(cameraId, false);

        Process p = processes.remove(cameraId);
        if (p != null) p.destroyForcibly();

        lastStart.remove(cameraId);

        healthService.setState(
            cameraId,
            HealthService.CameraState.STOPPED
        );

        System.out.println("🛑 Recording stopped: " + cameraId);
    }

    // ===================== EXIT WATCHDOG =====================
    private void exitWatchdog(String cameraId, Process process) {
        try {
            int code = process.waitFor();
            processes.remove(cameraId);

            if (!Boolean.TRUE.equals(autoRestart.get(cameraId))) return;

            System.err.println(
                "🔁 FFmpeg exited for " + cameraId + " (code=" + code + ")"
            );

            healthService.setState(
                cameraId,
                HealthService.CameraState.RESTARTING
            );

            Thread.sleep(7000);
            startRecording(cameraId);

        } catch (InterruptedException ignored) {}
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

                        Process p = processes.remove(cam);
                        if (p != null) p.destroyForcibly();

                        Thread.sleep(3000);
                        startRecording(cam);
                    }
                }
            } catch (InterruptedException ignored) {}
        }
    }
}
