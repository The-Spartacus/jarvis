package com.jarvis.ui;

import java.util.*;

/**
 * Terminal UI for Jarvis Terminal.
 * Handles user input/output, prompts, and colored output.
 */
public class TerminalUI {
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_CYAN = "\u001B[36m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_BLUE = "\u001B[34m";
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_BOLD = "\u001B[1m";

    private final Scanner scanner;
    private final List<String> commandHistory;

    public TerminalUI() {
        this.scanner = new Scanner(System.in);
        this.commandHistory = new ArrayList<>();
    }

    public void displayWelcomeScreen() {
        clearScreen();
        System.out.println(ANSI_BOLD + ANSI_CYAN + """
                ╔══════════════════════════════════════════════════════════════╗
                ║  Jarvis Terminal v1.0 - AI-Powered Command Assistant       ║
                ║  🚀 Secure • Intelligent • Accessible                       ║
                ╠══════════════════════════════════════════════════════════════╣
                ║  🟢 Status: Ready                                            ║
                ║  Type 'help' for commands  |  'exit' to quit               ║
                ╚══════════════════════════════════════════════════════════════╝
                """ + ANSI_RESET);
    }

    public String getInput() {
        System.out.print(ANSI_GREEN + "> " + ANSI_RESET);
        if (!scanner.hasNextLine()) {
            return "exit";
        }
        String input = scanner.nextLine().trim();
        if (!input.isEmpty()) {
            commandHistory.add(input);
        }
        return input;
    }

    public void displayCommand(String command) {
        System.out.println(ANSI_CYAN + "💡 Command: " + ANSI_BOLD + command + ANSI_RESET);
    }

    public void displayOutput(String output) {
        System.out.println(ANSI_BLUE + "📤 Output:" + ANSI_RESET);
        System.out.println(output);
        System.out.println(ANSI_GREEN + "✅ Execution completed" + ANSI_RESET);
    }

    public void displayMessage(String message) {
        System.out.println(ANSI_BLUE + message + ANSI_RESET);
    }

    public void displayError(String error) {
        System.out.println(ANSI_RED + error + ANSI_RESET);
    }

    public void displayWarning(String warning) {
        System.out.println(ANSI_YELLOW + "⚠️  " + warning + ANSI_RESET);
    }

    public void displaySuccess(String message) {
        System.out.println(ANSI_GREEN + "✅ " + message + ANSI_RESET);
    }

    public boolean requestConfirmation() {
        System.out.print(ANSI_YELLOW + "⚠️  Execute this command? (y/n): " + ANSI_RESET);
        if (!scanner.hasNextLine()) {
            return false;
        }
        String response = scanner.nextLine().trim().toLowerCase();
        return response.equals("y") || response.equals("yes");
    }

    public List<String> getCommandHistory() {
        return new ArrayList<>(commandHistory);
    }

    public void displayHelp() {
        System.out.println(ANSI_BOLD + ANSI_CYAN + """
                Available Commands:
                  help              - Show this help message
                  history           - Show command history
                  exit/quit         - Exit Jarvis Terminal
                  /plugin_name      - Execute a plugin command
                  status            - Show system status

                Tips:
                  • Use natural language (e.g., "list files in current directory")
                  • Commands are validated before execution
                  • All commands execute in a secure Docker sandbox
                """ + ANSI_RESET);
    }

    private void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    public void close() {
        scanner.close();
    }
}
