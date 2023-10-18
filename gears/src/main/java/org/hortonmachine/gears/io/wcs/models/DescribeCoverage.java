package org.hortonmachine.gears.io.wcs.models;

import java.util.Arrays;
import java.util.List;

import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.Range;
import org.hortonmachine.gears.io.wcs.XmlHelper;
import org.hortonmachine.gears.io.wcs.readers.WcsUtils;
import org.locationtech.jts.geom.Coordinate;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.w3c.dom.Node;

public class DescribeCoverage implements XmlHelper.XmlVisitor {
    public ReferencedEnvelope envelope;
    public String[] axisLabels;
    public String[] uomLabels;
    public int srsDimension;

    public String rectifiedGridId;
    public int rectifiedGridDimension;
    public GridEnvelope2D gridEnvelope;
    public String[] gridAxisLabels;

    public Coordinate lowerLeftCellCenter;
    public String lowerLeftCellCenterId;
    public int lowerLeftCellCenterSrid;
    public double xRes;
    public double yRes;
    public double xRotation;
    public double yRotation;

    public double novalue;
    public String uom;
    public Range<Double> range;

    public String nativeFormat;

    @Override
    public boolean checkElementName(String name) {
        if (name.equals("wcs:CoverageDescription") || name.endsWith(":CoverageDescription"))
            return true;
        return false;
    }

    @Override
    public void visit(Node node) {
        String spaceRegex = "\\s+";

        Node bboxNode = XmlHelper.findNode(node, "boundedBy");
        Node envelopeNode = XmlHelper.findNode(bboxNode, "envelope");

        String srsName = XmlHelper.findAttribute(envelopeNode, "srsName");

        CoordinateReferenceSystem crs = WcsUtils.getCrsFromSrsName(srsName);
        String srsDimensionStr = XmlHelper.findAttribute(envelopeNode, "srsDimension");
        if (srsDimensionStr != null) {
            srsDimension = Integer.parseInt(srsDimensionStr);
        }
        String axisLabelsStr = XmlHelper.findAttribute(envelopeNode, "axisLabels");
        if (axisLabelsStr != null) {
            axisLabels = axisLabelsStr.split(spaceRegex);
        }
        String uomLabelsStr = XmlHelper.findAttribute(envelopeNode, "uomLabels");
        if (uomLabelsStr != null) {
            uomLabels = uomLabelsStr.split(spaceRegex);
        }

        String lowerCorner = XmlHelper.findFirstTextInChildren(envelopeNode, "lowerCorner");
        String upperCorner = XmlHelper.findFirstTextInChildren(envelopeNode, "upperCorner");
        String[] lowerCornerSplit = lowerCorner.split(spaceRegex);
        String[] upperCornerSplit = upperCorner.split(spaceRegex);
        envelope = new ReferencedEnvelope(
                Double.parseDouble(lowerCornerSplit[0]),
                Double.parseDouble(upperCornerSplit[0]),
                Double.parseDouble(lowerCornerSplit[1]),
                Double.parseDouble(upperCornerSplit[1]),
                crs);

        Node domainSetNode = XmlHelper.findNode(node, "domainSet");
        Node rectifiedGridNode = XmlHelper.findNode(domainSetNode, "RectifiedGrid");
        rectifiedGridId = XmlHelper.findAttribute(rectifiedGridNode, "id");
        String rectifiedGridDimensionStr = XmlHelper.findAttribute(rectifiedGridNode, "dimension");
        if (rectifiedGridDimensionStr != null) {
            rectifiedGridDimension = Integer.parseInt(rectifiedGridDimensionStr);
        }

        Node limitsNode = XmlHelper.findNode(rectifiedGridNode, "limits");
        Node gridEnvelopeNode = XmlHelper.findNode(limitsNode, "GridEnvelope");
        String gridEnvelopeLowStr = XmlHelper.findFirstTextInChildren(gridEnvelopeNode, "low");
        String gridEnvelopeHighStr = XmlHelper.findFirstTextInChildren(gridEnvelopeNode, "high");
        String[] gridEnvelopeLowSplit = gridEnvelopeLowStr.split(spaceRegex);
        String[] gridEnvelopeHighSplit = gridEnvelopeHighStr.split(spaceRegex);
        int x1 = Integer.parseInt(gridEnvelopeLowSplit[0]);
        int y1 = Integer.parseInt(gridEnvelopeLowSplit[1]);
        int x2 = Integer.parseInt(gridEnvelopeHighSplit[0]);
        int y2 = Integer.parseInt(gridEnvelopeHighSplit[1]);
        gridEnvelope = new GridEnvelope2D(x1, y1, x2 - x1, y2 - y1);

        axisLabelsStr = XmlHelper.findFirstTextInChildren(rectifiedGridNode, "axisLabels");
        if (axisLabelsStr != null) {
            gridAxisLabels = axisLabelsStr.split(spaceRegex);
        }

        Node originNode = XmlHelper.findNode(rectifiedGridNode, "origin");
        Node pointNode = XmlHelper.findNode(originNode, "Point");
        String lowerLeftCellCenterStr = XmlHelper.findFirstTextInChildren(pointNode, "pos");
        String[] lowerLeftCellCenterSplit = lowerLeftCellCenterStr.split(spaceRegex);
        lowerLeftCellCenter = new Coordinate(
                Double.parseDouble(lowerLeftCellCenterSplit[0]),
                Double.parseDouble(lowerLeftCellCenterSplit[1]));
        lowerLeftCellCenterId = XmlHelper.findAttribute(pointNode, "id");
        srsName = XmlHelper.findAttribute(pointNode, "srsName");
        Integer srid = WcsUtils.getSridFromSrsName(srsName);
        if (srid != null) {
            lowerLeftCellCenterSrid = srid;
        }

        List<String> offsetVectorNode = XmlHelper.findAllTextsInChildren(rectifiedGridNode, "offsetVector");
        String[] offsetVectorSplit = offsetVectorNode.get(0).split(spaceRegex);
        xRes = Double.parseDouble(offsetVectorSplit[0]);
        yRotation = Double.parseDouble(offsetVectorSplit[1]);
        offsetVectorSplit = offsetVectorNode.get(1).split(spaceRegex);
        xRotation = Double.parseDouble(offsetVectorSplit[0]);
        yRes = Math.abs(Double.parseDouble(offsetVectorSplit[1]));

        Node rangeTypeNode = XmlHelper.findNode(node, "rangeType");
        Node dataRecordNode = XmlHelper.findNode(rangeTypeNode, "DataRecord");
        Node fieldNode = XmlHelper.findNode(dataRecordNode, "field");
        Node nilValueNode = XmlHelper.findNode(dataRecordNode, "nilValue");
        String nilValueStr = nilValueNode.getFirstChild().getNodeValue();
        // String nilValueStr = XmlHelper.findFirstTextInChildren(nilValueNode, "nilValue");
        if (nilValueStr != null) {
            novalue = Double.parseDouble(nilValueStr);
        }
        Node uomNode = XmlHelper.findNode(fieldNode, "uom");
        uom = XmlHelper.findAttribute(uomNode, "code");

        Node allowedValuesNode = XmlHelper.findNode(fieldNode, "AllowedValues");
        String rangeStr = XmlHelper.findFirstTextInChildren(allowedValuesNode, "interval");
        String[] rangeSplit = rangeStr.split(spaceRegex);
        range = new Range<Double>(Double.class, Double.parseDouble(rangeSplit[0]), Double.parseDouble(rangeSplit[1]));

        Node serviceParametersNode = XmlHelper.findNode(node, "ServiceParameters");
        nativeFormat = XmlHelper.findFirstTextInChildren(serviceParametersNode, "nativeFormat");
    }

    public String toString(){
        String s = "";
        s += "envelope: " + envelope + "\n";
        s += "axisLabels: " + Arrays.toString(axisLabels) + "\n";
        s += "uomLabels: " + Arrays.toString(uomLabels) + "\n";
        s += "srsDimension: " + srsDimension + "\n";
        s += "rectifiedGridId: " + rectifiedGridId + "\n";
        s += "rectifiedGridDimension: " + rectifiedGridDimension + "\n";
        s += "gridEnvelope: " + gridEnvelope + "\n";
        s += "gridAxisLabels: " + Arrays.toString(gridAxisLabels) + "\n";
        s += "lowerLeftCellCenter: " + lowerLeftCellCenter + "\n";
        s += "lowerLeftCellCenterId: " + lowerLeftCellCenterId + "\n";
        s += "lowerLeftCellCenterSrid: " + lowerLeftCellCenterSrid + "\n";
        s += "xRes: " + xRes + "\n";
        s += "yRes: " + yRes + "\n";
        s += "xRotation: " + xRotation + "\n";
        s += "yRotation: " + yRotation + "\n";
        s += "novalue: " + novalue + "\n";
        s += "uom: " + uom + "\n";
        s += "range: " + range + "\n";
        s += "nativeFormat: " + nativeFormat + "\n";
        return s;
    }

}
