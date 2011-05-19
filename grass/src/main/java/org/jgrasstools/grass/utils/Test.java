package org.jgrasstools.grass.utils;

import java.io.ByteArrayInputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.jgrasstools.grass.dtd.GrassInterface;
import org.jgrasstools.grass.dtd.Task;

public class Test {

    public Test() throws Exception {
        String result = GrassRunner.runModule(new String[]{"/usr/lib/grass64/bin/v.in.ascii", "--interface-description"},
                "/usr/lib/grass64", "/tmp/grass6-moovida-10940/gisrc");

        JAXBContext jaxbContext = JAXBContext.newInstance(GrassInterface.class);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        byte[] bytes = result.getBytes();
        Task grassInterface = (Task) unmarshaller.unmarshal(new ByteArrayInputStream(bytes));

        String name = grassInterface.getName();
        String desc = grassInterface.getDescription();
        
        System.out.println(name.trim());
        System.out.println(desc.trim());
    }

    public static void main( String[] args ) throws Exception {
        new Test();
    }

}
