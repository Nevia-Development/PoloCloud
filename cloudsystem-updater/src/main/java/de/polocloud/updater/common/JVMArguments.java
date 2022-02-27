package de.polocloud.updater.common;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class JVMArguments {
    public static void appendJavaExecutable(@NotNull List<String> cmd) {
        cmd.add(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java");
    }

    public static void appendVMArgs(@NotNull Collection<String> cmd) {
        Collection<String> vmArguments = ManagementFactory.getRuntimeMXBean().getInputArguments();

        String javaToolOptions = System.getenv("JAVA_TOOL_OPTIONS");
        if (javaToolOptions != null) {
            Collection<String> javaToolOptionsList = Arrays.asList(javaToolOptions.split(" "));
            vmArguments = new ArrayList<>(vmArguments);
            vmArguments.removeAll(javaToolOptionsList);
        }

        cmd.addAll(vmArguments);
    }
}
