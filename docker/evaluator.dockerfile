FROM openjdk:11

WORKDIR /glisten

ADD setupfuseki.sh .
RUN ./setupfuseki.sh

ADD docker/data_config.yaml database/
ADD load_triplestore.sh .

ADD target/glisten-test-1.0.0-SNAPSHOT.jar /glisten/controller.jar


CMD java -cp controller.jar org.hobbit.core.run.ComponentStarter  org.dice_group.glisten.hobbit.EvaluatorKt
