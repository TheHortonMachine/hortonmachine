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
package eu.hydrologis.edc.annotatedclasses;

import static eu.hydrologis.edc.utils.Constants.AGENCY;
import static eu.hydrologis.edc.utils.Constants.APPROVALDATE;
import static eu.hydrologis.edc.utils.Constants.CODE;
import static eu.hydrologis.edc.utils.Constants.DMV_ID;
import static eu.hydrologis.edc.utils.Constants.EDC_SCHEMA;
import static eu.hydrologis.edc.utils.Constants.ENDDATE;
import static eu.hydrologis.edc.utils.Constants.ID;
import static eu.hydrologis.edc.utils.Constants.OFFTAKES_PERMISSIONS;
import static eu.hydrologis.edc.utils.Constants.OFFTAKES_ID;
import static eu.hydrologis.edc.utils.Constants.PERMISSIONSDISCHARGE_ID;
import static eu.hydrologis.edc.utils.Constants.PERMISSIONSUSAGE_ID;
import static eu.hydrologis.edc.utils.Constants.RATEDPOWER;
import static eu.hydrologis.edc.utils.Constants.STARTDATE;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Type;
import org.joda.time.DateTime;

/**
 * The Offtakes Permissions Table.
 * 
 * <p>
 * The table of permissions provided by the administrations for particular 
 * usages of, for example, monitoring points or dams.
 * </p>
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 * @author Silvia Franceschi (www.hydrologis.com)
 */
@Entity
@Table(name = OFFTAKES_PERMISSIONS, schema = EDC_SCHEMA)
public class OfftakesPermissionsTable {
    /**
     * The unique id of the permission.
     */
    @Id
    @Column(name = ID, nullable = false)
    private Long id;

    /**
     * The offtake generating the value.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = OFFTAKES_ID, referencedColumnName = ID, nullable = false)
    private OfftakesTable offtake;

    /**
     * The permissions discharge.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = PERMISSIONSDISCHARGE_ID, referencedColumnName = ID, nullable = false)
    private PermissionsDischargeTable permissionsDischarge;

    /**
     * The administrative code given to this permission.
     */
    @Column(name = CODE, nullable = true)
    private String code;

    /**
     * The name of the agency that handles the permission.
     */
    @Column(name = AGENCY, nullable = true)
    private String agency;

    /**
     * The approval date of the permission.
     */
    @Column(name = APPROVALDATE, nullable = false)
    @Type(type = "org.joda.time.contrib.hibernate.PersistentDateTime")
    private DateTime approvalDate;

    /**
     * The activation date of the permission.
     */
    @Column(name = STARTDATE, nullable = false)
    @Type(type = "org.joda.time.contrib.hibernate.PersistentDateTime")
    private DateTime startDate;

    /**
     * The deactivation date of the permission.
     */
    @Column(name = ENDDATE, nullable = false)
    @Type(type = "org.joda.time.contrib.hibernate.PersistentDateTime")
    private DateTime endDate;

    /**
     * The rated power for this permission.
     */
    @Column(name = RATEDPOWER, nullable = true)
    private Double ratedPower;

    /**
     * The <a href="http://it.wikipedia.org/wiki/Deflusso_minimo_vitale">DMV</a> 
     * for this permission, as required by Italian law.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = DMV_ID, referencedColumnName = ID, nullable = true)
    private DmvTable dmv;

    /**
     * The usage of the permission.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = PERMISSIONSUSAGE_ID, referencedColumnName = ID, nullable = false)
    private PermissionsUsageTable permissionsUsage;

    /*
     * ====================================
     */

    public Long getId() {
        return id;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public void setOfftake( OfftakesTable offtake ) {
        this.offtake = offtake;
    }

    public OfftakesTable getOfftake() {
        return offtake;
    }

    public void setPermissionsDischarge( PermissionsDischargeTable permissionsDischarge ) {
        this.permissionsDischarge = permissionsDischarge;
    }

    public PermissionsDischargeTable getPermissionsDischarge() {
        return permissionsDischarge;
    }

    public String getCode() {
        return code;
    }

    public void setCode( String code ) {
        this.code = code;
    }

    public String getAgency() {
        return agency;
    }

    public void setAgency( String agency ) {
        this.agency = agency;
    }

    public DateTime getApprovalDate() {
        return approvalDate;
    }

    public void setApprovalDate( DateTime approvalDate ) {
        this.approvalDate = approvalDate;
    }

    public DateTime getStartDate() {
        return startDate;
    }

    public void setStartDate( DateTime startDate ) {
        this.startDate = startDate;
    }

    public DateTime getEndDate() {
        return endDate;
    }

    public void setEndDate( DateTime endDate ) {
        this.endDate = endDate;
    }

    public Double getRatedPower() {
        return ratedPower;
    }

    public void setRatedPower( Double ratedPower ) {
        this.ratedPower = ratedPower;
    }

    public void setDmv( DmvTable dmv ) {
        this.dmv = dmv;
    }

    public DmvTable getDmv() {
        return dmv;
    }

    public void setPermissionsUsage( PermissionsUsageTable permissionsUsage ) {
        this.permissionsUsage = permissionsUsage;
    }

    public PermissionsUsageTable getPermissionsUsage() {
        return permissionsUsage;
    }

}
