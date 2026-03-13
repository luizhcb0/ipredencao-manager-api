#!/bin/bash
set -e

cd "$(dirname "$0")"

echo "Building..."
mvn clean package -q

echo "Deploying..."
FIREBASE_JSON=$(cat ../../src/main/resources/ipredencao-manager-api-firebase-adminsdk.json | jq -c)

sam deploy \
  --template-file template.yaml \
  --stack-name ipredencao-firebase-proxy \
  --capabilities CAPABILITY_IAM \
  --parameter-overrides "FirebaseServiceAccountKey='${FIREBASE_JSON}'" \
  --no-confirm-changeset \
  --no-fail-on-empty-changeset

echo ""
echo "API URL:"
aws cloudformation describe-stacks \
  --stack-name ipredencao-firebase-proxy \
  --query 'Stacks[0].Outputs[?OutputKey==`ApiUrl`].OutputValue' \
  --output text
