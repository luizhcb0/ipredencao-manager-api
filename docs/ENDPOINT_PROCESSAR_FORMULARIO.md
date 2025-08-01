# Endpoint para Processar Formulário

## Visão Geral

O endpoint `POST /formulario-pessoa/processar` permite processar um formulário e criar/atualizar pessoas no sistema, incluindo todos os relacionamentos familiares.

## Funcionalidades

### 1. **Criação/Atualização da Pessoa Principal**
- Se `pessoaId` for fornecido: atualiza pessoa existente
- Se `pessoaId` for `null`: cria nova pessoa

### 2. **Processamento de Relacionamentos**
Os campos de relacionamento podem conter:
- **ID numérico**: referencia pessoa existente no sistema
- **Nome**: cria nova pessoa com apenas o nome

### 3. **Tipos de Relacionamento Suportados**
- **Pai** (`nome_pai`)
- **Mãe** (`nome_mae`) 
- **Filhos** (`nome_filhos` - array)
- **Parceiro** (`nome_parceiro`)
- **Chefe de Família** (`chefe_de_familia_id` - apenas ID)

### 4. **Propagação de Endereço**
Se `propagar_endereco_chefe_familia = true`, os dados de endereço do chefe de família são copiados para a pessoa principal.

## Request

```json
{
  "formularioId": 123,
  "pessoaId": null  // ou ID da pessoa para atualizar
}
```

## Response

```json
{
  "pessoaPrincipal": {
    "id": 456,
    "nome": "João Silva",
    // ... outros campos da pessoa
  },
  "relacionamentosCriados": [
    {
      "id": 1,
      "pessoaId": 456,
      "pessoaRelacionadaId": 789,
      "tipoRelacionamento": "PAI"
    }
  ],
  "mensagem": "Pessoa criada e relacionamentos criados com sucesso"
}
```

## Fluxo de Processamento

### 1. **Validação**
- Verifica se o formulário existe
- Valida se o chefe de família existe (se fornecido)

### 2. **Processamento de Relacionamentos (Ordem)**
1. **Pai**: cria/busca pessoa do pai
2. **Mãe**: cria/busca pessoa da mãe  
3. **Parceiro**: cria/busca pessoa do parceiro
4. **Filhos**: cria/busca pessoas dos filhos
5. **Chefe de Família**: busca pessoa existente

### 3. **Pessoa Principal**
- Cria nova pessoa OU atualiza existente
- Mapeia todos os dados do formulário
- Define chefe de família se fornecido
- Propaga endereço se solicitado

### 4. **Atualização do Formulário**
- Vincula o formulário à pessoa criada/atualizada (`pessoa_id`)

### 5. **Criação de Relacionamentos**
- **Parceiro**: tipo baseado no estado civil
  - `CASADO` → `CONJUGE`
  - `NOIVO` → `NOIVO` 
  - `NAMORANDO` → `NAMORADO`
  - Outros → `NAMORADO` (padrão)
- **Filhos**: todos como `FILHO`
- **Pai**: como `PAI`
- **Mãe**: como `MAE`

## Exemplos de Uso

### Exemplo 1: Criar Nova Pessoa
```json
{
  "formularioId": 123,
  "pessoaId": null
}
```

### Exemplo 2: Atualizar Pessoa Existente
```json
{
  "formularioId": 123,
  "pessoaId": 456
}
```

### Exemplo 3: Formulário com Relacionamentos
Formulário com:
- `nome_pai`: "José Silva" (nome - será criado)
- `nome_mae`: "123" (ID - pessoa existente)
- `nome_parceiro`: "Maria Santos" (nome - será criado)
- `nome_filhos`: ["Pedro Silva", "456"] (nomes e IDs mistos)
- `chefe_de_familia_id`: 789 (ID - pessoa existente)
- `propagar_endereco_chefe_familia`: true

## Tratamento de Erros

### 400 Bad Request
- Formulário não encontrado
- Chefe de família não encontrado
- Pessoa para atualização não encontrada

### 500 Internal Server Error
- Erros de banco de dados
- Erros inesperados no processamento

## Campos Mapeados do Formulário para Pessoa

Todos os campos do formulário são mapeados para a pessoa:
- Dados pessoais (nome, email, telefone, etc.)
- Endereço e localização
- Dados eclesiásticos
- Profissão e empresa
- Relacionamentos familiares

## Database Changes

### Nova Coluna: `pessoa_id`
- Referencia a pessoa criada/atualizada a partir do formulário
- Permite rastrear qual pessoa foi gerada de cada formulário

## Validações

- **Email único**: mantida a validação de email único por formulário
- **IDs válidos**: verifica se pessoas referenciadas existem
- **Dados obrigatórios**: mantém validações do modelo Pessoa

## Notas Importantes

1. **Ordem de criação**: relacionamentos são criados ANTES da pessoa principal
2. **Transações**: todo o processo deve ser atômico
3. **Chefe de família**: apenas aceita ID, não cria novas pessoas
4. **Propagação de endereço**: acontece após criação/atualização da pessoa principal
5. **Relacionamentos bidirecionais**: apenas cria relacionamento da pessoa principal para as outras