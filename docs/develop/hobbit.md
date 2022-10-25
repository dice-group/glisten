In this section we explain how to add your benchmark to Hobbit.

Be aware that you need a reachable server where you can store your target, sources and linked dataset.


## Add your Benchmark

clone the glisten repo from https://github.com/dice-group/Glisten 

```
git clone https://github.com/dice-group/Glisten
```

### Create Configuration

Now go into `raki-hobbit/docker` and edit the `data_config.yml` file and add your system as follows

```yaml
configurations:
	- name: "MyBenchmark"
      linksUrlZip: "https://hobbitdata.informatik.uni-leipzig.de/glisten/bp_links.zip"
      targetUrlZip: "https://hobbitdata.informatik.uni-leipzig.de/glisten/bp_targets.zip"
      sources:
        - "https://hobbitdata.informatik.uni-leipzig.de/glisten/birthPlace.nt"
      trueStmtDrawerOpt:
        stmtDrawerType: "allowlist"
        list:
          - "http://dbpedia.org/ontology/birthPlace"
      falseStmtDrawerOpt:
        stmtDrawerType: "allowlist"
        list:
          - "http://dbpedia.org/ontology/birthPlace"
      namespaces:
        - "http://dbpedia.org/ontology/"

```

Set your benchmark name (e.g MyBenchmark)

Set the target url zip file containing all target files.

set the Linked datasets url zip file containing all linked datasets.
These need to contain a file for each target named `SOURCENAME_TARGETNAME.nt`  which contains the target dataset and the links between the specified source dataset and the target dataset.

Add the links to your sources.

And for the fact checking add the `trueStmtDrawerOpt` which desribes how to retrieve positive facts, as well as the `falseStmtDrawerOpt`. 
Set the `stmtDrawerType` to either `allowlist` or `blocklist` depending on how you want to generate the facts and
add a `list` parameter which describes the `allowlist` (resp `blocklist`)

Finally add namespaces you want to use (mainly for performance usage)


### Update Docker container

now you nede to update the docker container by simply executing

```bash
./build_docker.sh 
```

### Change the Benchmark.ttl file

Now go to https://git.project-hobbit.eu/glisten/benchmark/ and edit the `benchmark.ttl` file and add

```ttl
glisten:MyBenchmark a glisten:Datasets;
		rdfs:label "MyBenchmark"@en;
		rdfs:comment "MyBenchmark Description"@en .

```

Be aware that the rdfs:label needs to be the same as the name in the configurations.


Now you should be able to use your benchmark in Hobbit. 

Make sure that the corresponding links in the configuration are accessable.