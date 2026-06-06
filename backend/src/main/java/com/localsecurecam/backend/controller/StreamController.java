package com.localsecurecam.backend.controller;

import com.localsecurecam.backend.config.CameraProperties;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.io.OutputStream;

@RestController
public class StreamController {

    private static final String FFMPEG = "ffmpeg";

    private final CameraProperties props;

    public StreamController(CameraProperties props) {
        this.props = props;
    }

    @GetMapping("/api/stream/{cameraId}")
    public void stream(@PathVariable String cameraId, HttpServletResponse response) {

        String rtsp = props.getCameras().get(cameraId);
        if (rtsp == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        CameraProperties.Stream cfg = props.getStream();

        // FFmpeg's mpjpeg muxer emits proper multipart boundaries ("--ffmpeg"),
        // which browsers render natively in an <img> tag.
        ProcessBuilder pb = new ProcessBuilder(
                FFMPEG,
                "-rtsp_transport", "tcp",
                "-i", rtsp,
                "-an",                                   // no audio for the preview
                "-r", String.valueOf(cfg.getFps()),      // throttle frame rate
                "-vf", "scale=" + cfg.getScaleWidth() + ":-1", // downscale to save CPU
                "-q:v", String.valueOf(cfg.getQuality()),
                "-f", "mpjpeg",
                "-"
        );
        pb.redirectErrorStream(false);

        Process process = null;
        try {
            response.setContentType("multipart/x-mixed-replace;boundary=ffmpeg");

            process = pb.start();

            try (InputStream is = process.getInputStream();
                 OutputStream os = response.getOutputStream()) {

                byte[] buffer = new byte[16 * 1024];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                    os.flush();
                }
            }

        } catch (Exception e) {
            // Client disconnect (tab closed / refreshed) lands here — normal, not an error.
            System.out.println("Stream ended for " + cameraId + ": " + e.getMessage());
        } finally {
            // CRITICAL: always kill FFmpeg, otherwise transcoders pile up and crash the Pi.
            if (process != null) {
                process.destroyForcibly();
            }
        }
    }
}
