package com.jarvis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Intent engine for classifying user input.
 * Placeholder for Phase 2 implementation.
 */
public class IntentEngine {
    private static final Logger logger = LoggerFactory.getLogger(IntentEngine.class);

    public enum Intent {
        FILE_OPERATION,
        SYSTEM_INFO,
        PROCESS_MANAGEMENT,
        NETWORK,
        DEVOPS,
        UNKNOWN
    }

    /**
     * Classify the intent of user input
     */
    public Intent classifyIntent(String input) {
        if (input == null || input.isEmpty()) {
            return Intent.UNKNOWN;
        }

        String lower = input.toLowerCase();

        // File operations
        if (lower.contains("file") || lower.contains("directory") || lower.contains("folder") ||
                lower.contains("find") || lower.contains("list") || lower.contains("show")) {
            if (lower.contains("process") || lower.contains("running")) {
                return Intent.PROCESS_MANAGEMENT;
            }
            return Intent.FILE_OPERATION;
        }

        // Process management
        if (lower.contains("process") || lower.contains("running") || lower.contains("ps ") ||
                lower.contains("kill") || lower.contains("stop")) {
            return Intent.PROCESS_MANAGEMENT;
        }

        // System information
        if (lower.contains("disk") || lower.contains("memory") || lower.contains("cpu") ||
                lower.contains("uptime") || lower.contains("system") || lower.contains("info")) {
            return Intent.SYSTEM_INFO;
        }

        // Network operations
        if (lower.contains("network") || lower.contains("ping") || lower.contains("curl") ||
                lower.contains("wget") || lower.contains("port") || lower.contains("connection")) {
            return Intent.NETWORK;
        }

        // DevOps operations
        if (lower.contains("docker") || lower.contains("git") || lower.contains("deploy") ||
                lower.contains("build") || lower.contains("compose")) {
            return Intent.DEVOPS;
        }

        return Intent.UNKNOWN;
    }

    public String getContextForIntent(Intent intent) {
        return switch (intent) {
            case FILE_OPERATION -> "Performing file system operations";
            case SYSTEM_INFO -> "Retrieving system information";
            case PROCESS_MANAGEMENT -> "Managing system processes";
            case NETWORK -> "Performing network operations";
            case DEVOPS -> "Executing DevOps commands";
            case UNKNOWN -> "Processing general command";
        };
    }
}
