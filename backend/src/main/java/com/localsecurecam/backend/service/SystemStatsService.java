package com.localsecurecam.backend.service;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import com.sun.management.OperatingSystemMXBean;
import java.io.File;
import java.io.FileReader;

@Service
public class SystemStatsService {

    public double getCpuUsage() {
        OperatingSystemMXBean osBean =
                (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

        return osBean.getCpuLoad() * 100;
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
        
            ProcessBuilder pb = new ProcessBuilder(
                    "cat",
                    "/sys/class/thermal/thermal_zone0/temp"
            );
        
            Process p = pb.start();
        
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(p.getInputStream())
            );
        
            String temp = br.readLine();
        
            return Integer.parseInt(temp) / 1000.0;
        
        } catch (Exception e) {
            return 0;
        }
    }

    public double getDiskUsage() {
        File root = new File("/");

        long total = root.getTotalSpace();
        long free = root.getFreeSpace();

        return ((double)(total - free) / total) * 100;
    }
}