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

import static eu.hydrologis.edc.utils.Constants.*;
import static eu.hydrologis.edc.utils.Constants.EDC_SCHEMA;
import static eu.hydrologis.edc.utils.Constants.HYDROMETERSDISCHARGESCALES;
import static eu.hydrologis.edc.utils.Constants.HYDROMETER_ID;
import static eu.hydrologis.edc.utils.Constants.ID;
import static eu.hydrologis.edc.utils.Constants.LEVEL;
import static eu.hydrologis.edc.utils.Constants.SCALETYPE_ID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;

/**
 * The Hydrometers Discharge Scales Table.
 * 
 * <p>
 * This table holds the scales for discharge calculations on hydrometers.
 * </p>
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 * @author Silvia Franceschi (www.hydrologis.com)
 */
@Entity
@Table(name = HYDROMETERSDISCHARGESCALES, schema = EDC_SCHEMA)
public class HydrometersDischargeScalesTable {
    /**
     * The unique id of the hydrometersdischargescales record.
     */
    @Id
    @Column(name = ID, nullable = false)
    private Long id;

    /**
     * The hydrometer this is part of.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = HYDROMETER_ID, referencedColumnName = ID, nullable = false)
    private HydrometersTable hydrometer;

    /**
     * The scale type.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = SCALETYPE_ID, referencedColumnName = ID, nullable = false)
    private ScaleTypeTable scaleType;

    /**
     * The measured level. 
     */
    @Column(name = LEVEL, nullable = false)
    private Double level;

    /**
     * The discharge corresponding to the measured level. 
     */
    @Column(name = DISCHARGE, nullable = false)
    private Double discharge;

    /**
     * The level's unit.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = LEVELUNIT_ID, referencedColumnName = ID, nullable = false)
    private UnitsTable levelUnit;

    /**
     * The discharge's unit.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = DISCHARGEUNIT_ID, referencedColumnName = ID, nullable = false)
    private UnitsTable dischargeUnit;

    /*
     * ===================================
     */

    public Long getId() {
        return id;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public HydrometersTable getHydrometer() {
        return hydrometer;
    }

    public void setHydrometer( HydrometersTable hydrometer ) {
        this.hydrometer = hydrometer;
    }

    public ScaleTypeTable getScaleType() {
        return scaleType;
    }

    public void setScaleType( ScaleTypeTable scaleType ) {
        this.scaleType = scaleType;
    }

    public Double getLevel() {
        return level;
    }

    public void setLevel( Double level ) {
        this.level = level;
    }

    public Double getDischarge() {
        return discharge;
    }

    public void setDischarge( Double discharge ) {
        this.discharge = discharge;
    }

    public void setLevelUnit( UnitsTable levelUnit ) {
        this.levelUnit = levelUnit;
    }

    public UnitsTable getLevelUnit() {
        return levelUnit;
    }

    public void setDischargeUnit( UnitsTable dischargeUnit ) {
        this.dischargeUnit = dischargeUnit;
    }

    public UnitsTable getDischargeUnit() {
        return dischargeUnit;
    }

}
