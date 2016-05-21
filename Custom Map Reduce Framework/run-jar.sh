# Author: Sarita,Sharmo
# This code starts a java jar on the certain cluster
#!/bin/sh
nohup java -cp /tmp/$1 $2 $3 $4 &

