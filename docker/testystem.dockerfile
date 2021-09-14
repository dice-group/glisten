FROM java

ADD target/glisten-test-1.0.0-SNAPSHOT.jar /glisten/systems.jar

WORKDIR /glisten

CMD java -cp systems.jar org.hobbit.core.run.ComponentStarter org.dice_group.glisten.hobbit.systems.test.TestSystemKt