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

def VERSION = "0.9.2-SNAPSHOT";

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

for (int i = 0; i < lines.size(); i++){
    def line = lines[i];

    if(line.startsWith("[INFO] org.hortonmachine") || line.endsWith(":compile")){
    	lista << line;
    }
}

// extract name pattern and version
def fileBeginList = []
def versionList = []

println "\n\n\n\n";

def gvsigDeps = [];
TreeSet<String> gvsigDepsList = new TreeSet<>();
lista.each{
   def split = it.split(":");


   def line = split[0] + ":" + split[1] + ":" + split[2];
   def lineSplit = line.trim().split("\\s+");
   def dep = lineSplit[lineSplit.length-1];
   
   if(dep.endsWith("jar")){
      if(!dep.contains("junit")){
          def added = gvsigDepsList.add(dep);
          /*if(added){
            println "ADDED: " + dep
          } else {
            println "NOT ADDED: " + dep
          }
            */
      }
   }
}


gvsigDepsList.each{ dep ->
	println "<include>" + dep + "</include>";
}


println "\n\nFound libraries: " + gvsigDepsList.size()



