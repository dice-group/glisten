FROM java

ADD target/glisten-test-1.0.0-SNAPSHOT.jar /glisten/controller.jar

WORKDIR /glisten

CMD java -cp controller.jar org.hobbit.core.run.ComponentStarter  org.dice_group.glisten.hobbit.TaskGeneratorKt
