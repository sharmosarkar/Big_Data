
//import scala.io.Source
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf
import org.apache.spark.rdd.RDD
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.mllib.tree.configuration.Algo
import org.apache.spark.mllib.tree.impurity.Gini
import org.apache.spark.mllib.tree.configuration.Strategy
import org.apache.spark.mllib.tree.RandomForest
import org.apache.spark.mllib.evaluation.MulticlassMetrics



object Prediction {

	def main(args: Array[String]) {
       val conf = new SparkConf().
            setAppName("Prediction!!").
            setMaster("local")
        val sc = new SparkContext(conf)      
       
        val DATE = 5
        val AC_DEP_TIME = 30    
        val SC_DEP_TIME = 29    
        val CANCEL_FLG = 47
        val FLT_NO = 10
        val TAIL_NO = 9
        val ARR_DELAY = 42 
        val DEP_DELAY = 31      
        val DOW = 4
        val AL_CARRIER = 8
        val ORIGIN = 11//14
        val DESTINATION = 20//23
        val DISTANCE = 54 
        val CRS_ELAPSED_TIME = 50    
        val DISANCE_GROUP = 55


        // Input   -----      ../Prediction-data/a6history
        val trainingDataSet = sc.textFile(args(0)).
            map (x => {
                 x.replace(", ", "").
                 replace("\"", "").
                 split(",")
            }).
            filter (x=>{ x(0) != "YEAR" && !Flight_Data_Sanity_Check.sanityTest(x)}).
            map(x=>{
                (x(FLT_NO),x(DATE),x(SC_DEP_TIME))  -> (
                (if (x(DOW).isEmpty) 0.0 else x(DOW).toDouble) + "::" +
                (if (x(ORIGIN).isEmpty) 0.0 else x(ORIGIN).toDouble) + "::" +
                (if (x(DESTINATION).isEmpty) 0.0 else x(DESTINATION).toDouble) + "::" +
                (if (x(DISTANCE).isEmpty) 0.0 else x(DISTANCE).toDouble) + "::" +
                (if (x(ARR_DELAY).toDouble > 0) 1.0 else 0.0) )   // 1 -> delayed otherwise 0
                }) 


        // testDataSet
        val testDataSet = sc.textFile(args(1)).
            map (x => {
                 x.replace(", ", "").
                 replace("\"", "").
                 split(",")
            }). 
            filter (x=>{ x(1) != "YEAR" }). 
            map(x=>{
                (x(FLT_NO+1)+"_"+x(DATE+1)+"_"+x(SC_DEP_TIME+1))  -> (
                (if (x(DOW+1).isEmpty) 0.0 else x(DOW+1).toDouble)  + "::" +
                (if (x(ORIGIN+1) == "NA") 0.0 else x(ORIGIN+1).toDouble) + "::" +
                (if (x(DESTINATION+1) == "NA") 0.0 else x(DESTINATION+1).toDouble) + "::" + 
                (if (x(DISTANCE+1) == "NA") 0.0 else x(DISTANCE+1).toDouble) + "::" +
                (if (x(ARR_DELAY+1) == "NA") 0.0 else (if (x(ARR_DELAY+1).toDouble > 0) 1 else 0)) )   // 1 -> delayed otherwise 0
                }) 

        
        // validation DataSet
        val validateDataSet = sc.textFile(args(2)). 
            map (x => {
                 x.replace(", ", "").
                 replace("\"", "").
                 split(",")
            }).
            filter (x=>{ x(1) != "YEAR" }).
            map(x => { 
                        x(0) -> (if (x(1) == "TRUE") 1 else 0)
                    })


        
        // creating the feature vectors and labelled points for test and traning datasets
        val trainingData = trainingDataSet.map(x => {
                                            val (k,v) = x;
                                            val arrv = v.split("::").map(_.toDouble)
                                            LabeledPoint(arrv.last,Vectors.dense(arrv.init))
                            })

        val testData = testDataSet.map(x => {
                                            val (k,v) = x;
                                            val arrv = v.split("::").map(_.toDouble)
                                            k -> LabeledPoint(arrv.last,Vectors.dense(arrv.init))
                            })

        
        // parameters for setting up the Random Forest Model
		val algorithm = Algo.Classification
		val impurity = Gini
		val maximumDepth = 3
		val treeCount = 200
		val featureSubsetStrategy = "sqrt"
		val seed = 5043

        // the model for the RandomForest classification
		val model = RandomForest.trainClassifier(trainingData, new Strategy(algorithm,impurity, maximumDepth), treeCount, featureSubsetStrategy, seed)

        // the final predicted outcome with all the required fields for the farther analysis
        val labeledPredictions = testData.map ( x => {
                                                val (key,labeledPoint) = x
        										val predictions = model.predict(labeledPoint.features)
                                                // (original_label, predicted_label , key , featureVectors)
    											(labeledPoint.label, predictions,key,labeledPoint.features)
											 })


        // the finalResult to be written out, (as per the question)
        val finalres = labeledPredictions.map ( x => {
                                                val (original_label,predictions,key,features) = x
                                                var logical = "FALSE"
                                                if (predictions == 1.0){
                                                    logical = "TRUE"
                                                }
                                                key -> logical
                                            } )

        // save the output to file
        finalres.saveAsTextFile(args(3))

        // we join the list of predicted results with the validate file for the performance evaluation
        val performanceTestDataSet = finalres.join(validateDataSet).
                                map (x => {
                                    val (k,v) = x
                                    val (expected_label,predicted_label) = v
                                    (expected_label.toDouble, predicted_label.toDouble)
                                    })
         
         // preparing model for performance evaluation                         
		val evaluationMetrics = new MulticlassMetrics(performanceTestDataSet.map(x => (x._2, x._1)))


        println ("PRECISION  is  " + evaluationMetrics.precision)
        println (evaluationMetrics.confusionMatrix)

        // The following is the commented out code for saving the confusion matrix to file
        /*
        val confuMat = evaluationMetrics.confusionMatrix
        //val ele01 = confuMat.index(0,1)
        //val ele10 = confuMat.apply(1,0)
        val localArray: Array[Double] = confuMat.toArray//.grouped(confuMat.numCols).toList
        //val ele01 = localArray(2)           
        //val ele10 = localArray(1)
        //localArray(1) = ele01
        //localArray(2) = ele10
        val localMatrix = localArray.grouped(confuMat.numCols).toList                   
        val lines: List[String] = localMatrix.map(line => line.mkString(" "))
        val printable_confusion_matrix = sc.parallelize(lines)
        
        val calculatedPrecision = evaluationMetrics.precision//.collect()
		
        //calculatedPrecision.save("PPP.txt")
        //sc.parallelize(calculatedPrecision).saveAsTextFile("out_stats")
        */



        // Shut down Spark, avoid errors.
        sc.stop()
    }
}