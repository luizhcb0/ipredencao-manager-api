#!/bin/bash

echo "Aguardando LocalStack estar pronto..."
sleep 10

# Usa variável de ambiente ou fallback para nome padrão
BUCKET_NAME="${S3_BUCKET_NAME:-ipredencao-storage}"

echo "Criando bucket S3: ${BUCKET_NAME}..."
awslocal s3 mb s3://${BUCKET_NAME}

echo "Verificando buckets criados..."
awslocal s3 ls

echo "Inicialização do LocalStack concluída!" 