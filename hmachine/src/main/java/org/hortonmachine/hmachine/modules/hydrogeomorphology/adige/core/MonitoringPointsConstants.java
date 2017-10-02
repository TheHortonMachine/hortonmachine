/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Library General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Library General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Library General Public License
 * along with this library; if not, write to the Free Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.hortonmachine.hmachine.modules.hydrogeomorphology.adige.core;

/**
 * @author Andrea Antonello - www.hydrologis.com
 */
public interface MonitoringPointsConstants {

    /*
     * tipi di sensori secondo la numerazione negli shapefile
     */
    public static int BORINGCLOSURE_TYPE_ID = -666;
    public static int HYDROMETER_TYPE_ID = 2;
    public static int DAM_TYPE_ID = 6;
    public static int DELIVERYPIPE_TYPE_ID = 9;
    public static int OFFTAKE_TYPE_ID = 3;

    /*
     * lakes
     */
    public static final String LAKE_CLOSED = "Laminazione naturale";
    public static final String LAKE_TRANSPARENT = "Lago trasparente";
    public static final String LAKE_DEFAULT = LAKE_TRANSPARENT;
    public static final String LAKE_DATELEVEL_CLOSED = "Data e livello per laminazione naturale";
    public static final String LAKE_DATELEVEL_TRANSPARENT = "Data e livello per lago trasparente";

    /*
     * barrage
     */
    public static final String BARRAGE_QMAX = "Portata massima";
    public static final String BARRAGE_CLOSED = "Sbarramento chiuso";
    public static final String BARRAGE_DEFAULT = BARRAGE_QMAX;

    /*
     * water pumps
     */
    public static final String WATERPUMPS_QMAX = "Portata massima";
    public static final String WATERPUMPS_CLOSED = "Idrovora chiusa";
    public static final String WATERPUMPS_DEFAULT = WATERPUMPS_QMAX;

    /*
     * dams
     */
    public static final String DAM_TRANSPARENT = "Diga trasparente";
    public static final String DAM_CLOSED = "Diga completamente chiusa";
    public static final String DATELEVEL_DAM_TRANSPARENT = "Data e livello per diga trasparente";
    public static final String DATELEVEL_DAM_CLOSED = "Data e livello per diga chiusa";
    public static final String DAM_TURB_DOWNLOAD_OPEN = "Scarico completamente aperto";
    public static final String DAM_TURB_CLOSED = "Turbina chiusa";
    public static final String DAM_DEFAULT = "Manovra di default";

    /*
     * other data
     */
    public static final String MOVES_DOWNLOADS = "Manovre degli scarichi";
    public static final String MOVES_TURBINATE = "Manovre delle turbinate";
    public static final String USERDATA = "Dati utente";
    public static final String LEVEL = "Livello";
    public static final String DISCHARGE = "Portata";
    public static final String DATE = "Data";

}