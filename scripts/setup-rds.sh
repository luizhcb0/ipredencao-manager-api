#!/bin/bash
set -e

# Variáveis
DB_NAME="ipredencao-prod-db"
DB_USER="ipredencao_admin"
DB_PASSWORD="${DB_PASSWORD:-CHANGE_ME}"  # Passar via env var
DB_INSTANCE_CLASS="db.t3.micro"
DB_STORAGE=20
AWS_REGION="us-east-1"

echo "🗄️  Creating RDS PostgreSQL instance..."

# Criar DB instance
aws rds create-db-instance \
  --db-instance-identifier ${DB_NAME} \
  --db-instance-class ${DB_INSTANCE_CLASS} \
  --engine postgres \
  --engine-version 16.1 \
  --master-username ${DB_USER} \
  --master-user-password ${DB_PASSWORD} \
  --allocated-storage ${DB_STORAGE} \
  --storage-type gp3 \
  --storage-encrypted \
  --db-name ipredencao_manager \
  --backup-retention-period 7 \
  --preferred-backup-window "03:00-04:00" \
  --preferred-maintenance-window "sun:04:00-sun:05:00" \
  --enable-performance-insights \
  --no-publicly-accessible \
  --region ${AWS_REGION} \
  --tags Key=Project,Value=ipredencao Key=Environment,Value=production

echo "⏳ Waiting for database to be available (this may take 5-10 minutes)..."
aws rds wait db-instance-available \
  --db-instance-identifier ${DB_NAME} \
  --region ${AWS_REGION}

# Obter endpoint
DB_ENDPOINT=$(aws rds describe-db-instances \
  --db-instance-identifier ${DB_NAME} \
  --region ${AWS_REGION} \
  --query 'DBInstances[0].Endpoint.Address' \
  --output text)

echo "✅ Database created successfully!"
echo "📊 Endpoint: ${DB_ENDPOINT}"
echo ""
echo "🔐 Connection string:"
echo "DATABASE_URL=jdbc:postgresql://${DB_ENDPOINT}:5432/ipredencao_manager"
echo "DATABASE_USERNAME=${DB_USER}"
echo "DATABASE_PASSWORD=${DB_PASSWORD}"
