import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.mllib.regression.LinearRegressionModel
import org.apache.spark.mllib.regression.LinearRegressionWithSGD
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf

object Routing{

        // time is in mins
    val CONNECTION_TIME_UPPER_lIMIT = 60
    val CONNECTION_TIME_LOWER_LIMIT = 30

	def main(args: Array[String]): Unit = {
    val conf = new SparkConf().setAppName("Routing !!").setMaster("local")
    val sc = new SparkContext(conf)
    	val DATE = 5
        //val AC_DEP_TIME = 30    // ??????
        val SC_DEP_TIME = 29    
        //val CANCEL_FLG = 47
        val FLT_NO = 10
        val TAIL_NO = 9
        val ARR_DELAY = 42 // Target ! Bingo!
        //val DEP_DELAY = 31      // ???????
        val DOW = 4
        val AL_CARRIER = 8
        val ORIGIN = 14//11//
        val ORIGIN_ID = 11
        val DESTINATION = 23//20//
        val DESTINATION_ID = 20
        val DISTANCE = 54 
        val CRS_ELAPSED_TIME = 50    // ??
        val DISTANCE_GROUP = 55
        //val ACTUAL_ELAPSED_TIME = 51
        val YEAR = 0
        val QUARTER = 1
        val MONTH = 2
        val DOM = 3
        val SC_ARR_TIME = 40
        /*        // ??
        val CRR_DELAY = 57
        val WTH_DELAY = 58
        val NAS_DELAY = 59
        val SEC_DELAY = 60
        val LATE_AR_DELAY =  61 */


        // Input   -----      ../Prediction-data/a6history
        val trainingDataSet = sc.textFile("input/a7history").//.collect().mkString.split("\\r?\\n") 
            map (x => {
                 x.replace(", ", "").
                 replace("\"", "").
                 split(",")
            }).
            filter (x=>{ x(0) != "YEAR" && !Flight_Data_Sanity_Check.sanityTest(x)}).//map(x=>{x(DEP_DELAY).mkString("")})/*.
            map(x=>{
                //(x(FLT_NO),x(DATE),x(SC_DEP_TIME))  -> (
                //x(AC_DEP_TIME).toDouble + "::" +
                //x(CANCEL_FLG).toDouble + "::" +
                //x(DEP_DELAY).toDouble + "::" + 
                (
                	x(SC_DEP_TIME) 	+ "::" +
                	x(SC_ARR_TIME)	+ "::" +
                	x(CRS_ELAPSED_TIME) + "::" +
                	x(DISTANCE_GROUP).toDouble + "::" +		
                	x(DISTANCE)+ "::" +
                	x(YEAR)	 + "::" +
                	x(QUARTER)	 + "::" +
                	x(MONTH)	 + "::" +
                	x(DOM)	 + "::" +
                	x(DOW) + "::" +
                	x(ORIGIN_ID)+ "::" +
                	x(DESTINATION_ID)+ "::" +
                	x(FLT_NO)+"::" +
                	x(ARR_DELAY) 	// 1 -> delayed otherwise 0
                )})


        //	../Prediction-data/a7test
        val testDataSet = sc.textFile("../Prediction-data/a7test").//.collect().mkString.split("\\r?\\n") 
            map (x => {
                 x.replace(", ", "|").
                 //replace("\"", "").
                 //replace("NA","").
                 split(",")
            }). //map(_.drop(0)).
            filter (x=>{ x.length > 29 }). // /*&& !Flight_Data_Sanity_Check.sanityTest(x)})*/.map(x=>{x.length}).
            map(x=>{
                    // //(x(FLT_NO),x(DATE),x(SC_DEP_TIME))  -> (
                    //x(AC_DEP_TIME).toDouble + "::" +
                    //x(CANCEL_FLG).toDouble + "::" +
                    //x(DEP_DELAY).toDouble + "::" + 
                    (
                    try {
                    x(SC_DEP_TIME).replace("\"", "").toDouble 	 + "::" +  		//1
                    x(SC_ARR_TIME).replace("\"", "").toDouble	+ "::" +		//2
                	x(CRS_ELAPSED_TIME).replace("\"", "").toDouble + "::" +	//3
                	x(DISTANCE_GROUP).replace("\"", "").toDouble + "::" +		//4
                	x(DISTANCE).replace("\"", "").toDouble+ "::" +				//5
                	x(YEAR).replace("\"", "").toDouble	 + "::" +				//6
                	x(QUARTER).replace("\"", "").toDouble	 + "::" +			//7
                	x(MONTH).replace("\"", "").toDouble	 + "::" +			//8
                	x(DOM).replace("\"", "").toDouble	 + "::" +				//9
                	x(DOW).replace("\"", "").toDouble + "::" +					//10
                	x(ORIGIN_ID).replace("\"", "").toDouble+ "::" +			//11
                	x(DESTINATION_ID).replace("\"", "").toDouble+ "::" +   	//12
                	x(FLT_NO).replace("\"", "").toDouble+"::" +			// till here we have our feature vector
                	x(ORIGIN).replace("\"", "")+ "::" +				//14
                	x(DESTINATION).replace("\"", "")+ "::" +			//15
                	x(TAIL_NO).replace("\"", "") + "::" +				//16
                	x(AL_CARRIER).replace("\"", "")//+ "::" +         	//17
                	//(if (x(ARR_DELAY).isEmpty) 0.0 else x(ARR_DELAY).toDouble)  // 1 -> delayed otherwise 0
                	}
                	catch {
                		case e : NumberFormatException => ("corrupt-record")
                	}
                    )}).
					filter ( x => { x != "corrupt-record"})

        
        // val validateDataSet = sc.textFile("input/a6validate").//.collect().mkString.split("\\r?\\n") 
        //     map (x => {
        //          x.replace(", ", "").
        //          replace("\"", "").
        //          //replace("NA","").
        //          split(",")
        //     }). //.map(_.drop(0)).


		val trainingData = trainingDataSet.map ( line => {
												//val (key,value) = line
												val parts = line.split("::").map(_.toDouble)												
												LabeledPoint(parts.last, Vectors.dense(parts.init))
												})//.cache()

		val testData = testDataSet.map ( line => {
												//val (key,value) = line
												val parts = line.split("::")
												val featureVectors = parts.take(13).map(_.toDouble)
												(line , LabeledPoint(0.0, Vectors.dense(featureVectors)) )
												})//.cache()


		// Building the model
		val numIterations = 1
		val stepSize = 0.09
		val model = LinearRegressionWithSGD.train(trainingData, numIterations, stepSize)

		// Evaluate model on training examples and compute training error
		val valuesAndPreds = testData.map ( line  => {
 											val (key, featureVectors) = line 
		  									val prediction = model.predict(featureVectors.features)
		  									//(featureVectors.label, prediction , key)
		  									// making the predicted ARR_DELAY as the last column of the testData
		  									(key + "::" + prediction)		  									
										})


		// making arrival and departure RDDs
		val arrFLT  = valuesAndPreds.map (line => {
												val parts = line.split("::")
												// key = year+"::"+month+"::"+day+"::"+airline+"::"+destinationId
												val key = parts(5)+"::"+parts(7)+"::"+parts(8)+"::"+parts(16)+"::"+parts(11)
												// value = origin+"::"+fltNum+"::"+CRSElapsedTime+"::"+arr_delay+"::"+CRS_Arr_time
												// NOTE :: for key we take destinationID , but for value we take origin(not originID)
												val value = parts(13)+"::"+parts(12)+"::"+parts(2)+"::"+parts(17)+"::"+parts(1)
												key -> value
										})

		val depFLT  = valuesAndPreds.map (line => {
												val parts = line.split("::")
												// key = year+"::"+month+"::"+day+"::"+airline+"::"+originId
												val key = parts(5)+"::"+parts(7)+"::"+parts(8)+"::"+parts(16)+"::"+parts(10)
												// value = destination+"::"+fltNum+"::"+CRSElapsedTime+"::"+arr_delay+"::"+CRS_Dep_time
												// NOTE :: for key we take originId , but for value we take destination(not destinationID)
												val value = parts(14)+"::"+parts(12)+"::"+parts(2)+"::"+parts(17)+"::"+parts(0)
												key -> value
										})


		
		// RDD for all the 2 hop flights which are all the potential connectons
		// 2 hop = A to b , b to C (A to C) ---- middleHopAirportID = b, origin = A, destination = C
		val connections = arrFLT.join(depFLT).
									map(line => {
										// key = year+"::"+month+"::"+day+"::"+airline+"::"+middleHopAirportID
										val (key, value) = line
										val keyParts = key.split("::")
										// arrFlights = origin+"::"+fltNum+"::"+CRSElapsedTime+"::"+arr_delay+"::"+CRS_Arr_time
										// depFlights = destination+"::"+fltNum+"::"+CRSElapsedTime+"::"+arr_delay+"::"+CRS_Dep_time
										val (arrFlights , depFlights) = value
										val arrParts = arrFlights.split("::")
										val depParts = depFlights.split("::")
										val a = toMins(depParts(4))
										val b = toMins(arrParts(4))
										val timeDiff =  a - b
										if (isConnected(timeDiff)) {
											// k = year+"::"+month+"::"+day+"::"+origin+"::"+destination
											val k = keyParts(0).dropRight(2).toInt+"::"+keyParts(1).dropRight(2).toInt+"::"+keyParts(2).dropRight(2).toInt+"::"+arrParts(0)+"::"+depParts(0)
											// totalDuration from origin to destination = 
											//								arrFlight(CRSElapsedTime + arr_delay) +
											//								depFlight (CRSElapsedTime + arr_delay)
											val totalDuration = arrParts(2).toDouble + arrParts(3).toDouble + depParts(2).toDouble + depParts(3).toDouble
											// v = originToMidHop_Flt+"::"+MidHopToDest_Flt+"::"+totalDuration
											val v = arrParts(1)+"::"+depParts(1)+"::"+totalDuration +"::"+a+"|||"+b+"|||"+timeDiff+"::"+key(3)
											key -> v
										}
										else
											"Not-Connection-Key" -> "Not-Connection-Value"

									}).
									// filter out all the records which are not connections (keep only the records which are valid connections)
									filter( line => {
											val (key,value) = line
											key != "Not-Connection-Key"
										})



		// Creating the request RDD 
		// Format of the request file ->>>  year,month,day,origin,destination,ignore
		val requests = sc.textFile("../Prediction-data/a7request").//.collect().mkString.split("\\r?\\n") 
            map (x => {
  				// the last column is to be ignored
                 val key = x.split(",").init.mkString("::")
                 // making a key-value pair (used for creating the itenary)
                 key -> "req"

            })/*.
            map (x => {
            		x.mkString(" ")
            	}) */


		// Creating the Missed connection RDD
		// Format of the missed connection data file ->>> year,month,day,origin,destination,fltNum,fltNum
		val missedConnections = sc.textFile("../Prediction-data/a7validate").//.collect().mkString.split("\\r?\\n") 
            map (x => {
                 x.split(",")
            })


        // for each request this list would hold a bunch of probable itineraries
        val itineraryList = requests.join(connections).
        							map( line => {
        									val (key , value) = line
        									val (request, connection) = value
        									key + " -->> " + connection
        								})


		connections.saveAsTextFile("out")
		requests.saveAsTextFile("out_stats")
		//	val MSE = valuesAndPreds.map{case(v, p) => math.pow((v - p), 2)}.mean()
		//  println("training Mean Squared Error = " + MSE)

// Save and load model
/*
model.save(sc, "myModelPath")
val sameModel = LinearRegressionModel.load(sc, "myModelPath") */

  }


	// check if a valid connection exists
    def isConnected(timeDiff : Int) : Boolean = {
        return ( timeDiff <= CONNECTION_TIME_UPPER_lIMIT && timeDiff >= CONNECTION_TIME_LOWER_LIMIT )
    }

    // converts hhmm or hmm to mins 
    def toMins(time : String) : Int = {
        //if (time.length < 2 || time.length > 4)
        //    return -1
        try {        
        if (time.length == 2)
            return  time.toInt
        if (time.length == 3)
            return (time.substring(0,1).toInt)*60 + (time.substring(1,3).toInt)
        else
            return (time.substring(0,2).toInt)*60 + (time.substring(2,4).toInt)  
        }
        catch {
        	  		case e : NumberFormatException => return 0
        }      
    }

}