# ✅ Checklist - Variáveis de Ambiente App Runner

## 🔴 Problema Identificado

Seu deploy no App Runner está **falhando no health check** porque as seguintes variáveis de ambiente estão faltando:

```
Health check failed on protocol `TCP` [Port: '8080']
```

## 🔧 Solução

O App Runner precisa das seguintes variáveis de ambiente configuradas:

### 1️⃣ Variáveis Obrigatórias

Copie e cole no console do App Runner (Configure service → Environment variables):

```bash
# Database (RDS)
DB_URL=jdbc:postgresql://SEU-RDS-ENDPOINT:5432/ipredencao_manager
DB_USERNAME=ipredencao_admin
DB_PASSWORD=SUA_SENHA_DO_RDS

# Firebase
FIREBASE_SERVICE_ACCOUNT_KEY_CONTENT={"type":"service_account","project_id":"..."}

# JWT
JWT_SECRET=SUA_CHAVE_SECRETA_256_BITS

# AWS S3
S3_BUCKET_NAME=seu-bucket-s3
AWS_REGION=us-east-1

# Spring
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=prod
```

### 2️⃣ Variáveis Opcionais (Recomendadas)

```bash
# CORS
ALLOWED_ORIGINS=https://seu-dominio.com,https://www.seu-dominio.com

# Liquibase
LIQUIBASE_ENABLED=true
```

---

## 🐳 Testar Localmente com Docker

Para testar a imagem Docker localmente antes de fazer deploy:

```bash
# 1. Ler o conteúdo JSON do Firebase (em uma linha)
FIREBASE_JSON=$(cat src/main/resources/ipredencao-manager-api-firebase-adminsdk.json | jq -c)

# 2. Rodar o container com variáveis de ambiente
docker run --rm -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e SERVER_PORT=8080 \
  -e DB_URL="jdbc:postgresql://host.docker.internal:5432/ipredencao_manager" \
  -e DB_USERNAME="ipredencao_manager" \
  -e DB_PASSWORD="ipredencao_manager" \
  -e FIREBASE_SERVICE_ACCOUNT_KEY_CONTENT="$FIREBASE_JSON" \
  -e JWT_SECRET="your-test-secret-key-must-be-256-bits-long" \
  -e S3_BUCKET_NAME="ipredencao-manager-photos" \
  -e AWS_REGION="us-east-1" \
  045935420308.dkr.ecr.us-east-1.amazonaws.com/ipredencao-manager-api:latest

# 3. Testar health check
curl http://localhost:8080/actuator/health
```

**Nota:** Use `host.docker.internal` no Mac/Windows para acessar o banco local. No Linux, use o IP do host ou `--network host`.

---

## 📝 Como Obter os Valores

### DB_URL (RDS Endpoint)
```bash
aws rds describe-db-instances \
  --db-instance-identifier ipredencao-prod-db \
  --region us-east-1 \
  --profile personal \
  --query 'DBInstances[0].Endpoint.Address' \
  --output text
```

Resultado: `ipredencao-prod-db.xxxxxxxxx.us-east-1.rds.amazonaws.com`

Use: `jdbc:postgresql://ipredencao-prod-db.xxxxxxxxx.us-east-1.rds.amazonaws.com:5432/ipredencao_manager`

### FIREBASE_SERVICE_ACCOUNT_KEY_CONTENT

1. Abra: `ipredencao-manager-api/src/main/resources/ipredencao-manager-api-firebase-adminsdk.json`
2. Copie **todo** o conteúdo JSON (como uma linha única, sem quebras)
3. Cole no App Runner

**Dica:** Para converter em uma linha:
```bash
cat src/main/resources/ipredencao-manager-api-firebase-adminsdk.json | jq -c
```

### JWT_SECRET

Gere uma chave aleatória segura:
```bash
openssl rand -base64 32
```

Exemplo de resultado: `XpR9kF2mN8qW1vT4hS7gL0bV3nC6zY5xJ8aK1wM2eD4=`

### S3_BUCKET_NAME

Nome do bucket S3 que você criou:
```bash
aws s3 ls --profile personal
```

---

## 🌐 Configuração de Rede (VPC Connector)

⚠️ **IMPORTANTE:** Configure o VPC Connector no App Runner:

1. No console do App Runner, vá em **Networking**
2. Configure:
   - **VPC**: Sua VPC existente
   - **Subnets**: 2 subnets em AZs diferentes
   - **Security Group**: Security group que permite acesso ao RDS na porta 5432

**Security Group Rules necessárias:**
- **Egress:** Permitir tráfego de saída para o RDS (porta 5432)
- **RDS Security Group Ingress:** Permitir tráfego do App Runner Security Group (porta 5432)

---

## 🚀 Passos para Deploy

### 1. Criar RDS (se ainda não criou)
```bash
# Configure suas variáveis
export DB_PASSWORD="sua-senha-forte"
export VPC_ID="vpc-xxxxxxxxx"
export SUBNET_ID_1="subnet-xxxxxxxxx"
export SUBNET_ID_2="subnet-yyyyyyyyy"
export SECURITY_GROUP_ID="sg-xxxxxxxxx"

# Execute o script
cd ipredencao-manager-api/scripts
./setup-rds.sh
```

### 2. Configurar App Runner

Via Console AWS:
1. Vá para **App Runner** → Selecione seu serviço → **Edit**
2. Configure **Environment variables** (todas as obrigatórias acima)
3. Configure **Networking** → VPC Connector
4. Salve as alterações

### 3. Rebuild & Deploy

O App Runner fará redeploy automaticamente após salvar as configurações.

---

## 🔍 Verificação

### Health Check Endpoint
```bash
curl https://seu-app-runner-url.us-east-1.awsapprunner.com/actuator/health
```

Resposta esperada:
```json
{
  "status": "UP"
}
```

### Verificar Logs
```bash
aws logs tail /aws/apprunner/ipredencao-manager-api --follow --profile personal
```

---

## ❌ Troubleshooting

### Erro: "Connection to database failed"
- ✅ Verifique se o VPC Connector está configurado
- ✅ Verifique se o Security Group permite conexão do App Runner → RDS
- ✅ Verifique se `DB_URL`, `DB_USERNAME` e `DB_PASSWORD` estão corretos

### Erro: "Firebase initialization failed"
- ✅ Verifique se o JSON do Firebase está completo (sem quebras de linha)
- ✅ Confirme que é o arquivo correto do Firebase Console

### Erro: "Health check timeout"
- ✅ Verifique se `SERVER_PORT=8080` está configurado
- ✅ Verifique se todas as variáveis obrigatórias estão presentes
- ✅ Veja os logs do App Runner para detalhes

---

## 📚 Referências

- [Documentação - App Runner Environment Variables](docs/APPRUNNER_ENV_VARS.md)
- [Script - Setup RDS](scripts/setup-rds.sh)
- [Script - Build e deploy do App Runner](scripts/quick-rebuild.sh)
- [Configuração - application-prod.properties](src/main/resources/application-prod.properties)
