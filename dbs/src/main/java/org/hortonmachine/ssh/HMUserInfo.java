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

import com.jcraft.jsch.UserInfo;

/**
 * {@link UserInfo} object to use for connections.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class HMUserInfo implements UserInfo {

    private String password;
    private String passphrase;
    /**
     * Contructor with password/passphrase.  
     * 
     * @param password the ssh password/passphrase to use.
     */
    public HMUserInfo( String password, String passphrase ) {
        this.password = password;
        this.passphrase = passphrase;
    }

    /**
     * Constructor without passowrd. 
     * 
     * <p>The pwd will be taken from the {@link SshUtilities#getPreference(String, String)} using the key {@link SshUtilities#PWD}.
     */
    public HMUserInfo() {
    }

    public String getPassword() {
        if (password == null || password.trim().length() == 0) {
            String thePwd = SshUtilities.getPreference(SshUtilities.PWD, "");
            if (thePwd.trim().length() > 0) {
                return thePwd;
            } else {
                return null;
            }
        } else {
            return password;
        }
    }

    public boolean promptYesNo( String str ) {
        return true;
    }

    public String getPassphrase() {
        if (passphrase == null || passphrase.trim().length() == 0) {
            String thePwd = SshUtilities.getPreference(SshUtilities.PWD, "");
            if (thePwd.trim().length() > 0) {
                return thePwd;
            } else {
                return null;
            }
        } else {
            return passphrase;
        }
    }

    public boolean promptPassphrase( String message ) {
        return true;
    }

    public boolean promptPassword( String message ) {
        return true;
    }

    public void showMessage( String message ) {
        System.out.println(message);
    }

}