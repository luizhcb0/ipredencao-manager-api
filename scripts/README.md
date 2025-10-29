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
- Cria pessoas via `POST /api/pessoas`
- Salva mapeamento de IDs antigos → novos em `id_mapping.json`

**Dados importados:**
- Nome, apelido, sexo, CPF, RG
- Data de nascimento
- Estado civil, categoria
- Telefones (primário e secundários)
- Emails (primário e secundários)
- Endereço (CEP, logradouro)
- Região
- Datas de batismo e profissão de fé
- Igreja anterior/de batismo
- Tipo de batismo (inferido)

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
- Outros → AGREGADO_FAMILIAR (id: 36)

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
- ~2737 pessoas
- ~1000 fotos
- ~5000 relacionamentos


