package org.hortonmachine.gears.io.wcs;

import java.util.List;

public interface IWcsCapabilities extends XmlHelper.XmlVisitor {

    String getVersion();

    List<String> getCoverageIds();

}