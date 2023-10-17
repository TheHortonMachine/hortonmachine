package org.hortonmachine.gears.io.wcs.models;

import java.util.ArrayList;
import java.util.List;

import org.hortonmachine.gears.io.wcs.XmlHelper;
import org.w3c.dom.Node;

public class WcsCapabilities implements XmlHelper.XmlVisitor {

    String version;

    Identification identification;

    List<CoverageSummary> coverageSummaries = new ArrayList<>();

    @Override
    public boolean checkElementName(String name) {
        if (name.equals("wcs:Capabilities") || name.endsWith(":Capabilities"))
            return true;
        return false;
    }

    @Override
    public void visit(Node node) {

        version = XmlHelper.findAttribute(node, "version");

        identification = new Identification();
        XmlHelper.apply(node, identification);

        XmlHelper.apply(node, new CoverageSummary(coverageSummaries));
    }

    public String toString() {
        String s = "WcsCapabilities [\n";
        s += "version=" + version + "\n";
        s += identification;
        s += "\n****************************************************************************************\n";
        for (CoverageSummary coverageSummary : coverageSummaries) {
            s += coverageSummary;
        }
        s += "\n****************************************************************************************\n";
        s += "\n]";

        return s;
    }

}
