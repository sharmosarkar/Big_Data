
all:
	(cd textsock && make)
	(cd client && sbt assembly && mv target/scala-*/client*jar client.jar)
	(cd server && sbt assembly && mv target/scala-*/server*jar server.jar)

clean:
	(cd textsock && make clean)
	(cd client && rm -rf client.jar target project/target project/project)
	(cd server && rm -rf server.jar target project/target project/project)

.PHONY: all clean


