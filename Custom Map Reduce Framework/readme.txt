Author : Sharmodeep,Sarita,Ashish,Yogiraj
FINAL PROJECT: -> Build a custom mapreduce
Date: 04/24/2016

PRE-REQUISITE:
Oracle JDK version 1.8
AWS CLI + Configuration
Folder structure on local host
Files to be included before running make file 
How folder structure is created on EC2 – master and slave nodes
AWS configuration

JAVA Files - 

Package-> custom.mr
Configuration.java 
FileInputFormat.java
FileOutputFormat.java
IntWritable.java 
Job.java
LocalJobClient.java
Mapper.java
PartitionHelper.java
Path.java
Reducer.java
ReducerDriver.java
SlaveDriver.java 
Text.java

Package-> custom.mr.utils
FileChunkLoader.java
FileIO.java
ProcessUtils.java
RunShellScript.java
TextSocket.java	

Bash Scripts -

start-cluster
stop-cluster
my-mapreduce
makessh
installjava.sh
run-jar.sh
config
awsSetup.sh 
exportTos3.sh
mergeMapperOutput
createReducerInput
startSlaves.sh
dataShipper.sh 
splitFileList.sh


Note : For below bash scripts , user have to individually enter the key value pair for automation
	startsort.sh
	ExportTos3.sh
	dataShipper.sh
	start-cluster.sh
	userConfig.sh


Makefile - for AWS configuration and jar file creation
STEPS TO EXECUTE
make setup (This to do the required pre configuration for JAVA and AWS configuration) 
make jar (To create the required custom hadoop jar file as per test suite)
make EMR (Runs the given jar in AWS EMR Mode) 
make pseudo (Runs the given jar in pseudo distributed mode using JAVA multithreading)


Make sure the following PATH variables are set before running:
- JAVA_HOME
- Also make sure that the aws configuration is done with required ssh setup done. Follow the script make ssh if the configuration is required
- Enter all the required credentials in the userConfig.sh and run the script

Directions to Execute:

Extract Awati_joshi_kalbhor_Sarkar_Final.tar.gz to folder Awati_joshi_kalbhor_Sarkar_Final and navigate inside.
This folder contains the list of all the java files and the scripts along with the rule required for the entire implementation
The entire architecture is shown in the file Custom_Map_Reduce_Implementation.pdf
REPORT : Custom_Map_Reduce_Implementation.pdf
Required output file -> in given EC2 bucket retrieved from the EC2 instance

To run the entire flow of the code, follow the below instructions:
TO STEP UP CLUSTER ->
start-cluster {arg}
This script creates the required number of EC2 instances and saves the list of instances onto a local file,hostEntry.txt
Go through the Makefile to see the sequence of rules to be executed.

### AWS Configuration File 
############Common steps to be executed in order to generate the JAR file
./start-cluster 8
./my-mapreduce WordCount.jar s3://foo/bar/input.txt s3://foo/bar/output
./stop-cluster
NOTE: Final output stored in given s3 bucket
###############################################################################
Working of the approach is mentioned below. Details of the same can be found in our report
###############################################################################

CONCLUSION :
The above describes our implementation of a Custom Map Reduce framework. This is a small implementation of the actual Hadoop Framework. For our implementation, we have taken some liberties and deviated from the actual Hadoop implementaion for many of the functionalities. This has made our implementaion extremely lucid and it also works faster than traditional Hadoop for our test cases. We can attribute our enhanced speed to the following factors ::
	
	(a) For load balancing amongst the various available slave clusters ,we split the input data depending on the number of slaves and hence spawns equal number of mappers which executes in parallel. Traditional hadoop system load balances by reading the actual data from the files and then spliting them up into chunks of 128MB. So, for smaller inputs(less than 128MB), there is only one mapper that's spawned. But in our implementaion, we split the data to the available slave clusters (mappers) based on the summary of the input file list. The actual file I/O is done in parallel by the slave clusters (mappers). This makes use of all the available clusters (started in start-cluster script) and hence adds on to our speed.
	
	(b) Mapper and Reducer outputs are transferred across EC2 nodes using SCP which is relatively faster than sending data using TCP or similar protocols. This is based on our observation from our previous assignmnment (EC2 Sorting) where we had used TCP to transfer data across EC2 nodes.
	
	(c) The Mapper output is created as files based on unique keys, ie. one file per key, whose name is a key and the values against that key is the content of the file. This method has proved to be extremly efficient during the merge and sort phase of the master node. Our implementaion allowed us to Linux's inbuilt sort command. Otherwise we would have to read a huge file(s) (mapper output(s)) in-memory and then sort by keys. This would have been extremly slow during sorting and could also result in an "out of heap memory" error during the merge phase. The technique of writing the mapper output in files allowed us to use Bash Scripts to merge files due to which there is no chance of our framework to get an "out of heap memory" error durinb this phase.
	
	(d) The actual shuffling of the mapper outputs for the reducers (reducer inputs) are done by Bash Scripts. This greatly adds on to the speed of the framework, against an equivalent java implementaion of the partitioning.
	

There are a few trade-offs of our implementaion. One of them is generalization. Our system handles only the test cases as mentioned in the report above. As our mapper output consists of files with Mapper output key as filenames there may is a probability that we might miss a few keys if the keys contain characters forbidden in filenames (\ / : * ? " < > |). But it is extremely rare that these 9 special characters would appear in the Mapper output keys hence the impact of this design decision proved to have more pros than cons. Overall this Custom Map Reduce implementaion works quite well and efficiently for our specific test cases.

###############################################################################

REFERENCES :

www.stackoverflow.com
Class demo programs for the concept of join and the code snippet for Demo.scala and DemoReducer.java
http://tldp.org/HOWTO/Bash-Prog-Intro-HOWTO.html
http://www.tldp.org/LDP/abs/html/wrapper.html
http://linuxguru.org
http://parallelcomp.uw.hu/ch09lev1sec5.html
http://www3.cs.stonybrook.edu/~rezaul/Spring-2012/CSE613/CSE613-lecture-16.pdf
http://www.umiacs.umd.edu/research/EXPAR/papers/spaa96/node7.html

###############################################################################

WORKING:
Role of Master Node:
Input jar (eg. WordCount.jar) runs on master node. The main class that implements functionality of Master Node is Job class. When the control comes to Job.waitForCompletion() , following process takes place:

1)	Spawns n Map Jobs
a) DataSplitting (add):
     We make connection to S3 bucket which is given as input to the program. We process the data by creating list of files as an input to mapper which are of approximately equal size. The outcome of this script is 0.txt , 1.txt .. and depends on number of mappers set by the start cluster argument. So if ./start cluster 8 , total of 7 mappers would be spawned and the functionality attempts to make 7 list of files to be downloaded by the mapper function on SlaveDriver.

b) Datashipper.sh (add): 
    This script transfers the files generated by Datasplitting to respective mapper. Like 0.txt is send to mapper with cluster id 0.

c) We run startSlave.sh that will start the Slaves in listening mode. Basically main function of SlaveDriver is called which makes slave nodes in listening mode
d) We send a request message to all SlaveDriver using TCP socket. This message contains start mapper tag along with mapper and reducer class name. The server at SlaveDriver can identify this request using the tag and start the given mapper class.
e) Master will now go in listening mode till it receives success acknowledgement from all mappers. 
f) Once all Mappers send their respective output to Master Node through SCP, the mapper outputs are accumulated in /tmp/allMapperOutput/ folder.
g) Master Node sends a kill request to all SlaveDriver which terminates Mapper process on all EC2 instances. At this point we have intermediate output from all the Mappers on Master Node. We need to shuffle and sort this intermediate Mapper outputs. Following is the shuffle process
h) Shuffling (Creating Reducer Input): 
 In this phase we process Mapper outputs and create reducer input. Following is the process:
1)	mergeMapperOutput (add):
 (input : allMapperoutput/ -> output :Summary(contains keys), 
  2) createPartitions: 
                   It takes summary and number of reducers as an input. Its partitions keys according to number of reducers using hash function. Accordingly, r0, r1 files are created which contains the keys list respective reducer0, reducer1 .. will get as an input.
 3) CreateReducerInput (add) (r0 is moved to r0 Folder and ships to respective EC2 instance and then runs reducer driver against it): 
i) Master Nodes goes into listening mode and waits for all Reducer success acknowledgements. 
j) For each acknowledgment received from Reducer, Master node receives part-* folder(reducer output) through SCP.
k) export.sh exports all part folders received to S3 bucket received as an output path

 Role of Slave Driver (Mapper Functionality)
a) Slave Driver is instantiated by Master node by running runslave.sh script. Each instance has cluster id required for the processing.
b) Downloading data from S3 bucket:
FileChunkLoader reads the 0.txt or 1.txt etc files present at that EC2 instance. This text file contains list of files to be downloaded from S3 bucket. This function creates connection with S3 bucket and download files from S3 bucket to /tmp/input folder
c) Starting Mapper using fetchMapperInput() : 
For each file entry in the /tmp/input folder , map() function is called. The implementation of the map() is given by Jar provided as input (WordCount.jar). 
d) context.write() : 
	This function process each key . It replaces “[:*?/<>|]” with “” for each key. Creates file with key name at location /tmp/output(clusterId) and writes value associated with that key in that file
e) Output of Mapper: 
 	At the end of all map() calls, we get output of the Mapper phase at location /tmp/output folder, which contains files with key name and values of that key inside respective key file. The output is sorted by key since we are creating files by key names.
f) SlaveDriver than SCP its output to Master Node and goes into listening mode and waits for Kill message from Master node.
g) Once SlaveDriver receives kill message from Master Node, Mapper terminates
Role of Reducer Driver (Reducer Functionality)
This is instantiated by createReducerInput script which runs on Master Node. The input to Reducer is made available by Master node. 
a)	reduce() of the provided jar (WordCount jar) is called on each file that is available in the input folder. 
b)	Context.write() : Writes output to location /tmp/part-clusterid /ouput.txt
c)	Once all reduce calls are done on each key, Reducer Driver SCP its output to Master node and exits
