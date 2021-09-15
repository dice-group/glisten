There are two Configurations in Glisten.

1. The Benchmark Configuration
2. The Evaluation Parameters

The Benchmark Configuration consists of all the information for one benchmark (datasets, namespaces, fact generation basics).

The Evaluation Parameter define the specifics to use inside the benchmark (seed for random stuff, how many facts should be generated etc.)

We will discuss both in the following and explain what you need to know. 

## Prelimineries

To understand the Configuration let's dive a little into what we need. 

As a base: 

* We need one or more source datasets
* several target datasets (some better fitting to the source/s than others)
* for each target dataset, the links between the source and the target dataset. (For simplicity these linked target dataset contain the target as well and not just the links)

For fact checking:

* A triplestore where we can load the datasets in
* how many true and false facts should be generated
* which seed to use (for reproducibility)
* Which facts we want to consider
* Some namespaces for performance reasons  


### Which facts we want to consider

Glisten currently implements two types on how to retrieve facts. 

* An Allow List
* A Block List

The Allow list contains several predicates which are allowed, the block list contains several predicates which aren't allowed.

These list represent the base. 

Further on we can specify that a property needs to occur at least N times in the source dataset to be actually considered.

True Facts will be then just drawn randomly based upon thes constrains. 

False Facts will be retrieved randomly, with the addition that the object of that Fact/Statement will be mutated to an object that exists in the source dataset with the property, but the Fact still remains false wrt to the source.


## Benchmark Configuration

To now create the benchmark configuration we use the YAML format. 

The configurations file contains several configurations and the Idea is that, you can add your configuration for anyone to use, without specifing certain details which are then based in the evaluation parameters. 

So each configuration contains only

* the benchmark name
* The URL to a ZIP file containing all targets (To send to the recommendation system)
* The URL to a ZIP file containing all linked target datasets (For internal use)
* The URLs of each source to evaluate against
* The namespaces to use 
* For the true fact generation:
	* Which List type to use (allow list or block list)
	* Which predicates to consider
* For the false fact generation:
	* Which List type to use (allow list or block list)
	* Which predicates to consider

The configuration then looks like the following example:

```yaml

configurations:
  - name: "test_benchmark"
    linksUrlZip: "file:///path/to/links.zip"
    targetUrlZip: "file:///path/to/targets.zip"
    sources:
      - "file:///path/to/source1.nt"
    trueStmtDrawerOpt:
      stmtDrawerType: "allowlist"
      list:
        - "http://dbpedia.org/ontology/mythology"
        - "http://dbpedia.org/ontology/creator"
    falseStmtDrawerOpt:
      stmtDrawerType: "blocklist"
      list:
        - "http://dbpedia.org/ontology/mythology"
        - "http://dbpedia.org/ontology/creator"
    namespaces:
      - "http://dbpedia.org/ontology/"
```

The benchmark name is `test_benchmark` and the URL links are both locally (you can use https:// as well and Glisten will download the zips).
The source is listed using a local URL as well (again would work perfectly fine with an online one)

Next the `trueStmtDrawerOpt` and `falseStmtDrawerOpt` represent the true and false fact generation (true/false statement fact drawer options)
It defined the statement drawer type (`allowlist` or `blocklist`) and the list of predicates in this list. 

Further on it defines the namespaces to be used which in our case is only `http://dbpedia.org/ontology/`.
This will boost the performance immense on datasets like DBpedia

## Evaluation Parameters

The evaluation parameters can be defined defined by running the benchmark itself (F.e. as CLI arguments) 

The parameters are listed in the following table

| Name | Description | Default |
| ----- | ----------------- | ----- |
| seed | The seed to use for any random acitivity | Will be random, but logged out for reproduciblity |
| minimum property occurrences | The minimum amount a property has to occur in the dataset to be considered for fact generation | 10 |
| max property limit | The max amount a property is retrieved for fact generation, if more facts are available will choose randomly the amount of facts. | 30 | 
| max recommendations | Only check against the Top N recommendations (For performance reasons) | 10 |
| Scorer Algorithm | The Fact Checking algorithm. Currently implemented: [COPAAL, SampleCOPAAL, SampleCOPAAL_AvgScore, COPAAL_AvgScore, SampleCopaal_RootMeanSquare, Copaal_RootMeanSquare] | COPAAL |
| Scorer threshold | The threshold to consider a true fact actually true. F.e. the scorer might state the true fact at 0.01, but that is not sufficient, we want at least a score of 0.2 to consider the fact true. This will put true facts behind false facts who are mostly at 0.0. This is more for debugging purposes . | 0.0 |
| number Of True Statements | The no. of true facts to generate | 5 |
| number Of False Statements | The no. of false facts to generate | 5 | 
| benchmarkName | The benchmark to use inside the configuration file | test_benchmark |
| config file | The Benchmark Configuration File described above | data_config.yml |
| order file | see below.  | random order |
| rdf Endpoint | The SPARQL endpoint to use to load the datasets into and query for fact checking | - |


### What is the order file

The order file is an optional argument to use to specify the order your recommendation system would provide (If you're using the CLI executor).

Each target dataset name should be listed inside this order file, seperated by new lines. 
The dataset on top is the best recommended dataset and the last dataset is the lowest recommended dataset. 

With this file you can execute the recommendation outside of Glisten and just check how well the produced recommendation ranking is. 

If you want to check your system directly you want to use our Hobbit Implementation instead. 