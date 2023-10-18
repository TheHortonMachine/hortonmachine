package org.hortonmachine.gears.io.wcs.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hortonmachine.gears.io.wcs.XmlHelper;
import org.w3c.dom.Node;

/**
 * This class represents the capabilities of a Web Coverage Service (WCS).
 */
public class WcsCapabilities implements XmlHelper.XmlVisitor {
    String version;
    Identification identification;
    Map<String, CoverageSummary> layerId2CoverageSummaryMap = new HashMap<>();
    
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
        
        List<CoverageSummary> coverageSummaries = new ArrayList<>();
        XmlHelper.apply(node, new CoverageSummary(coverageSummaries));
        for (CoverageSummary coverageSummary : coverageSummaries) {
            layerId2CoverageSummaryMap.put(coverageSummary.coverageId, coverageSummary);
        }
    }

    public String getVersion() {
        return version;
    }

    public List<String> getCoverageIds(){
        return new ArrayList<>(layerId2CoverageSummaryMap.keySet());
    }

    public CoverageSummary getCoverageSummaryById(String coverageId){
        return layerId2CoverageSummaryMap.get(coverageId);
    }

    public String toString() {
        String s = "WcsCapabilities [\n";
        s += "version=" + version + "\n";
        s += identification;
        s += "\n****************************************************************************************\n";
        for (CoverageSummary coverageSummary : layerId2CoverageSummaryMap.values()) {
            s += coverageSummary;
        }
        s += "\n****************************************************************************************\n";
        s += "\n]";

        return s;
    }

}
