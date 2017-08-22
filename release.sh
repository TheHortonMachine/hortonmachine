rm -rf extras/export/libs
rm -rf extras/deploy/libs
rm -rf extras/deploy/*.tar.gz
mvn install
cd apps
mvn install
cd ..
groovy extras/deploy/release.groovy
