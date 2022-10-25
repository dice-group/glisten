FROM openjdk:11

ADD target/glisten-test-1.0.0-SNAPSHOT.jar /glisten/controller2.jar

WORKDIR /glisten

CMD java -cp controller2.jar org.hobbit.core.run.ComponentStarter  org.dice_group.glisten.hobbit.Benchmark
