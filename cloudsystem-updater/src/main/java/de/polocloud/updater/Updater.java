package de.polocloud.updater;

import de.polocloud.api.CloudAPI;
import de.polocloud.api.json.Document;
import de.polocloud.api.logger.LogType;
import de.polocloud.updater.config.UpdaterConfig;
import de.polocloud.updater.executor.ProcessStarter;
import de.polocloud.updater.version.VersionProvider;
import de.polocloud.updater.version.base.Version;
import lombok.Getter;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.Callable;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Getter
public class Updater {

    @Getter
    private static Updater instance;

    private final VersionProvider versionProvider;
    private UpdaterConfig updaterConfig;

    public Updater() {
        instance = this;

        this.versionProvider = new VersionProvider();

        loadConfig();
    }

    public static File newFile(File destinationDir, ZipEntry zipEntry, String name) throws IOException {
        File destFile = new File(destinationDir, name);

        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }

        return destFile;
    }

    public void loadConfig() {
        File configFile = new File("updater_config.json");
        if (configFile.exists()) {
            this.updaterConfig = new Document(configFile).get(UpdaterConfig.class);
            return;
        }
        CloudAPI.getInstance().getLogger().log("§7It seems, that the §bUpdater-Config §7has it's first initialization. There Auto-Updating is enabled, if you don't want this, please set 'useUpdater' to 'false' in the 'updater_config.json'.", LogType.INFO);
        new Document(this.updaterConfig = new UpdaterConfig()).write(configFile);
    }

    public boolean testInternetConnection() {
        try {
            URL url = new URL("https://www.google.com");
            URLConnection connection = url.openConnection();
            connection.connect();
            return true;
        } catch (IOException e) {
            CloudAPI.getInstance().getLogger().log("§eIt seems that you aren't connected to the internet. Please check your connection. This is required to update or to download Server-Software's (Paper, Waterfall, etc.)", LogType.WARNING);
        }
        return false;
    }

    public boolean hasUpdate() {
        if (updaterConfig.isUseUpdater() && testInternetConnection()) {
            CloudAPI.getInstance().getLogger().log("§7Searching for new update...", LogType.INFO);

            Version thisVersion = this.versionProvider.getThisVersion();
            Version newestVersion = this.versionProvider.getNewestVersion();

            if (thisVersion.inValid() || newestVersion.inValid()) {
                CloudAPI.getInstance().getLogger().log("§cFailed to compare this Version with the newest version! Skipping update checking...", LogType.ERROR);
                return false;
            }

            boolean hasUpdate = thisVersion.isOlderThan(newestVersion);
            if (hasUpdate) {
                CloudAPI.getInstance().getLogger().log("§aFound §7a new §bUpdate§7! (You: §b" + thisVersion.buildAll() + " §7-> Newest: §b" + newestVersion.buildAll() + "§7)");
            } else {
                CloudAPI.getInstance().getLogger().log("§7You are up to date! (§b" + thisVersion.buildAll() + "§7). Launching...");
            }

            return hasUpdate;
        }
        return false;
    }

    public File installNewVersion() {
        CloudAPI.getInstance().getLogger().log("§7Start downloading newer version...", LogType.INFO);
        File downloadInto = new File("updater/temp/update.zip");
        downloadInto.getParentFile().mkdirs();
        try {
            //Downloading newest Version
            InputStream in = new URL(this.versionProvider.getDownloadURL()).openStream();
            Files.copy(in, Paths.get("updater/temp/update.zip"), StandardCopyOption.REPLACE_EXISTING);

            //Waiting for the Download to finish
            while (!downloadInto.exists()) {

            }

            CloudAPI.getInstance().getLogger().log("§7Downloading complete! Unzipping...", LogType.SUCCESS);

            //Extracting downloaded ZIP
            File destinationDir = new File(System.getProperty("user.dir"));
            byte[] buffer = new byte[1024];
            ZipInputStream zis = new ZipInputStream(new FileInputStream(downloadInto));
            ZipEntry zipEntry = zis.getNextEntry();
            File jar_File = null;

            while (zipEntry != null) {
                //Searching for the cloudsystem.jar
                if (!zipEntry.getName().endsWith(".jar") && !zipEntry.getName().toLowerCase().contains("cloudsystem")) {
                    zipEntry = zis.getNextEntry();
                } else {
                    File newFile = newFile(destinationDir, zipEntry, "update_" + zipEntry.getName());
                    if (zipEntry.isDirectory()) {
                        if (!newFile.isDirectory() && !newFile.mkdirs()) {
                            throw new IOException("Failed to create directory " + newFile);
                        }
                    } else {
                        File parent = newFile.getParentFile();
                        if (!parent.isDirectory() && !parent.mkdirs()) {
                            throw new IOException("Failed to create directory " + parent);
                        }

                        FileOutputStream fos = new FileOutputStream(newFile);
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                        fos.close();
                        jar_File = newFile;
                    }
                    break;
                }
            }
            zis.closeEntry();
            zis.close();
            CloudAPI.getInstance().getLogger().log("§7Finished unzipping. Cleaning up...", LogType.SUCCESS);
            downloadInto.delete();
            CloudAPI.getInstance().getLogger().log("§7Done!", LogType.SUCCESS);
            return jar_File;
        } catch (IOException exception) {
            exception.printStackTrace();
        }
        return null;
    }

    public void update(Callable<Void> onFailure, Callable<Void> noUpdate) {
        if (hasUpdate()) {
            File file = installNewVersion();
            if (file == null) {
                CloudAPI.getInstance().getLogger().log("§7Failed to install update (extracted cloudsystem.jar wasn't found). Starting normally...", LogType.ERROR);
                try {
                    onFailure.call();
                } catch (Exception e) {
                    e.printStackTrace();
                    System.exit(-1);
                }
                return;
            }

            ProcessStarter processStarter = new ProcessStarter(file, new File(System.getProperty("user.dir")));
            processStarter.start();
            while (processStarter.getProcess().isAlive()) {

            }
            File updateFile = new File("update_cloudsystem.jar");
            if (processStarter.isHasFailed()) {
                CloudAPI.getInstance().getLogger().log("§7Failed to launch newer Version! Starting from old version...", LogType.ERROR);
                if (updateFile.exists()) {
                    updateFile.delete();
                }
                try {
                    onFailure.call();
                } catch (Exception e) {
                    e.printStackTrace();
                    System.exit(-1);
                }
            } else {
                File cloud = new File("cloudsystem.jar");
                if (updateFile.exists()) {
                    CloudAPI.getInstance().getLogger().log("§aSuccessfully updated! §7Cleaning up...", LogType.SUCCESS);
                    cloud.delete();
                    updateFile.renameTo(cloud);
                }
            }
        } else {
            try {
                noUpdate.call();
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(-1);
            }
        }
    }
}
