package com.jarvis;

import com.jarvis.api.GeminiClient;
import com.jarvis.security.SecurityValidator;
import com.jarvis.docker.DockerSandbox;
import com.jarvis.ui.TerminalUI;
import com.jarvis.plugin.PluginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main application entry point for Jarvis Terminal.
 * Coordinates all components: API client, security validator, Docker sandbox, and UI.
 */
public class JarvisTerminal {
    private static final Logger logger = LoggerFactory.getLogger(JarvisTerminal.class);

    private final GeminiClient apiClient;
    private final SecurityValidator securityValidator;
    private final DockerSandbox dockerSandbox;
    private final TerminalUI terminalUI;
    private final PluginManager pluginManager;
    private final Configuration config;

    public JarvisTerminal() throws Exception {
        logger.info("Initializing Jarvis Terminal v1.0");

        this.config = new Configuration();
        this.apiClient = new GeminiClient(config);
        this.securityValidator = new SecurityValidator(config);
        this.dockerSandbox = new DockerSandbox(config);
        this.terminalUI = new TerminalUI();
        this.pluginManager = new PluginManager(config);

        logger.info("Jarvis Terminal initialization complete");
    }

    /**
     * Main processing loop: receive input -> generate command -> validate -> execute
     */
    public void run() {
        terminalUI.displayWelcomeScreen();

        while (true) {
            try {
                String userInput = terminalUI.getInput();

                if (userInput.equalsIgnoreCase("exit") || userInput.equalsIgnoreCase("quit")) {
                    terminalUI.displayMessage("👋 Goodbye!");
                    break;
                }

                // Check if it's a plugin command
                if (userInput.startsWith("/")) {
                    handlePluginCommand(userInput);
                    continue;
                }

                // Generate command using AI
                terminalUI.displayMessage("🤖 Generating command...");
                String generatedCommand = apiClient.generateCommand(userInput);

                // Validate command
                if (!securityValidator.validate(generatedCommand)) {
                    terminalUI.displayError("🚫 Command blocked for security reasons");
                    logger.warn("Blocked command: {}", generatedCommand);
                    continue;
                }

                terminalUI.displayCommand(generatedCommand);

                // Request user confirmation
                if (!terminalUI.requestConfirmation()) {
                    terminalUI.displayMessage("⏭️  Command skipped");
                    continue;
                }

                // Execute in Docker sandbox
                terminalUI.displayMessage("📤 Executing...");
                String output = dockerSandbox.executeCommand(generatedCommand);

                // Display and log result
                terminalUI.displayOutput(output);
                logger.info("Command executed: {} | Duration: ", generatedCommand);

            } catch (Exception e) {
                terminalUI.displayError("❌ Error: " + e.getMessage());
                logger.error("Error in main loop", e);
            }
        }

        shutdown();
    }

    private void handlePluginCommand(String command) {
        try {
            String result = pluginManager.executePlugin(command);
            terminalUI.displayOutput(result);
        } catch (Exception e) {
            terminalUI.displayError("❌ Plugin error: " + e.getMessage());
            logger.error("Plugin execution error", e);
        }
    }

    private void shutdown() {
        logger.info("Shutting down Jarvis Terminal");
        try {
            dockerSandbox.cleanup();
            apiClient.close();
        } catch (Exception e) {
            logger.error("Error during shutdown", e);
        }
    }

    public static void main(String[] args) {
        try {
            JarvisTerminal jarvis = new JarvisTerminal();
            jarvis.run();
        } catch (Exception e) {
            System.err.println("Failed to start Jarvis Terminal: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
