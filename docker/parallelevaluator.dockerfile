FROM openjdk:11

WORKDIR /glisten


ADD docker/data_config.yaml .
ADD load_triplestore.sh .

ADD target/glisten-test-1.0.0-SNAPSHOT.jar /glisten/controller.jar


CMD java -cp controller.jar org.hobbit.core.run.ComponentStarter  org.dice_group.glisten.hobbit.ParallelEvaluator
