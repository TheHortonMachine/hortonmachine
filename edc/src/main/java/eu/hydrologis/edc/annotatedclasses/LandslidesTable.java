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

import static eu.hydrologis.edc.utils.Constants.BASINTYPE_ID;
import static eu.hydrologis.edc.utils.Constants.CONTACT;
import static eu.hydrologis.edc.utils.Constants.DESCRIPTION;
import static eu.hydrologis.edc.utils.Constants.EDC_SCHEMA;
import static eu.hydrologis.edc.utils.Constants.ID;
import static eu.hydrologis.edc.utils.Constants.LANDSLIDEBASINRELATION_ID;
import static eu.hydrologis.edc.utils.Constants.LANDSLIDES;
import static eu.hydrologis.edc.utils.Constants.LANDSLIDESCLASSIFICATIONS_ID;
import static eu.hydrologis.edc.utils.Constants.NAME;
import static eu.hydrologis.edc.utils.Constants.SURVEYDATE;

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
 * The landslides table.
 * 
 * <p>
 * This table links has also a geometric representation in a spatial table,
 * linked by the id.
 * </p>
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 * @author Silvia Franceschi (www.hydrologis.com)
 */
@Entity
@Table(name = LANDSLIDES, schema = EDC_SCHEMA)
public class LandslidesTable {

    /**
     * The unique id of the landslide.
     */
    @Id
    @Column(name = ID, nullable = false)
    private Long id;

    /**
     * A name for the landslide.
     */
    @Column(name = NAME, nullable = false)
    private String name;

    /**
     * A name for the landslide.
     */
    @Column(name = DESCRIPTION, nullable = true)
    private String description;

    /**
     * The survey date.
     */
    @Column(name = SURVEYDATE, nullable = false)
    @Type(type = "org.joda.time.contrib.hibernate.PersistentDateTime")
    private DateTime surveyDate;

    /**
     * The surveyor's contact.
     */
    @Column(name = CONTACT, nullable = true)
    private String contact;

    /**
     * The classification of the landslide.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = LANDSLIDESCLASSIFICATIONS_ID, referencedColumnName = ID, nullable = false)
    private LandslidesClassificationsTable landslideClassification;

    /**
     * The type of basin in which the landslide is contained.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = BASINTYPE_ID, referencedColumnName = ID, nullable = false)
    private BasinTypeTable basinType;

    /**
     * The spatial relation between the landslide and the basin that containes it.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = LANDSLIDEBASINRELATION_ID, referencedColumnName = ID, nullable = false)
    private LandslideBasinRelationTable landslideBasinRelation;

    /*
     * ====================================
     */

    public Long getId() {
        return id;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public void setName( String name ) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setDescription( String description ) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setSurveyDate( DateTime surveyDate ) {
        this.surveyDate = surveyDate;
    }

    public DateTime getSurveyDate() {
        return surveyDate;
    }

    public void setContact( String contact ) {
        this.contact = contact;
    }

    public String getContact() {
        return contact;
    }

    public void setLandslideClassification( LandslidesClassificationsTable landslideClassification ) {
        this.landslideClassification = landslideClassification;
    }

    public LandslidesClassificationsTable getLandslideClassification() {
        return landslideClassification;
    }

    public void setBasinType( BasinTypeTable basinType ) {
        this.basinType = basinType;
    }

    public BasinTypeTable getBasinType() {
        return basinType;
    }

    public void setLandslideBasinRelation( LandslideBasinRelationTable landslideBasinRelation ) {
        this.landslideBasinRelation = landslideBasinRelation;
    }

    public LandslideBasinRelationTable getLandslideBasinRelation() {
        return landslideBasinRelation;
    }

}
