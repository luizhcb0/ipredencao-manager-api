#!/bin/bash
set -e

# Deploy rápido do Lambda firebase-proxy (só código, sem CloudFormation).
#
# ⚠️  NÃO USE MAIS PARA PRODUÇÃO.
# O App Runner invoca o alias `:live`, e `update-function-code` só mexe em
# $LATEST: não publica versão nem move o alias. Usar este script deixa a
# produção silenciosamente rodando o código antigo (e sem SnapStart).
# Use ./deploy.sh (SAM), que publica versão e move o alias.
#
# Uso: ./quick-deploy.sh

cd "$(dirname "$0")"

AWS_REGION="us-east-1"
AWS_PROFILE="personal"
FUNCTION_NAME="ipredencao-firebase-proxy"
JAR="target/firebase-proxy-lambda-1.0.0.jar"

echo "🔨 Building..."
mvn clean package -q

echo "☁️  Atualizando código do Lambda ${FUNCTION_NAME}..."
aws lambda update-function-code \
  --function-name "${FUNCTION_NAME}" \
  --zip-file "fileb://${JAR}" \
  --region "${AWS_REGION}" \
  --profile "${AWS_PROFILE}" \
  --no-cli-pager

echo "✅ Deploy concluído!"
