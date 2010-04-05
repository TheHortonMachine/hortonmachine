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

import static eu.hydrologis.edc.utils.Constants.EDC_SCHEMA;
import static eu.hydrologis.edc.utils.Constants.ID;
import static eu.hydrologis.edc.utils.Constants.RESERVOIRSVOLUMESSCALES;
import static eu.hydrologis.edc.utils.Constants.RESERVOIRS_ID;
import static eu.hydrologis.edc.utils.Constants.SCALETYPE_ID;
import static eu.hydrologis.edc.utils.Constants.SURFACELEVEL;
import static eu.hydrologis.edc.utils.Constants.SURFACELEVELUNITS_ID;
import static eu.hydrologis.edc.utils.Constants.VOLUME;
import static eu.hydrologis.edc.utils.Constants.VOLUMEUNITS_ID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;

/**
 * The Reservoirs Volumes Scales Table.
 * 
 * <p>
 * This table holds the scales for volumes calculations on reservoirs.
 * </p>
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 * @author Silvia Franceschi (www.hydrologis.com)
 */
@Entity
@Table(name = RESERVOIRSVOLUMESSCALES, schema = EDC_SCHEMA)
public class ReservoirsVolumesScalesTable {

    /**
     * The unique id of the reservoirsvolumesscales record.
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

    /**
     * The surface level.
     */
    @Column(name = SURFACELEVEL, nullable = false)
    private Double surfaceLevel;

    /**
     * The volume.
     */
    @Column(name = VOLUME, nullable = false)
    private Double volume;

    /**
     * The surface level's unit.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = SURFACELEVELUNITS_ID, referencedColumnName = ID, nullable = false)
    private UnitsTable surfacelevelUnit;

    /**
     * The volume's unit.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = VOLUMEUNITS_ID, referencedColumnName = ID, nullable = false)
    private UnitsTable volumeUnit;

    /*
     * ================================================
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

    public Double getSurfaceLevel() {
        return surfaceLevel;
    }

    public void setSurfaceLevel( Double surfaceLevel ) {
        this.surfaceLevel = surfaceLevel;
    }

    public Double getVolume() {
        return volume;
    }

    public void setVolume( Double volume ) {
        this.volume = volume;
    }

    public void setSurfacelevelUnit( UnitsTable surfacelevelUnit ) {
        this.surfacelevelUnit = surfacelevelUnit;
    }

    public UnitsTable getSurfacelevelUnit() {
        return surfacelevelUnit;
    }

    public void setVolumeUnit( UnitsTable volumeUnit ) {
        this.volumeUnit = volumeUnit;
    }

    public UnitsTable getVolumeUnit() {
        return volumeUnit;
    }

}
