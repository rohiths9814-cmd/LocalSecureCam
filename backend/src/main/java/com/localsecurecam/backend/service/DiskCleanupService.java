package com.localsecurecam.backend.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.Comparator;

@Service
public class DiskCleanupService {

    private static final String RECORDINGS_DIR = "/home/pi/LocalSecureCam/recordings";

    private static final int KEEP_DAYS = 7;
    private static final int MIN_FREE_PERCENT = 15;

    // ===================== SCHEDULE =====================
    // Runs every 30 minutes
    @Scheduled(fixedDelay = 30 * 60 * 1000)
    public void cleanupScheduler() {
        try {
            cleanupByAge();
            cleanupByDiskSpace();
        } catch (Exception e) {
            System.err.println("❌ Disk cleanup error");
            e.printStackTrace();
        }
    }

    // ===================== AGE BASED CLEANUP =====================
    private void cleanupByAge() throws Exception {
        LocalDate cutoff = LocalDate.now().minusDays(KEEP_DAYS);

        Files.walk(Paths.get(RECORDINGS_DIR), 2)
                .filter(Files::isDirectory)
                .filter(p -> isDateFolder(p.getFileName().toString()))
                .filter(p -> LocalDate.parse(p.getFileName().toString()).isBefore(cutoff))
                .forEach(this::deleteFolderSafe);
    }

    // ===================== DISK SPACE CLEANUP =====================
    private void cleanupByDiskSpace() throws Exception {
        File root = new File(RECORDINGS_DIR);

        while (getFreePercent(root) < MIN_FREE_PERCENT) {
            Path oldest = findOldestDateFolder();
            if (oldest == null) return;

            deleteFolderSafe(oldest);
        }
    }

    // ===================== HELPERS =====================
    private Path findOldestDateFolder() throws Exception {
        return Files.walk(Paths.get(RECORDINGS_DIR), 2)
                .filter(Files::isDirectory)
                .filter(p -> isDateFolder(p.getFileName().toString()))
                .filter(p -> !p.getFileName().toString().equals(LocalDate.now().toString()))
                .min(Comparator.comparing(Path::toString))
                .orElse(null);
    }

    private boolean isDateFolder(String name) {
        return name.matches("\\d{4}-\\d{2}-\\d{2}");
    }

    private long getFreePercent(File root) {
        return (root.getUsableSpace() * 100) / root.getTotalSpace();
    }

    private void deleteFolderSafe(Path folder) {
        try {
            System.out.println("🧹 Deleting old recordings: " + folder);

            Files.walk(folder)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);

        } catch (Exception e) {
            System.err.println("❌ Failed to delete " + folder);
            e.printStackTrace();
        }
    }
}
