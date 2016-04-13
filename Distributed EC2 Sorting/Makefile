
jar:
	./runjava

client-server:
	(cd socket/textsock && make)
	(cd socket/client && sbt assembly && mv target/scala-*/client*jar ../../client.jar)
	(cd socket/server && sbt assembly && mv target/scala-*/server*jar ../../server.jar)

clean:
	(cd socket/textsock && make clean)
	(rm -rf client.jar && cd socket/client && rm -rf client.jar target project/target project/project)
	(rm -rf client.jar && cd socket/server && rm -rf server.jar target project/target project/project)

.PHONY: all clean


