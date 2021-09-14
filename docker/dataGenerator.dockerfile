FROM java

ADD target/glisten-test-1.0.0-SNAPSHOT.jar /glisten/controller.jar

WORKDIR /glisten

ADD data_config.yaml .

CMD java -cp controller.jar org.hobbit.core.run.ComponentStarter  org.dice_group.glisten.hobbit.DataGeneratorKt
