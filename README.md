# ipredencao-manager-api

## 🚀 Getting Started

### Generate JOOQ classes
```bash
./gradlew generateJooq
```

### Run
```bash
./gradlew bootRun
```

## 📚 Documentação

- **[Configuração de CORS](docs/CORS_CONFIGURATION.md)** - Detalhes sobre a configuração de CORS
- **[Tratamento de Erros](docs/ERROR_HANDLING.md)** - Como erros são tratados na API
- **[Configuração S3](docs/S3_SETUP.md)** - Setup do Amazon S3 / LocalStack
- **[Integração de Autenticação (Frontend)](docs/FRONTEND_AUTH_INTEGRATION.md)** - Guia completo de integração JWT
- **[Logout Automático (Frontend)](docs/FRONTEND_LOGOUT_AUTOMATICO.md)** - Guia rápido para implementar logout automático

## LocalStack (S3 Local)

### Subindo o LocalStack

```
docker compose up -d localstack
```

### Configuração da aplicação para usar o LocalStack

No arquivo `src/main/resources/application.properties`:
```
cloud.aws.s3.endpoint=http://localhost:4566
cloud.aws.region.static=us-east-1
```

Ou defina as variáveis de ambiente:
```
AWS_ACCESS_KEY_ID=test
AWS_SECRET_ACCESS_KEY=test
AWS_REGION=us-east-1
```

### Criando buckets S3 no LocalStack

Instale o AWS CLI e execute:
```
aws --endpoint-url=http://localhost:4566 s3 mb s3://nome-do-bucket
```

Substitua `nome-do-bucket` pelo nome desejado.

### Criando buckets automaticamente no LocalStack

Um script está disponível em `localstack/create-buckets.sh` para criar o bucket S3 necessário:

```sh
chmod +x localstack/create-buckets.sh
./localstack/create-buckets.sh
```

Esse script utiliza o comando `awslocal`. Para instalar:

```sh
pip install awscli-local
```

**Como usar:**
1. Instale o awslocal:
   ```sh
   pip install awscli-local
   ```
2. Dê permissão de execução ao script:
   ```sh
   chmod +x localstack/create-buckets.sh
   ```
3. Execute o script:
   ```sh
   ./localstack/create-buckets.sh
   ```

Se precisar de mais algum ajuste ou quiser automatizar ainda mais (por exemplo, rodar o script automaticamente ao subir o LocalStack), é só avisar!