# Author :: Sarita , Sharmo
# This file starts up the sorting algo on the nodes
# $1 >>>> input bucket name
# $2 >>>> Sorting column
# $3 >>>> Total number of clusters
# $4 >>>> Individual Cluster Number
#!/bin/sh
nohup java -cp node.jar FileChunkLoader $1 input*.txt &
mkdir climate
mv *.gz climate
nohup java -cp node.jar SortNode $2 climate/ $3 $4 dnsFile.txt &
