package org.hortonmachine.gears.io.wcs;

import java.util.HashMap;
import java.util.Map;

/**
 * Base class to be subclassed by version dependent WCS classes. Provides 'high-level'
    version independent methods
 * 
 */
public class WCSBase {
    
    private String url;
    private String version;
    private String xml;
    private String cookies;
    private Authentication auth;
    private int timeout;
    private String headers;
    private HashMap<String, Object> _describeCoverage;
    public WCSBase(String url, String xml, String cookies, Authentication auth, int timeout, String headers){
        
        // """ overridden __new__ method

        // @type url: string
        // @param url: url of WCS capabilities document
        // @type xml: string
        // @param xml: elementtree object
        // @param auth: instance of owslib.util.Authentication
        // @param timeout: HTTP timeout, in seconds
        // @param headers: dict for geoserver's request's headers
        // @return: inititalised WCSBase object
        // """
        // obj = object.__new__(self)
        // obj.__init__(url, xml, cookies, auth=auth, headers=headers)

        
        
        
        this.url = url;
        this.xml = xml;
        this.cookies = cookies;
        this.auth = auth;
        this.timeout = timeout;
        this.headers = headers;
        _describeCoverage = new HashMap<>();  // cache for DescribeCoverage responses
    }

    // def __init__(self, auth=None, timeout=30, headers=None):
    //     self.auth = auth or Authentication()
    //     self.headers = headers
    //     self.timeout = timeout
    /**
     * returns a describe coverage document - checks the internal cache to see if it has been fetched before
     * 
     */
    // public Object getDescribeCoverage(String identifier){
    //     if(_describeCoverage.keySet().contains(identifier)){
    //         return _describeCoverage.get(identifier);
    //         // if identifier not in list(self._describeCoverage.keys()):
    //         var reader = DescribeCoverageReader(
    //             version, identifier, cookies, auth,timeout, headers);
    //             _describeCoverage.put(identifier, reader.read(url));
    //     }
    //     return _describeCoverage.get(identifier);
    // }
}
