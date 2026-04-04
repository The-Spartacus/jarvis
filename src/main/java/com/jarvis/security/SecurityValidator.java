package com.jarvis.security;

import com.jarvis.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/**
 * Security validator for commands before execution.
 * Implements blacklist checking, command validation, and safety rules.
 */
public class SecurityValidator {
    private static final Logger logger = LoggerFactory.getLogger(SecurityValidator.class);

    private static final String[] DEFAULT_BLOCKED_PATTERNS = {
            "rm -rf", "rm -fr", "rmdir",
            "sudo", "su ",
            "shutdown", "reboot", "halt", "poweroff",
            "mkfs", "dd ", "fdisk",
            ":(){", "fork bomb",
            "chmod 777", "chown",
            "curl.*\\|.*sh", "wget.*\\|.*sh",
            "passwd", "useradd", "groupadd",
            "iptables", "ufw",
            "> /dev/", ">> /dev/"
    };

    private static final int MAX_COMMAND_LENGTH = 200;
    private static final int MAX_ARGUMENTS = 10;
    private static final int MAX_PIPES = 2;

    private final Set<String> blacklistedPatterns;
    private final Configuration config;

    public SecurityValidator(Configuration config) {
        this.config = config;
        this.blacklistedPatterns = new HashSet<>();
        loadBlacklist();
    }

    private void loadBlacklist() {
        // Add default patterns
        for (String pattern : DEFAULT_BLOCKED_PATTERNS) {
            blacklistedPatterns.add(pattern.toLowerCase());
        }

        // Try to load custom blacklist
        try {
            Path customBlacklist = Paths.get("config/blacklist.txt");
            if (Files.exists(customBlacklist)) {
                Files.readAllLines(customBlacklist).forEach(line -> {
                    String trimmed = line.trim();
                    if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                        blacklistedPatterns.add(trimmed.toLowerCase());
                    }
                });
                logger.info("Loaded custom blacklist with {} patterns", blacklistedPatterns.size());
            }
        } catch (IOException e) {
            logger.warn("Could not load custom blacklist", e);
        }
    }

    /**
     * Validate if a command is safe to execute
     */
    public boolean validate(String command) {
        if (command == null || command.isEmpty()) {
            logger.warn("Empty command validation");
            return false;
        }

        // Check command length
        if (!validateCommandLength(command)) {
            logger.warn("Command too long: {} chars", command.length());
            return false;
        }

        // Check for blocked patterns
        if (!validateBlacklist(command)) {
            return false;
        }

        // Check special characters
        if (!validateSpecialCharacters(command)) {
            logger.warn("Invalid special characters in command");
            return false;
        }

        // Check argument count
        if (!validateArgumentCount(command)) {
            logger.warn("Too many arguments in command");
            return false;
        }

        // Check pipe count
        if (!validatePipeCount(command)) {
            logger.warn("Too many pipes in command");
            return false;
        }

        logger.info("Command validation passed: {}", maskCommand(command));
        return true;
    }

    private boolean validateCommandLength(String command) {
        int maxLength = config.getMaxCommandLength();
        return command.length() <= maxLength;
    }

    private boolean validateBlacklist(String command) {
        String lowerCommand = command.toLowerCase();
        
        for (String pattern : blacklistedPatterns) {
            if (lowerCommand.contains(pattern)) {
                logger.warn("Blocked pattern detected: {}", pattern);
                return false;
            }
        }
        
        return true;
    }

    private boolean validateSpecialCharacters(String command) {
        // Allow: alphanumeric, space, dash, slash, dot, underscore, pipe, semicolon, ampersand
        Pattern allowedPattern = Pattern.compile("^[a-zA-Z0-9\\s\\-/._;|&=${}()\"']+$");
        return allowedPattern.matcher(command).matches();
    }

    private boolean validateArgumentCount(String command) {
        String[] args = command.split("\\s+");
        return args.length <= MAX_ARGUMENTS;
    }

    private boolean validatePipeCount(String command) {
        long pipeCount = command.chars().filter(ch -> ch == '|').count();
        return pipeCount <= MAX_PIPES;
    }

    private String maskCommand(String command) {
        if (command.length() > 50) {
            return command.substring(0, 50) + "...";
        }
        return command;
    }

    public void addCustomBlacklist(String pattern) {
        blacklistedPatterns.add(pattern.toLowerCase());
        logger.info("Added custom blacklist pattern: {}", pattern);
    }

    public Set<String> getBlacklist() {
        return Collections.unmodifiableSet(blacklistedPatterns);
    }
}
