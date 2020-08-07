package org.hortonmachine.geoscript;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Collections;

import org.hortonmachine.dbs.log.Logger;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.gui.settings.SettingsController;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyShell;

public class GeoscriptConsole {

    public GeoscriptConsole() throws Exception {
        InputStream groovyClassIS = GeoscriptConsole.class.getResourceAsStream("Console.groovy");

        String classString = FileUtilities.readInputStreamToString(groovyClassIS);

        GroovyClassLoader gcl = new GroovyClassLoader();
        Class< ? > clazz = gcl.parseClass(classString);
        gcl.close();
        Object obj = clazz.getDeclaredConstructor().newInstance();
        Method method = obj.getClass().getMethod("run");
        method.invoke(obj);
    }

    public static void main( String[] args ) {
        try {
            if (args.length == 1) {
                GroovyShell shell = new GroovyShell();
                shell.run(new File(args[0]), Collections.emptyList());
            } else {
                Logger.INSTANCE.init();
                SettingsController.applySettings(null);
                new GeoscriptConsole();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
