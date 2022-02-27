package de.polocloud.updater.version.base;

import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.Objects;

public record Version(int major, int minor, int patch, String type) {

    public static Version parse(@NotNull String version) {
        String currentVersion = version;
        if (currentVersion.equalsIgnoreCase("Unknown")) {
            return new Version(0, 0, 0, "");
        }
        currentVersion = currentVersion.replace("V", "").replace("v", "");
        String[] splitType = currentVersion.split("-");
        if (splitType[0].equals("")) {
            return new Version(0, 0, 0, "");
        }
        String[] versionSplit = splitType[0].split("\\.");
        try {
            if (splitType.length == 1) {
                return new Version(versionSplit.length >= 1 ? Integer.parseInt(versionSplit[0]) : 0, versionSplit.length >= 2 ? Integer.parseInt(versionSplit[1]) : 0, versionSplit.length >= 3 ? Integer.parseInt(versionSplit[2]) : 0, "");
            } else {
                return new Version(versionSplit.length >= 1 ? Integer.parseInt(versionSplit[0]) : 0, versionSplit.length >= 2 ? Integer.parseInt(versionSplit[1]) : 0, versionSplit.length >= 3 ? Integer.parseInt(versionSplit[2]) : 0, splitType[1]);
            }
        } catch (NumberFormatException ignored) {

        }
        return new Version(0, 0, 0, "");
    }

    public String buildVersion() {
        return this.major + "." + this.minor + "." + this.patch;
    }

    public String buildAll() {
        if (type != null && !type.equals("")) {
            return this.major + "." + this.minor + "." + this.patch + "-" + type;
        }
        return this.major + "." + this.minor + "." + this.patch;
    }

    public boolean inValid() {
        return this.buildVersion().equals("0.0.0");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Version version = (Version) o;
        if (version.type.equals("") || type.equals("")) {
            return major == version.major && minor == version.minor && patch == version.patch;
        }
        return major == version.major && minor == version.minor && patch == version.patch && type.equalsIgnoreCase(version.type);
    }

    public int compareTo(Version version) {
        return Comparator
            .comparing(Version::major)
            .thenComparing(Version::minor)
            .thenComparing(Version::patch)
            .compare(this, version);
    }

    public boolean isOlderThan(Version version) {
        return compareTo(version) < 0;
    }

    public boolean isNewerThan(Version version) {
        return !isOlderThan(version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(major, minor, patch, type);
    }
}
