#!/bin/bash

: ${REGION=us-west-2}
if [ -z ${REGION} ]; then
        >&2 echo "Please define REGION with your AWS region, e.g. us-west-2"
        exit 1
fi

if [ -z ${VPC_ID} ]; then
        >&2 echo "You must provide a VPC id in env var VPC_ID"
        exit 1
fi

echo "VPC_ID=$VPC_ID"

STACKNAME=AwsDeleteStackS1

aws cloudformation create-stack --stack-name $STACKNAME \
	--region $REGION \
	--template-body file://create-stack.json --parameters \
	ParameterKey=vpcId,ParameterValue=$VPC_ID 

