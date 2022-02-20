package de.polocloud.base.command.defaults;

import de.polocloud.api.logger.LogType;
import de.polocloud.api.service.CloudService;
import de.polocloud.api.service.utils.ServiceState;
import de.polocloud.base.Base;
import de.polocloud.base.command.CloudCommand;

import java.util.Arrays;
import java.util.List;

public final class ServiceCommand extends CloudCommand {

    public ServiceCommand() {
        super("service", "Manage services", "ser");
    }

    @Override
    public void execute(Base base, String[] args) {
        final var logger = base.getLogger();

        if (args.length == 1 && args[0].equalsIgnoreCase("list")) {
            base.getServiceManager().getAllCachedServices().forEach(it -> logger
                .log("Name of service '§b" + it.getName() + "§7' (§7State of service '§b" + it.getServiceState().getName() + "§7' | Node: '" + it.getGroup().getNode() + "')"));
            return;
        } else if (args.length >= 1) {
            base.getServiceManager().getService(args[0]).ifPresentOrElse(service -> {

                if (args.length == 2 && args[1].equalsIgnoreCase("stop")) {
                    if (service.getServiceState() == ServiceState.PREPARED || service.getServiceState() == ServiceState.STOPPING) {
                        logger.log("This service ist not started or already in stopping state.", LogType.WARNING);
                        return;
                    }
                    service.stop();
                    logger.log("The service '§b" + service.getName() + "§7' is now stopped.");
                } else if (args.length > 1 && args[1].equalsIgnoreCase("command")) {
                    final var stringBuilder = new StringBuilder();
                    for (int i = 2; i < args.length; i++) stringBuilder.append(args[i]).append(" ");
                    final var command = stringBuilder.toString();
                    service.executeCommand(command);
                    logger.log("Executed command '" + command + "' on service " + service.getName());
                } else {
                    logger.log("Service information:",
                        "Name: §b" + service.getName(),
                        "Visibility: §b" + service.getServiceVisibility() + " &7/ State: §b" + service.getServiceState(),
                        "Players: §b" + service.getOnlineCount() + " &7/ Port: §b" + service.getMaxPlayers(),
                        "Host: §b" + service.getHostName() + " &7/ Port: §b" + service.getPort(),
                        "Motd: §b" + service.getMotd());
                }

            }, () -> logger.log("This service does not exists.", LogType.WARNING));
            return;
        }

        final var help = "§7Use following command: §b";
        logger.log(
            help + "service list §7- List all available services.",
            help + "service (name) start §7- Starting a specific service that not exists.",
            help + "service (name) stop §7- Stopping a specific service that exists.",
            help + "service (name) §7- Prints information about the specific service.",
            help + "service (name) command (command) §7- Executes a command on a server.");
    }

    @Override
    public List<String> tabComplete(String[] arguments) {
        if (arguments.length == 1) {
            return Base.getInstance().getServiceManager().getAllCachedServices().stream().map(CloudService::getName).toList();
        } else if (arguments.length == 2) {
            return Arrays.asList("list", "start", "stop", "command");
        }
        return super.tabComplete(arguments);
    }

}
