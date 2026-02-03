package org.hortonmachine;

public final class Version {
    private Version() {}

    public static final String MAVEN_VERSION = "${project.version}";
    public static final String BUILD_TIMESTAMP = "${hm.build.timestamp}";
    
    public static String getVersion() {
		return MAVEN_VERSION + " - " + BUILD_TIMESTAMP;
	}
}
