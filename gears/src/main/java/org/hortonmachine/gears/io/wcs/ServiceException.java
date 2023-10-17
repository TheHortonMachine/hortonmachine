package org.hortonmachine.gears.io.wcs;

/**
 * WCS ServiceException
 * 
 * Attributes:
 * message -- short error message
 * xml -- full xml error message from server
 **/
public class ServiceException {

    private String message;
    private String xml;

    public ServiceException(String message, String xml){
        this.message = message;
        this.xml = xml;
    }

    public String toString(){
        return this.message + "\n" + this.xml;
    }
}
