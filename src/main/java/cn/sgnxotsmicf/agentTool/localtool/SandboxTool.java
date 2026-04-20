package cn.sgnxotsmicf.agentTool.localtool;

import cn.hutool.core.util.StrUtil;
import cn.sgnxotsmicf.agentTool.config.ToolLocalConfig;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SandboxTool {

    private static Path WORKSPACE;
    // Thread-safe collection to prevent concurrency errors
    private final Map<String, Process> runningProcesses = new ConcurrentHashMap<>();

    // Status constants
    private static final String STATUS_RUNNING = "running";
    private static final String STATUS_COMPLETED = "completed";

    /**
     * Automatically create sandbox directory when Spring initializes the Bean
     */
    public SandboxTool() {
        // Mandatory sandbox working directory, all operations are restricted to this folder
        if (StrUtil.isEmpty(ToolLocalConfig.sandbox_workspace)) {
            WORKSPACE = Paths.get("ai_sandbox");
        }else {
            WORKSPACE = Paths.get(ToolLocalConfig.sandbox_workspace);
        }

        try {
            // Explicit check: create directory only if it does not exist
            if (!Files.exists(WORKSPACE)) {
                Files.createDirectories(WORKSPACE);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to create sandbox directory", e);
        }
    }

    // ====================== Core Command Execution Tool ======================
    @Tool(description = "Execute shell commands in a sandboxed environment. Supports blocking/non-blocking mode and session management")
    public String executeCommand(
            @ToolParam(description = "Shell command to execute") String command,
            @ToolParam(description = "Relative subdirectory to run the command") String folder,
            @ToolParam(description = "Session name for stateful command management") String sessionName,
            @ToolParam(description = "Wait for command completion (default: false, non-blocking)") Boolean blocking,
            @ToolParam(description = "Timeout in seconds for blocking commands (default: 60)") Integer timeout
    ) {
        if (command == null || command.isBlank()) {
            return "Error: Command cannot be empty";
        }

        // Default parameter assignment
        boolean isBlocking = Boolean.TRUE.equals(blocking);
        int timeoutSec = timeout == null ? 60 : timeout;
        String session = (sessionName == null || sessionName.isBlank())
                ? "session_" + System.currentTimeMillis()
                : sessionName;

        Process process = null;
        InputStream inputStream = null;

        try {
            ProcessBuilder pb = new ProcessBuilder();
            // Adapt to Windows/Linux systems
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                pb.command("cmd.exe", "/c", command);
            } else {
                pb.command("bash", "-c", command);
            }

            // Strictly restrict execution directory to the sandbox
            Path execDir = WORKSPACE;
            if (folder != null && !folder.isBlank()) {
                execDir = getSafePath(folder);
                Files.createDirectories(execDir);
            }
            pb.directory(execDir.toFile());
            pb.redirectErrorStream(true);

            // Blocking execution (wait for command to finish)
            if (isBlocking) {
                process = pb.start();
                inputStream = process.getInputStream();
                long deadline = System.currentTimeMillis() + timeoutSec * 1000L;
                StringBuilder output = new StringBuilder();
                byte[] buffer = new byte[2048];

                while (System.currentTimeMillis() < deadline) {
                    while (inputStream.available() > 0) {
                        int readLen = inputStream.read(buffer);
                        output.append(new String(buffer, 0, readLen, StandardCharsets.UTF_8));
                    }
                    if (!process.isAlive()) break;
                    Thread.sleep(50);
                }

                // Force terminate process on timeout
                if (process.isAlive()) {
                    process.destroyForcibly();
                    return "Error: Command timed out after " + timeoutSec + " seconds and was terminated";
                }
                return "Command executed successfully:\n" + output;
            }
            // Non-blocking execution (run in background)
            else {
                process = pb.start();
                runningProcesses.put(session, process);
                return "Command started in background session [" + session + "]. Use checkCommandOutput to view logs";
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Error: Command execution interrupted";
        } catch (IOException e) {
            return "Error: Command execution failed - " + e.getMessage();
        } finally {
            // Close stream and release resources for blocking mode
            if (isBlocking && inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception ignored) {}
            }
            // Ensure process is destroyed for blocking mode
            if (isBlocking && process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    // ====================== Check Command Output ======================
    @Tool(description = "View output logs of background commands")
    public String checkCommandOutput(
            @ToolParam(description = "Name of the command session") String sessionName,
            @ToolParam(description = "Terminate the session after checking output") Boolean killSession
    ) {
        if (sessionName == null || sessionName.isBlank()) {
            return "Error: Session name cannot be empty";
        }

        Process process = runningProcesses.get(sessionName);
        if (process == null) {
            return "Error: Session [" + sessionName + "] not found";
        }

        InputStream inputStream = null;
        try {
            inputStream = process.getInputStream();
            StringBuilder output = new StringBuilder();
            byte[] buffer = new byte[2048];

            while (inputStream.available() > 0) {
                int readLen = inputStream.read(buffer);
                output.append(new String(buffer, 0, readLen, StandardCharsets.UTF_8));
            }

            String status = process.isAlive() ? STATUS_RUNNING : STATUS_COMPLETED;
            // Terminate session if requested
            if (Boolean.TRUE.equals(killSession) && process.isAlive()) {
                process.destroyForcibly();
                runningProcesses.remove(sessionName);
                status = "terminated";
            }

            return "Session [" + sessionName + "] status: " + status + "\nOutput logs:\n" + output;
        } catch (IOException e) {
            return "Error: Failed to read output - " + e.getMessage();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception ignored) {}
            }
        }
    }

    // ====================== Terminate Command Session ======================
    @Tool(description = "Force terminate a running background command session")
    public String terminateCommand(@ToolParam(description = "Name of the session to terminate") String sessionName) {
        if (sessionName == null || sessionName.isBlank()) {
            return "Error: Session name cannot be empty";
        }

        Process process = runningProcesses.remove(sessionName);
        if (process == null) {
            return "Error: Session [" + sessionName + "] not found";
        }

        if (process.isAlive()) {
            process.destroyForcibly();
        }
        return "Session [" + sessionName + "] has been terminated successfully";
    }

    // ====================== List Active Sessions ======================
    @Tool(description = "List all background running command sessions")
    public String listCommands() {
        if (runningProcesses.isEmpty()) {
            return "No active command sessions";
        }

        StringBuilder sb = new StringBuilder("Active command sessions:\n");
        runningProcesses.forEach((session, process) -> {
            String status = process.isAlive() ? STATUS_RUNNING : STATUS_COMPLETED;
            sb.append("• ").append(session).append(" (").append(status).append(")\n");
        });
        return sb.toString();
    }

    // ====================== Create File ======================
    @Tool(description = "Create a new file in the sandbox environment")
    public String createFile(
            @ToolParam(description = "Relative file path") String filePath,
            @ToolParam(description = "Content to write into the file") String content,
            @ToolParam(description = "File permissions (default: 644)") String permissions
    ) {
        if (filePath == null || filePath.isBlank()) {
            return "Error: File path cannot be empty";
        }

        try {
            Path path = getSafePath(filePath);
            if (Files.exists(path)) {
                return "Error: File already exists: " + filePath;
            }

            Files.createDirectories(path.getParent());
            Files.writeString(path, content == null ? "" : content);
            setFilePermissions(path, permissions);
            return "File created successfully: " + filePath;
        } catch (IOException e) {
            return "Error: Failed to create file - " + e.getMessage();
        }
    }

    // ====================== Replace Text in File ======================
    @Tool(description = "Replace unique text content in a file")
    public String strReplace(
            @ToolParam(description = "Relative file path") String filePath,
            @ToolParam(description = "Original text to replace (must be unique)") String oldStr,
            @ToolParam(description = "New replacement text") String newStr
    ) {
        if (filePath == null || filePath.isBlank()) {
            return "Error: File path cannot be empty";
        }
        if (oldStr == null || oldStr.isBlank()) {
            return "Error: Original text cannot be empty";
        }

        try {
            Path path = getSafePath(filePath);
            if (!Files.exists(path)) {
                return "Error: File does not exist: " + filePath;
            }

            String content = Files.readString(path);
            if (!content.contains(oldStr)) {
                return "Error: Target text not found in file";
            }

            int count = countOccurrences(content, oldStr);
            if (count > 1) {
                return "Error: Multiple matches found, text is not unique";
            }

            String newContent = content.replace(oldStr, newStr);
            Files.writeString(path, newContent);
            return "Text replaced successfully: " + filePath;
        } catch (IOException e) {
            return "Error: Failed to replace text - " + e.getMessage();
        }
    }

    // ====================== Rewrite Entire File ======================
    @Tool(description = "Completely rewrite an existing file")
    public String fullFileRewrite(
            @ToolParam(description = "Relative file path") String filePath,
            @ToolParam(description = "New content for the file") String content,
            @ToolParam(description = "File permissions") String permissions
    ) {
        if (filePath == null || filePath.isBlank()) {
            return "Error: File path cannot be empty";
        }

        try {
            Path path = getSafePath(filePath);
            if (!Files.exists(path)) {
                return "Error: File does not exist: " + filePath;
            }

            Files.writeString(path, content == null ? "" : content);
            setFilePermissions(path, permissions);
            return "File rewritten successfully: " + filePath;
        } catch (IOException e) {
            return "Error: Failed to rewrite file - " + e.getMessage();
        }
    }

    // ====================== Delete File ======================
    @Tool(description = "Delete a file from the sandbox")
    public String deleteFile(@ToolParam(description = "Relative file path") String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return "Error: File path cannot be empty";
        }

        try {
            Path path = getSafePath(filePath);
            if (!Files.exists(path)) {
                return "Error: File does not exist: " + filePath;
            }

            Files.delete(path);
            return "File deleted successfully: " + filePath;
        } catch (IOException e) {
            return "Error: Failed to delete file - " + e.getMessage();
        }
    }

    // ====================== Convert Image to Base64 (AI Vision) ======================
    @Tool(description = "Read an image and return Base64 encoding for AI vision model recognition")
    public String seeImage(@ToolParam(description = "Relative image file path") String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return "Error: Image path cannot be empty";
        }

        try {
            Path path = getSafePath(filePath);
            if (!Files.exists(path)) {
                return "Error: Image does not exist: " + filePath;
            }

            byte[] imageBytes = Files.readAllBytes(path);
            String base64 = Base64.getEncoder().encodeToString(imageBytes);
            return "Image Base64 (ready for AI vision):\n" + base64;
        } catch (IOException e) {
            return "Error: Failed to read image - " + e.getMessage();
        }
    }

    // ====================== Security Utility Methods ======================
    /**
     * Path security check: Block access to files outside the sandbox (prevent path traversal attacks)
     */
    private Path getSafePath(String relativePath) throws IOException {
        Path safePath = WORKSPACE.resolve(relativePath).normalize();
        if (!safePath.startsWith(WORKSPACE)) {
            throw new IOException("Illegal access: Access to files outside the sandbox is forbidden");
        }
        return safePath;
    }

    /**
     * Set file permissions (compatible with Windows/Linux)
     */
    private void setFilePermissions(Path path, String permissions) {
        if (permissions == null || permissions.isBlank()) permissions = "644";
        try {
            String posix = switch (permissions) {
                case "644" -> "rw-r--r--";
                case "755" -> "rwxr-xr-x";
                case "777" -> "rwxrwxrwx";
                default -> "rw-r--r--";
            };
            Files.setPosixFilePermissions(path, PosixFilePermissions.fromString(posix));
        } catch (Exception ignored) {
            // Ignore permission errors on Windows systems
        }
    }

    /**
     * Count occurrences of a substring in text
     */
    private int countOccurrences(String text, String sub) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(sub, index)) != -1) {
            count++;
            index += sub.length();
        }
        return count;
    }
}