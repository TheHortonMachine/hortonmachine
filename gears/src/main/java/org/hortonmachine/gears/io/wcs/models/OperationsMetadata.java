package org.hortonmachine.gears.io.wcs.models;

import java.util.ArrayList;
import java.util.List;

import org.hortonmachine.gears.io.wcs.XmlHelper;
import org.w3c.dom.Node;

public class OperationsMetadata implements XmlHelper.XmlVisitor {

    String getCapabilitiesUrl;
    String describeCoverageUrl;
    String getCoverageUrl;

    @Override
    public boolean checkElementName(String name) {
        if (name.equals("ows:OperationsMetadata") || name.endsWith(":OperationsMetadata"))
            return true;
        return false;
    }

    @Override
    public void visit(Node node) {
        List<Node> operationsNodes = new ArrayList<>();
        XmlHelper.findNodes(node, "Operation", operationsNodes);

        for (Node operationNode : operationsNodes) {
            String name = XmlHelper.findAttribute(operationNode, "name");
            if (name.equals("GetCapabilities")) {
                Node getNode = XmlHelper.findNode(operationNode, "Get");
                getCapabilitiesUrl = XmlHelper.findAttribute(getNode, "xlink:href");
                if (getCapabilitiesUrl != null && getCapabilitiesUrl.endsWith("?")) {
                    getCapabilitiesUrl = getCapabilitiesUrl.substring(0, getCapabilitiesUrl.length() - 1);
                }
            } else if (name.equals("DescribeCoverage")) {
                Node getNode = XmlHelper.findNode(operationNode, "Get");
                describeCoverageUrl = XmlHelper.findAttribute(getNode, "xlink:href");
                if (describeCoverageUrl != null && describeCoverageUrl.endsWith("?")) {
                    describeCoverageUrl = describeCoverageUrl.substring(0, describeCoverageUrl.length() - 1);
                }
            } else if (name.equals("GetCoverage")) {
                Node getNode = XmlHelper.findNode(operationNode, "Get");
                getCoverageUrl = XmlHelper.findAttribute(getNode, "xlink:href");
                if (getCoverageUrl != null && getCoverageUrl.endsWith("?")) {
                    getCoverageUrl = getCoverageUrl.substring(0, getCoverageUrl.length() - 1);
                }
            }
        }
    }

    public String getGetCapabilitiesUrl() {
        return getCapabilitiesUrl;
    }

    public String getDescribeCoverageUrl() {
        return describeCoverageUrl;
    }

    public String getGetCoverageUrl() {
        return getCoverageUrl;
    }

    public String toString() {
        return "GetCapabilities: " + getCapabilitiesUrl + "\n" +
                "DescribeCoverage: " + describeCoverageUrl + "\n" +
                "GetCoverage: " + getCoverageUrl;
    }

}
