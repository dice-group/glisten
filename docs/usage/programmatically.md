In this section we go through Glisten as a library. 

We will discuss examples on how to use the Glisten core insider your application.

If you want to exend glisten hava a look at our develop section.

Glisten was programmed in Kotlin and is thus accessible in Java as well. 
It is advised to use Kotlin however.

## Add Glisten as a Maven dependency

TODO as soon as release is near...


## Execute A benchmark

To execute a benchmark programmatically you need to use the `CoreEvaluator`

1. create a Configuration (we will load one from file and uses the configuration with the name testBenchmark)

```kotlin
val conf = ConfigurationFactory.findCorrectConfiguration("/path/to/config.yaml", "testBenchmark")
```

2. create a Scorer Algorithm we want to use. (we will use Copaal for this one)

```kotlin
val scorer : Scorer = Copaal(conf.namespaces)
```

3. lets assume our triplestore is started at http://localhost:9999/sparql

```kotlin
val rdfEndpoint = "http://localhost:9999/sparql"
```

Be aware to load the datasets into the triplestore the script `load_triplestore.sh` is used  with the arguments   `/path/to/` and `file.nt` change that script to so that it loads the given file into your triplestore. An example is provided for Virtuoso

4. let's create the CoreEvaluator

```kotlin
val evaluator = CoreEvaluator(conf, rdfEndpoint, scorer)
```

init and download the files into the /tmp folder, we can choose a different one.

```kotlin
evaluator.init("/tmp/")
```

now we need a source file and some recommendations
 for the source file we will use the first one inside our configuration

```kotlin
val source = conf.sources[0]
```

for our recommendations, we will just use a mock here, here is the part where you can set your recommendation system
 important is only that you create a list containing all recommendations which are listed in the
 zip file stated in the configuration under conf.targetUrlZip

```kotlin
val recommendations = listOf(Pair("file:///path/to/target1", 0.1), Pair("file:///path/to/target2", 0.05))
```

Finally let's start the benchmark

```kotlin
val auc = evaluator.getAUC(source, recommendations)
println("AUC score is $auc")
```

## Create some Facts

If you want to create some facts, you'll need some `Statement Drawers`. 
These will retrieve some facts/statements based upon some constraints (e.g. An Allow list or a block list)

Let's assume we want to use an Allow List for both 

```kotlin

val minPropertyOccurrences = 10
val maxPropertyLimit = 30 
val seed = 123L


//Create your Model here and load it properyl
val model = ModelFactory.createDefaultModel()


// Let's allow both the properties name and author
val allowed = listOf("http://example.com/property/name", "http://example.com/property/author")

val trueDrawer = AllowListDrawer(allowed, seed, model, minPropertyOccurrences, maxPropertyLimit)

val falseDrawer = AllowListDrawer(allowed, seed, model, minPropertyOccurrences, maxPropertyLimit)

val facts = FactGenerator.createFacts(seed, 20, 10, trueDrawer, falseDrawer)
```

That's it now you have list of Pairs containg facts and if the fact is true (1.0) or false (-1.0)


## Get some Scores

If  you just want some scores using the fact checker you can use the Scorer Algorithm directly. 

First we need some facts to score against and a SPARQL endpoint to use. 

For simplictly we assume that you created two Apache Jena RDF Statements. The first one is the `trueStatement` and the second one is the `falseStatement`. 
It doesn't matter too much, you just need a list of pairs containg statments and if they are true or false.

```kotlin
val endpoint = "http://localhost:9999/sparql" //CHANGE THIS

//Create some Pairs of Jena Statements and if they are true or false, we assume that you have a true and false statement created beforehand
val facts = listOf(Pair(trueStatement, 1.0), Pair(falseStatement, -1.0))
```

With that we can now create our Scorer and execute the algorithm. We will use Copaal as the Scorer Algorithm.

```kotlin

// Create our scorer
val scorer : Scorer = ScorerFactory.createScorerOrDefault("Copaal",conf.namespaces)

//create our score
val score : Double = scorer.getScore(endpoint, facts)
```



## Javadoc/KDoc

You can find the KDoc at ... TODO