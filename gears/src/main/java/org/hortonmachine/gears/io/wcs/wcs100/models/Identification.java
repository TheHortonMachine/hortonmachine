package org.hortonmachine.gears.io.wcs.wcs100.models;

import java.util.ArrayList;
import java.util.List;

import org.hortonmachine.gears.io.wcs.XmlHelper;
import org.w3c.dom.Node;

public class Identification implements XmlHelper.XmlVisitor {
    String title;
    String abstract_;
    List<String> keywords = new ArrayList<>();

    // String serviceType;
    // List<String> supportedVersions = new ArrayList<>();

    String fees;
    String accessConstraints;

    @Override
    public boolean checkElementName(String name) {
        if (name.equals("wcs:Service") || name.endsWith(":Service"))
            return true;
        return false;
    }

    @Override
    public void visit(Node node) {
        title = XmlHelper.findFirstTextInChildren(node, "label");
        abstract_ = XmlHelper.findFirstTextInChildren(node, "description");

        Node keywordsNode = XmlHelper.findNode(node, "keywords");
        keywords = XmlHelper.findAllTextsInChildren(keywordsNode, "keyword");

        // serviceType = XmlHelper.findFirstTextInChildren(node, "servicetype");
        // supportedVersions = XmlHelper.findAllTextsInChildren(node, "servicetypeversion");

        fees = XmlHelper.findFirstTextInChildren(node, "fees");
        accessConstraints = XmlHelper.findFirstTextInChildren(node, "accessconstraints");
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
        // s += "serviceType: " + serviceType + "\n";
        // s += "supportedVersions: \n";
        // if (supportedVersions != null && supportedVersions.size() > 0)
        //     for (String sv : supportedVersions) {
        //         s += "\t\t" + sv + "\n";
        //     }
        s += "fees: " + fees + "\n";
        s += "accessConstraints: " + accessConstraints + "\n";

        return s;

    }
    
}
