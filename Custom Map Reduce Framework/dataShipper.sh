#Author : Sharmo , Sarita
# This is a bash utility that helps shipping the required data across EC2 Master/Slave instances
ip=`cat /tmp/hostEntry.txt|cut -d' ' -f2-`
path=/tmp
fileNumber=0
suffix=".txt"
keyValue=KEY-VALUE
for line in $ip
do
    rm -rf $path/filename
	mkdir $path/filename
	mv $path/$fileNumber$suffix $path/filename/.
	echo "moving " + $path/$fileNumber$suffix
	scp -i $path/$keyValue -o StrictHostKeyChecking=no -r $path/filename ec2-user@$line:/tmp/.
	sleep 3
	fileNumber=$(( fileNumber + 1))
done
echo 0
