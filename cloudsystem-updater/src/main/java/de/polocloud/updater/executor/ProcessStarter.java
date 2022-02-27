package de.polocloud.updater.executor;

import de.polocloud.updater.common.JVMArguments;
import lombok.Getter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Getter
public class ProcessStarter {

    private final File jar_File;
    private final File working_Directory;

    private boolean hasFailed = false;
    private Process process;

    public ProcessStarter(File jar_File, File working_directory) {
        this.jar_File = jar_File;
        working_Directory = working_directory;
    }

    public void start() {
        if (!jar_File.exists() && !jar_File.getName().endsWith(".jar")) {
            hasFailed = true;
            return;
        }
        List<String> commands = new ArrayList<>();
        JVMArguments.appendJavaExecutable(commands);
        JVMArguments.appendVMArgs(commands);
        commands.add("-jar");
        commands.add(jar_File.getAbsolutePath());

        try {
            process = new ProcessBuilder(commands)
                .directory(working_Directory)
                .inheritIO()
                .start();
            process.onExit().thenAccept(exitProcess -> checkExitOfProcess());
        } catch (IOException e) {
            hasFailed = true;
            return;
        }

        while (!process.isAlive()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                return;
            }
        }
        hasFailed = false;
    }

    public void terminate() {
        if (this.process.isAlive()) {
            this.process.destroyForcibly();
        }
    }

    public void checkExitOfProcess() {
        if (!this.process.isAlive()) {
            if (process.exitValue() == -1 || process.exitValue() == 1) {
                hasFailed = true;
            }
        }
    }
}
