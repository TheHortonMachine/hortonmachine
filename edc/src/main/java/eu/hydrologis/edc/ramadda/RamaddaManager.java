/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 *
 * This program is free software: you can redistribute it and/or modify
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
package eu.hydrologis.edc.ramadda;

import static eu.hydrologis.edc.utils.Constants.RAMADDA_BASE;
import static eu.hydrologis.edc.utils.Constants.RAMADDA_HOST;
import static eu.hydrologis.edc.utils.Constants.RAMADDA_PASS;
import static eu.hydrologis.edc.utils.Constants.RAMADDA_PORT;
import static eu.hydrologis.edc.utils.Constants.RAMADDA_USER;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import ucar.unidata.repository.Constants;
import ucar.unidata.repository.client.ClientEntry;
import ucar.unidata.repository.client.RepositoryClient;
import ucar.unidata.ui.HttpFormEntry;
import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.xml.XmlUtil;

/**
 * The class that manages dialogs with the ramadda repository server.  
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class RamaddaManager implements Constants {

    private static final String TAB = "   ";
    private RepositoryClient repositoryClient;
    private final PrintStream outputStream;
    private String host;
    private String port;
    private String base;
    private RamaddaEntry ramaddaRootEntry;
    private String user;
    private String pass;

    public RamaddaManager( Properties properties, PrintStream outputStream ) {
        this.outputStream = outputStream;
        host = properties.getProperty(RAMADDA_HOST);
        if (host == null) {
            throw new IllegalArgumentException("Missing ramadda host definition in properties. (RAMADDA_HOST=...)");
        }

        user = properties.getProperty(RAMADDA_USER);
        if (user == null) {
            throw new IllegalArgumentException("Missing ramadda user definition in properties. (RAMADDA_USER=...)");
        }

        pass = properties.getProperty(RAMADDA_PASS);
        if (pass == null) {
            throw new IllegalArgumentException("Missing ramadda password definition in properties. (RAMADDA_PASS=...)");
        }

        port = properties.getProperty(RAMADDA_PORT);
        if (port == null) {
            throw new IllegalArgumentException("Missing ramadda port definition in properties. (RAMADDA_PORT=...)");
        }

        base = properties.getProperty(RAMADDA_BASE);
        if (base == null) {
            throw new IllegalArgumentException("Missing ramadda base definition in properties. (RAMADDA_BASE=...)");
        }

    }

    public RepositoryClient getRepositoryClient() throws Exception {
        if (repositoryClient == null) {
            repositoryClient = new RepositoryClient(host, Integer.parseInt(port), base, user, pass);
            String[] msg = {""};
            if (!repositoryClient.isValidSession(true, msg)) {
                throw new IOException("An error occurred while connecting to the ramadda server instance.");
            }
        }
        return repositoryClient;
    }

    /**
     * Creates a new group folder on the server.
     * 
     * @param parentId the id of the parent.
     * @param name the name for the new group.
     * @throws Exception
     */
    public void createNewGroup( String parentId, String name ) throws Exception {
        if (parentId == null) {
            RamaddaEntry rootEntry = getRootEntry();
            parentId = rootEntry.getId();
        }
        repositoryClient.newGroup(parentId, name);
    }

    /**
     * Extract an entry by its id.
     * 
     * @param id the entry id.
     * @return the {@link RamaddaEntry} extracted.
     * @throws Exception
     */
    public RamaddaEntry getRamaddaEntryById( String id ) throws Exception {
        String[] args = new String[]{ARG_ENTRYID, id, ARG_OUTPUT, "xml.xml", ARG_SESSIONID, repositoryClient.getSessionId()};
        String url = HtmlUtil.url(repositoryClient.URL_ENTRY_SHOW.getFullUrl(), args);
        String xml = IOUtil.readContents(url, RamaddaManager.class);
        Element root = XmlUtil.getRoot(xml);

        RamaddaEntry ramaddaEntry = new RamaddaEntry(repositoryClient, root);
        return ramaddaEntry;
    }

    /**
     * Searches and extracts an entry in the repository by its name.
     * 
     * @param name the name to search for.
     * @return the matching entry or null.
     * @throws Exception
     */
    public RamaddaEntry findRamaddaEntryByName( String name ) throws Exception {
        return findRecursive(getRootEntry(), name);
    }

    /**
     * Fetch a file from ramadda and dump it to file.
     * 
     * @param entryId the id of the entry to fetch.
     * @param outputFile the file to which to dump to, can be a folder or a file.
     * @throws Exception
     */
    public void downloadFileFromRamadda( String entryId, File outputFile ) throws Exception {
        repositoryClient.writeFile(entryId, outputFile);
    }

    /**
     * Uploads a file to ramadda.
     * 
     * @param entryName name of the entry that will appear in the ramadda server.
     * @param entryDescription a description of the entry that will appear in the ramadda server.
     * @param type the file type that is uploaded.
     * @param parent this can be:
     *              <ul>
     *                <li>id of the parent</li>
     *                <li>
     *                  or the parent path inside the repository, into which to upload the file
     *                  ex. <i>morpheo/documents</i>, where <i>morpheo</i> is the base group and <i>documents</i>
     *                  the subgroup.
     *                </li>
     *                <li><code>null</code>, in which case the root of the repository is used.</li>
     *              </ul> 
     * @param filePath the path to the file to upload.
     * @return the id of the entry that was created.
     * @throws Exception
     */
    public String uploadFile( String entryName, String entryDescription, String type, String parent, String filePath )
            throws Exception {
        if (parent == null) {
            parent = getRootEntry().getId();
        }

        String id = repositoryClient.uploadFile(entryName, entryDescription, parent, filePath);
        return id;
    }

    /**
     * Dumps the tree of the repository.
     * 
     * @throws Exception
     */
    public void dumpTree() throws Exception {
        RamaddaEntry ramaddaEntry = getRootEntry();
        // the root id
        outputStream.println(ramaddaEntry.toString());

        dumpRecursive(ramaddaEntry, TAB);
    }

    /**
     * Creates a {@link List} of {@link ClientEntry}s for a particular entry.
     * 
     * @param repositoryClient the repository client reference.
     * @param parentId the id of the parent entry.
     * @return the list of child entries.
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public static List<RamaddaEntry> getChildEntries( RepositoryClient repositoryClient, String parentId ) throws Exception {
        String[] args = new String[]{ARG_ENTRYID, parentId, ARG_OUTPUT, "xml.xml", ARG_SESSIONID, repositoryClient.getSessionId()};
        String url = HtmlUtil.url(repositoryClient.URL_ENTRY_SHOW.getFullUrl(), args);
        String xml = IOUtil.readContents(url, RamaddaManager.class);
        Element root = XmlUtil.getRoot(xml);

        List<RamaddaEntry> childEntries = new ArrayList<RamaddaEntry>();
        List children = XmlUtil.getElements(root, "entry");
        for( int i = 0; i < children.size(); i++ ) {
            Object object = children.get(i);
            RamaddaEntry ramaddaEntry = new RamaddaEntry(repositoryClient, (Node) object);
            childEntries.add(ramaddaEntry);
        }
        return childEntries;
    }

    /**
     * Recursively search for the first entry with the given name.
     * 
     * @param entry the entry to traverse.
     * @param name the name to search for.
     * @return the matching entry or null.
     * @throws Exception
     */
    private RamaddaEntry findRecursive( RamaddaEntry entry, String name ) throws Exception {
        List<RamaddaEntry> childEntries = getChildEntries(repositoryClient, entry.getId());
        for( int i = 0; i < childEntries.size(); i++ ) {
            RamaddaEntry childEntry = childEntries.get(i);

            if (childEntry.getName().equals(name)) {
                return childEntry;
            }

            RamaddaEntry ramaddaEntry = findRecursive(childEntry, name);
            if (ramaddaEntry != null) {
                return ramaddaEntry;
            }
        }
        return null;
    }

    /**
     * Get the repository's root entry.
     * 
     * @return the root entry of the repository.
     * @throws Exception
     */
    private RamaddaEntry getRootEntry() throws Exception {
        if (ramaddaRootEntry == null) {
            List<HttpFormEntry> postEntries = new ArrayList<HttpFormEntry>();
            postEntries.add(HttpFormEntry.hidden(ARG_SESSIONID, repositoryClient.getSessionId()));
            postEntries.add(HttpFormEntry.hidden(ARG_OUTPUT, "xml.xml"));
            String[] result = repositoryClient.doPost(repositoryClient.URL_ENTRY_SHOW, postEntries);
            if (result[0] != null) {
                System.err.println("Error:" + result[0]);
            }
            Element response = XmlUtil.getRoot(result[1]);
            ramaddaRootEntry = new RamaddaEntry(repositoryClient, response);
        }
        return ramaddaRootEntry;
    }

    /**
     * Recursively traverses the entries and dumps its childs and subchilds.
     * 
     * @param entry the start entry.
     * @param tab the tabulator characters to use.
     * @throws Exception
     */
    private void dumpRecursive( RamaddaEntry entry, String tab ) throws Exception {
        List<RamaddaEntry> childEntries = getChildEntries(repositoryClient, entry.getId());
        for( int i = 0; i < childEntries.size(); i++ ) {
            RamaddaEntry childEntry = childEntries.get(i);
            outputStream.println(tab + childEntry.toString());
            dumpRecursive(childEntry, tab + tab);
        }
    }

}
