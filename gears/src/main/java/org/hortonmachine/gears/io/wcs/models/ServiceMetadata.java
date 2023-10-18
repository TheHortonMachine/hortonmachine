package org.hortonmachine.gears.io.wcs.models;

import java.util.List;

import org.hortonmachine.gears.io.wcs.XmlHelper;
import org.hortonmachine.gears.io.wcs.readers.WcsUtils;
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

        Node crsMetadataNode = XmlHelper.findNode(node, "CrsMetadata");
        List<String> supportedCrss = XmlHelper.findAllTextsInChildren(crsMetadataNode, "crsSupported");
        if (!supportedCrss.isEmpty()) {
            supportedSrids = new int[supportedCrss.size()];
            for (int i = 0; i < supportedCrss.size(); i++) {
                supportedSrids[i] = WcsUtils.getSridFromSrsName(supportedCrss.get(i));
            }
        }
        
        Node extensionNode = XmlHelper.findNode(node, "Extension");
        supportedInterpolations = XmlHelper.findAllTextsInChildren(extensionNode, "interpolationSupported");
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
