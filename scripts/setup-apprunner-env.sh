#!/bin/bash

# Script para configurar variáveis de ambiente no AWS App Runner
# Usage: ./setup-apprunner-env.sh <service-arn>

set -e

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Função para imprimir mensagens coloridas
print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_info() {
    echo -e "${GREEN}ℹ️  $1${NC}"
}

# Verificar se o service ARN foi fornecido
if [ -z "$1" ]; then
    print_error "Por favor, forneça o ARN do serviço App Runner"
    echo "Usage: $0 <service-arn>"
    exit 1
fi

SERVICE_ARN=$1

echo "=========================================="
echo "  AWS App Runner - Setup de Variáveis"
echo "=========================================="
echo ""

# Verificar se AWS CLI está instalado
if ! command -v aws &> /dev/null; then
    print_error "AWS CLI não está instalado"
    exit 1
fi

print_info "Serviço: $SERVICE_ARN"
echo ""

# Coletar variáveis de ambiente
print_warning "Por favor, forneça os valores para as variáveis de ambiente:"
echo ""

# Database
read -p "DB_URL (ex: jdbc:postgresql://xxx.rds.amazonaws.com:5432/ipredencao_manager): " DB_URL
read -p "DB_USERNAME: " DB_USERNAME
read -sp "DB_PASSWORD: " DB_PASSWORD
echo ""

# Firebase
print_info "Para FIREBASE_SERVICE_ACCOUNT_KEY_CONTENT, cole o conteúdo JSON completo e pressione Enter duas vezes:"
FIREBASE_JSON=""
while IFS= read -r line; do
    [ -z "$line" ] && break
    FIREBASE_JSON="${FIREBASE_JSON}${line}"
done

# JWT Secret
read -p "JWT_SECRET (ou pressione Enter para gerar automaticamente): " JWT_SECRET
if [ -z "$JWT_SECRET" ]; then
    JWT_SECRET=$(openssl rand -base64 32)
    print_success "JWT_SECRET gerado automaticamente: $JWT_SECRET"
fi

# S3
read -p "S3_BUCKET_NAME: " S3_BUCKET_NAME
read -p "AWS_REGION [us-east-1]: " AWS_REGION
AWS_REGION=${AWS_REGION:-us-east-1}

# CORS Origins
read -p "ALLOWED_ORIGINS (ex: https://app.example.com,https://www.example.com): " ALLOWED_ORIGINS

echo ""
print_info "Atualizando serviço App Runner..."
echo ""

# Criar JSON de configuração
cat > /tmp/apprunner-env-config.json <<EOF
{
  "SourceConfiguration": {
    "ImageRepository": {
      "ImageConfiguration": {
        "RuntimeEnvironmentVariables": {
          "SERVER_PORT": "8080",
          "SPRING_PROFILES_ACTIVE": "prod",
          "DB_URL": "${DB_URL}",
          "DB_USERNAME": "${DB_USERNAME}",
          "DB_PASSWORD": "${DB_PASSWORD}",
          "FIREBASE_SERVICE_ACCOUNT_KEY_CONTENT": ${FIREBASE_JSON},
          "JWT_SECRET": "${JWT_SECRET}",
          "S3_BUCKET_NAME": "${S3_BUCKET_NAME}",
          "AWS_REGION": "${AWS_REGION}",
          "ALLOWED_ORIGINS": "${ALLOWED_ORIGINS}",
          "LIQUIBASE_ENABLED": "true"
        }
      }
    }
  }
}
EOF

# Atualizar serviço
if aws apprunner update-service \
    --service-arn "$SERVICE_ARN" \
    --cli-input-json file:///tmp/apprunner-env-config.json > /dev/null 2>&1; then
    
    print_success "Variáveis de ambiente configuradas com sucesso!"
    echo ""
    print_info "O App Runner está fazendo o redeploy do serviço..."
    print_info "Isso pode levar alguns minutos. Monitore o status em:"
    echo "https://console.aws.amazon.com/apprunner/"
else
    print_error "Falha ao atualizar o serviço"
    print_info "Tente configurar manualmente via console AWS"
fi

# Limpar arquivo temporário
rm -f /tmp/apprunner-env-config.json

echo ""
echo "=========================================="
print_success "Configuração concluída!"
echo "=========================================="
