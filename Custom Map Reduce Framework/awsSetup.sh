# Author :: Sharmo Sarita 

# Set up the AWS Configuration for an Instance
cd /tmp && curl "https://s3.amazonaws.com/aws-cli/awscli-bundle.zip" -o "awscli-bundle.zip" && unzip awscli-bundle.zip
sudo /tmp/awscli-bundle/install -i /usr/local/aws -b /usr/local/bin/aws > awslog
rm -rf ~/.aws
mkdir ~/.aws
cp /tmp/config ~/.aws/.

