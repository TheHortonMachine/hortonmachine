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

import static eu.hydrologis.edc.utils.Constants.CALIBRATION;
import static eu.hydrologis.edc.utils.Constants.DATATYPE_ID;
import static eu.hydrologis.edc.utils.Constants.DESCRIPTION;
import static eu.hydrologis.edc.utils.Constants.DYNAMICMONITORINGPOINTS;
import static eu.hydrologis.edc.utils.Constants.EDC_SCHEMA;
import static eu.hydrologis.edc.utils.Constants.ID;
import static eu.hydrologis.edc.utils.Constants.POINTTYPE_ID;
import static eu.hydrologis.edc.utils.Constants.UNITS_ID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

/**
 * Table of dynamic monitoring points.
 * 
 * <p>
 * Dynamic here is related to the fact, that the 
 * monitoring points have no fixed position and can collect 
 * data both in time and on the vertical axis.
 * In one point more measure can therefore be taken. 
 * </p>
 * 
 * <p>
 * The name dynamic monitoring point was chosen in order to 
 * distinguish from those that fixed in location like for example 
 * {@link MonitoringPointsTable monitoring points}.
 * </p>
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 * @author Silvia Franceschi (www.hydrologis.com)
 *
 */
@Entity
@Table(name = DYNAMICMONITORINGPOINTS, schema = EDC_SCHEMA)
public class DynamicMonitoringPointsTable {
    /**
     * A unique id for the monitoring point. 
     */
    @Id
    @Column(name = ID, nullable = false)
    private Long id;

    /**
     * The type of the point.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = POINTTYPE_ID, referencedColumnName = ID, nullable = true)
    private PointTypeTable pointType;

    /**
     * The description of the monitoring point.
     */
    @Column(name = DESCRIPTION, nullable = true)
    private String description;

    /**
     * The data type that are provided by this monitoring point.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = DATATYPE_ID, referencedColumnName = ID, nullable = false)
    private DataTypeTable dataType;

    /**
     * TODO The calibration value.
     */
    @Column(name = CALIBRATION, nullable = true)
    private Double calibration;

    /**
     * The unit of the monitoring point.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = UNITS_ID, referencedColumnName = ID, nullable = false)
    private UnitsTable unit;
    
    /*
     * =====================================
     */

    public Long getId() {
        return id;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public PointTypeTable getPointType() {
        return pointType;
    }

    public void setPointType( PointTypeTable pointType ) {
        this.pointType = pointType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription( String description ) {
        this.description = description;
    }

    public DataTypeTable getDataType() {
        return dataType;
    }

    public void setDataType( DataTypeTable dataType ) {
        this.dataType = dataType;
    }

    public Double getCalibration() {
        return calibration;
    }

    public void setCalibration( Double calibration ) {
        this.calibration = calibration;
    }

    public void setUnit( UnitsTable unit ) {
        this.unit = unit;
    }

    public UnitsTable getUnit() {
        return unit;
    }
}
