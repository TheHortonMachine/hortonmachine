package org.hortonmachine.gears.io.wcs.wcs201.models;

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
                getCapabilitiesUrl = removeTrailingRubbish(getCapabilitiesUrl);
            } else if (name.equals("DescribeCoverage")) {
                Node getNode = XmlHelper.findNode(operationNode, "Get");
                describeCoverageUrl = XmlHelper.findAttribute(getNode, "xlink:href");
                describeCoverageUrl = removeTrailingRubbish(describeCoverageUrl);
            } else if (name.equals("GetCoverage")) {
                Node getNode = XmlHelper.findNode(operationNode, "Get");
                getCoverageUrl = XmlHelper.findAttribute(getNode, "xlink:href");
                getCoverageUrl = removeTrailingRubbish(getCoverageUrl);
            }
        }
    }

    private String removeTrailingRubbish(String url) {
        if (url != null && url.endsWith("?")) {
            url = url.substring(0, url.length() - 1);
        }
        if (url != null && url.endsWith("&amp;")) {
            url = url.substring(0, url.length() - 5);
        }
        return url;
    }

    public String getGetCapabilitiesBaseUrl() {
        return getCapabilitiesUrl;
    }

    public String getDescribeCoverageBaseUrl() {
        return describeCoverageUrl;
    }

    public String getGetCoverageBaseUrl() {
        return getCoverageUrl;
    }

    public String toString() {
        return "GetCapabilities: " + getCapabilitiesUrl + "\n" +
                "DescribeCoverage: " + describeCoverageUrl + "\n" +
                "GetCoverage: " + getCoverageUrl;
    }

}
