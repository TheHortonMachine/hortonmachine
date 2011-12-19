package org.jgrasstools.gears.utils;

/**
 * 
 * JGrasstools API version information.
 * <p>
 * Versions consist of a 3-part version number: <code>major.minor.patch</code>
 * An optional release status string may be present in the string version of
 * the version.
 * 
 */
public class JGTVersion {

    /**
     * The current version number of the JGrasstools API.
     */
    public static final JGTVersion CURRENT_VERSION = new JGTVersion();

    /**
     * The major version number.
     */
    public static final int MAJOR = 0;

    /**
     * The minor version number.
     */
    public static final int MINOR = 7;

    /**
     * The patch version number.
     */
    public static final int PATCH = 3;

    /**
     * An optional string providing further release info (such as "alpha 1");
     */
    private static final String releaseInfo = "SNAPSHOT";

    /**
     * Prints the current JGrasstools version to stdout.
     *
     * @param args the command-line arguments (none are required).
     */
    public static void main( String[] args ) {
        System.out.println(CURRENT_VERSION);
    }

    private JGTVersion() {
    }

    /**
     * Gets the major number of the release version.
     *
     * @return the major number of the release version.
     */
    public int getMajor() {
        return MAJOR;
    }

    /**
     * Gets the minor number of the release version.
     *
     * @return the minor number of the release version.
     */
    public int getMinor() {
        return MINOR;
    }

    /**
     * Gets the patch number of the release version.
     *
     * @return the patch number of the release version.
     */
    public int getPatch() {
        return PATCH;
    }

    /**
     * Gets the full version number, suitable for display.
     *
     * @return the full version number, suitable for display.
     */
    public String toString() {
        String ver = MAJOR + "." + MINOR + "." + PATCH;
        if (releaseInfo != null && releaseInfo.length() > 0)
            return ver + "-" + releaseInfo;
        return ver;
    }

}
