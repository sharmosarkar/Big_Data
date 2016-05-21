#Author : Sharmo , Sarita
#!/bin/bash
# This is the utility that starts the slave driver utility on an number of map tasks.
# The splitData utility decides and outputs the required number of map jobs to be executed in a 
# parallel environment
keyValue=/tmp/KEY-VALUE
hadoopFramework=CustomMapReduce.jar
inputBucket=$1
slavesIP=`cat /tmp/hostEntry.txt|cut -d' ' -f2-`
clusterId=0
for line in $slavesIP
do
    echo "starting SlaveDriver on " + $line
    ssh -i $keyValue -o StrictHostKeyChecking=no ec2-user@$line "/tmp/run-jar.sh $hadoopFramework custom.mr.SlaveDriver $1 $clusterId < /dev/null > /tmp/mylogfile 2>&1 &"
    clusterId=$(( clusterId + 1))
done
echo 0 
