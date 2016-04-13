# Author :: Sharmo , Sarita 
# Ships the file lists to each of the clusters

keyValue = {enter your key value pair file (.pem file) with absolute path}

ip=`cat out.txt`
fileNumber=0
prefix="input"
suffix=".txt"
for line in $ip
do
	scp -i $keyValue $prefix$fileNumber$suffix ec2-user@$line:/tmp/.
	sleep 3
	fileNumber=$(( fileNumber + 1))
done
