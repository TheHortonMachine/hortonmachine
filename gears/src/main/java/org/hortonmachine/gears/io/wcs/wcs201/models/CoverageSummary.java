package org.hortonmachine.gears.io.wcs.wcs201.models;

import java.util.ArrayList;
import java.util.List;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.hortonmachine.gears.io.wcs.WcsUtils;
import org.hortonmachine.gears.io.wcs.XmlHelper;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.w3c.dom.Node;

public class CoverageSummary implements XmlHelper.XmlVisitor {
    /**
     * List of coverage summaries to be filled by the visitor.
     */
    private List<CoverageSummary> coverageSummaries;

    public String title;
    public String abstract_;
    public List<String> keywords = new ArrayList<>();
    public String coverageId;
    public String coverageSubtype;
    public ReferencedEnvelope boundingBox;
    public ReferencedEnvelope wgs84BoundingBox;

    public CoverageSummary(List<CoverageSummary> coverageSummaries) {
        this.coverageSummaries = coverageSummaries;
    }

    public boolean checkElementName(String name) {
        if (name.equals("wcs:CoverageSummary") || name.endsWith(":CoverageSummary"))
            return true;
        return false;
    }

    public void visit(Node node) {
        CoverageSummary cs = new CoverageSummary(null);
        cs.title = XmlHelper.findFirstTextInChildren(node, "title");
        cs.abstract_ = XmlHelper.findFirstTextInChildren(node, "abstract");

        Node keywordsNode = XmlHelper.findNode(node, "keywords");
        cs.keywords = XmlHelper.findAllTextsInChildren(keywordsNode, "keyword");
        cs.coverageId = XmlHelper.findFirstTextInChildren(node, "coverageid");
        cs.coverageSubtype = XmlHelper.findFirstTextInChildren(node, "coveragesubtype");

        List<Node> bboxNodes = new ArrayList<>();
        XmlHelper.findNodes(node, "boundingbox", bboxNodes);

        if (!bboxNodes.isEmpty()) {
            for (Node bboxNode : bboxNodes) {
                String nodeName = bboxNode.getNodeName();

                if (nodeName.toLowerCase().contains("wgs84")) {
                    String lowerCorner = XmlHelper.findFirstTextInChildren(bboxNode, "lowercorner");
                    String upperCorner = XmlHelper.findFirstTextInChildren(bboxNode, "uppercorner");
                    cs.wgs84BoundingBox = new ReferencedEnvelope(
                            Double.parseDouble(lowerCorner.split(" ")[0]),
                            Double.parseDouble(upperCorner.split(" ")[0]),
                            Double.parseDouble(lowerCorner.split(" ")[1]),
                            Double.parseDouble(upperCorner.split(" ")[1]),
                            DefaultGeographicCRS.WGS84);
                } else {
                    String crsStr = XmlHelper.findAttribute(bboxNode, "crs");

                    CoordinateReferenceSystem crs = WcsUtils.getCrsFromSrsName(crsStr);

                    String lowerCorner = XmlHelper.findFirstTextInChildren(bboxNode, "lowercorner");
                    String upperCorner = XmlHelper.findFirstTextInChildren(bboxNode, "uppercorner");
                    cs.boundingBox = new ReferencedEnvelope(
                            Double.parseDouble(lowerCorner.split(" ")[0]),
                            Double.parseDouble(upperCorner.split(" ")[0]),
                            Double.parseDouble(lowerCorner.split(" ")[1]),
                            Double.parseDouble(upperCorner.split(" ")[1]),
                            crs);
                }
            }

        }

        // if (bboxNode != null) {

        //     String crsStr = XmlHelper.findAttribute(bboxNode, "crs");

        //     CoordinateReferenceSystem crs = WcsUtils.getCrsFromSrsName(crsStr);

        //     String lowerCorner = XmlHelper.findFirstTextInChildren(bboxNode, "lowercorner");
        //     String upperCorner = XmlHelper.findFirstTextInChildren(bboxNode, "uppercorner");
        //     cs.boundingBox = new ReferencedEnvelope(
        //             Double.parseDouble(lowerCorner.split(" ")[0]),
        //             Double.parseDouble(upperCorner.split(" ")[0]),
        //             Double.parseDouble(lowerCorner.split(" ")[1]),
        //             Double.parseDouble(upperCorner.split(" ")[1]),
        //             crs);
        // }
        // // now the same with wgs84BoundingBox
        // Node wgs84BboxNode = XmlHelper.findNode(node, "wgs84boundingbox");
        // if (wgs84BboxNode != null) {
        //     String lowerCorner = XmlHelper.findFirstTextInChildren(wgs84BboxNode, "lowercorner");
        //     String upperCorner = XmlHelper.findFirstTextInChildren(wgs84BboxNode, "uppercorner");
        //     cs.wgs84BoundingBox = new ReferencedEnvelope(
        //             Double.parseDouble(lowerCorner.split(" ")[0]),
        //             Double.parseDouble(upperCorner.split(" ")[0]),
        //             Double.parseDouble(lowerCorner.split(" ")[1]),
        //             Double.parseDouble(upperCorner.split(" ")[1]),
        //             DefaultGeographicCRS.WGS84);
        // }

        coverageSummaries.add(cs);
    }

    public String toString() {
        String s = "";
        s += "title: " + title + "\n";
        s += "\tabstract: " + abstract_ + "\n";
        s += "\tkeywords: \n";
        if (keywords != null && keywords.size() > 0)
            for (String k : keywords) {
                s += "\t\t" + k + "\n";
            }
        s += "\tcoverageId: " + coverageId + "\n";
        s += "\tcoverageSubtype: " + coverageSubtype + "\n";
        s += "\tboundingBox: " + boundingBox + "\n";
        s += "\twgs84BoundingBox: " + wgs84BoundingBox + "\n";
        return s;
    }

    // Element: wcs:CoverageSummary
    // Element: ows:Title
    // Text: Vektorgrundkarte 2007 1:10.000 - Carta Tecnica vettoriale 2007 1:10.000
    // Element: ows:Abstract
    // Text: Vektorgrundkarte 2007 1:10.000 - Carta Tecnica vettoriale 2007 1:10.000
    // Element: ows:Keywords
    // Element: ows:Keyword
    // Text: TopographicMap-2007-10k
    // Element: ows:Keyword
    // Text: WCS
    // Element: ows:Keyword
    // Text: GeoTIFF
    // Element: ows:Keyword
    // Text: Vektorgrundkarte
    // Element: ows:Keyword
    // Text: Carta Tecnica vettoriale
    // Element: ows:Keyword
    // Text: 1:10.000
    // Element: ows:Keyword
    // Text: 2007
    // Element: wcs:CoverageId
    // Text: p_bz-BasemapImagery__TopographicMap-2007-10k
    // Element: wcs:CoverageSubtype
    // Text: RectifiedGridCoverage
    // Element: ows:BoundingBox
    // Attribute: crs = http://www.opengis.net/def/crs/EPSG/0/EPSG:25832
    // Element: ows:LowerCorner
    // Text: 604999.50020424 5120245.056555561
    // Element: ows:UpperCorner
    // Text: 767093.60580424 5220800.49975556
    // Element: ows:WGS84BoundingBox
    // Element: ows:LowerCorner
    // Text: 10.361617325937896 46.1833646051653
    // Element: ows:UpperCorner
    // Text: 12.519057271973722 47.13233858874859
}
