package org.hortonmachine.hmachine.modules.networktools.trento_p.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.hortonmachine.gears.libs.exceptions.ModelsIOException;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.hmachine.modules.networktools.trento_p.utils.TrentoPFeatureType.PipesTrentoP;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.index.strtree.STRtree;
import org.opengis.feature.simple.SimpleFeature;

public class PipeCombo {
    private SimpleFeature pipeFeature;

    private double area;
    private double initialJunctionElev; // higher
    private double initialJunctionDepth;
    private double finalJunctionElev;
    private double finalJunctionDepth;

    public PipeCombo( SimpleFeature pipeFeature, SimpleFeature areaFeature, SimpleFeature junction1Feature,
            SimpleFeature junction2Feature ) {
        this.pipeFeature = pipeFeature;

        Geometry areaGeom = (Geometry) areaFeature.getDefaultGeometry();
        area = areaGeom.getArea() / 10000.0;

        double junction1Elev = ((Number) junction1Feature
                .getAttribute(TrentoPFeatureType.JunctionsTrentoP.ELEVATION.getAttributeName())).doubleValue();
        double junction1Depth = ((Number) junction1Feature
                .getAttribute(TrentoPFeatureType.JunctionsTrentoP.DEPTH.getAttributeName())).doubleValue();
        double junction2Elev = ((Number) junction2Feature
                .getAttribute(TrentoPFeatureType.JunctionsTrentoP.ELEVATION.getAttributeName())).doubleValue();
        double junction2Depth = ((Number) junction2Feature
                .getAttribute(TrentoPFeatureType.JunctionsTrentoP.DEPTH.getAttributeName())).doubleValue();

        if (junction1Elev > junction2Elev) {
            initialJunctionElev = junction1Elev;
            initialJunctionDepth = junction1Depth;
            finalJunctionElev = junction2Elev;
            finalJunctionDepth = junction2Depth;
        } else {
            initialJunctionElev = junction2Elev;
            initialJunctionDepth = junction2Depth;
            finalJunctionElev = junction1Elev;
            finalJunctionDepth = junction1Depth;
        }
    }

    public double getArea() {
        return area;
    }

    public double getInitialJunctionElev() {
        return initialJunctionElev;
    }

    public double getInitialJunctionDepth() {
        return initialJunctionDepth;
    }

    public double getFinalJunctionElev() {
        return finalJunctionElev;
    }

    public double getFinalJunctionDepth() {
        return finalJunctionDepth;
    }

    public SimpleFeature getPipeFeature() {
        return pipeFeature;
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
