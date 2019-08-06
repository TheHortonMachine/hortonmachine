package org.hortonmachine.geoscript;

import java.io.InputStream;
import java.lang.reflect.Method;

import org.hortonmachine.dbs.log.Logger;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.gui.settings.SettingsController;

import groovy.lang.GroovyClassLoader;

public class GeoscriptConsole {

    public GeoscriptConsole() throws Exception {
        InputStream groovyClassIS = GeoscriptConsole.class.getResourceAsStream("Console.groovy");

        String classString = FileUtilities.readInputStreamToString(groovyClassIS);

        GroovyClassLoader gcl = new GroovyClassLoader();
        Class< ? > clazz = gcl.parseClass(classString);
        gcl.close();
        Object obj = clazz.newInstance();
        Method method = obj.getClass().getMethod("run");
        method.invoke(obj);
    }

    public static void main( String[] args ) {
        try {
            Logger.INSTANCE.init();
            SettingsController.applySettings(null);
            new GeoscriptConsole();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
