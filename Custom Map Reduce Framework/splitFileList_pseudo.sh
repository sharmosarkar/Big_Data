# Author :: Sharmo , Sarita
# This is used for load distribution in the pseudo mode

#!/bin/bash
# This is of the form /xyz/abc/
inputBucket=$1
# file_size_list.txt is the modified file from the summary file in format { size filename }
ls -l -S -r $inputBucket* > fileContent
awk -F " " '{print $5 " " $9}' fileContent > inputSummary
# 10MB is the max optimal size that a thread should handle (but we don't break files if a file is larger that 12MB)
chunkSize=10485760
prefix=input
outputFolderNumber=0
currFolder=$inputBucket$prefix$outputFolderNumber
mkdir $currFolder
#fileSuffix=".txt"
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
	    		mv $name $currFolder/
	    	else
	    		counter=0;
	    		outputFolderNumber=$(( outputFolderNumber + 1 ));
	    		currFolder=$inputBucket$prefix$outputFolderNumber
	    		mkdir $currFolder
	    		mv $name $currFolder/
			totalMapFiles=$(( totalMapFiles + 1));
	    	fi
		fi
	    index=$((index + 1)) ;
	done
done < "inputSummary"
echo $totalMapFiles
