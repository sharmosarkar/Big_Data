# Author :: Sharmo , Sarita
# This is used for splitting the data for the load distribution

#!/bin/bash
inputBucket=$1
totalIPs=`wc -w /tmp/hostEntry.txt|awk -F " " '{print $1}'`
slaveIPs=$(( totalIPs - 1 ))
#get summary of the input bucket
/usr/local/bin/aws s3 ls $inputBucket --summarize > /tmp/dataanalysis.txt
# getting total size of the bucket (size of all files)
awk 'BEGIN {FS = ":"};{print $2}' /tmp/dataanalysis.txt |tail -2|awk '{print $1}' > /tmp/summarizefiles.txt
totalFiles=`head -1 /tmp/summarizefiles.txt`
totalSize=`tail -1 /tmp/summarizefiles.txt`

head -n -3 /tmp/dataanalysis.txt|awk -F" " '{print $3 , " " , $4}' > /tmp/file_size.txt
#sorting
sort -n /tmp/file_size.txt > /tmp/file_size_list.txt
rm -f /tmp/file_size.txt

splits=0
if [ "$slaveIPs" -lt "$totalFiles" ]
then
	splits=$slaveIPs;
else
	splits=$totalFiles;
fi

chunkSize=$((totalSize / splits));

prefix=/tmp/
outputFileNumber=0
fileSuffix=".txt"
totalMapFiles=1
while IFS='' read -r line || [ -n "$line" ]; do
    index=0

    for part in $line
	do
		# first column on each line of is the size of the file
		if [ "$index" = 0 ]
		then
			size=$part;
			counter=$((counter + size)) ;
			#echo $counter
		fi
		# second column on each line of is the name of the file
		if [ "$index" = 1 ]
		then
			name=$part;
			# counter keeps track of the current accumulated chunksize
			if [ "$counter" -le "$chunkSize" ]
	    	then
	    		echo $name >> $prefix$outputFileNumber$fileSuffix;
	    	else
	    		counter=0;
	    		outputFileNumber=$(( outputFileNumber + 1 ));
	    		echo $name >> $prefix$outputFileNumber$fileSuffix;
			totalMapFiles=$(( totalMapFiles + 1));
	    	fi
		fi
	    index=$((index + 1)) ;
	done
done < "/tmp/file_size_list.txt"
echo $totalMapFiles
