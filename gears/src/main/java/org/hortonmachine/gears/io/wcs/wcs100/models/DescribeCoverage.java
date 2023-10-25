package org.hortonmachine.gears.io.wcs.wcs100.models;

import java.util.ArrayList;
import java.util.List;

import org.hortonmachine.gears.io.wcs.IDescribeCoverage;
import org.hortonmachine.gears.io.wcs.WcsUtils;
import org.hortonmachine.gears.io.wcs.XmlHelper;
import org.locationtech.jts.geom.Envelope;
import org.w3c.dom.Node;

public class DescribeCoverage implements IDescribeCoverage {
    public Envelope envelope;
    public Integer envelopeSrid;

    public List<String> supportedFormats;
    public String nativeFormat;
    public int[] supportedSrids;
    public List<String> supportedInterpolations;

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

        Node formatNode = XmlHelper.findNode(node, "supportedFormats");
        supportedFormats = XmlHelper.findAllTextsInChildren(formatNode, "formats");
        
        
        Node crsNode = XmlHelper.findNode(node, "supportedCRSs");
        List<Node> supportedCrsNodes = new ArrayList<>();
        XmlHelper.findNodes(crsNode,  "requestResponseCRSs", supportedCrsNodes);
        if (!supportedCrsNodes.isEmpty()) {
            supportedSrids = new int[supportedCrsNodes.size()];
            for (int i = 0; i < supportedCrsNodes.size(); i++) {
                String textContent = supportedCrsNodes.get(i).getTextContent();
                supportedSrids[i] = WcsUtils.getSridFromSrsName(textContent);
            }
        }
        
        Node interpolationsNode = XmlHelper.findNode(node, "supportedInterpolations");
        List<Node> interpolationSupportedNodes = new ArrayList<>();
        XmlHelper.findNodes(interpolationsNode,  "interpolationMethod", interpolationSupportedNodes);
        if (!interpolationSupportedNodes.isEmpty()) {
            supportedInterpolations = new ArrayList<>();
            for (int i = 0; i < interpolationSupportedNodes.size(); i++) {
                String textContent = interpolationSupportedNodes.get(i).getTextContent();
                supportedInterpolations.add(textContent);
            }
        }

    }

    public String toString() {
        String s = "";
        s += "envelope: " + envelope + "\n";
        s += "envelopeSrid: " + envelopeSrid + "\n";
        s += "supportedFormats: \n";
        if (supportedFormats != null && supportedFormats.size() > 0)
            for (String sf : supportedFormats) {
                s += "\t" + sf + "\n";
            }
        s += "supportedSrids: \n";
        if (supportedSrids != null && supportedSrids.length > 0)
            for (int srid : supportedSrids) {
                s += "\t" + srid + "\n";
            }
        s += "supportedInterpolations: \n";
        if (supportedInterpolations != null && supportedInterpolations.size() > 0)
            for (String si : supportedInterpolations) {
                s += "\t" + si + "\n";
            }
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
    public String[] getWorldAxisLabels() {
        return null;
    }

    @Override
    public String[] getGridAxisLabels() {
        return null;
    }

    @Override
    public List<String> getSupportedFormats() throws Exception {
        return supportedFormats;
    }

    @Override
    public String getNativeFormat() throws Exception {
        return nativeFormat;
    }

    @Override
    public int[] getSupportedSrids() throws Exception {
        return supportedSrids;
    }

}
