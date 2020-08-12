/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * The HortonMachine is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.hortonmachine.ssh;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

/**
 * SSH utilities methods.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class SshUtilities {
    private static final String PREFS_NODE_NAME = "HM_SSH_TOOLS";

    public static final String HOST = "HM_SSH_TOOLS_HOST";
    public static final String PORT = "HM_SSH_TOOLS_PORT";
    public static final String USER = "HM_SSH_TOOLS_USER";
    public static final String PWD = "HM_SSH_TOOLS_PWD";

    public static final String TUNNELHOST = "HM_SSH_TOOLS_TUNNELHOST";
    public static final String TUNNELPORT_REMOTE = "HM_SSH_TOOLS_TUNNELPORT_REMOTE";
    public static final String TUNNELPORT_LOCAL = "HM_SSH_TOOLS_TUNNELPORT_LOCAL";
    public static final String TUNNELUSER = "HM_SSH_TOOLS_TUNNELUSER";
    public static final String TUNNELPWD = "HM_SSH_TOOLS_TUNNELPWD";

    public static final String KEYPATH = "HM_SSH_TOOLS_SSHKEYPATH";
    public static final String KEYPASSPHRASE = "HM_SSH_TOOLS_SSHKEYPASSPHRASE";

    /**
     * Get from preference.
     * 
     * @param preferenceKey
     *            the preference key.
     * @param defaultValue
     *            the default value in case of <code>null</code>.
     * @return the string preference asked.
     */
    public static String getPreference( String preferenceKey, String defaultValue ) {
        Preferences preferences = Preferences.userRoot().node(PREFS_NODE_NAME);
        String preference = preferences.get(preferenceKey, defaultValue);
        return preference;
    }

    /**
     * Set a preference.
     * 
     * @param preferenceKey
     *            the preference key.
     * @param value
     *            the value to set.
     */
    public static void setPreference( String preferenceKey, String value ) {
        Preferences preferences = Preferences.userRoot().node(PREFS_NODE_NAME);
        if (value != null) {
            preferences.put(preferenceKey, value);
        } else {
            preferences.remove(preferenceKey);
        }
    }

    /**
     * Get the pid of a process identified by a consequent grepping on a ps aux command. 
     * 
     * <P><b>THIS WORKS ONLY ON LINUX HOSTS</b></p> 
     * 
     * @param session the jsch sesssion to use.
     * @param userName an optional username to check the process on.
     * @param grep1 the first grep filter.
     * @param grep2 the second grep filter, done in the java code.
     * @return  the process pid or <code>null</code> if no process was found.
     * @throws JSchException
     * @throws IOException
     */
    public static String getProcessPid( Session session, String userName, String grep1, String grep2 )
            throws JSchException, IOException {
        String command = "ps aux | grep \"" + grep1 + "\" | grep -v grep";
        String remoteResponseStr = launchACommand(session, command);
        if (remoteResponseStr.length() == 0) {
            return null;
        }

        List<String> pidsList = new ArrayList<>();
        String[] linesSplit = remoteResponseStr.split("\n");
        for( String line : linesSplit ) {
            if (!line.contains(grep2)) {
                continue;
            }
            String[] psSplit = line.split("\\s+");
            if (psSplit.length < 3) {
                throw new JSchException("Could not retrieve process data. Result was: " + line);
            }

            String user = psSplit[0];
            if (userName != null && !user.equals(userName)) {
                continue;
            }
            String pid = psSplit[1];

            try {
                Integer.parseInt(pid);
            } catch (Exception e) {
                throw new JSchException("The pid is invalid: " + pid);
            }

            pidsList.add(pid);
        }
        if (pidsList.size() > 1) {
            throw new JSchException("More than one process was identified with the given filters. Check your filters.");
        } else if (pidsList.size() == 0) {
            return null;
        }
        return pidsList.get(0);
    }

    /**
     * Kills a process using its pid.
     * 
     * <P><b>THIS WORKS ONLY ON LINUX HOSTS</b></p>
     * 
     * @param session the session to use.
     * @param pid the pid to use.
     * @throws Exception
     */
    public static void killProcessByPid( Session session, int pid ) throws Exception {
        String command = "kill -9 " + pid;
        String remoteResponseStr = launchACommand(session, command);
        if (remoteResponseStr.length() == 0) {
            return;
        } else {
            new Exception(remoteResponseStr);
        }

    }

    /**
     * Get the container id of a running docker container.
     * 
     * @param session the session to use.
     * @param containerName the name of the container, used in a grep filter.
     * @return the id of the container or null.
     * @throws JSchException
     * @throws IOException
     */
    public static String getRunningDockerContainerId( Session session, String containerName ) throws JSchException, IOException {
        String command = "docker ps | grep " + containerName;
        String remoteResponseStr = launchACommand(session, command);
        if (remoteResponseStr.length() == 0) {
            return null;
        }

        List<String> containerIdsList = new ArrayList<>();
        String[] linesSplit = remoteResponseStr.split("\n");
        for( String line : linesSplit ) {
            String[] psSplit = line.split("\\s+");
            if (psSplit.length < 3) {
                throw new JSchException("Could not retrieve container data. Result was: " + line);
            }
            String cid = psSplit[0];
            containerIdsList.add(cid);
        }
        if (containerIdsList.size() > 1) {
            throw new JSchException("More than one container was identified with the given filters. Check your filters.");
        } else if (containerIdsList.size() == 0) {
            return null;
        }
        return containerIdsList.get(0);
    }

    /**
     * Launch a command on the remote host and exit once the command is done.
     * 
     * @param session the session to use.
     * @param command the command to launch.
     * @return the output of the command.
     * @throws JSchException
     * @throws IOException
     */
    private static String launchACommand( Session session, String command ) throws JSchException, IOException {
        Channel channel = session.openChannel("exec");
        ((ChannelExec) channel).setCommand(command);
        channel.setInputStream(null);
        ((ChannelExec) channel).setErrStream(System.err);
        InputStream in = channel.getInputStream();
        channel.connect();
        StringBuilder sb = new StringBuilder();
        byte[] tmp = new byte[1024];
        while( true ) {
            while( in.available() > 0 ) {
                int i = in.read(tmp, 0, 1024);
                if (i < 0)
                    break;
                sb.append(new String(tmp, 0, i));
            }
            if (channel.isClosed()) {
                if (in.available() > 0)
                    continue;
                System.out.println("exitstatus: " + channel.getExitStatus());
                break;
            }
            try {
                Thread.sleep(1000);
            } catch (Exception ee) {
            }
        }
        channel.disconnect();
        String remoteResponseStr = sb.toString().trim();
        return remoteResponseStr;
    }

    /**
     * Launch a command on the remote host and exit even if the command is not done (useful for launching daemons).
     * 
     * @param session the session to use.
     * @param command the command to launch.
     * @return the output of the command.
     * @throws JSchException
     * @throws IOException
     */
    private static String launchACommandAndExit( Session session, String command ) throws JSchException, IOException {
        Channel channel = session.openChannel("exec");
        ((ChannelExec) channel).setCommand(command);
        channel.setInputStream(null);
        ((ChannelExec) channel).setErrStream(System.err);
        InputStream in = channel.getInputStream();
        channel.connect();
        StringBuilder sb = new StringBuilder();
        byte[] tmp = new byte[1024];
        while( true ) {
            while( in.available() > 0 ) {
                int i = in.read(tmp, 0, 1024);
                if (i < 0)
                    break;
                sb.append(new String(tmp, 0, i));
            }
            if (channel.isClosed()) {
                if (in.available() > 0)
                    continue;
                break;
            }
            try {
                if (sb.length() > 0) {
                    break;
                }
                Thread.sleep(1000);
            } catch (Exception ee) {
            }
        }
        channel.disconnect();
        String remoteResponseStr = sb.toString().trim();
        return remoteResponseStr;
    }

    /**
     * Launch a shell command.
     * 
     * @param session the session to use.
     * @param command the command to launch.
     * @return the output of the command.
     * @throws JSchException
     * @throws IOException
     */
    public static String runShellCommand( Session session, String command ) throws JSchException, IOException {
        String remoteResponseStr = launchACommand(session, command);
        return remoteResponseStr;
    }

    /**
     * Run a daemon command or commands that do not exit quickly.
     * 
     * @param session the session to use.
     * @param command the command to launch.
     * @return the output of the command.
     * @throws JSchException
     * @throws IOException
     */
    public static String runDemonShellCommand( Session session, String command ) throws JSchException, IOException {
        String remoteResponseStr = launchACommandAndExit(session, command);
        return remoteResponseStr;
    }

    /**
     * Download a remote file via scp.
     * 
     * @param session the session to use.
     * @param remoteFilePath the remote file path.
     * @param localFilePath the local file path to copy into.
     * @throws Exception
     */
    public static void downloadFile( Session session, String remoteFilePath, String localFilePath ) throws Exception {
        // exec 'scp -f rfile' remotely
        String command = "scp -f " + remoteFilePath;
        Channel channel = session.openChannel("exec");
        ((ChannelExec) channel).setCommand(command);

        // get I/O streams for remote scp
        OutputStream out = channel.getOutputStream();
        InputStream in = channel.getInputStream();

        channel.connect();

        byte[] buf = new byte[1024];

        // send '\0'
        buf[0] = 0;
        out.write(buf, 0, 1);
        out.flush();

        while( true ) {
            int c = checkAck(in);
            if (c != 'C') {
                break;
            }

            // read '0644 '
            in.read(buf, 0, 5);

            long filesize = 0L;
            while( true ) {
                if (in.read(buf, 0, 1) < 0) {
                    // error
                    break;
                }
                if (buf[0] == ' ')
                    break;
                filesize = filesize * 10L + (long) (buf[0] - '0');
            }

            String file = null;
            for( int i = 0;; i++ ) {
                in.read(buf, i, 1);
                if (buf[i] == (byte) 0x0a) {
                    file = new String(buf, 0, i);
                    break;
                }
            }

            System.out.println("filesize=" + filesize + ", file=" + file);

            // send '\0'
            buf[0] = 0;
            out.write(buf, 0, 1);
            out.flush();

            // read a content of lfile
            FileOutputStream fos = new FileOutputStream(localFilePath);
            int foo;
            while( true ) {
                if (buf.length < filesize)
                    foo = buf.length;
                else
                    foo = (int) filesize;
                foo = in.read(buf, 0, foo);
                if (foo < 0) {
                    // error
                    break;
                }
                fos.write(buf, 0, foo);
                filesize -= foo;
                if (filesize == 0L)
                    break;
            }
            fos.close();

            if (checkAck(in) != 0) {
                throw new RuntimeException();
            }

            // send '\0'
            buf[0] = 0;
            out.write(buf, 0, 1);
            out.flush();
        }

    }

    /**
     * Upload a file to the remote host via scp.
     * 
     * @param session the session to use.
     * @param localFile the local file path.
     * @param remoteFile the remote file path to copy into. 
     * @throws Exception
     */
    public static void uploadFile( Session session, String localFile, String remoteFile ) throws Exception {
        boolean ptimestamp = true;
        // exec 'scp -t rfile' remotely
        String command = "scp " + (ptimestamp ? "-p" : "") + " -t " + remoteFile;
        Channel channel = session.openChannel("exec");
        ((ChannelExec) channel).setCommand(command);

        // get I/O streams for remote scp
        OutputStream out = channel.getOutputStream();
        InputStream in = channel.getInputStream();

        channel.connect();

        if (checkAck(in) != 0) {
            throw new RuntimeException();
        }

        File _lfile = new File(localFile);

        if (ptimestamp) {
            command = "T" + (_lfile.lastModified() / 1000) + " 0";
            // The access time should be sent here,
            // but it is not accessible with JavaAPI ;-<
            command += (" " + (_lfile.lastModified() / 1000) + " 0\n");
            out.write(command.getBytes());
            out.flush();
            if (checkAck(in) != 0) {
                throw new RuntimeException();
            }
        }

        // send "C0644 filesize filename", where filename should not include '/'
        long filesize = _lfile.length();
        command = "C0644 " + filesize + " ";
        if (localFile.lastIndexOf('/') > 0) {
            command += localFile.substring(localFile.lastIndexOf('/') + 1);
        } else {
            command += localFile;
        }
        command += "\n";
        out.write(command.getBytes());
        out.flush();
        if (checkAck(in) != 0) {
            throw new RuntimeException();
        }

        // send a content of lfile
        FileInputStream fis = new FileInputStream(localFile);
        byte[] buf = new byte[1024];
        while( true ) {
            int len = fis.read(buf, 0, buf.length);
            if (len <= 0)
                break;
            out.write(buf, 0, len); // out.flush();
        }
        fis.close();
        fis = null;
        // send '\0'
        buf[0] = 0;
        out.write(buf, 0, 1);
        out.flush();
        if (checkAck(in) != 0) {
            throw new RuntimeException();
        }
        out.close();

        channel.disconnect();
    }

    private static int checkAck( InputStream in ) throws IOException {
        int b = in.read();
        // b may be 0 for success,
        // 1 for error,
        // 2 for fatal error,
        // -1
        if (b == 0)
            return b;
        if (b == -1)
            return b;

        if (b == 1 || b == 2) {
            StringBuffer sb = new StringBuffer();
            int c;
            do {
                c = in.read();
                sb.append((char) c);
            } while( c != '\n' );
            if (b == 1) { // error
                System.err.print(sb.toString());
            }
            if (b == 2) { // fatal error
                System.err.print(sb.toString());
            }
        }
        return b;
    }

}
