# About

Glisten is a benchmarking framework for linkage recommendation systems.


## What is a "Linkage Recommendation System"?


A linkage recommendation system can analyze datasets and given another dataset, can tell you which of the analyzed datasets fits topically to the given dataset. It creates a score for each of the analyzed datasets which represents how well the datasets link to the given dataset.


## How does Glisten work?


Glisten provides a core library, which can be executed as a CLI benchmark or can be used inside Hobbit. Glisten provides a source dataset and target datasets, which will be given to the recommendation system. The system should then respond with a ranking of the best fitting target dataset to the least fitting target datasets wrt linkage with the source dataset.

Glisten then goes through the ranking, and adds one the recommendations after another to the source dataset. Each time a dataset was added, glisten uses a Scorer system (e.g. a fact checking system like Copaal) to create a score for the current dataset. The Idea is, that the facts will get better scores (and thus the scorer will produce a better overall score) when good fitting target datasets were added. Hence the earlier the good fitting datasets were added, the earlier the score will rise.

To take this into account, Glisten uses a ROC curve and the corresponding AUC score as how good the recommendation system is. Each time the score gets better the ROC curve gets better, and hence the overall AUC score.

### More indepth Idea

A Benchmark in Glisten is defined to check one or more source datasets against several target datasets. 
For each source dataset a recommendation system should get a score for each target dataset representing how well this dataset fits to the source.

Hence we get a ranking of the target datasets from best fitting to least fitting.


Glisten then uses Fact Checking to check if the ranking puts all the relevant datasets on top. 

It will generate some true and some wrong facts from the source dataset.


Now using these facts the Fact Checking system will be used to check each fact against the source dataset. 
This represents the baseline.

Now we add the highest ranked target dataset and check again. 
Ideally the fact checker could declare more true facts true and more wrong facts wrong. 
The fact checking score rises. 

Hence if the recommendation system rates the better fitting target datasets higher, the score the system will retrieve will be better than
if the system ranks low fitting target datasets higher. 


> A small Note! We don't directly add the target, but a precomputed linked dataset of the target with the source file (It contains the target, but also the links between the source and the target)


## What is Hobbit?


[Hobbit](http://project-hobbit.eu/) is a platform for holisitc benchmarking of Big Linked Data. 

It deploys a platform [https://master.project-hobbit.eu](https://master.project-hobbit.eu) that allows users to deploy and execute their benchmarks.

The glisten benchmark is integrated into the platform and can be easily executed inside hobbit, without setting anything up. 

If you want to deploy your Recommendation System to be used in Hobbit, have a look at: [How to add my Recommendation System to Hobbit]


## How to use the local benchmark execution?


Sadly a prebuild release would be too big (about 500MB as it includes the Stanford NLP models) and thus cannot be downloaded directly,
however you can build Glisten easily yourself.

```bash
# Download Glisten
git clone https://github.com/dice-group/Glisten
cd Glisten 

# build glisten
mvn clean package

# Execute the Help option to see what you can do
java -jar glisten-test-{{ release_version }}.jar -h

```

The help execution will print this screen. 

```bash
Usage: glisten-test [-hV] [--clean-up] [-c=<configFile>]
                    [-F=<numberOfFalseStatements>] [-m=<maxRecommendations>]
                    [--max-property-limit=<maxPropertyLimit>]
                    [--min-prop-occ=<minPropOcc>] [-N=<benchmarkName>]
                    [-o=<orderFile>] [-s=<scorerAlgorithm>] [-S=<seed>]
                    [-t=<threshold>] [-T=<numberOfTrueStatements>] <rdfEndpoint>
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
                      The Scorer algorithm to use. Algorithms: [Copaal]
  -S, --seed=<seed>   the seed to use for anything random we do. Default is random
  -t, --scorer-threshold=<threshold>
                      the threshold to use inside the scorer. A true fact needs
                        to be better than the threshold. Default=0.0
  -T, --no-of-true-stmts=<numberOfTrueStatements>
                      the no. of true statements to generate. Default=5
  -V, --version       Print version information and exit.
```


Have a look on [how to setup a benchark] for further information.



## How do you calculate the Scorer scores?


The Scorer scores are calculatet using a ROC curve as well. The score is again the corresponding AUC value. The actual creation of the ROC curve is dependent of the scorer system.

### Using Copaal:

For each fact copaal checks the veracity score. A score that basically means how "true" the fact seems. We generate some true and some false facts from the source model for this.

We then sort the facts after their veracity scores and for each score we then check if the fact is a true fact or a false fact. If the fact is a true fact the ROC curve goes up, if it is a false fact the ROC curve goes right.

Hence if an added target dataset provides some value for a true fact, the true fact should be seen as truer than before by Copaal. Thus it ideally goes up in the ranking, and the ROC curve is going up earlier, providing a better AUC score.


## Where is the code?

The code is open source at [https://github.com/dice-group/Glisten](https://github.com/dice-group/Glisten) and you can code with us if you want to :)

## Where do I submit a bug or enhancement?

Please use the Github Issue Tracker at [https://github.com/dice-group/Glisten/issues](https://github.com/dice-group/Glisten/issues)