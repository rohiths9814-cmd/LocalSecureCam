package com.localsecurecam.backend.service;

import com.localsecurecam.backend.config.CameraProperties;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@Service
public class SystemStatsService {

    private final CameraProperties props;

    // Previous /proc/stat snapshot, so we can measure CPU usage over the
    // interval between calls instead of "average since boot".
    private long prevTotal = 0;
    private long prevIdle = 0;

    public SystemStatsService(CameraProperties props) {
        this.props = props;
    }

    public synchronized double getCpuUsage() {
        try (BufferedReader br = new BufferedReader(new FileReader("/proc/stat"))) {

            String[] parts = br.readLine().split("\\s+");

            long idle = Long.parseLong(parts[4]); // idle column
            long total = 0;
            for (int i = 1; i < parts.length; i++) {
                total += Long.parseLong(parts[i]);
            }

            long totalDelta = total - prevTotal;
            long idleDelta = idle - prevIdle;

            prevTotal = total;
            prevIdle = idle;

            if (totalDelta <= 0) return 0; // first call / no elapsed time

            return (1.0 - ((double) idleDelta / totalDelta)) * 100.0;

        } catch (Exception e) {
            return 0;
        }
    }

    public double getRamUsage() {
        try {
            long total = 0;
            long available = 0;

            List<String> lines = Files.readAllLines(Paths.get("/proc/meminfo"));
            for (String line : lines) {
                if (line.startsWith("MemTotal")) {
                    total = Long.parseLong(line.replaceAll("\\D+", ""));
                } else if (line.startsWith("MemAvailable")) {
                    available = Long.parseLong(line.replaceAll("\\D+", ""));
                }
            }

            if (total == 0) return 0;
            long used = total - available;
            return (used * 100.0) / total;

        } catch (Exception e) {
            return 0;
        }
    }

    public double getTemperature() {
        try (BufferedReader br = new BufferedReader(
                new FileReader("/sys/class/thermal/thermal_zone0/temp"))) {

            return Integer.parseInt(br.readLine().trim()) / 1000.0;

        } catch (Exception e) {
            return 0;
        }
    }

    public double getDiskUsage() {
        try {
            // Report usage of the drive that actually holds the recordings.
            File dir = new File(props.getRecordingsDir());
            File target = dir.exists() ? dir : new File("/");

            long total = target.getTotalSpace();
            long free = target.getFreeSpace();
            if (total == 0) return 0;

            long used = total - free;
            return (used * 100.0) / total;

        } catch (Exception e) {
            return 0;
        }
    }
}
