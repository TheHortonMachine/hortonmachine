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

import static eu.hydrologis.edc.utils.Constants.CREATIONDATE;
import static eu.hydrologis.edc.utils.Constants.DESCRIPTION;
import static eu.hydrologis.edc.utils.Constants.EDC_SCHEMA;
import static eu.hydrologis.edc.utils.Constants.ENDDATE;
import static eu.hydrologis.edc.utils.Constants.GEOLOGYMAP_ID;
import static eu.hydrologis.edc.utils.Constants.ID;
import static eu.hydrologis.edc.utils.Constants.INPUTSURL;
import static eu.hydrologis.edc.utils.Constants.LANDCOVERMAP_ID;
import static eu.hydrologis.edc.utils.Constants.METEOMAP_ID;
import static eu.hydrologis.edc.utils.Constants.MODELS_ID;
import static eu.hydrologis.edc.utils.Constants.MORPHOLOGYMAP_ID;
import static eu.hydrologis.edc.utils.Constants.PARENTRUNS_ID;
import static eu.hydrologis.edc.utils.Constants.RESULTSURL;
import static eu.hydrologis.edc.utils.Constants.RUNS;
import static eu.hydrologis.edc.utils.Constants.SOILTYPEMAP_ID;
import static eu.hydrologis.edc.utils.Constants.STARTDATE;
import static eu.hydrologis.edc.utils.Constants.TIMESTEP;
import static eu.hydrologis.edc.utils.Constants.TITLE;
import static eu.hydrologis.edc.utils.Constants.USERS_ID;

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
 * The runs Table.
 * 
 * <p>
 * The table defines the runs, i.e. simulations and the necessary data 
 * to identify the launched model, its inputs and outputs.
 * </p>
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 * @author Silvia Franceschi (www.hydrologis.com)
 */
@Entity
@Table(name = RUNS, schema = EDC_SCHEMA)
public class RunsTable {
    /**
     * The unique id of the run.
     */
    @Id
    @Column(name = ID, nullable = false)
    private Long id;

    /**
     * The title of the run.
     */
    @Column(name = TITLE, nullable = false)
    private String title;

    /**
     * The description of the run.
     */
    @Column(name = DESCRIPTION, nullable = true)
    private String description;

    /**
     * The model used in the run.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = MODELS_ID, referencedColumnName = ID, nullable = false)
    private ModelsTable model;

    /**
     * The date of the run creation.
     */
    @Column(name = CREATIONDATE, nullable = false)
    @Type(type = "org.joda.time.contrib.hibernate.PersistentDateTime")
    private DateTime creationDate;

    /**
     * The date of the run's simulation start date.
     */
    @Column(name = STARTDATE, nullable = true)
    @Type(type = "org.joda.time.contrib.hibernate.PersistentDateTime")
    private DateTime startDate;

    /**
     * The date of the run's simulation end date.
     */
    @Column(name = ENDDATE, nullable = true)
    @Type(type = "org.joda.time.contrib.hibernate.PersistentDateTime")
    private DateTime endDate;

    /**
     * The timestep of the simulation in minutes.
     */
    @Column(name = TIMESTEP, nullable = true)
    private Double timestep;

    /**
     * The url to the location of the inputs of the run.
     */
    @Column(name = INPUTSURL, nullable = false)
    private String inputsUrlString;

    /**
     * The url to the location of the outputs of the run.
     */
    @Column(name = RESULTSURL, nullable = false)
    private String resultsUrlString;

    /**
     * The landcover map used in the run.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = LANDCOVERMAP_ID, referencedColumnName = ID, nullable = true)
    private LandcoverMapTable landcoverMap;

    /**
     * The soiltype map used in the run.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = SOILTYPEMAP_ID, referencedColumnName = ID, nullable = true)
    private SoilTypeMapTable soilTypeMap;

    /**
     * The geology map used in the run.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = GEOLOGYMAP_ID, referencedColumnName = ID, nullable = true)
    private GeologyMapTable geologyMap;

    /**
     * The block of base maps derived from the digital elevation model used in the run.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = MORPHOLOGYMAP_ID, referencedColumnName = ID, nullable = true)
    private MorphologyMapTable morphologyMap;

    /**
     * The meteo map used in the run.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = METEOMAP_ID, referencedColumnName = ID, nullable = true)
    private MeteoMapTable meteoMap;

    /**
     * The user that executed the run.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = USERS_ID, referencedColumnName = ID, nullable = false)
    private UsersTable user;

    /**
     * The run that created the input data for the current run.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = PARENTRUNS_ID, referencedColumnName = ID, nullable = true)
    private RunsTable parentRun;

    /*
     * ================================================
     */

    public Long getId() {
        return id;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle( String title ) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription( String description ) {
        this.description = description;
    }

    public void setModel( ModelsTable model ) {
        this.model = model;
    }

    public ModelsTable getModel() {
        return model;
    }

    public DateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate( DateTime creationDate ) {
        this.creationDate = creationDate;
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

    public void setTimestep( Double timestep ) {
        this.timestep = timestep;
    }

    public Double getTimestep() {
        return timestep;
    }

    public void setInputsUrlString( String inputsUrlString ) {
        this.inputsUrlString = inputsUrlString;
    }

    public String getInputsUrlString() {
        return inputsUrlString;
    }

    public String getResultsUrlString() {
        return resultsUrlString;
    }

    public void setResultsUrlString( String resultsUrlString ) {
        this.resultsUrlString = resultsUrlString;
    }

    public void setLandcoverMap( LandcoverMapTable landcoverMap ) {
        this.landcoverMap = landcoverMap;
    }

    public LandcoverMapTable getLandcoverMap() {
        return landcoverMap;
    }

    public void setSoilTypeMap( SoilTypeMapTable soilTypeMap ) {
        this.soilTypeMap = soilTypeMap;
    }

    public SoilTypeMapTable getSoilTypeMap() {
        return soilTypeMap;
    }

    public void setGeologyMap( GeologyMapTable geologyMap ) {
        this.geologyMap = geologyMap;
    }

    public GeologyMapTable getGeologyMap() {
        return geologyMap;
    }

    public void setMorphologyMap( MorphologyMapTable morphologyMap ) {
        this.morphologyMap = morphologyMap;
    }

    public MorphologyMapTable getMorphologyMap() {
        return morphologyMap;
    }

    public void setMeteoMap( MeteoMapTable meteoMap ) {
        this.meteoMap = meteoMap;
    }

    public MeteoMapTable getMeteoMap() {
        return meteoMap;
    }

    public void setUser( UsersTable user ) {
        this.user = user;
    }

    public UsersTable getUser() {
        return user;
    }

    public void setParentRun( RunsTable parentRun ) {
        this.parentRun = parentRun;
    }

    public RunsTable getParentRun() {
        return parentRun;
    }

}
