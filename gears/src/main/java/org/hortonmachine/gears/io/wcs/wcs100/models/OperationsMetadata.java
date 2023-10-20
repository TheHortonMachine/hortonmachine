package org.hortonmachine.gears.io.wcs.wcs100.models;

import java.util.ArrayList;
import java.util.List;

import org.hortonmachine.gears.io.wcs.XmlHelper;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class OperationsMetadata implements XmlHelper.XmlVisitor {

    String getCapabilitiesUrl;
    String describeCoverageUrl;
    String getCoverageUrl;

    @Override
    public boolean checkElementName(String name) {
        if (name.equals("wcs:Capability") || name.endsWith(":Capability"))
            return true;
        return false;
    }

    @Override
    public void visit(Node node) {
        Node requestNode = XmlHelper.findNode(node, "Request");

        NodeList childNodes = requestNode.getChildNodes();
        // loop over childnodes
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node childNode = childNodes.item(i);
            String name = childNode.getNodeName().toLowerCase();
            if (name.endsWith(":getcapabilities")) {
                Node getNode = XmlHelper.findNode(childNode, "Get");
                if (getNode != null) {
                    Node onlineResNode = XmlHelper.findNode(getNode, "OnlineResource");
                    getCapabilitiesUrl = XmlHelper.findAttribute(onlineResNode, "xlink:href");
                    if (getCapabilitiesUrl != null && getCapabilitiesUrl.endsWith("?")) {
                        getCapabilitiesUrl = getCapabilitiesUrl.substring(0, getCapabilitiesUrl.length() - 1);
                    }
                }
            } else if (name.endsWith(":describecoverage")) {
                Node getNode = XmlHelper.findNode(childNode, "Get");
                if (getNode != null) {
                    Node onlineResNode = XmlHelper.findNode(getNode, "OnlineResource");
                    describeCoverageUrl = XmlHelper.findAttribute(onlineResNode, "xlink:href");
                    if (describeCoverageUrl != null && describeCoverageUrl.endsWith("?")) {
                        describeCoverageUrl = describeCoverageUrl.substring(0, describeCoverageUrl.length() - 1);
                    }
                }
            } else if (name.endsWith(":getcoverage")) {
                Node getNode = XmlHelper.findNode(childNode, "Get");
                if (getNode != null) {
                    Node onlineResNode = XmlHelper.findNode(getNode, "OnlineResource");
                    getCoverageUrl = XmlHelper.findAttribute(onlineResNode, "xlink:href");
                    if (getCoverageUrl != null && getCoverageUrl.endsWith("?")) {
                        getCoverageUrl = getCoverageUrl.substring(0, getCoverageUrl.length() - 1);
                    }
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
