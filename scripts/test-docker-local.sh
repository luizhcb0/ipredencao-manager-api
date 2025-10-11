#!/bin/bash
set -e

# Script para testar a imagem Docker localmente
# Uso: ./test-docker-local.sh

echo "🐳 Testando imagem Docker localmente..."
echo ""

# Verificar se o arquivo Firebase existe
if [ ! -f "src/main/resources/ipredencao-manager-api-firebase-adminsdk.json" ]; then
    echo "❌ Arquivo Firebase não encontrado!"
    echo "   src/main/resources/ipredencao-manager-api-firebase-adminsdk.json"
    exit 1
fi

# Verificar se jq está instalado
if ! command -v jq &> /dev/null; then
    echo "⚠️  jq não encontrado, tentando sem compactação..."
    FIREBASE_JSON=$(cat src/main/resources/ipredencao-manager-api-firebase-adminsdk.json | tr -d '\n' | tr -d ' ')
else
    FIREBASE_JSON=$(cat src/main/resources/ipredencao-manager-api-firebase-adminsdk.json | jq -c)
fi

echo "✅ Firebase JSON carregado"
echo ""

# Configurações
IMAGE_NAME="045935420308.dkr.ecr.us-east-1.amazonaws.com/ipredencao-manager-api:latest"
# DB_HOST="host.docker.internal"  # Use seu IP no Linux
# DB_PORT="54329"                  # Porta mapeada do seu PostgreSQL local
DB_HOST="ipredencao-prod-db.cqon6ha0kufq.us-east-1.rds.amazonaws.com"
DB_PORT="5432"
DB_NAME="ipredencao_manager"
DB_USER="ipredencao_admin"
DB_PASSWORD="${DB_PASSWORD:-ipredencao_manager}"  # Passar via env var se necessário
AWS_ACCESS_KEY_ID="AKIAQVMPW4OKMX36BK5N"
AWS_SECRET_ACCESS_KEY="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"

echo "🚀 Rodando container..."
echo "   Imagem: $IMAGE_NAME"
echo "   Porta: 8080"
echo ""

# Verificar se as credenciais AWS estão configuradas
if [ -z "$AWS_ACCESS_KEY_ID" ] || [ -z "$AWS_SECRET_ACCESS_KEY" ]; then
    echo "⚠️  Credenciais AWS não encontradas!"
    echo "   Configure as variáveis de ambiente:"
    echo "   export AWS_ACCESS_KEY_ID='sua-access-key'"
    echo "   export AWS_SECRET_ACCESS_KEY='sua-secret-key'"
    echo ""
    echo "   Ou use as credenciais do IAM user ipredencao-apprunner"
    echo ""
    exit 1
fi

docker run --rm -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e SERVER_PORT=8080 \
  -e DB_URL="jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}" \
  -e DB_USERNAME="${DB_USER}" \
  -e DB_PASSWORD="${DB_PASSWORD}" \
  -e FIREBASE_SERVICE_ACCOUNT_KEY_CONTENT="${FIREBASE_JSON}" \
  -e JWT_SECRET="test-secret-key-for-local-development-must-be-256-bits" \
  -e S3_BUCKET_NAME="ipredencao-prod-storage" \
  -e AWS_REGION="us-east-1" \
  -e AWS_ACCESS_KEY_ID="${AWS_ACCESS_KEY_ID}" \
  -e AWS_SECRET_ACCESS_KEY="${AWS_SECRET_ACCESS_KEY}" \
  -e ALLOWED_ORIGINS="http://localhost:3000,http://localhost:3001" \
  ${IMAGE_NAME}

