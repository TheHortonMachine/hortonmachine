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
package org.jgrasstools.hortonmachine.modules.hydrogeomorphology.adige.core;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.jgrasstools.gears.libs.exceptions.ModelsIllegalargumentException;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class NetBasinsManager {

    private List<SimpleFeature> hillslopeFeaturesList;
    private List<Long> hillslopeIdsList;

    /**
     * @param netFeatureCollection the network features
     * @param hillslopeFeatureCollection the hillslope features
     * @param netnumAttr the attribute name of the field connecting the two layers
     * @param pfafAttr the net pfafstetter field name
     * @param startelevAttr the field name of the start elevation of the net (can be null)
     * @param endelevAttr the field name of the end elevation of the net (can be null)
     * @param baricenterAttr the field holding the baricenter of the hasin elevation (can be null)
     * @param vegetationAttributeName the field holding the vegetation id of the hasin (can be null)
     * @param pSatconst 
     * @param pEtrate 
     * @param pPorosity 
     * @param pSpecyield 
     * @param pMstexp 
     * @param pKs 
     * @param pDepthmnsat 
     * @param out a printstream for logging
     * @return the list of ordered hillslopes, starting from the most downstream one
     * @throws Exception
     */
    public List<HillSlope> operateOnLayers( FeatureCollection<SimpleFeatureType, SimpleFeature> netFeatureCollection,
            FeatureCollection<SimpleFeatureType, SimpleFeature> hillslopeFeatureCollection, String netnumAttr, String pfafAttr,
            String startelevAttr, String endelevAttr, String baricenterAttr, String vegetationAttributeName, double pKs,
            double pMstexp, double pSpecyield, double pPorosity, Double pEtrate, double pSatconst, double pDepthmnsat,
            IJGTProgressMonitor out ) throws Exception {

        SimpleFeatureType fT = netFeatureCollection.getSchema();
        // netnum attribute
        int lAttrIndex = fT.indexOf(netnumAttr);
        if (lAttrIndex == -1) {
            String pattern = "Attribute {0} not found in layer {1}.";
            Object[] args = new Object[]{netnumAttr, fT.getTypeName()};
            String newPattern = MessageFormat.format(pattern, args);
            throw new ModelsIllegalargumentException(newPattern, this);
        }
        // pfafstetter attribute
        int pAttrIndex = fT.indexOf(pfafAttr);
        if (pAttrIndex == -1) {
            String pattern = "Attribute {0} not found in layer {1}.";
            Object[] args = new Object[]{pfafAttr, fT.getTypeName()};
            String newPattern = MessageFormat.format(pattern, args);
            throw new ModelsIllegalargumentException(newPattern, this);
        }
        // net start elevation attribute
        int startNetElevAttrIndex = -1;
        if (startelevAttr != null) {
            startNetElevAttrIndex = fT.indexOf(startelevAttr);
            if (startNetElevAttrIndex == -1) {
                String pattern = "Attribute {0} not found in layer {1}.";
                Object[] args = new Object[]{startelevAttr, fT.getTypeName()};
                String newPattern = MessageFormat.format(pattern, args);
                throw new ModelsIllegalargumentException(newPattern, this.getClass().getSimpleName());
            }
        }
        // net end elevation attribute
        int endNetElevAttrIndex = -1;
        if (endelevAttr != null) {
            endNetElevAttrIndex = fT.indexOf(endelevAttr);
            if (endNetElevAttrIndex == -1) {
                String pattern = "Attribute {0} not found in layer {1}.";
                Object[] args = new Object[]{endelevAttr, fT.getTypeName()};
                String newPattern = MessageFormat.format(pattern, args);
                throw new ModelsIllegalargumentException(newPattern, this.getClass().getSimpleName());
            }
        }

        out.message("Analizing the network layer...");
        List<SimpleFeature> netFeaturesList = new ArrayList<SimpleFeature>();
        List<Long> netIdsList = new ArrayList<Long>();
        ArrayList<PfafstetterNumber> netPfaffsList = new ArrayList<PfafstetterNumber>();
        FeatureIterator<SimpleFeature> featureIterator = netFeatureCollection.features();
        PfafstetterNumber mostDownStreamPNumber = null;
        SimpleFeature mostDownStreamNetFeature = null;
        Long mostDownStreamLinkId = -1l;
        while( featureIterator.hasNext() ) {
            SimpleFeature f = (SimpleFeature) featureIterator.next();
            String attribute = (String) f.getAttribute(pAttrIndex);
            PfafstetterNumber current = new PfafstetterNumber(attribute);
            Long tmpId = ((Number) f.getAttribute(lAttrIndex)).longValue();
            if (mostDownStreamPNumber == null) {
                mostDownStreamPNumber = current;
            } else {
                if (current.isDownStreamOf(mostDownStreamPNumber)) {
                    mostDownStreamLinkId = tmpId;
                    mostDownStreamNetFeature = f;
                    mostDownStreamPNumber = current;
                }
            }
            netFeaturesList.add(f);
            netIdsList.add(tmpId);
            netPfaffsList.add(current);
        }
        featureIterator.close();

        /*
         * search subbasins
         */
        out.message("Analyzing the hillslopes layer...");
        SimpleFeatureType ft = hillslopeFeatureCollection.getSchema();
        // netnum attribute on basins
        int linkAttrIndexInBasinLayerIndex = ft.indexOf(netnumAttr);
        if (linkAttrIndexInBasinLayerIndex == -1) {
            String pattern = "Attribute {0} not found in layer {1}.";
            Object[] args = new Object[]{netnumAttr, ft.getTypeName()};
            pattern = MessageFormat.format(pattern, args);
            throw new ModelsIllegalargumentException(pattern, this);
        }

        // baricenter attribute
        int baricenterAttributeIndex = -1;
        if (baricenterAttr != null) {
            baricenterAttributeIndex = ft.indexOf(baricenterAttr);
            if (baricenterAttributeIndex == -1) {
                String pattern = "Attribute {0} not found in layer {1}.";
                Object[] args = new Object[]{baricenterAttr, ft.getTypeName()};
                pattern = MessageFormat.format(pattern, args);
                throw new ModelsIllegalargumentException(pattern, this);
            }
        }
        int vegetationAttributeIndex = -1;
        if (vegetationAttributeName != null) {
            vegetationAttributeIndex = ft.indexOf(vegetationAttributeName);
            if (vegetationAttributeIndex == -1) {
                String pattern = "Attribute {0} not found in layer {1}.";
                Object[] args = new Object[]{vegetationAttributeName, ft.getTypeName()};
                pattern = MessageFormat.format(pattern, args);
                throw new ModelsIllegalargumentException(pattern, this);
            }
        }

        hillslopeFeaturesList = new ArrayList<SimpleFeature>();
        hillslopeIdsList = new ArrayList<Long>();
        FeatureIterator<SimpleFeature> hillslopeIterator = hillslopeFeatureCollection.features();
        SimpleFeature mostDownstreamHillslopeFeature = null;
        while( hillslopeIterator.hasNext() ) {
            SimpleFeature f = hillslopeIterator.next();
            Long linkAttribute = ((Number) f.getAttribute(linkAttrIndexInBasinLayerIndex)).longValue();
            if (mostDownStreamLinkId == linkAttribute) {
                mostDownstreamHillslopeFeature = f;
            }
            hillslopeIdsList.add(linkAttribute);
            hillslopeFeaturesList.add(f);
        }
        /*
         * create all the hillslopes and connect them with their net feature and other hillslopes
         */
        out.message("Linking together network and hillslopes layers...");
        ArrayList<HillSlope> hillslopeElements = new ArrayList<HillSlope>();
        HillSlope mostDownstreamHillslope = null;
        if (mostDownStreamPNumber.isEndPiece()) {
            HillSlope tmpHslp = new HillSlope(mostDownStreamNetFeature, mostDownstreamHillslopeFeature, mostDownStreamPNumber,
                    hillslopeIdsList.get(0).intValue(), baricenterAttributeIndex, startNetElevAttrIndex, endNetElevAttrIndex,
                    vegetationAttributeIndex, pKs, pMstexp, pSpecyield, pPorosity, pEtrate, pSatconst, pDepthmnsat);
            hillslopeElements.add(tmpHslp);
            mostDownstreamHillslope = tmpHslp;
        } else {
            /*
             * almost there, now get from the basins list the ones with that netNums
             */
            ArrayList<SimpleFeature> selectedNetFeatureList = new ArrayList<SimpleFeature>();
            ArrayList<Long> selectedNetId = new ArrayList<Long>();
            for( int i = 0; i < hillslopeFeaturesList.size(); i++ ) {
                SimpleFeature basinFeature = hillslopeFeaturesList.get(i);
                Long link = hillslopeIdsList.get(i);
                for( int j = 0; j < netFeaturesList.size(); j++ ) {
                    Long netNum = netIdsList.get(j);
                    if (netNum.equals(link)) {
                        SimpleFeature netFeature = netFeaturesList.get(j);
                        HillSlope tmpHslp = new HillSlope(netFeature, basinFeature, netPfaffsList.get(j), netNum.intValue(),
                                baricenterAttributeIndex, startNetElevAttrIndex, endNetElevAttrIndex, vegetationAttributeIndex,
                                pKs, pMstexp, pSpecyield, pPorosity, pEtrate, pSatconst, pDepthmnsat);
                        hillslopeElements.add(tmpHslp);
                        selectedNetFeatureList.add(netFeature);
                        selectedNetId.add(netNum);
                        break;
                    }
                }
            }

            mostDownStreamPNumber = null;
            Long mostDownStreamNetId = null;
            for( SimpleFeature feature : selectedNetFeatureList ) {
                String attribute = (String) feature.getAttribute(pAttrIndex);
                PfafstetterNumber current = new PfafstetterNumber(attribute);
                Long tmpId = ((Number) feature.getAttribute(lAttrIndex)).longValue();
                if (mostDownStreamPNumber == null) {
                    mostDownStreamPNumber = current;
                } else {
                    if (current.isDownStreamOf(mostDownStreamPNumber)) {
                        mostDownStreamNetId = tmpId;
                        mostDownStreamPNumber = current;
                    }
                }
            }

            for( int i = 0; i < hillslopeElements.size(); i++ ) {
                Long hId = hillslopeIdsList.get(i);
                if (hId.equals(mostDownStreamNetId)) {
                    mostDownstreamHillslope = hillslopeElements.get(i);
                    break;
                }
            }

        }

        if (mostDownstreamHillslope == null)
            throw new RuntimeException();
        HillSlope.connectElements(hillslopeElements);

        List<HillSlope> orderedHillslopes = new ArrayList<HillSlope>();
        mostDownstreamHillslope.getAllUpstreamElements(orderedHillslopes, null);

        return orderedHillslopes;

    }
}
