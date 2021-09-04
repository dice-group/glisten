# Glisten

Glisten is a benchmarking framework for linkage recommendation systems. 


## What is a "Linkage Recommendation System"?

A linkage recommendation system can analyze datasets and given another dataset, can tell you which of the analyzed datasets fits topically to the given dataset.
It creates a score for each of the analyzed datasets which represents how well the datasets link to the given dataset.

## How does Glisten work?

Glisten provides a core library, which can be executed as a CLI benchmark or can be used inside [Hobbit](https://project-hobbit.eu).
Glisten provides a source dataset and target datasets, which will be given to the recommendation system. 
The system should then respond with a ranking of the best fitting target dataset to the least fitting target datasets wrt linkage with the source dataset.

Glisten then goes through the ranking, and adds one the recommendations after another to the source dataset.
Each time a dataset was added, glisten uses a Scorer system (e.g. a fact checking system like Copaal) to create a score for the current dataset. 
The Idea is, that the facts will get better scores (and thus the scorer will produce a better overall score) when good fitting target datasets were added.
Hence the earlier the good fitting datasets were added, the earlier the score will rise.

To take this into account, Glisten uses a ROC curve and the corresponding AUC score as how good the recommendation system is. 
Each time the score gets better the ROC curve gets better, and hence the overall AUC score. 

## How do you calculate the Scorer scores.

The Scorer scores are calculatet using a ROC curve as well. The score is again the corresponding AUC value.
The actual creation of the ROC curve is dependent of the scorer system.

### Using Copaal:

For each fact copaal checks the veracity score. A score that basically means how "true" the fact seems. 
We generate some true and some false facts from the source model for this.

We then sort the facts after their veracity scores and for each score we then check if the fact is a true fact or a false fact.
If the fact is a true fact the ROC curve goes up, if it is a false fact the ROC curve goes right. 

Hence if an added target dataset provides some value for a true fact, the true fact should be seen as truer than before by Copaal. 
Thus it ideally goes up in the ranking, and the ROC curve is going up earlier, providing a better AUC score.

## CLI usage:

You can use the shaded jar file provided in the releases to execute a benchmark locally.

To do this execute the following

```bash
java -jar glisten-shaded.jar -h 
```

This will print the following help

```bash
Usage: glisten-test [-hV] [--clean-up] [-c=<configFile>]
                    [-m=<maxRecommendations>]
                    [--max-property-limit=<maxPopertyLimit>]
                    [--min-prop-occ=<minPropOcc>]
                    [-nofs=<numberOfFalseStatements>]
                    [-nots=<numberOfTrueStatements>] [-o=<orderFile>]
                    [-s=<scorerAlgorithm>] [-S=<seed>] <rdfEndpoint>
Executes the glisten workflow without Hobbit and prints the ROC curve at the
end. Mostly useful for debugging.
      <rdfEndpoint>   the rdf endpoint to use
  -c, --config=<configFile>
                      the config file for glisten. Default: data_config.yml
      --clean-up      if set, will remove the testing directory which includes
                        all downloaded and extracted datasets.
  -h, --help          Show this help message and exit.
  -m, --max-recommendations=<maxRecommendations>
                      the no. of max recommendations, 0 or lower means that all
                        recommendations will be looked at. Default=10
      --max-property-limit=<maxPopertyLimit>
                      the maximum a property is allowed to be added for
                        performance reasons. Default=30
      --min-prop-occ=<minPropOcc>
                      the minimum a property has to occur to be considered for
                        the fact generation. Default=10
      -nofs, --no-of-false-stmts=<numberOfFalseStatements>
                      the no. of false statements to generate. Default=5
      -nots, --no-of-true-stmts=<numberOfTrueStatements>
                      the no. of true statements to generate. Default=5
  -o, --order-file=<orderFile>
                      A file containing the order of the recommendations, if
                        not set, will be random
  -s, --scorer=<scorerAlgorithm>
                      The Scorer algorithm to use. Algorithms: [Copaal]
  -S, --seed=<seed>   the seed to use for anything random we do. Default=1234L
  -V, --version       Print version information and exit.

```


# Documentation

For more information have a look at our documentation at [TODO](#)
