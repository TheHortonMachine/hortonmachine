package org.hortonmachine.gears.io.wcs.wcs201.models;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

    // <ows:ServiceIdentification>
    // <ows:Title>Web Coverage Service: R28</ows:Title>
    // <ows:Abstract>Abteilung 28. Natur, Landschaft und Raumentwicklung - Ripartizione 28: Natura, paesaggio e sviluppo del territorio</ows:Abstract>
    // <ows:Keywords>
    // ...
    // </ows:Keywords>
    // <ows:ServiceType>urn:ogc:service:wcs</ows:ServiceType>
    // <ows:ServiceTypeVersion>2.0.1</ows:ServiceTypeVersion>
    // <ows:ServiceTypeVersion>1.1.1</ows:ServiceTypeVersion>
    // <ows:ServiceTypeVersion>1.1.0</ows:ServiceTypeVersion>
    // <ows:Profile>http://www.opengis.net/spec/WCS/2.0/conf/core</ows:Profile>
    // <ows:Profile>http://www.opengis.net/spec/WCS_protocol-binding_get-kvp/1.0.1</ows:Profile>
    // <ows:Profile>http://www.opengis.net/spec/WCS_protocol-binding_post-xml/1.0</ows:Profile>
    // <ows:Profile>http://www.opengis.net/spec/WCS_service-extension_crs/1.0/conf/crs-gridded-coverage</ows:Profile>
    // <ows:Profile> http://www.opengis.net/spec/WCS_geotiff-coverages/1.0/conf/geotiff-coverage</ows:Profile>
    // <ows:Profile>http://www.opengis.net/spec/GMLCOV/1.0/conf/gml-coverage</ows:Profile>
    // <ows:Profile>http://www.opengis.net/spec/GMLCOV/1.0/conf/special-format</ows:Profile>
    // <ows:Profile>http://www.opengis.net/spec/GMLCOV/1.0/conf/multipart</ows:Profile>
    // <ows:Profile>http://www.opengis.net/spec/WCS_service-extension_scaling/1.0/conf/scaling</ows:Profile>
    // <ows:Profile>http://www.opengis.net/spec/WCS_service-extension_crs/1.0/conf/crs</ows:Profile>
    // <ows:Profile>http://www.opengis.net/spec/WCS_service-extension_interpolation/1.0/conf/interpolation</ows:Profile>
    // <ows:Profile>http://www.opengis.net/spec/WCS_service-extension_interpolation/1.0/conf/interpolation-per-axis</ows:Profile>
    // <ows:Profile>http://www.opengis.net/spec/WCS_service-extension_interpolation/1.0/conf/nearest-neighbor</ows:Profile>
    // <ows:Profile>http://www.opengis.net/spec/WCS_service-extension_interpolation/1.0/conf/linear</ows:Profile>
    // <ows:Profile>http://www.opengis.net/spec/WCS_service-extension_interpolation/1.0/conf/cubic</ows:Profile>
    // <ows:Profile>http://www.opengis.net/spec/WCS_service-extension_range-subsetting/1.0/conf/record-subsetting</ows:Profile>
    // <ows:Fees>NONE</ows:Fees>
    // <ows:AccessConstraints>NONE</ows:AccessConstraints>
    // </ows:ServiceIdentification>


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
        keywords = XmlHelper.findAllTextsInChildren(keywordsNode, "keyword");

        serviceType = XmlHelper.findFirstTextInChildren(node, "servicetype");
        supportedVersions = XmlHelper.findAllTextsInChildren(node, "servicetypeversion");

        profiles = XmlHelper.findAllTextsInChildren(node, "profile");

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
