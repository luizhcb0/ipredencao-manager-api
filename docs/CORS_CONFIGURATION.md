# Configuração CORS

## Visão Geral

A API está configurada com CORS (Cross-Origin Resource Sharing) para permitir requisições de diferentes origens, com configurações específicas para desenvolvimento e produção.

## Configurações por Ambiente

### 🔧 **Local (Padrão)**

**Origens Permitidas:**
- `http://localhost:3000` (React padrão)
- `http://localhost:3001` (React alternativo)
- `http://127.0.0.1:3000` (IP local)
- `http://localhost:4200` (Angular padrão)
- `http://127.0.0.1:4200` (Angular IP local)

**Características:**
- Permissivo para desenvolvimento local
- Logs detalhados de CORS habilitados
- Todos os headers permitidos (`*`)
- SQL logs habilitados para debug
- Métodos: `GET, POST, PUT, DELETE, OPTIONS, PATCH, HEAD`

### 🔒 **Produção (Profile: prod)**

**Origens Permitidas:**
- Configuradas via variável de ambiente `ALLOWED_ORIGINS`
- Padrão: `https://seu-dominio.com,https://www.seu-dominio.com`

**Características:**
- Mais restritivo para segurança
- Headers específicos apenas
- Logs reduzidos
- Cache de preflight mais longo (24h)

## Como Usar

### Ambiente Local

```bash
# Rodar em modo local (padrão)
make run

# Ou diretamente
./gradlew bootRun
```

### Produção

```bash
# Configurar origens permitidas
export ALLOWED_ORIGINS="https://meuapp.com,https://www.meuapp.com"
export SPRING_PROFILES_ACTIVE=prod

# Rodar
make run-prod
```

### Docker/Container

```bash
# Via variáveis de ambiente
docker run -e SPRING_PROFILES_ACTIVE=prod \
           -e ALLOWED_ORIGINS="https://meuapp.com" \
           minha-api
```

## Configurações Customizadas

### Via Variáveis de Ambiente

```bash
# Origens permitidas
export ALLOWED_ORIGINS="http://localhost:3000,https://meuapp.com"

# Métodos permitidos
export ALLOWED_METHODS="GET,POST,PUT,DELETE"

# Headers permitidos
export ALLOWED_HEADERS="Content-Type,Authorization"

# Permitir credenciais
export ALLOW_CREDENTIALS=true

# Cache de preflight (segundos)
export CORS_MAX_AGE=3600
```

### Via application.properties

```properties
# Personalizar CORS
app.cors.allowed-origins=http://localhost:3000,http://localhost:3001
app.cors.allowed-methods=GET,POST,PUT,DELETE,OPTIONS
app.cors.allowed-headers=Content-Type,Authorization
app.cors.allow-credentials=true
app.cors.max-age=3600
```

## Estrutura de Arquivos

```
src/main/resources/
├── application.properties          # Configurações locais (padrão)
├── application-prod.properties     # Produção
└── ...

src/main/java/.../config/
├── CorsConfig.java                 # Configuração principal
└── CorsFilter.java                 # Filtro adicional
```

## Testando CORS

### Teste Manual com curl

```bash
# Teste de preflight
curl -X OPTIONS \
  -H "Origin: http://localhost:3000" \
  -H "Access-Control-Request-Method: POST" \
  -H "Access-Control-Request-Headers: Content-Type" \
  http://localhost:8080/formulario-pessoa

# Teste de requisição real
curl -X GET \
  -H "Origin: http://localhost:3000" \
  http://localhost:8080/formulario-pessoa
```

### Teste no Browser

```javascript
// Teste no console do navegador
fetch('http://localhost:8080/formulario-pessoa', {
  method: 'GET',
  headers: {
    'Content-Type': 'application/json',
  },
  credentials: 'include'
})
.then(response => console.log('CORS funcionando!', response))
.catch(error => console.error('Erro CORS:', error));
```

## Troubleshooting

### Erro: "CORS policy: No 'Access-Control-Allow-Origin' header"

**Solução:**
1. Verificar se a origem está na lista de permitidas
2. Conferir se o profile correto está ativo
3. Verificar logs da aplicação

```bash
# Ver logs de CORS
tail -f logs/application.log | grep CORS
```

### Erro: "CORS policy: Credentials flag is 'true'"

**Solução:**
- Verificar se `app.cors.allow-credentials=true`
- Não usar `*` em origins quando credentials=true

### Erro: "CORS policy: Method not allowed"

**Solução:**
- Adicionar método na configuração `app.cors.allowed-methods`

## Configuração para Deploy

### Heroku

```bash
# Configurar variáveis
heroku config:set SPRING_PROFILES_ACTIVE=prod
heroku config:set ALLOWED_ORIGINS="https://meuapp.herokuapp.com"
```

### AWS/Docker

```yaml
# docker-compose.yml
environment:
  - SPRING_PROFILES_ACTIVE=prod
  - ALLOWED_ORIGINS=https://meudominio.com,https://www.meudominio.com
```

### Kubernetes

```yaml
# deployment.yaml
env:
  - name: SPRING_PROFILES_ACTIVE
    value: "prod"
  - name: ALLOWED_ORIGINS
    value: "https://meuapp.com"
```

## Segurança

### ✅ **Boas Práticas Implementadas**

- Origins específicas (não usar `*` em produção)
- Headers limitados em produção
- Logs de CORS em desenvolvimento
- Configuração por ambiente
- Suporte a credenciais controlado

### ⚠️ **Configuração para Produção**

1. **SEMPRE** definir origins específicas
2. **NUNCA** usar `*` com `credentials: true`
3. Limitar headers ao mínimo necessário
4. Configurar cache adequado para preflight
5. Monitorar logs de CORS

## Exemplos de Frontend

### React

```javascript
// axios configuração
const api = axios.create({
  baseURL: 'http://localhost:8080',
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json'
  }
});
```

### Angular

```typescript
// http interceptor
export class CorsInterceptor implements HttpInterceptor {
  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const corsRequest = req.clone({
      setHeaders: {
        'Content-Type': 'application/json'
      }
    });
    return next.handle(corsRequest);
  }
}
```

### Vue.js

```javascript
// axios config
import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8080',
  withCredentials: true
});
```