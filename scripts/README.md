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

# Script de Atualização de Transferidos

Este script atualiza pessoas transferidas para o status EX_MEMBRO com informações de demissão.

## Pré-requisitos

1. Python 3.8 ou superior
2. Arquivo `transferidos.xlsx` em `src/main/resources/IPR_Dump/`
3. API rodando e acessível
4. Token JWT de autenticação válido com role PRESBITERO ou ADMIN
5. Pessoas já devem existir no sistema (importadas previamente)

## Instalação

```bash
cd scripts
pip install -r requirements.txt
```

## Uso

### 1. Preparar o arquivo Excel

O arquivo `transferidos.xlsx` deve conter:
- **Primeira coluna (ou coluna "Nome"):** Nome da pessoa a ser atualizada
- **Última coluna:** Tipo de membro ("Comungante" ou "Não Comungante")

Exemplo:

| Nome | ... | Tipo |
|------|-----|------|
| João Silva | ... | Comungante |
| Maria Santos | ... | Não Comungante |

### 2. Executar o script

#### Opção A: Modo Dry-Run (Teste sem alterações)

Recomendado para validar antes de executar:

```bash
export API_TOKEN="seu-token-jwt-aqui"

cd scripts

python3 atualizar_transferidos.py \
  --api-url http://localhost:8080 \
  --xlsx-path ../src/main/resources/IPR_Dump/transferidos.xlsx \
  --dry-run
```

#### Opção B: Execução Real

Após validar no dry-run:

```bash
export API_TOKEN="seu-token-jwt-aqui"

cd scripts

python3 atualizar_transferidos.py \
  --api-url http://localhost:8080 \
  --xlsx-path ../src/main/resources/IPR_Dump/transferidos.xlsx
```

#### Opção C: Com logs em arquivo

```bash
export API_TOKEN="seu-token-jwt-aqui"

cd scripts

python3 atualizar_transferidos.py \
  --api-url http://localhost:8080 \
  --xlsx-path ../src/main/resources/IPR_Dump/transferidos.xlsx \
  > atualizar_transferidos_output.log 2>&1 &

# Acompanhar progresso
tail -f atualizar_transferidos_output.log
```

## O que o script faz

Para cada pessoa no arquivo `transferidos.xlsx`:

1. **Busca pelo nome** via `POST /api/pessoas/search`
2. **Valida resultado:**
   - Se não encontrar: registra como "não encontrada"
   - Se encontrar múltiplas: tenta match exato do nome
   - Se encontrar uma única: prossegue
3. **Obtém dados completos** via `GET /api/pessoas/{id}`
4. **Verifica se já é EX_MEMBRO** - se sim, pula
5. **Determina tipo de demissão** baseado na última coluna do xlsx:
   - "Comungante" → `30/11/2025 | Demissão de membro comungante | Demissão por transferência | Ata: 292`
   - "Não Comungante" → `30/11/2025 | Demissão de menor não comungante | Demissão por transferência | Ata: 292`
6. **Atualiza via PUT** `/api/pessoas/{id}`:
   - **categoria:** 33 (EX_MEMBRO)
   - **campus:** VIDEIRA
   - **informacoesAdicionais:** adiciona a linha de demissão (com quebra de linha se já houver conteúdo)

## Logs

O script gera logs em:
- Console (stdout/stderr)
- Arquivo `atualizar_transferidos.log` (sempre criado)

## Monitoramento

```bash
# Ver últimas 30 linhas do log
tail -30 atualizar_transferidos.log

# Acompanhar em tempo real
tail -f atualizar_transferidos.log

# Ver apenas erros
grep -i error atualizar_transferidos.log

# Ver pessoas atualizadas com sucesso
grep "atualizada com sucesso" atualizar_transferidos.log

# Ver pessoas não encontradas
grep "não encontrada" atualizar_transferidos.log

# Ver múltiplos resultados
grep "Múltiplas pessoas" atualizar_transferidos.log
```

## Relatório Final

Ao final, o script exibe um resumo:

```
================================================================================
PROCESSAMENTO CONCLUÍDO
Total de registros: 50
Atualizados com sucesso: 45
Não encontrados: 2
Múltiplos resultados (sem match exato): 1
Pulados (já ex-membro ou inválido): 2
Erros: 0
================================================================================
```

## Tratamento de Casos Especiais

### Múltiplas pessoas com nome similar

Se a busca retornar múltiplas pessoas:
- O script tenta fazer **match exato** (case-insensitive)
- Se houver 1 match exato: usa esse
- Se houver 0 ou mais de 1: pula e registra no log

### Pessoas já EX_MEMBRO

Se a pessoa já tiver categoria EX_MEMBRO (33), o script:
- Pula a atualização
- Registra no log
- Conta como "pulado"

### Nome não encontrado

Se nenhuma pessoa for encontrada com o nome:
- Registra no log
- Conta como "não encontrado"
- Continua processando os demais

## Troubleshooting

### Erro: "API_TOKEN não definida"
Configure a variável de ambiente com seu token JWT.

### Erro: "Arquivo não encontrado"
Verifique o caminho do arquivo xlsx em `--xlsx-path`.

### Erro: "401 Unauthorized"
Seu token expirou ou não tem permissões. Faça login novamente.

### Aviso: "Coluna 'Nome' não encontrada"
O script usará a primeira coluna. Verifique se está correto.

### Aviso: "Múltiplas pessoas encontradas"
Revise o nome no xlsx ou atualize manualmente. O script pula automaticamente casos ambíguos.

## Exemplo de Execução

```bash
export API_TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

python3 atualizar_transferidos.py \
  --api-url http://localhost:8080 \
  --xlsx-path ../src/main/resources/IPR_Dump/transferidos.xlsx

# Saída esperada:
# ================================================================================
# PROCESSANDO TRANSFERIDOS
# ================================================================================
# Arquivo carregado com 50 linhas
# Colunas encontradas: ['Nome', 'Tipo']
# Usando coluna 'Nome' para nomes
# Usando coluna 'Tipo' para tipo de membro
# [1/50] Processando: João Silva (Comungante)
# Pessoa encontrada: ID 123 - João Silva
# ✓ Pessoa 123 (Comungante) atualizada com sucesso!
# [2/50] Processando: Maria Santos (Não Comungante)
# ...
# ================================================================================
# PROCESSAMENTO CONCLUÍDO
# Total de registros: 50
# Atualizados com sucesso: 48
# ...
# ================================================================================
```


