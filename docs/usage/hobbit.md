In this section we explain how to execute a benchmark in Hobbit, what parameters you can use and how to add your own recommendation system to be tested in Hobbit.

## How to execute a Benchmark


Go to https://main.project-hobbit.eu and login. 

Got to `Benchmarks` and choose `Glisten Benchmark`.

Now choose your System to test, the benchmark you want to execute and all the parameters yo use.


### Parameters 

| Name | Description  |
| ----- | ----------------- | 
| benchmarkName | The benchmark to use inside the configuration file | 
| seed | The seed to use for any random acitivity |
| minimum property occurrences | The minimum amount a property has to occur in the dataset to be considered for fact generation | 
| max property limit | The max amount a property is retrieved for fact generation, if more facts are available will choose randomly the amount of facts. |  
| max recommendations | Only check against the Top N recommendations (For performance reasons) | 
| Scorer Algorithm | The Fact Checking algorithm. Currently implemented: [COPAAL, SampleCOPAAL] |
| number Of True Statements | The no. of true facts to generate | 
| number Of False Statements | The no. of false facts to generate |  


## How to add my own System 


To add your own recommendation system to Hobbits Glisten Benchmark you need to

1. create a glisten system wrapper
2. Create a repository add https://git.project-hobbit.eu/
3. Add you system docker container to this repository


### create a glisten system wrapper

For this section we assume you simply use the Glisen repo at https://github.com/dice-group/Glisten and adding your system to the `raki-system-adapter` module.


Create a Class (we call this `MySystem` in the `org.dice_group.glisten.hobbit.systems` package for now and implement it using Kotlin)

```kotlin

package org.dice_group.glisten.hobbit.systems

import org.dice_group.glisten.hobbit.systems.AbstractGlistenHobbitSystem
import java.io.File


/**
 * Simple test system which just randomly sets scores from 0.0 to 1.0
 */
class MySystem : AbstractGlistenHobbitSystem() {

	
	override fun init(){
		super.init()

		//You can init your system here
	}


	/**
     * Given a source file and a list of target files, the system shall create a score
     * for each target file.
     *
     * The score represents how fitting the target is for the source file (aka, is it a good recommendation)
     *
     * It shall return the mapping File->Score
     *
     * @param source The source to generate the recommendation scores with
     * @param targets The targets to generate the recommendations scores for
     * @return The mapping from target file to recommendation score
     */
    override fun generateRecommendationScores(source: File, targets: ArrayList<File>): List<Pair<File, Double>> {
        val ret = mutableListOf<Pair<File, Double>>()
        targets.forEach{ target ->

        	//TODO create your score for this target here
        	val score = 0.0

            ret.add(Pair(target, score))
        }
        return ret
    }


}

```

Thats it, implement the TODOs and your system is ready to go.


### Create a repository add https://git.project-hobbit.eu/

Go to https://git.project-hobbit.eu/ and create a new project. 

Add a file called `system.ttl` containing the following


```ttl
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
@prefix hobbit: <http://w3id.org/hobbit/vocab#> .
@prefix glisten: <http://w3id.org/glisten/hobbit/vocab#> .


raki:MySystem a  hobbit:SystemInstance;
	rdfs:label	"MySystem"@en;
	rdfs:comment	"Description of your system."@en;
	hobbit:imageName "git.project-hobbit.eu:4567/YOUR_USERNAME/YOUR_PROJECT_NAME";
	hobbit:implementsAPI glisten:Glisten-API .

```

### Add you system docker container to this repository

Now we need to create a Dockercontainer for the system and push that systme to the hobbit registry.

Create a `Dockerfile` in your project

```dockerfile
FROM java

ADD target/glisten-test-1.0.0-SNAPSHOT.jar /glisten/systems.jar

WORKDIR /glisten

CMD java -cp systems.jar org.hobbit.core.run.ComponentStarter org.dice_group.glisten.hobbit.systems.MySystemKt
```

now build the project

```bash
mvn clean package -P shaded
```

build and push the container to the hobbit registry.

```bash
docker login git.project-hobbit.eu:4567
docker build -t git.project-hobbit.eu:4567/YOUR_USERNAME/YOUR_PROJECT_NAME .
docker push git.project-hobbit.eu:4567/YOUR_USERNAME/YOUR_PROJECT_NAME
```

Now you should be able to access your system on Hobbit.