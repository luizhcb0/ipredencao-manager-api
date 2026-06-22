#!/bin/bash
set -e

# Script para testar a imagem Docker localmente (apontando para DB local)
#
# Pre-requisito: sincronize os dados de prod antes com:
#   ./sync-prod-data.sh sync
#
# Uso: ./test-docker-local.sh

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
API_DIR="$(dirname "$SCRIPT_DIR")"
ENV_FILE="$SCRIPT_DIR/.env.local"

if [ -f "$ENV_FILE" ]; then
    source "$ENV_FILE"
fi

echo "Testando imagem Docker localmente..."
echo ""

# Verificar se o arquivo Firebase existe
FIREBASE_PATH="$API_DIR/src/main/resources/ipredencao-manager-api-firebase-adminsdk.json"
if [ ! -f "$FIREBASE_PATH" ]; then
    echo "ERRO: Arquivo Firebase nao encontrado: $FIREBASE_PATH"
    exit 1
fi

FIREBASE_MOUNT="/run/secrets/firebase-adminsdk.json"

echo "Firebase JSON: $FIREBASE_PATH (montado em $FIREBASE_MOUNT)"

# DB local (compose.yaml)
DB_HOST="host.docker.internal"
DB_PORT="54329"
DB_NAME="ipredencao_manager"
DB_USER="ipredencao_manager"
DB_PASSWORD="ipredencao_manager"

# Garantir que DB local e LocalStack estao rodando
docker compose -f "$API_DIR/compose.yaml" up -d --wait postgres localstack

IMAGE_NAME="045935420308.dkr.ecr.us-east-1.amazonaws.com/ipredencao-manager-api:latest"

echo "Rodando container..."
echo "  Imagem: $IMAGE_NAME"
echo "  DB: $DB_HOST:$DB_PORT (local)"
echo "  Porta: 8080"
echo ""

docker run --rm -p 8080:8080 \
  -v "$FIREBASE_PATH:$FIREBASE_MOUNT:ro" \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e SERVER_PORT=8080 \
  -e LIQUIBASE_ENABLED=true \
  -e DB_URL="jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}" \
  -e DB_USERNAME="${DB_USER}" \
  -e DB_PASSWORD="${DB_PASSWORD}" \
  -e FIREBASE_SERVICE_ACCOUNT_KEY_PATH="$FIREBASE_MOUNT" \
  -e FIREBASE_SERVICE_ACCOUNT_KEY_CONTENT="" \
  -e FIREBASE_LAMBDA_NAME="" \
  -e JWT_SECRET="test-secret-key-for-local-development-must-be-256-bits" \
  -e S3_BUCKET_NAME="ipredencao-storage" \
  -e CLOUD_AWS_S3_ENDPOINT="http://host.docker.internal:4566" \
  -e AWS_REGION="us-east-1" \
  -e AWS_ACCESS_KEY_ID="localstack" \
  -e AWS_SECRET_ACCESS_KEY="localstack" \
  -e ALLOWED_ORIGINS="http://localhost:3000,http://localhost:3001" \
  ${IMAGE_NAME}
