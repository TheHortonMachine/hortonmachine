package org.jgrasstools.grass.utils;

import org.jgrasstools.grass.dtd64.Task;

public class Test {

    public Test() throws Exception {
        System.setProperty(GrassUtils.GRASS_ENVIRONMENT_GISBASE_KEY, "/usr/lib/grass64");

        GrassRunner grassRunner = new GrassRunner(null, null, false);
        String result = grassRunner.runModule(new String[]{"/usr/lib/grass64/bin/r.sun", "--interface-description"});

        Task task = GrassUtils.getTask(result);

        Oms3CodeGenerator gen = new Oms3CodeGenerator(task);

        System.out.println(gen.getOms3Class());
        
        

        // String name = task.getName();
        // String desc = task.getDescription();
        // System.out.println(name.trim());
        // System.out.println(desc.trim());
        //
        // List<Flag> flagList = task.getFlag();
        // System.out.println("Flags:");
        // for( Flag flag : flagList ) {
        // String flagName = flag.getName().trim();
        // System.out.print("\t" + flagName + " - ");
        // String descr = flag.getDescription().trim();
        // System.out.println(descr);
        // }
        //
        // List<ParameterGroup> parameterGroupList = task.getParameterGroup();
        // if (parameterGroupList.size() > 0) {
        // System.out.println("ParameterGroup:");
        // for( ParameterGroup parameterGroup : parameterGroupList ) {
        // String pgName = parameterGroup.getName().trim();
        // System.out.println(pgName);
        // String pgDescr = parameterGroup.getDescription().trim();
        // System.out.println(pgDescr);
        // }
        // }
        //
        // List<Parameter> parameterList = task.getParameter();
        // if (parameterList.size() > 0) {
        // System.out.println("Parameters:");
        // for( Parameter parameter : parameterList ) {
        // String pgName = parameter.getName().trim();
        // System.out.print("\t" + pgName + " - ");
        // String pgDescr = parameter.getDescription().trim();
        // System.out.println(pgDescr);
        //
        // String req = parameter.getRequired().trim();
        // System.out.println("\t\tRequired: " + req);
        // String type = parameter.getType().trim();
        // System.out.println("\t\tType:" + type);
        // String defaultv = parameter.getDefault();
        // if (defaultv != null) {
        // defaultv = defaultv.trim();
        // System.out.println("\t\tDefault: " + defaultv);
        // }
        // String multiple = parameter.getMultiple().trim();
        // System.out.println("\t\tMultiple: " + multiple);
        //
        // Values values = parameter.getValues();
        // if (values != null) {
        // System.out.println("\t\tValues:");
        // List<Value> value = values.getValue();
        // for( Value v : value ) {
        // String name2 = v.getName().trim();
        // System.out.print("\t\t\t" + name2 + " - ");
        // String description = v.getDescription().trim();
        // System.out.println(description);
        // }
        // }
        // }
        // }
    }

    public static void main( String[] args ) throws Exception {
        new Test();
    }

}
