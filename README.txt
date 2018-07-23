Team 301 submission

To build our solution simply run:

mvn clean compile

In order to solve using our portfolio solver (the combination of all our solvers), one should run (assuming models reside in ./models):

mvn exec:java -Dexec.mainClass=icfpc2018.MainKt -Dexec.args="-s portfolio --mode all"

The results will reside in ./submit and a .zip archive ready for submission will be created as well.
