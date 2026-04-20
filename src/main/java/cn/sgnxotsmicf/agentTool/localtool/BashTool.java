package cn.sgnxotsmicf.agentTool.localtool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.concurrent.*;

@Component
public class BashTool {

    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private Process bashProcess;
    private PrintWriter bashWriter;
    private BufferedReader bashReader;
    private BufferedReader bashErrorReader;

    @Tool(description = "Execute a bash command in the terminal. Long running commands should be run in the background with output redirected to a file. Interactive commands may require multiple calls.")
    public BashResult executeBash(
            @ToolParam(description = "The bash command to execute. Can be empty to view additional logs when previous exit code is -1. Can be ctrl+c to interrupt the currently running process.") String command,
            @ToolParam(description = "Whether to restart the bash session") Boolean restart
    ) {
        if (restart == null) restart = false;
        if (command == null) command = "";

        if (restart || bashProcess == null || !bashProcess.isAlive()) {
            restartBash();
        }

        if ("ctrl+c".equalsIgnoreCase(command)) {
            bashProcess.destroy();
            return new BashResult("Process interrupted", "", true);
        }

        try {
            bashWriter.println(command);
            bashWriter.println("echo '<<exit>>'");
            bashWriter.flush();

            StringBuilder output = new StringBuilder();
            StringBuilder error = new StringBuilder();

            CompletableFuture<BashResult> future = CompletableFuture.supplyAsync(() -> {
                try {
                    String line;
                    while ((line = bashReader.readLine()) != null) {
                        if (line.equals("<<exit>>")) {
                            break;
                        }
                        output.append(line).append("\n");
                    }

                    while ((line = bashErrorReader.readLine()) != null) {
                        error.append(line).append("\n");
                    }

                    return new BashResult(output.toString().trim(), error.toString().trim(), true);
                } catch (IOException e) {
                    return new BashResult("", "Error reading bash output: " + e.getMessage(), false);
                }
            }, executorService);

            return future.get(120, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            bashProcess.destroy();
            return new BashResult("", "Command timed out after 120 seconds", false);
        } catch (Exception e) {
            return new BashResult("", "Error executing bash command: " + e.getMessage(), false);
        }
    }

    private void restartBash() {
        if (bashProcess != null && bashProcess.isAlive()) {
            bashProcess.destroy();
        }

        try {
            ProcessBuilder processBuilder = new ProcessBuilder("bash");
            processBuilder.redirectErrorStream(false);
            bashProcess = processBuilder.start();

            bashWriter = new PrintWriter(bashProcess.getOutputStream(), true);
            bashReader = new BufferedReader(new InputStreamReader(bashProcess.getInputStream()));
            bashErrorReader = new BufferedReader(new InputStreamReader(bashProcess.getErrorStream()));

            while (bashReader.ready()) {
                bashReader.readLine();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to start bash process: " + e.getMessage());
        }
    }

    public record BashResult(String output, String error, boolean success) {}
}
