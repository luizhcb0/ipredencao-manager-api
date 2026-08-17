#!/bin/bash
set -euo pipefail

# Build da imagem da API, teste local opcional e publicacao para producao.
#
# Uso:
#   ./quick-rebuild.sh            constroi e pergunta se publica
#   ./quick-rebuild.sh --yes      constroi e publica sem perguntar (make deploy)
#   ./quick-rebuild.sh --no-push  so constroi, sem perguntar nada (make build)
#
# A imagem sai em linux/amd64, porque o App Runner roda x86_64, e e carregada no
# daemon local (--load). E isso que permite testar com ./test-docker-local.sh
# exatamente o mesmo artefato que vai para producao. Em maquina arm64 esse teste
# local roda emulado e sobe mais devagar que nativo.
#
# O App Runner esta com AutoDeployments observando a tag :latest, ou seja, o push
# e o gatilho do deploy - nao existe comando de deploy separado. Junto de :latest
# sobe a tag do commit, que e o caminho de rollback: para voltar atras, aponte o
# servico para a tag anterior.

AWS_REGION="us-east-1"
AWS_ACCOUNT_ID="045935420308"
ECR_REPOSITORY="ipredencao-manager-api"
AWS_PROFILE="${AWS_PROFILE:-personal}"
SERVICE_NAME="ipredencao-manager-api"

REGISTRY="${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"
IMAGE_URI="${REGISTRY}/${ECR_REPOSITORY}"

ASSUME_YES=false
PUSH=true
case "${1:-}" in
    --yes)     ASSUME_YES=true ;;
    --no-push) PUSH=false ;;
    "")        ;;
    *)         echo "Uso: $(basename "$0") [--yes|--no-push]" >&2; exit 1 ;;
esac

cd "$(dirname "$0")/.."

GIT_SHA="$(git rev-parse --short HEAD)"
if [ -n "$(git status --porcelain -- src build.gradle Dockerfile)" ]; then
    GIT_SHA="${GIT_SHA}-dirty"
fi

echo "🔨 Rebuild da imagem"
echo "   Tags: latest, ${GIT_SHA}"
echo "   Plataforma: linux/amd64"
echo ""

echo "🔄 Gerando codigo do JOOQ..."
./gradlew generateJooq -x composeUp

echo "📦 Build da imagem..."
docker buildx build --platform linux/amd64 --load \
    -t "${IMAGE_URI}:latest" \
    -t "${IMAGE_URI}:${GIT_SHA}" \
    .

echo "🧹 Limpando imagens orfas..."
docker image prune -f

echo ""
echo "✅ Build concluido. Para testar este mesmo artefato localmente:"
echo "   ./scripts/test-docker-local.sh"
echo ""

if [ "$PUSH" = false ]; then
    echo "⏭️  Somente build, nada publicado."
    exit 0
fi

if [ "$ASSUME_YES" = false ]; then
    echo "🚀 Publicar para producao? Isso dispara o deploy no App Runner. (y/n)"
    read -r response
    if [[ ! "$response" =~ ^([yY][eE][sS]|[yY])$ ]]; then
        echo "⏭️  Push cancelado. A imagem ficou disponivel localmente."
        exit 0
    fi
fi

echo "🔐 Login no ECR..."
aws ecr get-login-password --region "${AWS_REGION}" --profile "${AWS_PROFILE}" \
    | docker login --username AWS --password-stdin "${REGISTRY}"

echo "☁️  Publicando..."
docker push "${IMAGE_URI}:latest"
docker push "${IMAGE_URI}:${GIT_SHA}"

SERVICE_ARN="$(aws apprunner list-services --region "${AWS_REGION}" --profile "${AWS_PROFILE}" \
    --query "ServiceSummaryList[?ServiceName=='${SERVICE_NAME}'].ServiceArn" --output text)"
: "${SERVICE_ARN:=<arn-do-servico-${SERVICE_NAME}>}"

# O script nao espera o deploy de proposito: o AutoDeployments ja foi disparado
# pelo push da tag :latest e leva ~4 min. Como ele roda sozinho no App Runner,
# fechar o terminal aqui nao interrompe nada.
echo ""
echo "✅ Publicado como ${GIT_SHA}. O App Runner ja esta deployando (leva ~4 min)."
echo ""
echo "Acompanhar:"
echo "   aws apprunner list-operations --service-arn ${SERVICE_ARN} \\"
echo "     --region ${AWS_REGION} --profile ${AWS_PROFILE} --output table \\"
echo "     --query 'OperationSummaryList[:1].{Type:Type,Status:Status,Ended:EndedAt}'"
echo ""
echo "Conferir a aplicacao:"
echo "   curl https://api.gestao.ipredencao.com/actuator/health"
echo ""
echo "Logs:"
echo "   aws logs tail /aws/apprunner/${SERVICE_NAME}/*/application --follow --region ${AWS_REGION}"
echo ""
echo "Rollback (aponta o servico para a tag anterior, sem rebuild):"
echo "   aws apprunner update-service --service-arn ${SERVICE_ARN} \\"
echo "     --region ${AWS_REGION} --profile ${AWS_PROFILE} \\"
echo "     --source-configuration '{\"ImageRepository\":{\"ImageIdentifier\":\"${IMAGE_URI}:<tag-anterior>\",\"ImageRepositoryType\":\"ECR\"}}'"
