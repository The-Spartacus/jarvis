package com.jarvis.docker;

import com.jarvis.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.concurrent.*;

/**
 * Docker sandbox executor for safe command execution.
 * Manages container lifecycle and command execution with timeout.
 */
public class DockerSandbox {
    private static final Logger logger = LoggerFactory.getLogger(DockerSandbox.class);

    private final String containerName;
    private final String dockerImage;
    private final long commandTimeoutSeconds;
    private final int maxOutputLength;

    public DockerSandbox(Configuration config) {
        this.containerName = config.getDockerContainerName();
        this.dockerImage = config.getDockerImage();
        this.commandTimeoutSeconds = config.getCommandTimeout();
        this.maxOutputLength = config.getMaxOutputLength();

        logger.info("DockerSandbox initialized with container: {} image: {}", containerName, dockerImage);
    }

    /**
     * Execute a command in the Docker sandbox with timeout
     */
    public String executeCommand(String command) throws IOException, TimeoutException {
        try {
            // Ensure container is running
            ensureContainerRunning();

            // Build docker exec command
            String dockerCommand = String.format(
                    "docker exec %s bash -c \"%s\"",
                    containerName,
                    escapeCommand(command)
            );

            logger.debug("Executing command in sandbox: {}", command);

            // Execute with timeout
            String output = executeWithTimeout(dockerCommand);

            logger.info("Command executed successfully, output length: {}", output.length());
            return truncateOutput(output);

        } catch (TimeoutException e) {
            logger.error("Command execution timeout", e);
            throw e;
        } catch (Exception e) {
            logger.error("Error executing command in sandbox", e);
            throw new IOException("Sandbox execution failed: " + e.getMessage(), e);
        }
    }

    private void ensureContainerRunning() throws IOException {
        try {
            // Check if container exists and is running
            ProcessBuilder checkPb = new ProcessBuilder(
                    "docker", "ps", "--filter", "name=" + containerName, "--format", "{{.Names}}"
            );
            Process checkProcess = checkPb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(checkProcess.getInputStream()));
            String result = reader.readLine();

            if (result == null || result.isEmpty()) {
                logger.info("Container not running, starting it...");
                startContainer();
            }
        } catch (Exception e) {
            logger.error("Error checking container status", e);
            throw new IOException("Failed to check container status", e);
        }
    }

    private void startContainer() throws IOException {
        ProcessBuilder pb = new ProcessBuilder(
                "docker", "run", "-dit", "--name", containerName, dockerImage
        );
        Process process = pb.start();
        
        try {
            if (!process.waitFor(30, TimeUnit.SECONDS)) {
                logger.error("Container startup timeout");
                throw new IOException("Container startup timeout");
            }
            
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                String errorOutput = readStream(process.getErrorStream());
                logger.error("Container startup failed: {}", errorOutput);
                // Container might already exist, continue
            }
            
            logger.info("Container started successfully");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Container startup interrupted", e);
        }
    }

    private String executeWithTimeout(String dockerCommand) throws IOException, TimeoutException {
        ProcessBuilder pb = new ProcessBuilder("bash", "-c", dockerCommand);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        try {
            boolean completed = process.waitFor(commandTimeoutSeconds, TimeUnit.SECONDS);
            
            if (!completed) {
                process.destroyForcibly();
                throw new TimeoutException("Command execution exceeded " + commandTimeoutSeconds + " seconds");
            }

            int exitCode = process.exitValue();
            String output = readStream(process.getInputStream());

            if (exitCode != 0) {
                logger.warn("Command exited with code {}: {}", exitCode, output);
            }

            return output;

        } catch (InterruptedException e) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            throw new IOException("Command execution interrupted", e);
        }
    }

    private String readStream(InputStream stream) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        return sb.toString();
    }

    private String escapeCommand(String command) {
        return command.replace("\"", "\\\"").replace("$", "\\$");
    }

    private String truncateOutput(String output) {
        if (output.length() > maxOutputLength) {
            String truncated = output.substring(0, maxOutputLength);
            return truncated + "\n... (output truncated)";
        }
        return output;
    }

    public void cleanup() {
        try {
            logger.info("Cleaning up Docker sandbox...");
            ProcessBuilder pb = new ProcessBuilder("docker", "rm", "-f", containerName);
            Process process = pb.start();
            process.waitFor(10, TimeUnit.SECONDS);
            logger.info("Docker cleanup completed");
        } catch (Exception e) {
            logger.error("Error during Docker cleanup", e);
        }
    }

    public boolean isHealthy() {
        try {
            ensureContainerRunning();
            return true;
        } catch (Exception e) {
            logger.error("Docker sandbox health check failed", e);
            return false;
        }
    }
}
