# Tratamento de Erros - API

## Visão Geral

A API agora retorna erros de forma consistente e estruturada para o frontend, sem precisar de handlers globais ou classes complexas.

## Formato da Resposta de Erro

### Estrutura Padrão

```json
{
  "error": "Erro",
  "message": "Mensagem específica do erro"
}
```

### Estrutura com Contexto

```json
{
  "error": "Erro interno", 
  "message": "Detalhes técnicos do erro"
}
```

## Tipos de Erro por Status Code

### 400 - Bad Request
**Quando:** Dados inválidos, regras de negócio violadas

**Exemplo - Email duplicado:**
```json
{
  "error": "Erro",
  "message": "Já existe um formulário cadastrado com este email: joao@email.com"
}
```

**Exemplo - Validação:**
```json
{
  "error": "Erro", 
  "message": "Nome é obrigatório"
}
```

### 404 - Not Found
**Quando:** Recurso não encontrado

**Exemplo - Formulário:**
```json
{
  "error": "Erro",
  "message": "Formulário não encontrado"
}
```

**Exemplo - Pessoa:**
```json
{
  "error": "Erro",
  "message": "Pessoa não encontrada"
}
```

### 500 - Internal Server Error
**Quando:** Erros inesperados, problemas de infraestrutura

**Exemplo - Erro geral:**
```json
{
  "error": "Erro interno",
  "message": "Detalhes técnicos do erro"
}
```

**Exemplo - Upload:**
```json
{
  "error": "Erro ao fazer upload",
  "message": "IOException: Arquivo corrompido"
}
```

## Endpoints Atualizados

### FormularioPessoaController

| Endpoint | Erros Tratados |
|----------|---------------|
| `POST /formulario-pessoa` | Email duplicado (400), Erro interno (500) |
| `PUT /formulario-pessoa/{id}` | Email duplicado (400), Erro interno (500) |
| `POST /formulario-pessoa/{id}/foto` | Formulário não encontrado (404), Erro upload (500) |
| `GET /formulario-pessoa/{id}` | Formulário não encontrado (404), Erro interno (500) |
| `POST /formulario-pessoa/search` | Erro na busca (500) |

### PessoaController

| Endpoint | Erros Tratados |
|----------|---------------|
| `POST /pessoas` | Validação (400), Erro interno (500) |
| `PUT /pessoas/{id}` | Pessoa não encontrada (404), Validação (400), Erro interno (500) |
| `POST /pessoas/{id}/foto` | Pessoa não encontrada (404), Erro upload (500) |
| `GET /pessoas/{id}` | Pessoa não encontrada (404), Erro interno (500) |
| `POST /pessoas/search` | Erro na busca (500) |
| `POST /pessoas/{id}/relacionamentos` | Pessoa não encontrada (404), Validação (400) |
| `GET /pessoas/{id}/relacionamentos` | Pessoa não encontrada (404), Erro interno (500) |

## Como Tratar no Frontend

### JavaScript/Fetch

```javascript
fetch('/formulario-pessoa', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify(formulario)
})
.then(response => {
  if (!response.ok) {
    return response.json().then(error => {
      throw new Error(error.message || 'Erro desconhecido');
    });
  }
  return response.json();
})
.then(data => {
  console.log('Sucesso:', data);
})
.catch(error => {
  console.error('Erro:', error.message);
  // Mostrar erro para o usuário
  alert(error.message);
});
```

### Axios

```javascript
axios.post('/formulario-pessoa', formulario)
  .then(response => {
    console.log('Sucesso:', response.data);
  })
  .catch(error => {
    const message = error.response?.data?.message || 'Erro desconhecido';
    console.error('Erro:', message);
    alert(message);
  });
```

### React Hook

```javascript
const [error, setError] = useState('');

const criarFormulario = async (dados) => {
  try {
    setError('');
    const response = await fetch('/formulario-pessoa', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(dados)
    });
    
    if (!response.ok) {
      const errorData = await response.json();
      throw new Error(errorData.message);
    }
    
    const resultado = await response.json();
    // Sucesso...
  } catch (err) {
    setError(err.message);
  }
};
```

## Implementação no Backend

### Padrão Usado

Cada endpoint usa try-catch simples:

```java
@PostMapping
public ResponseEntity<?> criar(@RequestBody FormularioPessoa formulario) {
    try {
        FormularioPessoa criado = service.criar(formulario);
        return ResponseEntity.ok(criado);
    } catch (IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
    } catch (Exception e) {
        return ResponseEntity.internalServerError().body(new ErrorResponse("Erro interno", e.getMessage()));
    }
}
```

### Classe ErrorResponse

```java
public class ErrorResponse {
    private String error;
    private String message;
    
    public ErrorResponse(String message) {
        this.error = "Erro";
        this.message = message;
    }
    
    public ErrorResponse(String error, String message) {
        this.error = error;
        this.message = message;
    }
    
    // getters e setters...
}
```

## Vantagens da Abordagem

✅ **Simples**: Sem handlers globais ou configurações complexas
✅ **Consistente**: Mesmo formato em todos os endpoints  
✅ **Flexível**: Cada endpoint pode customizar suas mensagens
✅ **Informativo**: Mensagens claras em português
✅ **Compatível**: Funciona com qualquer frontend

## Testando

### Teste de Email Duplicado

```bash
# Criar primeiro formulário
curl -X POST http://localhost:8080/formulario-pessoa \
  -H "Content-Type: application/json" \
  -d '{"email": "teste@email.com", "nome": "João"}'

# Tentar criar segundo com mesmo email
curl -X POST http://localhost:8080/formulario-pessoa \
  -H "Content-Type: application/json" \
  -d '{"email": "teste@email.com", "nome": "Maria"}'

# Resposta esperada:
# Status: 400
# Body: {"error": "Erro", "message": "Já existe um formulário cadastrado com este email: teste@email.com"}
```

### Teste de Recurso Não Encontrado

```bash
curl -X GET http://localhost:8080/formulario-pessoa/99999

# Resposta esperada:
# Status: 404  
# Body: {"error": "Erro", "message": "Formulário não encontrado"}
```