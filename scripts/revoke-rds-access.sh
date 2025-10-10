#!/bin/bash
set -e

echo "🔒 Removendo acesso público ao RDS..."
echo ""

# 1. Remover regra do Security Group
echo "1️⃣ Removendo seu IP do Security Group..."
aws ec2 revoke-security-group-ingress \
  --group-id sg-0622d28c048e22fa8 \
  --protocol tcp \
  --port 5432 \
  --cidr 189.6.34.199/32 \
  --region us-east-1 \
  --profile personal

echo "✅ Regra removida"
echo ""

# 2. Tornar RDS privado novamente
echo "2️⃣ Tornando RDS privado novamente..."
aws rds modify-db-instance \
  --db-instance-identifier ipredencao-manager-prod-db \
  --no-publicly-accessible \
  --region us-east-1 \
  --profile personal \
  --apply-immediately \
  --query 'DBInstance.[DBInstanceIdentifier,PubliclyAccessible]' \
  --output json

echo ""
echo "✅ RDS protegido novamente!"
echo "⏳ Aguarde 2-3 minutos para as mudanças serem aplicadas"

