wget https://dlcdn.apache.org/jena/binaries/apache-jena-4.2.0.tar.gz
tar -xzvf apache-jena-4.2.0.tar.gz 
rm apache-jena-4.2.0.tar.gz

wget https://dlcdn.apache.org/jena/binaries/apache-jena-fuseki-4.2.0.tar.gz
tar -xzvf apache-jena-fuseki-4.2.0.tar.gz 
rm apache-jena-fuseki-4.2.0.tar.gz


#rm ./empty.nt
touch ./empty.nt
rm -rf ./TESTDB/
./apache-jena-4.2.0/bin/tdbloader --loc=./TESTDB/ ./empty.nt
