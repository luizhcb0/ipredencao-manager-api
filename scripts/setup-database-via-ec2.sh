#!/bin/bash
set -e

echo "🚀 Setup: Criar database via EC2 temporária"
echo ""

# Configurações
RDS_ENDPOINT="ipredencao-manager-prod-db.cqon6ha0kufq.us-east-1.rds.amazonaws.com"
DB_USER="ipredencao_admin"
DB_PASSWORD="ipredencao_manager"
DB_NAME="ipredencao_manager"
REGION="us-east-1"
SUBNET_ID="subnet-029471b4f3cf7ed70"
SECURITY_GROUP="sg-066f737aa2f887ca5"

echo "1️⃣ Lançando EC2 temporária..."
INSTANCE_ID=$(aws ec2 run-instances \
  --image-id ami-0453ec754f44f9a4a \
  --instance-type t2.micro \
  --subnet-id $SUBNET_ID \
  --security-group-ids $SECURITY_GROUP \
  --iam-instance-profile Name=AmazonSSMManagedInstanceCore \
  --region $REGION \
  --profile personal \
  --tag-specifications 'ResourceType=instance,Tags=[{Key=Name,Value=temp-bastion-rds}]' \
  --query 'Instances[0].InstanceId' \
  --output text 2>/dev/null)

if [ -z "$INSTANCE_ID" ]; then
    echo "❌ Erro ao criar EC2"
    echo ""
    echo "Tente manualmente:"
    echo "1. EC2 Console → Launch Instance"
    echo "2. Amazon Linux 2023"
    echo "3. t2.micro"
    echo "4. Same VPC/Subnet as RDS"
    echo "5. Security Group: $SECURITY_GROUP"
    exit 1
fi

echo "✅ EC2 criada: $INSTANCE_ID"
echo ""

echo "2️⃣ Aguardando EC2 ficar disponível..."
aws ec2 wait instance-running --instance-ids $INSTANCE_ID --region $REGION --profile personal
sleep 30  # Aguardar SSM Agent iniciar

echo "✅ EC2 disponível"
echo ""

echo "3️⃣ Instalando PostgreSQL client..."
aws ssm send-command \
  --instance-ids $INSTANCE_ID \
  --document-name "AWS-RunShellScript" \
  --parameters 'commands=["sudo yum install -y postgresql15"]' \
  --region $REGION \
  --profile personal \
  --output text

sleep 10

echo "4️⃣ Criando database..."
COMMAND_ID=$(aws ssm send-command \
  --instance-ids $INSTANCE_ID \
  --document-name "AWS-RunShellScript" \
  --parameters "commands=[\"PGPASSWORD='$DB_PASSWORD' psql -h $RDS_ENDPOINT -U $DB_USER -d postgres -c \\\"CREATE DATABASE $DB_NAME;\\\" || echo 'Database may already exist'\"]" \
  --region $REGION \
  --profile personal \
  --query 'Command.CommandId' \
  --output text)

sleep 5

# Verificar resultado
aws ssm get-command-invocation \
  --command-id $COMMAND_ID \
  --instance-id $INSTANCE_ID \
  --region $REGION \
  --profile personal \
  --query 'StandardOutputContent' \
  --output text

echo ""
echo "5️⃣ Limpando: Terminando EC2..."
aws ec2 terminate-instances \
  --instance-ids $INSTANCE_ID \
  --region $REGION \
  --profile personal \
  --output text > /dev/null

echo "✅ Concluído!"
echo ""
echo "📋 Próximo passo:"
echo "   - Teste o deployment do App Runner"
echo "   - Liquibase agora poderá criar as tabelas"

