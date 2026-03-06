package com.localsecurecam.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;

import java.io.InputStream;

@RestController
public class StreamController {

    private static final String FFMPEG = "ffmpeg";
    private static final String RTSP = "rtsp://192.168.31.196:554/";

    @GetMapping("/api/stream")
    public void stream(HttpServletResponse response) {

        try {

            response.setContentType("multipart/x-mixed-replace; boundary=frame");

            ProcessBuilder pb = new ProcessBuilder(
                    FFMPEG,
                    "-rtsp_transport", "tcp",
                    "-i", RTSP,
                    "-f", "mjpeg",
                    "-q:v", "5",
                    "-"
            );

            Process process = pb.start();

            InputStream is = process.getInputStream();

            byte[] buffer = new byte[1024];

            int bytesRead;

            while ((bytesRead = is.read(buffer)) != -1) {

                response.getOutputStream().write(buffer, 0, bytesRead);
                response.getOutputStream().flush();

            }

        } catch (Exception e) {

            e.printStackTrace();

        }

    }
}