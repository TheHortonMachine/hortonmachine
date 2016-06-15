# To use ww with maven in the project:
#    * export worldwind project as jar and copy it in the libs folder of the nww module
#    * run this script to install ww into teh local maven repo
#    * after that maven should work
mvn install:install-file -Dfile=./worldwind.jar -DgroupId=gov.nasa -DartifactId=worldwind -Dversion=2.0.X -Dpackaging=jar

# for windows:
# mvn install:install-file -Dfile=D:\development\jgrasstools-git\nww\libs\worldwind.jar -DgroupId="gov.nasa" -DartifactId=worldwind -Dversion="2.0.X" -Dpackaging=jar
