#!/bin/bash

echo "Aguardando LocalStack estar pronto..."
sleep 10

echo "Criando bucket S3..."
awslocal s3 mb s3://ipredencao-manager-photos

echo "Verificando buckets criados..."
awslocal s3 ls

echo "Inicialização do LocalStack concluída!" 