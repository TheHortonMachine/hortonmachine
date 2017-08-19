rm -rf extras/export/libs
rm -rf extras/deploy/libs
rm -rf extras/deploy/*.tar.gz
mvn install
groovy extras/deploy/release.groovy
