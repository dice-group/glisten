mvn clean package

docker build -f docker/controller.dockerfile -t  git.project-hobbit.eu:4567/glisten/benchmark/controller .
docker build -f docker/dataGenerator.dockerfile -t  git.project-hobbit.eu:4567/glisten/benchmark/datagenerator .
docker build -f docker/taskGenerator.dockerfile -t  git.project-hobbit.eu:4567/glisten/benchmark/taskgenerator .
docker build -f docker/evaluator.dockerfile -t  git.project-hobbit.eu:4567/glisten/benchmark/evaluationmodule .
docker build -f docker/testsystem.dockerfile -t  git.project-hobbit.eu:4567/glisten/systems/testsystem .

docker push  git.project-hobbit.eu:4567/glisten/benchmark/controller 
docker push  git.project-hobbit.eu:4567/glisten/benchmark/datagenerator
docker push  git.project-hobbit.eu:4567/glisten/benchmark/taskgenerator
docker push  git.project-hobbit.eu:4567/glisten/benchmark/evaluationmodule 
docker push  git.project-hobbit.eu:4567/glisten/systems/testsystem 
