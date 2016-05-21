#!/bin/bash
# Author : Sharmo , Sarita

# This file takes a bunch of user specific parameters and configures the scripts accordingly

keyValuePath={}
keyValueFileName={}
securityGroup={}

keyWithPath=$keyValuePath$keyValueFileName
keyValueName=${keyValueFileName%.*}
sed -i "s|KEY-VALUE|${keyWithPath}|g" my-mapreduce

sed -i "s|KEY-VALUE|${keyValueFileName}|g" dataShipper.sh

sed -i "s|KEY-VALUE|${keyValueFileName}|g" createReducerInput

sed -i "s|KEY-VALUE-WITH-PATH|${keyWithPath}|g" start-cluster
sed -i "s|KEY-VALUE-NAME|${keyValueName}|g" start-cluster
sed -i "s|SECURITY-GROUP|${securityGroup}|g" start-cluster

sed -i "s|KEY-VALUE|${keyValueFileName}|g" startSlaves.sh