# This file helps to create a MyKeyPair and required authentication for the ssh from local to the EC2
aws ec2 create-key-pair --key-name MyKeyPair --query 'KeyMaterial' --output text > MyKeyPair.pem
chmod 400 MyKeyPair.pem
ssh-keygen -t rsa -f ~/.ssh/ec2 -b 4096
aws ec2 import-key-pair --key-name my-ec2-key --public-key-material "$(cat ~/.ssh/ec2.pub)"
curl ifconfig.me > myipconfig
ip=`cat myipconfig`
aws ec2 create-security-group --group-name MySecurityGroupSSHOnly --description "Inbound SSH only from my IP address"
aws ec2 authorize-security-group-ingress --group-name MySecurityGroupSSHOnly --cidr $ip/32 --protocol tcp --port 22

