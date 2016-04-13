# Author :: Sharmo Sarita 

outputBucket = $1

# Set up the AWS Configuration for an Instance
mkdir ~/.aws
cp config ~/.aws

# move output file to s3 output bucket
aws s3 cp output* $outputBucket