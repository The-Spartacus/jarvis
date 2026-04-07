package com.jarvis.plugin;

import com.jarvis.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Plugin manager for loading and executing custom command plugins.
 * Supports .plugin file format with simple configuration.
 */
public class PluginManager {
    private static final Logger logger = LoggerFactory.getLogger(PluginManager.class);

    private final Map<String, Plugin> plugins;
    private final Path pluginDirectory;
    private final Configuration config;

    public PluginManager(Configuration config) {
        this.config = config;
        this.plugins = new HashMap<>();
        this.pluginDirectory = Paths.get(config.getPluginDirectory());
        loadPlugins();
    }

    private void loadPlugins() {
        try {
            if (!Files.exists(pluginDirectory)) {
                Files.createDirectories(pluginDirectory);
                logger.info("Created plugin directory");
            }

            Files.list(pluginDirectory)
                    .filter(path -> path.toString().endsWith(".plugin"))
                    .forEach(this::loadPlugin);

            logger.info("Loaded {} plugins", plugins.size());
        } catch (IOException e) {
            logger.error("Error loading plugins", e);
        }
    }

    private void loadPlugin(Path pluginFile) {
        try {
            Properties props = new Properties();
            props.load(Files.newInputStream(pluginFile));

            String name = props.getProperty("name");
            String command = props.getProperty("command");
            String description = props.getProperty("description", "");
            String category = props.getProperty("category", "custom");
            boolean requiresConfirmation = Boolean.parseBoolean(props.getProperty("requires_confirmation", "false"));

            if (name != null && command != null) {
                Plugin plugin = new Plugin(name, command, description, category, requiresConfirmation);
                plugins.put(name.toLowerCase(), plugin);
                logger.info("Loaded plugin: {}", name);
            }
        } catch (IOException e) {
            logger.error("Error loading plugin {}", pluginFile, e);
        }
    }

    public String executePlugin(String pluginCommand) throws Exception {
        String[] parts = pluginCommand.split("\\s+", 2);
        String pluginName = parts[0].substring(1).toLowerCase();
        String args = parts.length > 1 ? parts[1] : "";

        Plugin plugin = plugins.get(pluginName);
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin not found: " + pluginName);
        }

        return plugin.execute(args);
    }

    public List<Plugin> listPlugins() {
        return new ArrayList<>(plugins.values());
    }

    public Plugin getPlugin(String name) {
        return plugins.get(name.toLowerCase());
    }

    public void reloadPlugins() {
        plugins.clear();
        loadPlugins();
        logger.info("Plugins reloaded");
    }

    /**
     * Inner class representing a plugin
     */
    public static class Plugin {
        private final String name;
        private final String command;
        private final String description;
        private final String category;
        private final boolean requiresConfirmation;

        public Plugin(String name, String command, String description, String category, boolean requiresConfirmation) {
            this.name = name;
            this.command = command;
            this.description = description;
            this.category = category;
            this.requiresConfirmation = requiresConfirmation;
        }

        public String execute(String args) throws IOException {
            String fullCommand = command;
            if (!args.isEmpty()) {
                fullCommand = fullCommand.replace("${args}", args);
            }

            ProcessBuilder pb = new ProcessBuilder("bash", "-c", fullCommand);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            try {
                process.waitFor();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            return output.toString();
        }

        public String getName() { return name; }
        public String getCommand() { return command; }
        public String getDescription() { return description; }
        public String getCategory() { return category; }
        public boolean isRequiresConfirmation() { return requiresConfirmation; }
    }
}
