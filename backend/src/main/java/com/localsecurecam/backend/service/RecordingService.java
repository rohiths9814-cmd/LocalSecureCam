package com.localsecurecam.backend.service;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;

@Service
public class RecordingService {

    private final Map<String, Process> processes = new HashMap<>();

    private final Map<String, String> cameraUrls = Map.of(
        "camera1", "rtsp://192.168.31.196:554/",
        "camera2", "rtsp://192.168.31.107:554/"
    );

    public synchronized void startRecording(String cameraId) {

        if (processes.containsKey(cameraId)) {
            System.out.println(cameraId + " already recording");
            return;
        }

        String rtspUrl = cameraUrls.get(cameraId);
        if (rtspUrl == null) {
            throw new RuntimeException("Unknown camera: " + cameraId);
        }

        try {
            Path outputDir = Paths.get(
                "recordings",
                cameraId,
                LocalDate.now().toString()
            );

            Files.createDirectories(outputDir);

            String outputTemplate = outputDir + "/%H-%M.mkv";

            List<String> command = List.of(
                "ffmpeg",

                "-rtsp_transport", "tcp",
                "-use_wallclock_as_timestamps", "1",
                "-fflags", "+genpts",

                "-i", rtspUrl,

                // ✅ FIXED ASPECT RATIO + ANGLE
                "-vf", "transpose=clock,scale=1920:-2",

                // ✅ HARDWARE ENCODING (PI SAFE)
                "-c:v", "h264_v4l2m2m",
                "-b:v", "2000k",
                "-maxrate", "2000k",
                "-bufsize", "4000k",

                // ✅ SEGMENTED RECORDING
                "-f", "segment",
                "-segment_time", "300",
                "-reset_timestamps", "1",
                "-strftime", "1",

                outputTemplate
            );

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            processes.put(cameraId, process);
            System.out.println("STARTED recording: " + cameraId);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized void stopRecording(String cameraId) {
        Process process = processes.remove(cameraId);
        if (process != null) {
            process.destroy();
            System.out.println("STOPPED recording: " + cameraId);
        }
    }
}
