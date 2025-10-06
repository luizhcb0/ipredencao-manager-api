#!/bin/bash
set -e

echo "🚀 Deploying to AWS App Runner..."

# Variáveis
AWS_REGION="us-east-1"
AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
ECR_REPO="${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/ipredencao-manager-api"
IMAGE_TAG=${1:-latest}

# Login ECR
echo "📦 Logging into ECR..."
aws ecr get-login-password --region ${AWS_REGION} | \
  docker login --username AWS --password-stdin ${ECR_REPO}

# Build
echo "🔨 Building Docker image..."
docker build -t ipredencao-manager-api:${IMAGE_TAG} .

# Tag
echo "🏷️  Tagging image..."
docker tag ipredencao-manager-api:${IMAGE_TAG} ${ECR_REPO}:${IMAGE_TAG}

# Push
echo "⬆️  Pushing to ECR..."
docker push ${ECR_REPO}:${IMAGE_TAG}

echo "✅ Deploy complete! App Runner will automatically update."
echo "📊 Check status: https://console.aws.amazon.com/apprunner"
