package com.localsecurecam.backend.service;

import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RecordingService {

    private static final String FFMPEG = "/usr/bin/ffmpeg";
    private static final String BASE_DIR = "/home/pi/LocalSecureCam/recordings";

    private final Map<String, Process> processes = new ConcurrentHashMap<>();

    private final Map<String, String> cameraUrls = Map.of(
        "camera1", "rtsp://192.168.31.196:554/",
        "camera2", "rtsp://192.168.31.107:554/"
    );

    public synchronized void startRecording(String cameraId) {

        if (processes.containsKey(cameraId)) {
            System.out.println("⚠ Already recording: " + cameraId);
            return;
        }

        String rtspUrl = cameraUrls.get(cameraId);
        if (rtspUrl == null) {
            throw new RuntimeException("Unknown camera: " + cameraId);
        }

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
                "/usr/bin/ffmpeg",
                        
                "-rtsp_transport", "tcp",
                "-probesize", "10M",
                "-analyzeduration", "10M",
                        
                "-fflags", "+genpts",
                "-use_wallclock_as_timestamps", "1",
                "-avoid_negative_ts", "make_zero",
                        
                "-i", rtspUrl,
                        
                // COPY (NO RE-ENCODE)
                "-map", "0:v:0",
                "-c:v", "copy",
                        
                // SAFE MP4
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

            new Thread(() -> log(cameraId, process)).start();

            System.out.println("✅ Recording started: " + cameraId);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("FFmpeg failed", e);
        }
    }

    public synchronized void stopRecording(String cameraId) {
        Process p = processes.remove(cameraId);
        if (p != null) {
            p.destroy();
            System.out.println("🛑 Recording stopped: " + cameraId);
        }
    }

    private void log(String cam, Process p) {
        try (BufferedReader br =
                 new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println("[" + cam + "] " + line);
            }
        } catch (IOException ignored) {}
    }
}
