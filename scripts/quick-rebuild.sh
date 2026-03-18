#!/bin/bash
set -e

# Script para rebuild rápido da imagem
# Uso: ./quick-rebuild.sh

echo "🔨 Fazendo rebuild da imagem..."
cd "$(dirname "$0")/.."

# Configurações
AWS_REGION="us-east-1"
AWS_ACCOUNT_ID="045935420308"
ECR_REPOSITORY="ipredencao-manager-api"
IMAGE_TAG="latest"
AWS_PROFILE="personal"

IMAGE_URI="${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${ECR_REPOSITORY}:${IMAGE_TAG}"

echo "📦 Building Docker image..."
docker buildx build --platform linux/amd64 -t ${IMAGE_URI} .

echo "🧹 Cleaning up dangling images..."
docker image prune -f

echo "✅ Build concluído!"
echo ""
echo "🚀 Deseja fazer push para ECR? (y/n)"
read -r response

if [[ "$response" =~ ^([yY][eE][sS]|[yY])$ ]]; then
    echo "🔐 Login no ECR..."
    aws ecr get-login-password --region ${AWS_REGION} --profile ${AWS_PROFILE} | \
        docker login --username AWS --password-stdin ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com
    
    echo "☁️  Pushing to ECR..."
    docker push ${IMAGE_URI}
    
    echo ""
    echo "✅ Push concluído!"
    echo "🌐 Imagem: ${IMAGE_URI}"
else
    echo "⏭️  Push cancelado"
fi

echo ""
echo "🧪 Para testar localmente:"
echo "   ./scripts/test-docker-local.sh"

