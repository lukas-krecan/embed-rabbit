package net.javacrumbs.embed.rabbit;


import de.flapdoodle.embed.process.distribution.IVersion;

public enum Version implements IVersion {
    V_3_6_1("3.6.1");

    private final String specificVersion;

    Version(String specificVersion) {
        this.specificVersion = specificVersion;
    }

    @Override
    public String asInDownloadPath() {
        return specificVersion;
    }
}
