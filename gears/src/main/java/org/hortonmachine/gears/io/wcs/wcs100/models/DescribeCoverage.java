package org.hortonmachine.gears.io.wcs.wcs100.models;

import java.util.ArrayList;
import java.util.List;

import org.hortonmachine.gears.io.wcs.IDescribeCoverage;
import org.hortonmachine.gears.io.wcs.XmlHelper;
import org.locationtech.jts.geom.Envelope;
import org.w3c.dom.Node;

public class DescribeCoverage implements IDescribeCoverage {
    public Envelope envelope;
    public Integer envelopeSrid;



    @Override
    public boolean checkElementName(String name) {
        if (name.equals("wcs:CoverageDescription") || name.endsWith(":CoverageDescription"))
            return true;
        return false;
    }

    @Override
    public void visit(Node node) {
        Node envNode = XmlHelper.findNodeInTree(node, "domainSet", "spatialDomain", "Envelope");
        if (envNode !=null) {
            String srsName = XmlHelper.findAttribute(envNode, "srsName");
            
            if (srsName != null) {
                srsName = srsName.split(":")[1];
                envelopeSrid = Integer.parseInt(srsName);
            }
            List<Node> posNodes = new ArrayList<>();
            XmlHelper.findNodes(envNode, "pos", posNodes);

            String lowerCorner = null;
            String upperCorner = null;
            for (Node posNode : posNodes) {
                if (lowerCorner == null) {
                    lowerCorner = posNode.getTextContent();
                } else if (upperCorner == null) {
                    upperCorner = posNode.getTextContent();
                }
            }

            if (lowerCorner != null && upperCorner != null) {
                envelope = new Envelope(
                        Double.parseDouble(lowerCorner.split(" ")[0]),
                        Double.parseDouble(upperCorner.split(" ")[0]),
                        Double.parseDouble(lowerCorner.split(" ")[1]),
                        Double.parseDouble(upperCorner.split(" ")[1]));
            }
        }



    }

    public String toString() {
        String s = "";
        s += "envelope: " + envelope + "\n";
        s += "envelopeSrid: " + envelopeSrid + "\n";
        return s;
    }

    @Override
    public Envelope getCoverageEnvelope() {
        return envelope;
    }

    @Override
    public Integer getCoverageEnvelopeSrid() {
        return envelopeSrid;
    }

    @Override
    public String[] getAxisLabels() {
        return null;
    }

    @Override
    public String[] getGridAxisLabels() {
        return null;
    }

}
