/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Library General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Library General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Library General Public License
 * along with this library; if not, write to the Free Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

// THIS FILE HAS TO BE RUN FROM THE PROJECT ROOT LIKE:
// groovy extras/deploy/deploylibs.groovy 

def VERSION = "0.8.1-SNAPSHOT";

def javaHome = System.getProperty("java.home");
def javaHomeFile = new File(javaHome);
def toolsJar = new File(javaHomeFile.getParentFile(), "lib/tools.jar");
if(!toolsJar.exists()){
    println "The JAVA_HOME variable has to be set and point to a JDK";
}


// copy also source jars?
def alsoSources = false;
// copy also javadoc jars?
def alsojavaDocs = false;
// your maven repo path
def mvnRepo = System.getProperty("user.home");
def repo = "${mvnRepo}/.m2/repository/"
println "Using maven repo in: ${repo}";
def repoFile = new File(repo);
if(!repoFile.exists()){
    println "The maven repo is not in the default location, please set it by hand in the script";
    System.exit(0);
}
// path to which to copy them
def copyPath = "./extras/deploy/libs/"
def copyPathFile = new File(copyPath);
if(!copyPathFile.exists()){
    copyPathFile.mkdir();
}

// copy jgrasstools modules
JGTMODULESCOPY: {
    def modulesFolder = "./extras/deploy/modules/"
    def modulesFolderFile = new File(modulesFolder);
    if(!modulesFolderFile.exists()){
        modulesFolderFile.mkdir();
    }
    // take latest jgrassgears jar
    def jarFiles = new File("./jgrassgears/target").listFiles(new FilenameFilter() {  
        public boolean accept(File f, String filename) {  
            return filename.startsWith("jgt-jgrassgears-") && filename.endsWith(".jar")  
        }  
    });
    Arrays.sort(jarFiles, Collections.reverseOrder());
    def jgtJar = jarFiles[0];
    def jgtCopyToFile = new File(modulesFolderFile.absolutePath, jgtJar.name).absolutePath;
    (new AntBuilder()).copy( file : jgtJar , tofile : jgtCopyToFile )

    // take latest hortonmachine jar
    jarFiles = new File("./hortonmachine/target").listFiles(new FilenameFilter() {  
        public boolean accept(File f, String filename) {  
            return filename.startsWith("jgt-hortonmachine-") && filename.endsWith(".jar")  
        }  
    });
    Arrays.sort(jarFiles, Collections.reverseOrder());
    def hmJar = jarFiles[0];
    def hmCopyToFile = new File(modulesFolderFile.absolutePath, hmJar.name).absolutePath;
    (new AntBuilder()).copy( file : hmJar , tofile : hmCopyToFile )

    // take latest modules jar
    jarFiles = new File("./modules/target").listFiles(new FilenameFilter() {  
        public boolean accept(File f, String filename) {  
            return filename.startsWith("jgt-modules-") && filename.endsWith(".jar")  
        }  
    });
    Arrays.sort(jarFiles, Collections.reverseOrder());
    def modJar = jarFiles[0];
    def modCopyToFile = new File(modulesFolderFile.absolutePath, modJar.name).absolutePath;
    (new AntBuilder()).copy( file : modJar , tofile : modCopyToFile )

    // take latest dbs jar
    jarFiles = new File("./dbs/target").listFiles(new FilenameFilter() {  
        public boolean accept(File f, String filename) {  
            return filename.startsWith("jgt-dbs-") && filename.endsWith(".jar")  
        }  
    });
    Arrays.sort(jarFiles, Collections.reverseOrder());
    def dbsJar = jarFiles[0];
    def dbsCopyToFile = new File(modulesFolderFile.absolutePath, dbsJar.name).absolutePath;
    (new AntBuilder()).copy( file : dbsJar , tofile : dbsCopyToFile )

    // take latest lesto jar
    jarFiles = new File("./lesto/target").listFiles(new FilenameFilter() {  
        public boolean accept(File f, String filename) {  
            return filename.startsWith("jgt-lesto-") && filename.endsWith(".jar")  
        }  
    });
    Arrays.sort(jarFiles, Collections.reverseOrder());
    def lestoJar = jarFiles[0];
    def lestoCopyToFile = new File(modulesFolderFile.absolutePath, lestoJar.name).absolutePath;
    (new AntBuilder()).copy( file : lestoJar , tofile : lestoCopyToFile )

    // take latest gui jar
    jarFiles = new File("./gui/target").listFiles(new FilenameFilter() {  
        public boolean accept(File f, String filename) {  
            return filename.startsWith("jgt-gui-") && filename.endsWith(".jar")  
        }  
    });
    Arrays.sort(jarFiles, Collections.reverseOrder());
    def guiJar = jarFiles[0];
    def guiCopyToFile = new File(modulesFolderFile.absolutePath, guiJar.name).absolutePath;
    (new AntBuilder()).copy( file : guiJar , tofile : guiCopyToFile )

    // take latest apps jar
    jarFiles = new File("./apps/target").listFiles(new FilenameFilter() {  
        public boolean accept(File f, String filename) {  
            return filename.startsWith("jgt-apps-") && filename.endsWith(".jar")  
        }  
    });
    Arrays.sort(jarFiles, Collections.reverseOrder());
    def nwwJar = jarFiles[0];
    def nwwCopyToFile = new File(modulesFolderFile.absolutePath, nwwJar.name).absolutePath;
    (new AntBuilder()).copy( file : nwwJar , tofile : nwwCopyToFile )

    // tools.jar
    def newToolsJar = new File(copyPathFile, "tools.jar");
    new AntBuilder().copy ( file : toolsJar.absolutePath , tofile : newToolsJar.absolutePath )
}

def outputCatcher = new StringBuffer();
def errorCatcher = new StringBuffer();
def proc;
try{
	def mvnCommand = "mvn dependency:tree";
	proc = mvnCommand.execute();
	println "...launching maven deps tree: " + mvnCommand;
} catch(Exception e) {
	// dirty test for windows
	def mvnCommand = "cmd /c mvn dependency:tree";
	println "...launching maven deps tree: " + mvnCommand;
	proc = mvnCommand.execute();
}
proc.consumeProcessOutput(outputCatcher, errorCatcher);
proc.waitFor();
	
def output = outputCatcher.toString();
//println output;
println "...launching maven deps tree (done)";

println "...parsing maven outputs";
// clean out what we need
def lista=[];
def lines = output.split("\n");
def depsList = [];
def startIndex = -1;
def endIndex = -1;

/*
// uncomment the following for maven 2
for (int i = 0; i < lines.size(); i++){
    def line = lines[i];
	
    //if(line.startsWith("[INFO] [dependency:tree")){
    if(line.startsWith("[INFO] --- maven-dependency-plugin")){
		println "...found start line: " + line;
        startIndex = i + 1;
        continue;
    }
    if(startIndex != -1 && line.startsWith("[INFO] ----------")){
		println "...found end line: " + line;
		endIndex = i - 1
        break;
    }
    if(startIndex == -1){
        continue;
    }

    lista << line;
} // end maven 2
*/

// with maven 3
for (int i = 0; i < lines.size(); i++){
    def line = lines[i];

    if(line.startsWith("[INFO] org.jgrasstools") || line.endsWith(":compile")){
    	lista << line;
    }
} // end maven 3
println "...parsing maven outputs (done)";
println "Search for:"
lista.each{
    println it
}

println "---------------------------------------"
println "---------------------------------------"

// find jars
def basedir = new File(repo)
def files = [];
basedir.eachDirRecurse () { dir ->
    dir.eachFileMatch(~/.*.jar/) { file ->  
         files << file
    }  
} 

// extract name pattern and version
def fileBeginList = []
def versionList = []
lista.each{
   def split = it.split(":");
   fileBeginList << split[1]
   versionList << split[3]
   println "${split[1]} --- ${split[3]}"
}

println "---------------------------------------"
println "---------------------------------------"

TreeMap<String, String> finalList = new TreeMap<>();
// extract right jars paths from list
for (it in files){
    def name = it.getName()
    def path = it.getAbsolutePath()
    
    if(!alsoSources && name.matches(".*sources.*")){
        continue;
    }
    if(!alsojavaDocs && name.matches(".*javadoc.*")){
        continue;
    }

    for (int i = 0; i < fileBeginList.size(); i++){
        def fBegin = fileBeginList.get(i);
        def version = versionList.get(i);
        version = version.replaceAll("\\.", "\\\\.");
        if(name.startsWith(fBegin)){
            if(name.matches(".*"+version+".*")){
                finalList.put(it.getName(), it);
                break;
            }
        }
    }
}

println "---------------------------------------"
println "---------------------------------------"
println "Found:"
finalList.each{ k, v -> 
    println k;
}


println "---------------------------------------"
println "---------------------------------------"
if(copyPath){
    if(new File(copyPath).exists()){
        println "Copy deps jars to: ${copyPath}"
        finalList.each{ k, file ->
            def name = file.getName();
            def path = file.getAbsolutePath();
            def newPath = new File(copyPath, name).getAbsolutePath();

            // exclude those that go in the modules folder
            if(!name.startsWith("jgt-jgrassgears-") && 
               !name.startsWith("jgt-hortonmachine-") && 
               !name.startsWith("jgt-modules-")
            )
                new AntBuilder().copy ( file : path , tofile : newPath )
        }
    }


    // zip the thing
    def ant = new AntBuilder()  
    def date = new java.text.SimpleDateFormat("yyyyMMddHHmm").format(new Date());
    def versionStr = date;
    if(VERSION) versionStr = VERSION;
    ant.zip(destfile: "./extras/deploy/jgrasstools-${versionStr}.zip",  basedir: "./extras/deploy/",  includes: "**",  excludes: "*deploylibs.groovy*,jgrasstools*.zip")  
}
