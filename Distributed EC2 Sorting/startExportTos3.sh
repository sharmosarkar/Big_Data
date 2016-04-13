# Author :: Sharmodeep Sarita Yogiraj Ashish

keyValue = {enter your key value pair file (.pem file) with absolute path}
ip = `cat out.txt`
outputBucket = $1
# for all the clustetrs
for line in $ip
do
	echo $line
	# move all the config file and the execution script for output shipping to the cluster
	scp -i $keyValue exportTos3.sh config ec2-user@$line:/tmp/.
	sleep 3
	# execute the s3 data shipping script
	ssh -i $keyValue ec2-user@$line "/tmp/exportTos3.sh $outputBucket < /dev/null > /tmp/mylogfilenode 2>&1 &"
	sleep 1m
done