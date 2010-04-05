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

import static eu.hydrologis.edc.utils.Constants.ADISCR;
import static eu.hydrologis.edc.utils.Constants.AMAX;
import static eu.hydrologis.edc.utils.Constants.EDC_SCHEMA;
import static eu.hydrologis.edc.utils.Constants.HASSCALE;
import static eu.hydrologis.edc.utils.Constants.H_HS_MAX;
import static eu.hydrologis.edc.utils.Constants.H_HS_MIN;
import static eu.hydrologis.edc.utils.Constants.ID;
import static eu.hydrologis.edc.utils.Constants.RESERVOIRSDISCHARGESCALES;
import static eu.hydrologis.edc.utils.Constants.RESERVOIRS_ID;
import static eu.hydrologis.edc.utils.Constants.SCALETYPE_ID;
import static eu.hydrologis.edc.utils.Constants.SILLLEVEL;
import static eu.hydrologis.edc.utils.Constants.UNITS_ID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;

/**
 * The Reservoirs Discharge Scales Table.
 * 
 * <p>
 * This table holds the scales for discharge calculations on reservoirs.
 * </p>
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 * @author Silvia Franceschi (www.hydrologis.com)
 */
@Entity
@Table(name = RESERVOIRSDISCHARGESCALES, schema = EDC_SCHEMA)
public class ReservoirsDischargeScalesTable {

    /**
     * The unique id of the reservoirsdischargescales record.
     */
    @Id
    @Column(name = ID, nullable = false)
    private Long id;

    /**
     * The reservoir this is part of.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = RESERVOIRS_ID, referencedColumnName = ID, nullable = false)
    private ReservoirsTable reservoir;

    /**
     * The scale type.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = SCALETYPE_ID, referencedColumnName = ID, nullable = false)
    private ScaleTypeTable scaleType;

    @Column(name = SILLLEVEL, nullable = false)
    private Double sillLevel;

    @Column(name = AMAX, nullable = true)
    private Double aMax;

    @Column(name = ADISCR, nullable = true)
    private Double aDiscr;

    @Column(name = HASSCALE, nullable = false)
    private Boolean hasScale;

    @Column(name = H_HS_MIN, nullable = false)
    private Double hHsMin;

    @Column(name = H_HS_MAX, nullable = false)
    private Double hHsMax;
    
    /**
     * The common unit of {@link #sillLevel}, {@link #aMax}, {@link #aDiscr}, {@link #hHsMin}, {@link #hHsMax}.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = UNITS_ID, referencedColumnName = ID, nullable = false)
    private UnitsTable unit;

    /*
     * =========================================
     */

    public Long getId() {
        return id;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public ReservoirsTable getReservoir() {
        return reservoir;
    }

    public void setReservoir( ReservoirsTable reservoir ) {
        this.reservoir = reservoir;
    }

    public ScaleTypeTable getScaleType() {
        return scaleType;
    }

    public void setScaleType( ScaleTypeTable scaleType ) {
        this.scaleType = scaleType;
    }

    public Double getSillLevel() {
        return sillLevel;
    }

    public void setSillLevel( Double sillLevel ) {
        this.sillLevel = sillLevel;
    }

    public Double getaMax() {
        return aMax;
    }

    public void setaMax( Double aMax ) {
        this.aMax = aMax;
    }

    public Double getaDiscr() {
        return aDiscr;
    }

    public void setaDiscr( Double aDiscr ) {
        this.aDiscr = aDiscr;
    }

    public Boolean getHasScale() {
        return hasScale;
    }

    public void setHasScale( Boolean hasScale ) {
        this.hasScale = hasScale;
    }

    public Double gethHsMin() {
        return hHsMin;
    }

    public void sethHsMin( Double hHsMin ) {
        this.hHsMin = hHsMin;
    }

    public Double gethHsMax() {
        return hHsMax;
    }

    public void sethHsMax( Double hHsMax ) {
        this.hHsMax = hHsMax;
    }

    public void setUnit( UnitsTable unit ) {
        this.unit = unit;
    }

    public UnitsTable getUnit() {
        return unit;
    }

}
