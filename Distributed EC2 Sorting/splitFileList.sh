# Author :: Sharmo , Sarita

#!/bin/bash

inputBucket = $2

#get summary of the input bucket
aws s3 ls $inputBucket --summarize > dataanalysis 

# getting total size of the bucket (size of all files)
tot=`awk 'BEGIN {FS = ":"};{print $2}' dataanalysis |tail -1|awk '{print $1}'`

# file_size_list.txt is the modified file from the summary file in format { size filename }
awk -F" " '{print $3 , " " , $4}' dataanalysis > file_size_list.txt

splits=$1

chunkSize=$((tot / splits))
outputFileNumber=0
filePrefix="input"
fileSuffix=".txt"

while IFS='' read -r line || [ -n "$line" ]; do
    index=0

    for part in $line
	do
		# first column on each line of is the size of the file
		if [ "$index" = 0 ]
		then
			size=$part;
			counter=$((counter + size)) ;
		fi
		# second column on each line of is the name of the file
		if [ "$index" = 1 ]
		then
			name=$part;
			# counter keeps track of the current accumulated chunksize
			if [ "$counter" -lt "$chunkSize" ]
	    	then
	    		echo $name >> $filePrefix$outputFileNumber$fileSuffix;
	    	else
	    		counter=0;
	    		outputFileNumber=$(( outputFileNumber + 1 ));
	    		echo $name >> $filePrefix$outputFileNumber$fileSuffix;
	    	fi
		fi
	    index=$((index + 1)) ;
	done
done < "file_size_list.txt"


