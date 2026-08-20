#!/bin/bash
set -e

cd "$(dirname "$0")"

# Sem isso o deploy usa o profile default, que costuma estar com token expirado.
export AWS_PROFILE="${AWS_PROFILE:-personal}"

# Regiao fixa, NAO herdada do ambiente. Toda a stack (App Runner, RDS, ECR) vive
# em us-east-1, e um AWS_REGION exportado no shell ja causou deploy duplicado em
# us-west-2. Nao troque por ${AWS_REGION:-...}.
export AWS_REGION="us-east-1"
export AWS_DEFAULT_REGION="us-east-1"

echo "Building..."
mvn clean package -q

echo "Deploying..."
FIREBASE_JSON=$(cat ../../src/main/resources/ipredencao-manager-api-firebase-adminsdk.json | jq -c)

sam deploy \
  --template-file template.yaml \
  --stack-name ipredencao-firebase-proxy \
  --capabilities CAPABILITY_IAM \
  --parameter-overrides "FirebaseServiceAccountKey='${FIREBASE_JSON}'" \
  --resolve-s3 \
  --region "${AWS_REGION}" \
  --no-confirm-changeset \
  --no-fail-on-empty-changeset

echo ""
echo "FIREBASE_LAMBDA_NAME (App Runner):"
aws cloudformation describe-stacks \
  --stack-name ipredencao-firebase-proxy \
  --query 'Stacks[0].Outputs[?OutputKey==`QualifiedFunctionName`].OutputValue' \
  --output text

echo ""
echo "SnapStart (leva alguns minutos para ficar On):"
aws lambda get-function-configuration \
  --function-name ipredencao-firebase-proxy:live \
  --query 'SnapStart' \
  --no-cli-pager
