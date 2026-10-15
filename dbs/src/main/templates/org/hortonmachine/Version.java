package org.hortonmachine;

public final class Version {
    private Version() {}

    public static final String MAVEN_VERSION = "${project.version}";
    public static final String BUILD_TIMESTAMP = "${hm.build.timestamp}";

    /**
     * The User-Agent to use for all http requests of the project, tile servers like
     * OpenStreetMap block generic or missing ones.
     */
    public static final String USER_AGENT = "HortonMachine/" + MAVEN_VERSION + " (+https://www.hortonmachine.org)";

    public static String getVersion() {
		return MAVEN_VERSION + " - " + BUILD_TIMESTAMP;
	}
}
