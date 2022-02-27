package de.polocloud.updater.version;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.polocloud.updater.version.base.Version;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.jar.Manifest;

public class VersionProvider {

    public String getManifestVersion() {
        try (final var stream = VersionProvider.class.getClassLoader().getResources("META-INF/MANIFEST.MF")
            .nextElement().openStream()) {
            String value = new Manifest(stream).getMainAttributes().getValue("Version");
            return value == null ? "Unknown" : value;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "Unknown";
    }

    public Version getThisVersion() {
        return getVersion(getManifestVersion());
    }

    public Version getVersion(String version) {
        String currentVersion = version;
        if (currentVersion.equalsIgnoreCase("Unknown")) {
            return new Version(0, 0, 0, "");
        }
        currentVersion = currentVersion.replace("V", "").replace("v", "");
        String[] splitType = currentVersion.split("-");
        String[] versionSplit = splitType[0].split("\\.");
        if (splitType.length == 1) {
            return new Version(versionSplit.length >= 1 ? Integer.parseInt(versionSplit[0]) : 0, versionSplit.length >= 2 ? Integer.parseInt(versionSplit[1]) : 0, versionSplit.length >= 3 ? Integer.parseInt(versionSplit[2]) : 0, "");
        } else {
            return new Version(versionSplit.length >= 1 ? Integer.parseInt(versionSplit[0]) : 0, versionSplit.length >= 2 ? Integer.parseInt(versionSplit[1]) : 0, versionSplit.length >= 3 ? Integer.parseInt(versionSplit[2]) : 0, splitType[1]);
        }
    }

    public Version getNewestVersion() {
        HttpClient httpClient = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.github.com/repos/PoloServices/PoloCloud/releases/latest"))
            .timeout(Duration.ofSeconds(5))
            .GET()
            .build();

        CompletableFuture<HttpResponse<String>> responseFuture =
            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        try {
            HttpResponse<String> response = responseFuture.get();
            JsonObject jsonObject = JsonParser.parseString(response.body()).getAsJsonObject();
            String version = jsonObject.has("name") ? jsonObject.get("name").getAsString() : "0.0.0-ERROR";
            return getVersion(version);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return new Version(0, 0, 0, "");
    }

    public String getDownloadURL() {
        HttpClient httpClient = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.github.com/repos/PoloServices/PoloCloud/releases/latest"))
            .timeout(Duration.ofSeconds(5))
            .GET()
            .build();

        CompletableFuture<HttpResponse<String>> responseFuture =
            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        try {
            HttpResponse<String> response = responseFuture.get();
            JsonObject jsonObject = JsonParser.parseString(response.body()).getAsJsonObject();
            if (!jsonObject.has("assets")) {
                return null;
            }
            JsonArray assets = jsonObject.get("assets").getAsJsonArray();
            for (JsonElement asset : assets) {
                JsonObject assetObject = asset.getAsJsonObject();
                if (assetObject.has("name")) {
                    String assetName = assetObject.get("name").getAsString();
                    if (assetName.endsWith(".zip") && assetName.toLowerCase().contains("polocloud")) {
                        return assetObject.get("browser_download_url").getAsString();
                    }
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

}
