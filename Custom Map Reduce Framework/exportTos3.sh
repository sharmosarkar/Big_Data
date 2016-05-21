# Author :: Sharmo Sarita 
# This script updates exports the Reducer output from master to the s3 bucket

outputBucket=$1

# making an output folder to sync to S3
rm -rf /tmp/output
mkdir /tmp/output
mv /tmp/part-* /tmp/output

# move output file to s3 output bucket
aws s3 sync /tmp/output/ $outputBucket
