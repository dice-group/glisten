This section explains how to change and extend the Core library.

## Add your own Scorer

Create your Scorer Algorithm to use inside glisten.

You need to 

1. Create your Scorer Algorithm
2. Add the Algorithm to the ScorerFactory

### Create your Scorer Algorithm

To create your `Scorer` simply extend the `Scorer` class located in `org.dice_group.glisten.core.scorer`  and you  only need to implement one function

```
class MyScorer2000(namespaces: List<String>)  : Scorer(namespaces){


    /**
     * Gets the Scores for each Fact as a Pair whereas the returned Pairs consists of the original trueness Value as the first element
     * and the score as the second.
     *
     * @param endpoint the rdf endpoint to use
     * @param facts the facts to calculate the scores against, each pair consists of the statement and its trueness value
     *
     * @return a list of pairs containing the trueness value of the fact and the score
     */
    override fun getScores(endpoint: String, facts: List<Pair<Statement, Double>>): MutableList<Pair<Double, Double>>{

        //TODO

    }


}

```

As you can see you need to extend the `getScores` function, you get some facts and a SPARQL endpoint provided and you should create a list of Pairs representing the trueness value and your score. 

In other words if the fact at position 4 looks is `Statement, 1.0` and your score for that Statement is f.e. `0.4`, 

then the list you're returing has to have the Pair `1.0, 0.4` at position `4`.


That's it now let's add your Scorer algorithm to the `ScorerFactory` so we can use it by simply stating it in the Evaluation parameters.


### Add you Scorer to the ScorerFactory 

This step is pretty basic and you only need to name your algorithm like `MyScorer2000`. 

Inside the `ScorerFactory` in `org.dice_group.glisten.core.scorer.Scorer.kt` add to the createOrDefault method your scorer name like

```kotlin
    fun createScorerOrDefault(scorerAlgorithm: String, namespaces: List<String>) : Scorer  {
        var scorer : Scorer = Copaal(namespaces)
        when(scorerAlgorithm.lowercase(Locale.getDefault())){
            "copaal" -> scorer = Copaal(namespaces)

            "myscorer2000" -> scorer = MyScorer2000(namespaces)

        }
        return scorer
    }

```

and add your Scorer name to the KDoc. 


Additionally you should add the name to the `Test.kt` file so Users can know that they can use your scorer.

Do this by simply adding it to the Option description of the `scorerAlgorithm`  parameter like "The Scorer algorithm to use. Algorithms: [Copaal, MyScorer2000]"


### Add your Scorer to Hobbit.

If you want to use the Scorer inside Hobbit, you need to add the Scorer to the `benchmark.ttl` inside the Glisten Hobbit repository at [TODO]().

Add the following to the `benchmark.ttl`

```ttl
TODO
```


## Add your own Fact Generation Base List

Create a fact generation base list (or Statmenet Drawer) like the Allow list or Block list 


There are two steps involved.

1. Create your Statement Drawer
2. Add the Statment Drawer to the Configuration


### Create the Statment Drawer


To create a Statement Drawer we simply need to extend the `StmtDrawer` class.

Let's create our Drawer 

```kotlin

class MyDrawer2000(private val blockList: Collection<String>, private val seed: Long, override val model : Model, private val minPropOcc: Int, private val maxPropertyLimit: Int) : StmtDrawer(seed, model, minPropOcc, maxPropertyLimit) {

		//This is the heart of our Drawer
	    override fun getStmts(): MutableList<Statement> {
	    	//create your statements here. and return them.
	    }	
}
```


Inside the `getStmts()` method we create and return the list of Statements to consider either for true or for false statement creation. 

If you want to completly rewrite how true and false statements are generated you need to change the `TaskDrawer` accordingly. 


### Add your Drawer to the Configuration

Now let's name our drawer `MyDrawer2000`

However we want to state our name inside the configuration just as we can state `Allowlist` or `Blocklist` as the type. 

To do this go to the `Configurations` file inside the package `org.dice_group.glisten.core.config.Configuration`.
There us the `Configuration` class and the method `createStmtDrawer` 

inside this method you'll find a when statment. 
Add your Drawer as following

```kotlin
    private fun createStmtDrawer(type: String, list: Collection<String>, seed: Long, model: Model, minPropOcc: Int, maxPropertyLimit: Int  ): StmtDrawer {
        //Add your new statement drawer type here to the `when` clause
        return when{
            type.lowercase(Locale.getDefault()) == "allowlist" ->
                AllowListDrawer(list, seed, model, minPropOcc, maxPropertyLimit)
            
            type == "MyDrawer2000" -> MyDrawer2000(list, seed, model, minPropOcc, maxPropertyLimit)


            else ->
                BlockListDrawer(list, seed, model, minPropOcc, maxPropertyLimit)
        }
    }
```

that is it. You can now use your Statement Drawer from the Configuration file. 
