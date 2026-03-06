package com.localsecurecam.backend.service;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileReader;

@Service
public class SystemStatsService {

    public double getCpuUsage() {

        try {

            BufferedReader br = new BufferedReader(new FileReader("/proc/stat"));

            String line = br.readLine();

            String[] parts = line.split("\\s+");

            long idle = Long.parseLong(parts[4]);

            long total = 0;

            for (int i = 1; i < parts.length; i++) {
                total += Long.parseLong(parts[i]);
            }

            br.close();

            double usage = (1.0 - ((double) idle / total)) * 100;

            return usage;

        } catch (Exception e) {
            return 0;
        }
    }

    public double getRamUsage() {

        try {

            BufferedReader br = new BufferedReader(new FileReader("/proc/meminfo"));

            long total = 0;
            long available = 0;

            String line;

            while ((line = br.readLine()) != null) {

                if (line.startsWith("MemTotal")) {
                    total = Long.parseLong(line.replaceAll("\\D+", ""));
                }

                if (line.startsWith("MemAvailable")) {
                    available = Long.parseLong(line.replaceAll("\\D+", ""));
                }

            }

            br.close();

            long used = total - available;

            return (used * 100.0) / total;

        } catch (Exception e) {
            return 0;
        }
    }

    public double getTemperature() {

        try {

            BufferedReader br = new BufferedReader(
                    new FileReader("/sys/class/thermal/thermal_zone0/temp")
            );

            String temp = br.readLine();

            br.close();

            return Integer.parseInt(temp) / 1000.0;

        } catch (Exception e) {
            return 0;
        }
    }

    public double getDiskUsage() {

        try {

            java.io.File file = new java.io.File("/");

            long total = file.getTotalSpace();
            long free = file.getFreeSpace();

            long used = total - free;

            return (used * 100.0) / total;

        } catch (Exception e) {
            return 0;
        }
    }
}