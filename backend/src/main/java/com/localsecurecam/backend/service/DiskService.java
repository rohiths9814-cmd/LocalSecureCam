package com.localsecurecam.backend.service;

import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

@Service
public class DiskService {

    private static final long MIN_FREE_PERCENT = 15;
    private static final int KEEP_DAYS = 7;

    public long getFreePercent(File root) {
        if (!root.exists()) return 100;
        return (root.getUsableSpace() * 100) / root.getTotalSpace();
    }

    public void ensureSpace(File recordingsRoot) {
        if (getFreePercent(recordingsRoot) < MIN_FREE_PERCENT) {
            cleanupOldRecordings(recordingsRoot);
        }
    }

    private void cleanupOldRecordings(File root) {
        File[] cameras = root.listFiles(File::isDirectory);
        if (cameras == null) return;

        for (File cam : cameras) {
            File[] days = cam.listFiles(File::isDirectory);
            if (days == null || days.length <= KEEP_DAYS) continue;

            Arrays.sort(days, Comparator.comparing(File::getName));

            for (int i = 0; i < days.length - KEEP_DAYS; i++) {
                delete(days[i]);
            }
        }
    }

    private void delete(File f) {
        if (f.isDirectory()) {
            for (File c : f.listFiles()) delete(c);
        }
        f.delete();
    }
}
