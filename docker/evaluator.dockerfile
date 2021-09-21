FROM openjdk:11

RUN apt-get update && apt-get upgrade
RUN apt-get install -y maven

WORKDIR /glisten

ADD hdt.zip . 
RUN unzip hdt.zip

ADD setupfuseki.sh .
RUN ./setupfuseki.sh

ADD docker/data_config.yaml database/
ADD load_triplestore.sh .

ADD target/glisten-test-1.0.0-SNAPSHOT.jar /glisten/controller.jar


CMD java -cp controller.jar org.hobbit.core.run.ComponentStarter  org.dice_group.glisten.hobbit.EvaluatorKt
