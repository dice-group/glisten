wget https://hobbitdata.informatik.uni-leipzig.de/glisten/hdt.zip
mvn clean package -P shaded

sudo docker build -f docker/controller.dockerfile -t  git.project-hobbit.eu:4567/glisten/benchmark/controller .
sudo docker build -f docker/dataGenerator.dockerfile -t  git.project-hobbit.eu:4567/glisten/benchmark/datagenerator .
sudo docker build -f docker/taskGenerator.dockerfile -t  git.project-hobbit.eu:4567/glisten/benchmark/taskgenerator .
sudo docker build -f docker/evaluator.dockerfile -t  git.project-hobbit.eu:4567/glisten/benchmark/evaluationmodule .
sudo docker build -f docker/parallelevaluator.dockerfile -t  git.project-hobbit.eu:4567/glisten/benchmark/parallelevaluationmodule .
sudo docker build -f docker/scorer.dockerfile -t  git.project-hobbit.eu:4567/glisten/benchmark/scorer .

sudo docker build -f docker/testsystem.dockerfile -t  git.project-hobbit.eu:4567/glisten/systems/testsystem .

sudo docker push  git.project-hobbit.eu:4567/glisten/benchmark/controller 
sudo docker push  git.project-hobbit.eu:4567/glisten/benchmark/datagenerator
sudo docker push  git.project-hobbit.eu:4567/glisten/benchmark/taskgenerator
sudo docker push  git.project-hobbit.eu:4567/glisten/benchmark/evaluationmodule
sudo docker push  git.project-hobbit.eu:4567/glisten/benchmark/parallelevaluationmodule

sudo docker push  git.project-hobbit.eu:4567/glisten/benchmark/scorer
sudo docker push  git.project-hobbit.eu:4567/glisten/systems/testsystem 
