#@ Author -> sarita, Sharmo, Yogi, Ashish
# This script copies the actual background job to be executed in each EC2 instances and starts the program for sample sort across all the cluster nodes

keyValue = {enter your key value pair file (.pem file) with absolute path}
ip=`cat out.txt`
inputBucket=$1
sortColumn=$2
totalinstances=`wc -w out.txt |awk -F " " '{print $1}'`
counter=0
for line in $ip
do
	echo $line
	scp -i $keyValue execute.sh ec2-user@$line:/tmp/.
	sleep 3
	ssh -i $keyValue ec2-user@$line "/tmp/execute.sh $1 $2 $totalinstances $counter< /dev/null > /tmp/mylogfilenode 2>&1 &"
	sleep 10
	counter=$(( counter + 1 ));
done
