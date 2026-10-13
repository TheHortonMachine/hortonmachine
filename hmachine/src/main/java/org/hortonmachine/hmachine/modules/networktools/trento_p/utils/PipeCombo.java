package org.hortonmachine.hmachine.modules.networktools.trento_p.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.hortonmachine.gears.libs.exceptions.ModelsIOException;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.hmachine.modules.networktools.trento_p.utils.TrentoPFeatureType.AreasTrentoP;
import org.hortonmachine.hmachine.modules.networktools.trento_p.utils.TrentoPFeatureType.JunctionsTrentoP;
import org.hortonmachine.hmachine.modules.networktools.trento_p.utils.TrentoPFeatureType.PipesTrentoP;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.index.strtree.STRtree;
import org.geotools.api.feature.simple.SimpleFeature;

public class PipeCombo {
    private SimpleFeature pipeFeature;

    private double area;
    private double initialJunctionElev; // higher
    private double finalJunctionElev;

    private SimpleFeature areaFeature;

    private SimpleFeature junction1Feature;

    private SimpleFeature junction2Feature;

    public PipeCombo( SimpleFeature pipeFeature, SimpleFeature areaFeature, SimpleFeature junction1Feature,
            SimpleFeature junction2Feature ) throws ModelsIOException {
        this.pipeFeature = pipeFeature;
        this.areaFeature = areaFeature;
        this.junction1Feature = junction1Feature;
        this.junction2Feature = junction2Feature;

        if (areaFeature == null) {
            throw new ModelsIOException("The pipe " + pipeFeature.getAttribute(PipesTrentoP.ID.getAttributeName())
                    + " has no area with its ID.", "PipeCombo");
        }
        Geometry areaGeom = (Geometry) areaFeature.getDefaultGeometry();
        area = areaGeom.getArea() / 10000.0;

        double junction1Elev = elevation(junction1Feature);
        double junction2Elev = elevation(junction2Feature);

        if (junction1Elev > junction2Elev) {
            initialJunctionElev = junction1Elev;
            finalJunctionElev = junction2Elev;
        } else {
            initialJunctionElev = junction2Elev;
            finalJunctionElev = junction1Elev;
        }
    }

    private static double elevation( SimpleFeature junction ) throws ModelsIOException {
        Object elev = junction.getAttribute(TrentoPFeatureType.JunctionsTrentoP.ELEVATION.getAttributeName());
        if (!(elev instanceof Number)) {
            throw new ModelsIOException("The junction " + junction.getAttribute(JunctionsTrentoP.ID.getAttributeName())
                    + " has no elevation.", "PipeCombo");
        }
        return ((Number) elev).doubleValue();
    }

    public double getArea() {
        return area;
    }

    public double getInitialJunctionElev() {
        return initialJunctionElev;
    }

    public double getFinalJunctionElev() {
        return finalJunctionElev;
    }

    public SimpleFeature getPipeFeature() {
        return pipeFeature;
    }

    @Override
    public String toString() {
        return "pipe id: " + pipeFeature.getAttribute(PipesTrentoP.ID.getAttributeName()) + 
                " area id: " + areaFeature.getAttribute(AreasTrentoP.ID.getAttributeName()) + 
                " junct1 id: " + junction1Feature.getAttribute(JunctionsTrentoP.ID.getAttributeName()) + 
                " junct2 id: " + junction2Feature.getAttribute(JunctionsTrentoP.ID.getAttributeName()) 
                ;
    }

    public static List<PipeCombo> joinPipeCombos( SimpleFeatureCollection pipes, SimpleFeatureCollection areas,
            SimpleFeatureCollection junctions ) throws ModelsIOException {
        List<PipeCombo> comboList = new ArrayList<PipeCombo>();

        List<SimpleFeature> pipesList = FeatureUtilities.featureCollectionToList(pipes);
        List<SimpleFeature> areasList = FeatureUtilities.featureCollectionToList(areas);

        Map<Integer, SimpleFeature> idToAreasMap = areasList.stream()
                .collect(Collectors.toMap(f -> ((Number) f.getAttribute(PipesTrentoP.ID.getAttributeName())).intValue(), f -> f));

        STRtree junctionsTree = FeatureUtilities.featureCollectionToSTRtree(junctions);

        for( SimpleFeature pipe : pipesList ) {
            int pipeId = ((Number) pipe.getAttribute(PipesTrentoP.ID.getAttributeName())).intValue();

            SimpleFeature areaF = idToAreasMap.get(pipeId);

            Geometry pipeGeometry = (Geometry) pipe.getDefaultGeometry();
            Coordinate[] coordinates = pipeGeometry.getGeometryN(0).getCoordinates();
            Envelope env1 = new Envelope(coordinates[0]);
            env1.expandBy(0.1);
            Envelope env2 = new Envelope(coordinates[coordinates.length - 1]);
            env2.expandBy(0.1);

            SimpleFeature junction1F = getJunction(junctionsTree, pipeId, env1);
            SimpleFeature junction2F = getJunction(junctionsTree, pipeId, env2);

            PipeCombo combo = new PipeCombo(pipe, areaF, junction1F, junction2F);
            comboList.add(combo);
        }

        return comboList;
    }

    private static SimpleFeature getJunction( STRtree junctionsTree, int pipeId, Envelope env ) throws ModelsIOException {
        List resList = junctionsTree.query(env);
        if (resList.size() != 1) {
            throw new ModelsIOException("Could not identify 1 junction for pipe " + pipeId + ". Check your data.", "PipeCombo");
        }
        SimpleFeature junctionF = (SimpleFeature) resList.get(0);
        return junctionF;
    }

}
