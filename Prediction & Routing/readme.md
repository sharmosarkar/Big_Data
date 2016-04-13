SCALA Implementation on  SPARK FRAMEWORK

Author : Sharmo and Sarita
Team: Sarita and Sharmo
Date: 03/19/2016

Assignment 7 : Prediction & Routing
JAVA File - Flight_Data_Sanity_Check.java
SCALA Files
1) Prediction.scala
2) Routing.scala

 
Prerequisites:
 Install the required softwares
 - Oracle JDK version 1.7
 - Hadoop version 2.7.1
 - AWS CLI + Configuration
 - Spark Framework
 - knitr and Markdown required for the automated pdf report generation via makefile rule , run report

Make sure the following PATH variables are set before running:
- JAVA_HOME
- SPARK configuration
- sbt pre-setup, to load all the dependencies

Source Code Files :::
Prediction.scala (Business logic for predicton)
Routing.scala (Business logic for Routing)
Flight_Data_Sanity_Check (Data Cleansing)

Directions to Execute:

1. Extract Joshi_Sarkar_A7.tar.gz to folder Joshi_Sarkar_A7 and navigate inside.

2. Copy the folders specified in the problem statement i.e a6history, a6test, a6validate and a7history,a7test,a7validate,a7request file from s3://mrclassvitek. This can be done using the aws sync command or while running on cloud make sure that you set the input variables in the Makefile accordingly.

Go through the Makefile to see the sequence of rules to be executed.
############Common steps to be executed in order to generate the JAR file
• make jar
############To run the program locally
make runpred to run the part 1 i.e prediction or
make runrout to run the routing part for generating the itinerary and scoring
###########to clean i.e removing jar and classes
• make clean
###########For the program to run on AWS
• make awspred -> For Prediction
This takes 4 arguments to be placed as per the directions in the makefile
i.e train dataset, test dataset, validate dataset, output folder

• make awsrout -> For Routing
This takes 5 arguments to be placed as per the directions in the makefile
i.e train dataset, test dataset,request file, validate dataset, output folder

• make output
This is to copy the output file from AWS onto the local system

• make report
This is to automatically generate the pdf reports, i.e MyPredReport.pdf and MyRoutReport.pdf containing detailed analysis


ANALYSIS and OUTPUT:
This implementation is done using Spark Scala framework. The first part of the assignment uses the random forest algorithm to predict the number of delayed flights for the airline dataset. The resultant o f the scala code is the predicted result for the test data based on the model generated from the train data
Detailed analysis of the same is present in MyPredReport.pdf

The second part of the assignment is based on the concept of multiple linear regression, where in based on the received input we predict the value of ARR_DELAY and thus calculate the scoring as given in the assignment for connection/missed connection.
Then the scoring for missed connection is updated based on the lookup from validatefile

Based on the learrning from previous assignments and permission granted from Professor Nat Tuck, we went ahead with this efficient approach of designing the same in Spark with both Random Forest and Multiple Linear Regression approach. Detailed Analysis is provided in MyRoutReport.pdf

*******************************************************************************************************************************************************************
 We have used Random Forest Algorithm for predicting the ARR_DELAY field. If the ARR_DELAY was predicted as 1 then its a TRUE (predicting that the flight would be delayed) else FALSE (flight won't be delay
ed)
 We have used Spark's Mllib library for this assignment. The following have been used for the following fields as the feature vector the use in Random Forest :::
		+ DATE = 5
        + ACTUAL_DEP_TIME     
        + CRS_DEP_TIME    
        + FLT_NO 
        + ARR_DELAY 
        + DEP_DELAY     
        + DAY OF THE WEEK
        + AL_CARRIER 
        + ORIGIN 
        + DESTINATION 
        + DISTANCE 
        + CRS_ELAPSED_TIME   
        + DISANCE_GROUP

The following are the tunables used in the Random forest :::
		+ algorithm = Algo.Classification
		+ impurity = Gini
		+ maximumDepth = 3
		+ treeCount = 200
		+ featureSubsetStrategy = "sqrt"  

For calculating the Accuracy  and the Confusion Matrix we have used the MulticlassMetrics of the Mllib library

*** Results & Performance 
 Our Implementation yeilds an Accuracy of 53.9 % when compared to the Validation Dataset 
 Confusion Matrix ::
	0			1
 0 1405663		1062120
 1 1192605 		1353731
 
   True Positive : 1405663
   False Positive : 1062120
   True Negative : 1353731
   False Negtive : 1192605
   
   (0 is positive , 1 is negative)
   (0 is flight is delayed , 1 is flight is not delayed)
   
  Our code runs on local machine in 1.5 hour aprox and on EMR it takes 50mins (with 4 worker clusters) for the entire process of building the model and predicting the output.



REFERENCES :

www.scalacookbook
www.Mlib.com
apache.spark.com
Sanity file taken from the previous assignments
