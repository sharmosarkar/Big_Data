# Author : Sharmo and Sarita
# This is the common make file for both Parts of the assignment, change the mentioned variables to get the appropriate file running
variable_bucket_name = ${bucket_name}  # please replace variable_bucket_name with your AWS bucket name
outputDir = ${out}
inputTestDir = ${ipTestDir}
inputTrainDir =${ipTrainDir}
inputValidateDir =${ipValidateDir}
requestFile = ${rFile}
MAINCLASS = ${MAINCLASS}
JARFILE = ${MY_JAR}
CLASS = ${MY_CLASS}
WAITTIME =${WAIT_TIME}


jar:
	sbt package
	cp target/scala-*/*.jar ${MYJAR}

runpred:
	rm -rf out
	rm -rf out_stats
	sbt "run ${inputTrainDir} ${inputTestDir} ${inputValidateDir} ${out}"
runrout:
	rm -rf out
	rm -rf out_stats
	sbt "run ${inputTrainDir} ${inputTestDir} ${requestFile} ${inputValidateDir} ${out}"
	
clean:
	rm -rf out derby.log metastore_db project target
	rm -rf *.class *.jar
permission:
	sudo chmod 777 ${MYJAR}

awspred:jar
	aws s3 cp ${MYJAR} s3://${variable_bucket_name}/job/
	aws s3 cp ./${inputTrainDir}/*  s3://${variable_bucket_name}/input --recursive
	aws s3 cp ./${inputTestDir}/*  s3://${variable_bucket_name}/input --recursive
	aws s3 cp ./${inputValidateDir}/*  s3://${variable_bucket_name}/input --recursive
	aws s3 rm s3://${variable_bucket_name}/output --recursive
	aws emr create-cluster --applications Name=Hadoop Name=Spark --ec2-attributes '{"InstanceProfile":"EMR_EC2_DefaultRole"}' --service-role EMR_DefaultRole --enable-debugging --release-label emr-4.3.0 --log-uri s3://${variable_bucket_name}/logs --steps '[{"Args":["spark-submit","--deploy-mode","cluster","--class","'${MAIN_CLASS}'","s3://${variable_bucket_name}/${MYJAR}","s3://${variable_bucket_name}/input/${inputTrainDir}","s3://${variable_bucket_name}/input/${inputTestDir}","s3://${variable_bucket_name}/input/${inputValidateDir}","s3://${variable_bucket_name}/${out}"],"Type":"CUSTOM_JAR","ActionOnFailure":"CONTINUE","Jar":"command-runner.jar","Properties":"","Name":"'${MYCLASS}'"}]'  --name 'Spark cluster' --instance-groups '[{"InstanceCount":1,"InstanceGroupType":"MASTER","InstanceType":"m1.medium","Name":"Master Instance Group"},{"InstanceCount":4,"InstanceGroupType":"CORE","InstanceType":"m1.medium","Name":"Core Instance Group"}]' --configurations '[{"Classification":"spark","Properties":{"maximizeResourceAllocation":"true"},"Configurations":[]}]' --auto-terminate --region us-west-2

awsrout:jar     
	        aws s3 cp ${MYJAR} s3://${variable_bucket_name}/job/
		aws s3 cp ./${inputTrainDir}/*  s3://${variable_bucket_name}/input --recursive
		aws s3 cp ./${inputTestDir}/*  s3://${variable_bucket_name}/input --recursive
		aws s3 cp ./${inputValidateDir}/*  s3://${variable_bucket_name}/input --recursive
		aws s3 cp ./${requestFile}/*  s3://${variable_bucket_name}/input --recursive
	        aws s3 rm s3://${variable_bucket_name}/output --recursive
	aws emr create-cluster --applications Name=Hadoop Name=Spark --ec2-attributes '{"InstanceProfile":"EMR_EC2_DefaultRole"}' --service-role EMR_DefaultRole --enable-debugging --release-label emr-4.3.0 --log-uri s3://${variable_bucket_name}/logs --steps '[{"Args":["spark-submit","--deploy-mode","cluster","--class","'${MAIN_CLASS}'","s3://${variable_bucket_name}/${MYJAR}","s3://${variable_bucket_name}/input/${inputTrainDir}","s3://${variable_bucket_name}/input/${inputTestDir}","s3://${variable_bucket_name}/input/${requestFile}","s3://${variable_bucket_name}/input/${inputValidateDir}","s3://${variable_bucket_name}/${out}"],"Type":"CUSTOM_JAR","ActionOnFailure":"CONTINUE","Jar":"command-runner.jar","Properties":"","Name":"'${MYCLASS}'"}]'  --name 'Spark cluster' --instance-groups '[{"InstanceCount":1,"InstanceGroupType":"MASTER","InstanceType":"m1.medium","Name":"Master Instance Group"},{"InstanceCount":4,"InstanceGroupType":"CORE","InstanceType":"m1.medium","Name":"Core Instance Group"}]' --configurations '[{"Classification":"spark","Properties":{"maximizeResourceAllocation":"true"},"Configurations":[]}]' --auto-terminate --region us-west-2


output:
	sleep 40S
	aws s3 cp s3://${variable_bucket_name}/output/part-00000 output

report:
	Rscript MyPredReport.R
	Rscript MyRoutingReport.R
