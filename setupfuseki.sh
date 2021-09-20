#wget https://hobbitdata.aksw.uni-leipzig.de/glisten/hdt.zip 
#unzip hdt.zip

cd /glisten/hdt-java/ && mvn install && cd ..
cd /glisten/hdt-java/hdt-fuseki/ && mvn package dependency:copy-dependencies

rm /glisten/tmp_source.nt
rm /glisten/empty.nt
touch /glisten/empty.nt
cd /glisten/hdt-java/hdt-java-cli/ && ./bin/rdf2hdt.sh /glisten/empty.nt /glisten/second.hdt
cd /glisten/hdt-java/hdt-java-cli/ && ./bin/rdf2hdt.sh /glisten/empty.nt /glisten/main.hdt
