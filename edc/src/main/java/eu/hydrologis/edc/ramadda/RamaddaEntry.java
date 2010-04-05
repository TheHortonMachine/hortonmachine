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

import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import ucar.unidata.repository.Constants;
import ucar.unidata.repository.client.ClientEntry;
import ucar.unidata.repository.client.RepositoryClient;
import ucar.unidata.xml.XmlUtil;

/**
 * Wrapper around {@link ClientEntry}, to extend it with some of the missing info.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class RamaddaEntry {

    private String id;
    private String parentGroupId;
    private String name;
    private String description;
    private boolean isGroup;
    private boolean acceptsNew;
    private boolean acceptsUpload;
    private String type;
    private DateTime creationDate;
    private DateTime fromDate;
    private DateTime toDate;

    private Double north;
    private Double south;
    private Double east;
    private Double west;

    private List<RamaddaEntry> childEntries;
    private final RepositoryClient repositoryClient;
    private ClientEntry entry;

    public RamaddaEntry( RepositoryClient repositoryClient, Node entryNode ) throws Exception {

        this.repositoryClient = repositoryClient;
        id = XmlUtil.getAttribute(entryNode, "id");

        name = XmlUtil.getAttribute(entryNode, "name");
        description = XmlUtil.getGrandChildText(entryNode, Constants.TAG_DESCRIPTION);

        acceptsNew = XmlUtil.getAttribute(entryNode, "candonew", false);
        acceptsUpload = XmlUtil.getAttribute(entryNode, "candoupload", false);

        DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss 'GMT'");
        String createDateStr = XmlUtil.getAttribute(entryNode, "createdate");
        creationDate = formatter.parseDateTime(createDateStr);
        String fromDateStr = XmlUtil.getAttribute(entryNode, "fromdate");
        fromDate = formatter.parseDateTime(fromDateStr);
        String toDateStr = XmlUtil.getAttribute(entryNode, "todate");
        toDate = formatter.parseDateTime(toDateStr);

        // ClientEntry tmpEntry = repositoryClient.getEntry(id);

        entry = ClientEntry.getEntry((Element) entryNode);

        // TODO where to get projection
        if (entry.hasNorth() && entry.hasSouth() && entry.hasEast() && entry.hasWest()) {
            north = entry.getNorth();
            south = entry.getSouth();
            east = entry.getEast();
            west = entry.getWest();
        }

        String isGroupStr = XmlUtil.getAttribute(entryNode, "isgroup");
        isGroup = Boolean.parseBoolean(isGroupStr);
        parentGroupId = XmlUtil.getAttribute(entryNode, "group");
        type = XmlUtil.getAttribute(entryNode, "type");
    }

    /**
     * Extract the child entries of the entry.
     * 
     * @return the {@link List} of child entries.
     * @throws Exception
     */
    public List<RamaddaEntry> getChildEntries() throws Exception {
        if (childEntries == null) {
            childEntries = RamaddaManager.getChildEntries(repositoryClient, id);
        }
        return childEntries;
    }

    public String getId() {
        return id;
    }

    public String getParentGroupId() {
        return parentGroupId;
    }

    public String getOpendapUrl() throws Exception {
        for( ClientEntry.Service service : entry.getServices() ) {
            if (service.getType().equals(Constants.SERVICE_OPENDAP)) {
                String url = service.getUrl();
                return url;
            }
        }
        return null;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean isGroup() {
        return isGroup;
    }

    public boolean isAcceptsNew() {
        return acceptsNew;
    }

    public boolean isAcceptsUpload() {
        return acceptsUpload;
    }

    public String getType() {
        return type;
    }

    public DateTime getCreationDate() {
        return creationDate;
    }

    public DateTime getFromDate() {
        return fromDate;
    }

    public DateTime getToDate() {
        return toDate;
    }

    public Double getNorth() {
        return north;
    }

    public Double getSouth() {
        return south;
    }

    public Double getEast() {
        return east;
    }

    public Double getWest() {
        return west;
    }

    public String toString() {
        StringBuilder retValue = new StringBuilder();
        retValue.append("RamaddaEntry ( ").append(" name = ").append(this.name).append(
                " / description = ").append(this.description == null ? "" : this.description)
                .append(" /  id = ").append(this.id).append(" /  parentGroupId = ").append(
                        this.parentGroupId).append(" /  isGroup = ").append(this.isGroup).append(
                        " /  type = ").append(this.type).append(" /  creationDate = ").append(
                        this.creationDate).append(" /  fromDate = ").append(this.fromDate).append(
                        " /  toDate = ").append(this.toDate).append(" /  north = ").append(
                        this.north).append(" /  south = ").append(this.south).append(" /  east = ")
                .append(this.east).append(" /  west = ").append(this.west).append(
                        " /  acceptsNew = ").append(this.acceptsNew).append(" /  acceptsUpload = ")
                .append(this.acceptsUpload).append(" )");
        return retValue.toString();
    }

}
