package de.bytemc.cloud.services.process;

import de.bytemc.cloud.Base;
import de.bytemc.cloud.api.CloudAPI;
import de.bytemc.cloud.api.common.ConfigSplitSpacer;
import de.bytemc.cloud.api.common.ConfigurationFileEditor;
import de.bytemc.cloud.api.network.packets.services.ServiceRemovePacket;
import de.bytemc.cloud.api.services.IService;
import de.bytemc.cloud.api.services.impl.SimpleService;
import de.bytemc.cloud.api.services.utils.ServiceState;
import de.bytemc.cloud.services.process.args.ProcessJavaArgs;
import de.bytemc.cloud.services.process.file.PropertyFileWriter;
import de.bytemc.cloud.services.properties.BungeeProperties;
import de.bytemc.cloud.services.properties.SpigotProperties;
import de.bytemc.cloud.services.statistics.SimpleStatisticManager;
import de.bytemc.network.promise.CommunicationPromise;
import de.bytemc.network.promise.ICommunicationPromise;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public record ProcessServiceStarter(IService service) {

    @SneakyThrows
    public ProcessServiceStarter(final IService service) {
        this.service = service;
        this.service.setServiceState(ServiceState.STARTING);

        //add statistic to service
        SimpleStatisticManager.registerStartingProcess(this.service);

        this.service.getServiceGroup().getGameServerVersion().download();

        //create tmp file
        FileUtils.forceMkdir(new File("tmp/" + service.getName() + "/"));

        //load all current group templates
        Base.getInstance().getGroupTemplateService().copyTemplates(service);

        String jar = service.getServiceGroup().getGameServerVersion().getJar();
        FileUtils.copyFile(new File("storage/jars/" + jar), new File("tmp/" + service.getName() + "/" + jar));

        //copy plugin
        FileUtils.copyFile(new File("storage/jars/plugin.jar"), new File("tmp/" + service.getName() + "/plugins/plugin.jar"));

        //write property for identify service
        new PropertyFileWriter(service);

        //check properties and modify
        if (service.getServiceGroup().getGameServerVersion().isProxy()) {
            var file = new File("tmp/" + service.getName() + "/config.yml");
            if (file.exists()) {
                var editor = new ConfigurationFileEditor(file, ConfigSplitSpacer.YAML);
                editor.setValue("host", "0.0.0.0:" + service.getPort());
                editor.saveFile();
            } else new BungeeProperties(new File("tmp/" + service.getName() + "/"), service.getPort());
        } else {
            var file = new File("tmp/" + service.getName() + "/server.properties");
            if (file.exists()) {
                var editor = new ConfigurationFileEditor(file, ConfigSplitSpacer.PROPERTIES);
                editor.setValue("server-port", String.valueOf(service.getPort()));
                editor.saveFile();
            } else new SpigotProperties(new File("tmp/" + service.getName() + "/"), service.getPort());
        }
    }

    @SneakyThrows
    public ICommunicationPromise<IService> start() {
        final var communicationPromise = new CommunicationPromise<IService>();
        final var command = ProcessJavaArgs.args(this.service);

        final var processBuilder = new ProcessBuilder(command).directory(new File("tmp/" + this.service.getName() + "/"));
        final var process = processBuilder.start();

        final var thread = new Thread(() -> {
            ((SimpleService) this.service).setProcess(process);
            communicationPromise.setSuccess(this.service);

            try {
                process.waitFor();

                //stop service
                final var file = new File("tmp/" + this.service.getName() + "/");
                if (file.exists()) FileUtils.deleteDirectory(file);
                CloudAPI.getInstance().getLoggerProvider().logMessage("The service '§b" + this.service.getName() + "§7' is now successfully offline.");
                Base.getInstance().getNode().sendPacketToAll(new ServiceRemovePacket(this.service.getName()));
                CloudAPI.getInstance().getServiceManager().getAllCachedServices().remove(this.service);

                //check queue
                Base.getInstance().getQueueService().checkForQueue();

            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        });
        thread.start();
        return communicationPromise;
    }


}
