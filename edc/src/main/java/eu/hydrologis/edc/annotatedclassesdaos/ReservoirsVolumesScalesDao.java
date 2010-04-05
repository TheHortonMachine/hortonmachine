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
package eu.hydrologis.edc.annotatedclassesdaos;

import eu.hydrologis.edc.annotatedclasses.ReservoirsTable;
import eu.hydrologis.edc.annotatedclasses.ReservoirsVolumesScalesTable;
import eu.hydrologis.edc.annotatedclasses.ScaleTypeTable;
import eu.hydrologis.edc.annotatedclasses.UnitsTable;
import eu.hydrologis.edc.databases.EdcSessionFactory;

/**
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class ReservoirsVolumesScalesDao extends AbstractEdcDao {

    public ReservoirsVolumesScalesDao( EdcSessionFactory edcSessionFactory ) {
        super(edcSessionFactory);
    }

    protected void processLine( String[] lineSplit ) {
        ReservoirsVolumesScalesTable rvsT = new ReservoirsVolumesScalesTable();

        String idString = lineSplit[0].trim();
        if (idString.length() > 0) {
            Long id = new Long(idString);
            rvsT.setId(id);
        }

        String reservoirId = lineSplit[1].trim();
        if (reservoirId.length() > 0) {
            ReservoirsTable rT = new ReservoirsTable();
            rT.setId(Long.parseLong(reservoirId));
            rvsT.setReservoir(rT);
        }

        String scaleTypeId = lineSplit[2].trim();
        if (scaleTypeId.length() > 0) {
            ScaleTypeTable stT = new ScaleTypeTable();
            stT.setId(Long.parseLong(scaleTypeId));
            rvsT.setScaleType(stT);
        }

        String surfaceLevel = lineSplit[3].trim();
        if (surfaceLevel.length() > 0) {
            rvsT.setSurfaceLevel(Double.parseDouble(surfaceLevel));
        }

        String volume = lineSplit[4].trim();
        if (volume.length() > 0) {
            rvsT.setVolume(Double.parseDouble(volume));
        }

        String surfacelevelUnit = lineSplit[5].trim();
        if (surfacelevelUnit.length() > 0) {
            UnitsTable unitsTable = new UnitsTable();
            unitsTable.setId(Long.parseLong(surfacelevelUnit));
            rvsT.setSurfacelevelUnit(unitsTable);
        }

        String volumeUnit = lineSplit[6].trim();
        if (volumeUnit.length() > 0) {
            UnitsTable unitsTable = new UnitsTable();
            unitsTable.setId(Long.parseLong(volumeUnit));
            rvsT.setVolumeUnit(unitsTable);
        }

        session.save(rvsT);
    }

    public String getRecordDefinition() throws Exception {
        StringBuilder sB = new StringBuilder();
        sB.append(tableAnnotationToString(ReservoirsVolumesScalesTable.class)).append(": ");
        sB.append(columnAnnotationToString(ReservoirsVolumesScalesTable.class, "id")).append(", ");
        sB.append(joinColumnAnnotationToString(ReservoirsVolumesScalesTable.class, "reservoir"))
                .append(", ");
        sB.append(joinColumnAnnotationToString(ReservoirsVolumesScalesTable.class, "scaleType"))
                .append(", ");
        sB.append(columnAnnotationToString(ReservoirsVolumesScalesTable.class, "surfaceLevel"))
                .append(", ");
        sB.append(columnAnnotationToString(ReservoirsVolumesScalesTable.class, "volume")).append(
                ", ");
        sB.append(joinColumnAnnotationToString(ReservoirsVolumesScalesTable.class, "surfacelevelUnit"))
                .append(", ");
        sB.append(joinColumnAnnotationToString(ReservoirsVolumesScalesTable.class, "volumeUnit"));
        return sB.toString();
    }
}
