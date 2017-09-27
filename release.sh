rm -rf extras/export/libs
rm -rf extras/deploy/libs
rm -rf extras/deploy/*.tar.gz

# fix pom 
mv pom.xml pom.xml.orig
cat pom.xml.orig | sed '/REMONREL/d' > pom.xml

mvn install -Dmaven.test.skip=true -Dmaven.javadoc.skip=true
groovy ./extras/deploy/release.groovy

rm pom.xml
mv pom.xml.orig pom.xml
