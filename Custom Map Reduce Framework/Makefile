
setUp:
	makessh.sh
	installjava.sh

jar:
	cp WordCount.java src/
	mvn clean compile assembly:single
	mv target/*.jar WordCount.jar


EMR:	jar
	cp WordCount.jar CustomMapReduce.jar

pseudo:	jar
	cp WordCount.jar /tmp/.

a2-jar:
	cp A2.java src/
	mvn clean compile assembly:single
	mv target/*.jar A2.jar

a2-EMR:	
	a2-jar
	cp A2.jar CustomMapReduce.jar

a2-pseudo:	
	a2-jar
	cp A2.jar /tmp/.

missed-jar:
	cp MissedConnections.java src/
	mvn clean compile assembly:single
	mv target/*.jar MissedConnections.jar

missed-EMR:	
	missed-jar
	cp MissedConnections.jar CustomMapReduce.jar

missed-pseudo:	
	missed-jar
	cp MissedConnections.jar /tmp/.

reg-jar:
	cp Regression.java src/
	mvn clean compile assembly:single
	mv target/*.jar Regression.jar

reg-EMR:	
	reg-jar
	cp Regression.jar CustomMapReduce.jar

reg-pseudo:	
	reg-jar
	cp Regression.jar /tmp/.

median-jar:
	cp WordMedian.java src/
	mvn clean compile assembly:single
	mv target/*.jar WordMedian.jar

median-EMR:	
	median-jar
	cp WordMedian.jar CustomMapReduce.jar

median-pseudo:	
	median-jar
	cp WordMedian.jar /tmp/.
