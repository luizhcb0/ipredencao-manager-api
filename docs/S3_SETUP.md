# Configuração do S3 (LocalStack)

## Problema Resolvido

**Erro:** `The specified bucket does not exist (Service: Amazon S3; Status Code: 404; Error Code: NoSuchBucket)`

**Causa:** O bucket S3 `ipredencao-manager-photos` não estava sendo criado automaticamente quando os containers subiam.

## Soluções Implementadas

### 1. Script de Inicialização Automática

O arquivo `scripts/init-localstack.sh` é executado automaticamente quando o LocalStack sobe e cria o bucket necessário:

```bash
#!/bin/bash

echo "Aguardando LocalStack estar pronto..."
sleep 10

echo "Criando bucket S3..."
awslocal s3 mb s3://ipredencao-manager-photos

echo "Verificando buckets criados..."
awslocal s3 ls

echo "Inicialização do LocalStack concluída!"
```

### 2. Criação Automática no Código

O novo serviço `S3Service` verifica e cria o bucket automaticamente:

- No `@PostConstruct`: verifica e cria o bucket na inicialização da aplicação
- No upload: verificação adicional antes de cada operação de upload

### 3. Refatoração da Arquitetura

- **Antes:** `PessoaService` gerenciava diretamente as operações S3
- **Depois:** `S3Service` dedicado para todas as operações S3
- **Benefícios:** 
  - Separação de responsabilidades
  - Reutilização em outros serviços
  - Melhor tratamento de erros

## Como Usar

### Comandos do Makefile

```bash
# Iniciar containers pela primeira vez
make run

# Reiniciar containers (mantém dados)
make restart

# Limpeza completa (remove volumes)
make clean
```

### Verificar se o Bucket Foi Criado

```bash
# Via Docker
docker exec localstack awslocal s3 ls

# Logs do LocalStack
docker logs localstack | grep -i bucket
```

### Upload de Fotos

```java
@Autowired
private S3Service s3Service;

// Upload direto
String url = s3Service.uploadFile("meu-arquivo.jpg", multipartFile);

// Upload de foto de pessoa
String url = s3Service.uploadPersonPhoto(pessoaId, foto);
```

## Configuração do Docker Compose

O `compose.yaml` foi atualizado para incluir o script de inicialização:

```yaml
localstack:
  image: localstack/localstack:latest
  container_name: localstack
  ports:
    - "4566:4566"
  environment:
    - SERVICES=s3
    - DEBUG=1
  volumes:
    - "./localstack:/var/lib/localstack"
    - "./scripts:/etc/localstack/init/ready.d"  # ← Script de inicialização
```

## Estrutura de Arquivos

```
scripts/
├── init-localstack.sh          # Script de inicialização
src/main/java/.../service/
├── S3Service.java               # Serviço dedicado para S3
├── PessoaService.java          # Refatorado para usar S3Service
```

## Funcionalidades do S3Service

- ✅ Criação automática do bucket
- ✅ Upload de arquivos genéricos
- ✅ Upload específico para fotos de pessoas
- ✅ Tratamento de erros robusto
- ✅ Verificação de existência do bucket
- ✅ Geração automática de nomes de arquivo únicos
- ✅ Exclusão de arquivos

## Logs de Sucesso

Quando tudo está funcionando, você verá logs como:

```
Aguardando LocalStack estar pronto...
Criando bucket S3...
make_bucket: ipredencao-manager-photos
Verificando buckets criados...
2025-08-01 05:10:29 ipredencao-manager-photos
Inicialização do LocalStack concluída!
```

E na aplicação:
```
Bucket já existe: ipredencao-manager-photos
```

## Troubleshooting

### Bucket não foi criado
```bash
# Verificar se o script tem permissão
ls -la scripts/init-localstack.sh

# Dar permissão se necessário
chmod +x scripts/init-localstack.sh

# Reiniciar containers
make restart
```

### LocalStack não está rodando
```bash
# Verificar status
docker ps

# Ver logs
docker logs localstack
```

### Erro de permissão no S3
```bash
# Recriar tudo do zero
make clean
``` 