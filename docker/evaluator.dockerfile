FROM java

RUN sudo apt-get update && sudo apt-get upgrade
RUN sudo apt-get install wget tar

WORKDIR /virtuoso

RUN wget https://github.com/openlink/virtuoso-opensource/releases/download/v7.2.6.1/virtuoso-opensource.x86_64-generic_glibc25-linux-gnu.tar.gz
RUN tar -xzvf virtuoso-opensource.x86_64-generic_glibc25-linux-gnu.tar.gz

ADD docker/config.ini .

RUN cd /virtuoso/virtuoso-opensource/bin && ./virtuoso-t +configfile /virtuoso/config.ini && cd ..

WORKDIR /glisten

ADD docker/data_config.yml .
ADD load_triplestore.sh .

ADD target/glisten-test-1.0.0-SNAPSHOT.jar /glisten/controller.jar


CMD java -cp controller.jar org.hobbit.core.run.ComponentStarter  org.dice_group.glisten.hobbit.EvaluatorKt
