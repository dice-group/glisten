We will explain how to get started with using Glisten in this section.

There are two ways you can use Glisten as a benchmarking tool

1. Using the local CLI interface
2. Using Hobbit


In this case we will show you how you can execute a benchmark using the CLI interface.

If you want to use Hobbit, have a look at our [Hobbit Usage] documentation.

## Build the code

First thing to do is to build the glisten code.

### Download

```bash
git clone https://github.com/dice-group/Glisten
cd Glisten
```

### Build

Now we can build our code using maven

```bash
mvn clean package
```

If you encounter a problem telling you that `Corraborative-0.2.0.jar` cannot be retrieved, we supplied the jar file and the library with this code. 

Simply copy the file structure in `./lib/repository/` to `~/.m2/repository/` and maven should buid Glisten just fine.

### Execute

Now that Glisten is build we can execute it. 
For clarification let's move the execution jar file to our current directory

```bash
mv target/glisten-test-{{ release_version }} ./
```

In the following steps the execution of the benchmark is declared. 

However you can use the help parameter to get some additional information on what you can change in your execution

```bash
Usage: glisten-test [-hV] [--clean-up] [-c=<configFile>]
                    [-F=<numberOfFalseStatements>] [-m=<maxRecommendations>]
                    [--max-property-limit=<maxPropertyLimit>]
                    [--min-prop-occ=<minPropOcc>] [-N=<benchmarkName>]
                    [-o=<orderFile>] [-s=<scorerAlgorithm>] [-S=<seed>]
                    [--sample-size=<sampleSize>] [-t=<threshold>]
                    [-T=<numberOfTrueStatements>] <rdfEndpoint>
Executes the glisten workflow without Hobbit and prints the ROC curve at the
end. Mostly useful for debugging. Uses the load_triplestore.sh script file to
upload datasets to the triplestore.
      <rdfEndpoint>   the rdf endpoint to use
  -c, --config=<configFile>
                      the config file for glisten. Default: data_config.yml
      --clean-up      if set, will remove the testing directory which includes
                        all downloaded and extracted datasets.
  -F, --no-of-false-stmts=<numberOfFalseStatements>
                      the no. of false statements to generate. Default=5
  -h, --help          Show this help message and exit.
  -m, --max-recommendations=<maxRecommendations>
                      the no. of max recommendations, 0 or lower means that all
                        recommendations will be looked at. Default=10
      --max-property-limit=<maxPropertyLimit>
                      the maximum a property is allowed to be added for
                        performance reasons. Default=30
      --min-prop-occ=<minPropOcc>
                      the minimum a property has to occur to be considered for
                        the fact generation. Default=10
  -N, --benchmark-name=<benchmarkName>
                      The name of the benchmark to use. Name is specified
                        inside the given configuration file.
  -o, --order-file=<orderFile>
                      A file containing the order of the recommendations, if
                        not set, will be random
  -s, --scorer=<scorerAlgorithm>
                      The Scorer algorithm to use. Algorithms: [Copaal,
                        SampleCopaal]
  -S, --seed=<seed>   the seed to use for anything random we do. Default is
                        random
      --sample-size=<sampleSize>
                      the sample size to use if Scorer uses samples. Default=30
  -t, --scorer-threshold=<threshold>
                      the threshold to use inside the scorer. A true fact needs
                        to be better than the threshold. Default=0.0
  -T, --no-of-true-stmts=<numberOfTrueStatements>
                      the no. of true statements to generate. Default=5
  -V, --version       Print version information and exit.
```

For more information on each parameter have a look at our [Configuration]




## Execute a benchmark

Now that you can execute glisten, we can execute a benchmark we want to use. 

The example.yml file which was delivered with the code contains some pre defined benchmarks we can use. 
(Be aware that some of these files might be huge and you need quite a bit of RAM for these to be executed.)


If you want to build your own benchmark checkout: [How to create a Benchmark]


let's execute a benchmark

1. Choose a benchmark from the example.yml (we will use `MySimpleBenchmark`)
2. Setup your recommendation file 
3. Prepare a triplestore to use
4. Execute the benchmark

As stated we will use the `MySimpleBenchmark` benchmark.

### Setup the recommendation file


> If you don't want to use a recommendation system, and just checking out glisten, you can ignore this step, it will then use a random order.


At this moment the CLI interface doesn't handle recommendations systems in itself (please refer to the Hobbit version for that), however you can execute the recommendations
on your system yourself and provide the order (highest recommendation at top, lowest at the bottom) of the recommendations.

In our case we have the following 4 target datasets, the recommendation system should rate wrt the source dataset `change_this.nt`.

```
change_this1.nt
change_this2.nt
change_this3.nt
change_this4.nt
``` 

Either create a file with each line holding one dataset name. 
And the stated order will be used. 
Be aware that the dataset on the top should be the best recommendation. 

we will call this file the order file and store it under the name `recommendations_order.txt`.


### Prepare a triplestore to use

You can choose any triplestore you want, the only important thing is, that it needs to be able to update the dataset on the fly with rather big files. 
(We will not use big files in our example benchmark, but you get the gist.)

If you choose one and set it up as you want, start it. 

Now we only need to change the `load_triplestore.sh`  script which is responsible for loading additional datasets into the triplestore. 

The script is setup for a Virtuoso instance with the isql port at 1111.


To change the script it is important to note, that the script will get two arguments, the first describing the path to the current file, and the file name.
E.g 1=`/path/to/` and 2=`fileName.nt`

If your choosen triplestore cannot update the database on the fly, you can use some tricks to do this, like stopping the server, updating the database and starting the server again. 

For no we will assume that the SPARQL endpoint is at `http://localhost:8890/sparql` 


### Execute the benchmark


Now that we have glisten build, a benchmark choosen, the triplestore set up and optionally an order file, we can execute the benchmark

```
java -jar glisten-test-{{ release_version }}.jar -o recommendations_order.txt -N MySimpleBenchmark http://localhost:8890/sparql
```

or if you do not have an order file

```
java -jar glisten-test-{{ release_version }}.jar -N MySimpleBenchmark http://localhost:8890/sparql
```


This execution will download and unzip all targets and links for the `MySimpleBenchmark` to the `testing` folder and executes the benchmark. 

There should be some log output and at the end you'll get the ROC Curve and the Area under the Curve for the given order (or a random one, if you used no order file). 