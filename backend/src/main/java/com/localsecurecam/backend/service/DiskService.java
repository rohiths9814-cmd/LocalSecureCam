package com.localsecurecam.backend.service;

import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class DiskService {

    public long getFreePercent(File root) {
        if (!root.exists()) return 100;
        return (root.getUsableSpace() * 100) / root.getTotalSpace();
    }
}
