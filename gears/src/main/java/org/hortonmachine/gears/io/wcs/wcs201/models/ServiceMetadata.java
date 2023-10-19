package org.hortonmachine.gears.io.wcs.wcs201.models;

import java.util.ArrayList;
import java.util.List;

import org.hortonmachine.gears.io.wcs.WcsUtils;
import org.hortonmachine.gears.io.wcs.XmlHelper;
import org.w3c.dom.Node;

public class ServiceMetadata implements XmlHelper.XmlVisitor {

    public List<String> supportedFormats;
    public int[] supportedSrids;
    public List<String> supportedInterpolations;

    @Override
    public boolean checkElementName(String name) {
        if (name.equals("wcs:ServiceMetadata") || name.endsWith(":ServiceMetadata"))
            return true;
        return false;
    }

    @Override
    public void visit(Node node) {
        supportedFormats = XmlHelper.findAllTextsInChildren(node, "formatSupported");

        List<Node> supportedCrsNodes = new ArrayList<>();
        XmlHelper.findNodes(node,  "crsSupported", supportedCrsNodes);
        if (!supportedCrsNodes.isEmpty()) {
            supportedSrids = new int[supportedCrsNodes.size()];
            for (int i = 0; i < supportedCrsNodes.size(); i++) {
                String textContent = supportedCrsNodes.get(i).getTextContent();
                supportedSrids[i] = WcsUtils.getSridFromSrsName(textContent);
            }
        }
        
        List<Node> interpolationSupportedNodes = new ArrayList<>();
        XmlHelper.findNodes(node,  "interpolationSupported", interpolationSupportedNodes);
        if (!interpolationSupportedNodes.isEmpty()) {
            supportedInterpolations = new ArrayList<>();
            for (int i = 0; i < interpolationSupportedNodes.size(); i++) {
                String textContent = interpolationSupportedNodes.get(i).getTextContent();
                supportedInterpolations.add(textContent);
            }
        }
    }

    public List<String> getSupportedFormats() {
        return supportedFormats;
    }

    public List<String> getSupportedInterpolations() {
        return supportedInterpolations;
    }

    public int[] getSupportedSrids() {
        return supportedSrids;
    }

    public boolean hasSrid(int srid){
        for (int i = 0; i < supportedSrids.length; i++) {
            if (supportedSrids[i] == srid) {
                return true;
            }
        }
        return false;
    }
    
    public boolean hasFormat(String format){
        for (String supportedFormat : supportedFormats) {
            if (supportedFormat.equals(format)) {
                return true;
            }
        }
        return false;
    }

    public String toString() {
        String s = "";
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
}
