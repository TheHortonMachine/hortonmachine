package org.hortonmachine.gears.io.wcs.wcs201.models;

import java.util.ArrayList;
import java.util.List;

import org.hortonmachine.gears.io.wcs.XmlHelper;
import org.w3c.dom.Node;

public class Identification implements XmlHelper.XmlVisitor {
    String title;
    String abstract_;
    List<String> keywords = new ArrayList<>();

    String serviceType;
    List<String> supportedVersions = new ArrayList<>();

    List<String> profiles = new ArrayList<>();

    String fees;
    String accessConstraints;

    @Override
    public boolean checkElementName(String name) {
        if (name.equals("ows:ServiceIdentification") || name.endsWith(":ServiceIdentification"))
            return true;
        return false;
    }

    @Override
    public void visit(Node node) {
        title = XmlHelper.findFirstTextInChildren(node, "title");
        abstract_ = XmlHelper.findFirstTextInChildren(node, "abstract");

        Node keywordsNode = XmlHelper.findNode(node, "keywords");
        if (keywordsNode != null)
            keywords = XmlHelper.findAllTextsInChildren(keywordsNode, "keyword");

        serviceType = XmlHelper.findFirstTextInChildren(node, "servicetype");
        supportedVersions = XmlHelper.findAllTextsInChildren(node, "servicetypeversion");

        profiles = XmlHelper.findAllTextsInChildren(node, "profile");

        fees = XmlHelper.findFirstTextInChildren(node, "fees");
        accessConstraints = XmlHelper.findFirstTextInChildren(node, "accessconstraints");
    }

    public boolean supportsScaling(){
        return profiles.stream().anyMatch(p -> p.toLowerCase().contains("scaling") && p.toLowerCase().contains("wcs_service-extension"));
    }

    public String toString(){
        String s = "";
        s += "title: " + title + "\n";
        s += "abstract: " + abstract_ + "\n";
        s += "keywords: \n";
        if (keywords != null && keywords.size() > 0)
            for (String keyword : keywords) {
                s += "\t\t" + keyword + "\n";
            }
        s += "serviceType: " + serviceType + "\n";
        s += "supportedVersions: \n";
        if (supportedVersions != null && supportedVersions.size() > 0)
            for (String sv : supportedVersions) {
                s += "\t\t" + sv + "\n";
            }
        s += "profiles: \n";
        if (profiles != null && profiles.size() > 0)
            for (String p : profiles) {
                s += "\t\t" + p + "\n";
            }
        s += "fees: " + fees + "\n";
        s += "accessConstraints: " + accessConstraints + "\n";

        return s;

    }
    
}
