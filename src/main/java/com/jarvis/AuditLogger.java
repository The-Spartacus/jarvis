package com.jarvis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Audit logger for command execution tracking.
 * Logs all generated and executed commands with timestamps.
 */
public class AuditLogger {
    private static final Logger auditLogger = LoggerFactory.getLogger("com.jarvis.audit");
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Configuration config;
    private final Path auditLogPath;

    public AuditLogger(Configuration config) {
        this.config = config;
        this.auditLogPath = Paths.get(config.getLogsDirectory(), "execution.log");
        ensureLogDirectory();
    }

    private void ensureLogDirectory() {
        try {
            Path logDir = auditLogPath.getParent();
            if (!Files.exists(logDir)) {
                Files.createDirectories(logDir);
            }
        } catch (IOException e) {
            auditLogger.error("Failed to create log directory", e);
        }
    }

    /**
     * Log a command execution with details
     */
    public void logCommandExecution(String input, String generatedCommand, boolean allowed, String output, long executionTimeMs) {
        StringBuilder logEntry = new StringBuilder();
        logEntry.append(LocalDateTime.now().format(TIMESTAMP_FORMAT))
                .append(" | INPUT: ").append(sanitize(input))
                .append(" | COMMAND: ").append(sanitize(generatedCommand))
                .append(" | STATUS: ").append(allowed ? "EXECUTED" : "BLOCKED")
                .append(" | TIME: ").append(executionTimeMs).append("ms");

        if (output != null && !output.isEmpty()) {
            logEntry.append(" | OUTPUT: ").append(sanitize(truncate(output, 100)));
        }

        auditLogger.info(logEntry.toString());
        writeToFile(logEntry.toString());
    }

    /**
     * Log a blocked command attempt
     */
    public void logBlockedCommand(String input, String reason) {
        StringBuilder logEntry = new StringBuilder();
        logEntry.append(LocalDateTime.now().format(TIMESTAMP_FORMAT))
                .append(" | BLOCKED | INPUT: ").append(sanitize(input))
                .append(" | REASON: ").append(reason);

        auditLogger.warn(logEntry.toString());
        writeToFile(logEntry.toString());
    }

    /**
     * Log an error
     */
    public void logError(String message, Exception e) {
        String logEntry = LocalDateTime.now().format(TIMESTAMP_FORMAT) + " | ERROR: " + message + " | " + e.getMessage();
        auditLogger.error(logEntry, e);
        writeToFile(logEntry);
    }

    private void writeToFile(String entry) {
        try {
            Files.write(auditLogPath, (entry + "\n").getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            auditLogger.error("Failed to write audit log", e);
        }
    }

    private String sanitize(String input) {
        if (input == null) return "";
        return input.replace("\n", "\\n").replace("\t", "\\t");
    }

    private String truncate(String input, int maxLength) {
        if (input == null) return "";
        return input.length() > maxLength ? input.substring(0, maxLength) + "..." : input;
    }
}
