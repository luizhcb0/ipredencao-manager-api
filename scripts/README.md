# Script de Importação IPR Dump

Este script importa dados do dump IPR_Dump para a API ipredencao-manager.

## Pré-requisitos

1. Python 3.8 ou superior
2. Dados do IPR_Dump em `src/main/resources/IPR_Dump/`
3. API rodando e acessível
4. Token JWT de autenticação válido com role PRESBITERO ou ADMIN

## Instalação

```bash
cd scripts
pip install -r requirements.txt
```

## Configuração

### 1. Obter Token de Autenticação

Faça login na API e obtenha um token JWT válido. Configure a variável de ambiente:

```bash
export API_TOKEN="seu-token-jwt-aqui"
```

### 2. Verificar API

Certifique-se de que a API está rodando:

```bash
curl http://localhost:8080/api/auth/health
```

## Uso

### Importação Completa

#### Opção A: Comando Completo (Recomendado)

Execute tudo de uma vez em background com acompanhamento automático:

```bash
cd /Users/lbarboza/code/ipredencao/ipredencao-manager-api/scripts && \
rm -f import_checkpoint.json id_mapping.json && \
export API_TOKEN="SEU_TOKEN_JWT_AQUI" && \
echo "Iniciando importação completa..." && \
/Library/Developer/CommandLineTools/usr/bin/python3 import_dump.py \
  --api-url http://localhost:8080 \
  --dump-path ../src/main/resources/IPR_Dump \
  > import.log 2>&1 & \
echo "Script iniciado em background. PID: $!" && \
echo "Acompanhe com: tail -f import.log" && \
sleep 5 && \
tail -50 import.log
```

**O que este comando faz:**
1. Limpa checkpoints anteriores (importação do zero)
2. Define o token JWT
3. Executa o script em background com logs em `import.log`
4. Mostra o PID do processo
5. Aguarda 5 segundos e exibe as últimas 50 linhas do log

#### Opção B: Comando Simples (Foreground)

Execute em primeiro plano:

```bash
export API_TOKEN="seu-token-jwt-aqui"
python3 import_dump.py --api-url http://localhost:8080 --dump-path ../src/main/resources/IPR_Dump
```

#### Opção C: Continuar de onde parou

Se o script foi interrompido, continue sem limpar checkpoints:

```bash
export API_TOKEN="seu-token-jwt-aqui"
/Library/Developer/CommandLineTools/usr/bin/python3 import_dump.py \
  --api-url http://localhost:8080 \
  --dump-path ../src/main/resources/IPR_Dump \
  > import.log 2>&1 &
  
# Acompanhar progresso
tail -f import.log
```

### Monitoramento

```bash
# Ver últimas 50 linhas do log
tail -50 import.log

# Acompanhar em tempo real
tail -f import.log

# Ver apenas erros
grep -i error import.log

# Contar pessoas criadas
grep "Criando pessoa:" import.log | wc -l

# Ver progresso de fotos
grep "Upload foto" import.log | tail -10

# Ver progresso de relacionamentos
grep "Criando relacionamento:" import.log | tail -10

# Verificar se o script ainda está rodando
ps aux | grep import_dump.py
```

## Fases de Importação

### Fase 1: Importar Pessoas (com contatos consolidados)

- Lê `Pessoa_Contato.csv` e consolida telefones, emails e endereços por pessoa
- Lê `Pessoa.csv` e cria payload completo com todos os dados
- Lê `IPVideira.csv` para identificar pessoas da congregação (campus)
- Lê `Igreja.csv` para mapear IDs de igreja para nomes
- Lê `AtoOficial.csv` para incluir histórico de atos de admissão e demissão
- Cria pessoas via `POST /api/pessoas`
- Salva mapeamento de IDs antigos → novos em `id_mapping.json`

**Dados importados:**
- Nome, apelido, sexo, CPF, RG
- Data de nascimento, data de falecimento
- Estado civil, categoria, campus (SEDE ou VIDEIRA)
- Telefones (primário e secundários)
- Emails (primário e secundários)
- Profissões e empresas (arrays)
- Endereço completo (CEP, logradouro, cidade, estado, coordenadas)
- Datas de batismo e profissão de fé
- Igreja anterior/de batismo
- Tipo de batismo (inferido)
- **Informações adicionais:** Histórico de atos oficiais (admissão e demissão) formatado como:
  - `04/12/2016 | Admissão de membro comungante | Admissão por transferência | Ata: 1`
  - `05/10/2021 | Demissão de membro comungante | Demissão por transferência | Ata: 180`
  - Ordenados por data ascendente, um ato por linha

### Fase 2: Upload de Fotos

- Para cada pessoa com foto registrada
- Localiza arquivo em `IPR_Dump/fotos/`
- Faz upload via `POST /api/pessoas/{id}/foto`

### Fase 2.5: Definir Chefe de Família

- Para cada pessoa com ChefeDeFamília definido no CSV
- Atualiza o campo `chefeDeFamiliaId` no objeto Pessoa
- Usa `GET /api/pessoas/{id}` e `PUT /api/pessoas/{id}`
- **Nota:** ChefeDeFamília não é um relacionamento, é um campo da pessoa

### Fase 3: Criar Relacionamentos

- Processa relacionamentos do CSV:
  - Pai → tipo PAI
  - Mãe → tipo MAE
  - Cônjuge → tipo CONJUGE (com data de início do relacionamento)
- Usa mapeamento de IDs para converter
- Cria via `POST /api/pessoas/{id}/relacionamento`

## Recuperação de Erros

O script salva progresso automaticamente em:
- `import_checkpoint.json` - Estado de cada fase
- `id_mapping.json` - Mapeamento de IDs antigos → novos

Se a importação falhar, simplesmente execute o script novamente. Ele continuará de onde parou.

### Resetar Importação

Para começar do zero, delete os arquivos de checkpoint:

```bash
rm import_checkpoint.json id_mapping.json
```

## Logs

O script gera logs em:
- Console (stdout)
- Arquivo `import_dump.log`

## Mapeamentos de Dados

### Sexo
- M → MASCULINO
- F → FEMININO

### Estado Civil
- Casado/Casada → CASADO
- Solteiro/Solteira → SOLTEIRO_SEM_RELACIONAMENTO
- Divorciado/Divorciada → DIVORCIADO_SEM_RELACIONAMENTO
- Viúvo/Viúva → VIUVO_SEM_RELACIONAMENTO

### Categoria
- "01.Membro" → MEMBRO_COMUNGANTE (id: 3)
- "08.Agregado não membro (p.ex. familiar frequente)" → AGREGADO_FAMILIAR (id: 31)
- Outros → PESSOA_REFERENCIADA (id: 32)

### Tipo de Batismo (Inferido)

Baseado nas datas de BatismoData e ProfissãoDeFéData:
- Ambas existem e são diferentes → INFANTIL
- Apenas uma existe → ADULTO
- Ambas existem e são iguais → ADULTO
- Nenhuma existe → NAO_BATIZADO

## Exemplo de Execução

```bash
# 1. Configurar token
export API_TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

# 2. Executar importação
python import_dump.py

# Saída esperada:
# ================================================================================
# FASE 1: Importando Pessoas
# ================================================================================
# Consolidando contatos por pessoa...
# Consolidados contatos de 1234 pessoas
# [1/2737] Criando pessoa: João Silva (ID antigo: 1)
# ...
# ================================================================================
# FASE 1 COMPLETA: 2737 pessoas criadas, 0 erros
# ================================================================================
# ...
```

## Troubleshooting

### Erro: "API_TOKEN não definida"
Configure a variável de ambiente com seu token JWT.

### Erro: "Arquivo não encontrado"
Verifique se os arquivos CSV e pasta de fotos estão em `src/main/resources/IPR_Dump/`.

### Erro: "401 Unauthorized"
Seu token expirou ou não tem permissões. Faça login novamente e obtenha um novo token.

### Erro: "Connection refused"
A API não está rodando. Inicie a API primeiro.

## Estatísticas Esperadas

Com o dump completo:
- ~3.708 pessoas
- ~1.000 fotos
- ~5.000 relacionamentos
- ~2.931 atos oficiais (admissão/demissão) para ~1.454 pessoas
- Endereços copiados do chefe de família conforme necessário

---

# Desenvolvimento Local

Scripts para rodar o backend localmente com dados de produção, sem afetar o ambiente de produção.

## Pré-requisitos

| Ferramenta | Para que | Necessário em |
|------------|----------|---------------|
| Docker | Rodar PostgreSQL, LocalStack e a API | Ambos os scripts |
| PostgreSQL 17+ client tools | `pg_dump`, `pg_restore`, `createdb`, `dropdb` | `sync-prod-data.sh` (comandos `dump`, `restore`, `sync`) |
| AWS CLI (`aws`) | Sincronizar bucket S3 | `sync-prod-data.sh` (comandos `s3`, `sync`) |
| Credenciais de prod (DB + AWS) | Acesso aos dados de produção | `sync-prod-data.sh` apenas |

> O `test-docker-local.sh` precisa apenas de **Docker**. Não requer PostgreSQL client tools, AWS CLI nem credenciais de produção.

### Configurar credenciais (apenas para sync)

```bash
cp scripts/.env.local.example scripts/.env.local
```

Preencha os valores no `.env.local`:

```
PROD_DB_HOST=ipredencao-prod-db.cqon6ha0kufq.us-east-1.rds.amazonaws.com
PROD_DB_PORT=5432
PROD_DB_NAME=ipredencao_manager
PROD_DB_USER=ipredencao_admin
PROD_DB_PASSWORD=<sua-senha>

AWS_ACCESS_KEY_ID=<sua-access-key>
AWS_SECRET_ACCESS_KEY=<sua-secret-key>
```

> O arquivo `.env.local` é ignorado pelo git. Essas credenciais são usadas **apenas** pelo `sync-prod-data.sh` para baixar dados de produção.

### PostgreSQL 17

Se o `pg_dump` no PATH for uma versão anterior, o script tenta encontrar a versão 17 automaticamente em caminhos conhecidos (Homebrew no macOS, `/usr/lib/postgresql` no Linux, `Program Files` no Windows).

Para forçar um caminho específico:

```bash
export PG_BIN_DIR="/opt/homebrew/opt/postgresql@17/bin"
```

## sync-prod-data.sh

Sincroniza dados de produção (DB e S3) para o ambiente local Docker.

### Comandos

```bash
# Sincronização completa: dump DB + restore DB + sync S3 (padrão)
./scripts/sync-prod-data.sh

# Apenas dump do banco de produção
./scripts/sync-prod-data.sh dump

# Apenas restore do dump para o DB local
./scripts/sync-prod-data.sh restore

# Apenas sync dos arquivos S3
./scripts/sync-prod-data.sh s3

# Ajuda
./scripts/sync-prod-data.sh help
```

### O que cada comando faz

| Comando   | Descrição |
|-----------|-----------|
| `sync`    | Executa `dump` + `restore` + `s3` em sequência |
| `dump`    | Faz `pg_dump` do banco de produção e salva em `scripts/.dump/prod.dump` |
| `restore` | Recria o DB local a partir do dump (encerra conexões ativas, dropa e recria o banco) |
| `s3`      | Baixa os arquivos do bucket S3 de produção para `scripts/.dump/s3/` e sobe para o LocalStack local |

### Fluxo

```
Produção                          Local
┌──────────┐   pg_dump    ┌─────────────────┐
│  RDS DB  │ ──────────── │ scripts/.dump/   │
└──────────┘              │   prod.dump      │
                          └────────┬────────┘
                            pg_restore │
                          ┌────────▼────────┐
                          │ Docker Postgres  │
                          │ localhost:54329  │
                          └─────────────────┘

┌──────────┐   aws s3 sync   ┌──────────────┐   aws s3 sync   ┌─────────────────┐
│  S3 Prod │ ──────────────  │ scripts/.dump │ ──────────────  │   LocalStack    │
│  Bucket  │                 │   /s3/        │                 │ localhost:4566  │
└──────────┘                 └──────────────┘                  └─────────────────┘
```

## test-docker-local.sh

Roda a imagem Docker do backend apontando para os serviços locais (DB + S3).

```bash
./scripts/test-docker-local.sh
```

### O que faz

1. Sobe PostgreSQL e LocalStack via `docker compose`
2. Roda o container da API na porta `8080` apontando para:
   - **DB:** `host.docker.internal:54329` (PostgreSQL local)
   - **S3:** `host.docker.internal:4566` (LocalStack local, bucket `ipredencao-storage`)
3. Liquibase desabilitado (`LIQUIBASE_ENABLED=false`) — o schema já vem do dump
4. Credenciais AWS são dummy (`localstack`/`localstack`) — o LocalStack aceita qualquer valor

> **Nenhuma credencial de produção é necessária** para rodar este script. Tudo aponta para serviços locais.

## Fluxo completo de setup

```bash
# 1. Configurar credenciais (uma vez)
cp scripts/.env.local.example scripts/.env.local
# Editar .env.local com suas credenciais

# 2. Sincronizar dados de produção
./scripts/sync-prod-data.sh

# 3. Rodar o backend local
./scripts/test-docker-local.sh

# App disponível em http://localhost:8080
```
