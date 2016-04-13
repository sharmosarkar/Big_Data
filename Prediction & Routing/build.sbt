

lazy val root = (project in file(".")).
    settings(
        name := "Prediction",
        
	libraryDependencies ++= Seq(
  		"org.apache.spark"  % "spark-core_2.10"              % "1.1.0",
  		"org.apache.spark"  % "spark-mllib_2.10"             % "1.2.0"
  	)
		
    )


compileOrder := CompileOrder.JavaThenScala 
//mainClass in (Compile, run) := Some("Routing")
//mainClass in (Compile, packageBin) := Some("Routing")
