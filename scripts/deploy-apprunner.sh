#!/bin/bash
set -euo pipefail

# Build da imagem, push para o ECR e acompanhamento do deploy no App Runner.
#
# Uso: ./deploy-apprunner.sh          (ou: make deploy)
#
# O App Runner esta com AutoDeployments habilitado observando a tag :latest, ou
# seja, o push e o gatilho do deploy - nao existe comando de deploy separado.
# Alem de :latest a imagem sobe com a tag do commit, que e o que permite voltar
# atras apontando o servico para uma tag anterior.

AWS_PROFILE="${AWS_PROFILE:-personal}"
AWS_REGION="us-east-1"
export AWS_PROFILE AWS_REGION

ACCOUNT_ID="045935420308"
REGISTRY="${ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"
REPO="ipredencao-manager-api"
SERVICE_NAME="ipredencao-manager-api"

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
API_DIR="$(dirname "$SCRIPT_DIR")"
cd "$API_DIR"

# App Runner roda x86_64. Como a maquina de desenvolvimento e arm64, sem o
# --platform a imagem sobe arm64 e o servico falha ao iniciar.
PLATFORM="linux/amd64"

GIT_SHA="$(git rev-parse --short HEAD)"
if [ -n "$(git status --porcelain -- src build.gradle Dockerfile)" ]; then
    GIT_SHA="${GIT_SHA}-dirty"
fi

echo "==> Deploy do backend para o App Runner"
echo "    Imagem:    ${REGISTRY}/${REPO}"
echo "    Tags:      latest, ${GIT_SHA}"
echo "    Plataforma:${PLATFORM}"
echo ""

if ! docker info >/dev/null 2>&1; then
    echo "ERRO: Docker nao esta rodando."
    exit 1
fi

# O Dockerfile faz COPY target ./target: o codigo gerado pelo JOOQ nao e criado
# durante o build da imagem, precisa existir antes.
if [ ! -d "$API_DIR/target" ]; then
    echo "ERRO: diretorio target/ (codigo gerado pelo JOOQ) nao existe."
    echo "      Rode 'make jooq-only' antes."
    exit 1
fi

echo "==> Login no ECR"
aws ecr get-login-password --region "$AWS_REGION" \
    | docker login --username AWS --password-stdin "$REGISTRY"

echo ""
echo "==> Build e push (emulacao amd64, pode levar varios minutos)"
docker buildx build \
    --platform "$PLATFORM" \
    -t "${REGISTRY}/${REPO}:latest" \
    -t "${REGISTRY}/${REPO}:${GIT_SHA}" \
    --push \
    .

SERVICE_ARN="$(aws apprunner list-services --region "$AWS_REGION" \
    --query "ServiceSummaryList[?ServiceName=='${SERVICE_NAME}'].ServiceArn" \
    --output text)"

if [ -z "$SERVICE_ARN" ]; then
    echo "AVISO: servico ${SERVICE_NAME} nao encontrado, nada a acompanhar."
    exit 0
fi

echo ""
echo "==> Push concluido. O AutoDeployments dispara o deploy sozinho."
echo "    Aguardando o servico sair de OPERATION_IN_PROGRESS (ate 15 min)..."

for _ in $(seq 1 90); do
    STATUS="$(aws apprunner describe-service --service-arn "$SERVICE_ARN" \
        --region "$AWS_REGION" --query 'Service.Status' --output text)"
    if [ "$STATUS" != "OPERATION_IN_PROGRESS" ]; then
        break
    fi
    printf '.'
    sleep 10
done
echo ""

echo "==> Status final do servico: ${STATUS}"
aws apprunner list-operations --service-arn "$SERVICE_ARN" --region "$AWS_REGION" \
    --query 'OperationSummaryList[:1].{Type:Type,Status:Status,Ended:EndedAt}' \
    --output table

SERVICE_URL="$(aws apprunner describe-service --service-arn "$SERVICE_ARN" \
    --region "$AWS_REGION" --query 'Service.ServiceUrl' --output text)"

echo ""
echo "==> Checando a aplicacao"
curl -fsS --max-time 30 "https://${SERVICE_URL}/actuator/health" && echo ""

if [ "$STATUS" != "RUNNING" ]; then
    echo "ERRO: servico terminou em ${STATUS}. Veja os logs:"
    echo "  aws logs tail /aws/apprunner/${SERVICE_NAME}/*/application --follow --region ${AWS_REGION}"
    exit 1
fi

echo ""
echo "Deploy concluido. Rollback, se precisar: aponte o servico para a tag anterior"
echo "com 'aws apprunner update-service --source-configuration ...ImageIdentifier=${REGISTRY}/${REPO}:<tag>'."
