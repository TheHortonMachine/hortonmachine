# To use ww with maven in the project:
#    * export worldwind project as jar and copy it in the libs folder of the nww module
#    * run this script to install ww into teh local maven repo
#    * after that maven should work
mvn install:install-file -Dfile=./worldwind.jar -DgroupId=gov.nasa -DartifactId=worldwind -Dversion=2.0.X -Dpackaging=jar
mvn install:install-file -Dfile=./gluegen-rt.jar -DgroupId="org.jogamp.gluegen" -DartifactId=gluegen-rt -Dversion="2.1.5" -Dpackaging=jar
mvn install:install-file -Dfile=./jogl-all.jar -DgroupId="org.jogamp.jogl" -DartifactId=jogl-all -Dversion="2.1.5" -Dpackaging=jar

# for windows:
# mvn install:install-file -Dfile=E:/SOFTWARE/jgrasstools-git/nww/libs/worldwind.jar -DgroupId="gov.nasa" -DartifactId=worldwind -Dversion="2.0.X" -Dpackaging=jar
# mvn install:install-file -Dfile=E:/SOFTWARE/WorldWindJava/gluegen-rt.jar -DgroupId="org.jogamp.gluegen" -DartifactId=gluegen-rt -Dversion="2.1.5" -Dpackaging=jar
# mvn install:install-file -Dfile=E:/SOFTWARE/WorldWindJava/jogl-all.jar -DgroupId="org.jogamp.jogl" -DartifactId=jogl-all -Dversion="2.1.5" -Dpackaging=jar




