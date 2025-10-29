# Integração de Autenticação - Frontend

## 📋 Visão Geral

Este documento descreve como o frontend deve integrar-se com o sistema de autenticação JWT do backend, especialmente para lidar com tokens inválidos e realizar logout automático.

---

## 🔒 Validação de Token no Backend

O backend valida os seguintes aspectos do token JWT:

1. ✅ **Token não expirado** - Verifica a data de expiração
2. ✅ **Assinatura válida** - Verifica a integridade do token
3. ✅ **Usuário existe** - Consulta o banco de dados
4. ✅ **Usuário ativo** - Verifica se `active = true`
5. ✅ **Permissões corretas** - Verifica se a role no token corresponde à role atual

### Quando o Token é Inválido

Se qualquer validação falhar, o backend retorna:

**Status:** `401 Unauthorized`

**Body:**
```json
{
  "error": "Token inválido: usuário não existe"
}
```

Outras mensagens possíveis:
- `"Token inválido: usuário inativo"`
- `"Token inválido: permissões alteradas"`

---

## 🎯 Implementação Obrigatória no Frontend

### 1. Interceptor HTTP Global

O frontend **DEVE** implementar um interceptor HTTP que captura todas as respostas 401 e realiza logout automático.

#### Exemplo com Axios (React/Vue/Angular)

```javascript
import axios from 'axios';

// Configurar interceptor de resposta
axios.interceptors.response.use(
  (response) => {
    // Resposta bem-sucedida, retornar normalmente
    return response;
  },
  (error) => {
    // Verificar se é erro 401 (Unauthorized)
    if (error.response?.status === 401) {
      // Executar logout
      performLogout();
    }
    
    return Promise.reject(error);
  }
);

function performLogout() {
  // 1. Limpar tokens do armazenamento local
  localStorage.removeItem('accessToken');
  localStorage.removeItem('refreshToken');
  localStorage.removeItem('user');
  sessionStorage.clear();
  
  // 2. Limpar estado da aplicação (Redux/Vuex/Context)
  // store.dispatch('auth/logout');
  
  // 3. Redirecionar para página de login
  window.location.href = '/login';
  
  // 4. Opcional: Mostrar mensagem ao usuário
  // toast.error('Sua sessão expirou. Faça login novamente.');
}
```

#### Exemplo com Fetch (Vanilla JavaScript)

```javascript
// Wrapper para fetch com tratamento de 401
async function fetchWithAuth(url, options = {}) {
  const token = localStorage.getItem('accessToken');
  
  const response = await fetch(url, {
    ...options,
    headers: {
      ...options.headers,
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    }
  });
  
  // Verificar se é 401
  if (response.status === 401) {
    performLogout();
    throw new Error('Não autorizado');
  }
  
  return response;
}

function performLogout() {
  localStorage.clear();
  sessionStorage.clear();
  window.location.href = '/login';
}
```

#### Exemplo com React Query

```javascript
import { QueryClient } from '@tanstack/react-query';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      onError: (error) => {
        if (error.response?.status === 401) {
          performLogout();
        }
      },
    },
    mutations: {
      onError: (error) => {
        if (error.response?.status === 401) {
          performLogout();
        }
      },
    },
  },
});
```

---

### 2. Armazenamento de Tokens

**Recomendações:**

#### ✅ Fazer:
```javascript
// Armazenar tokens após login
function handleLoginSuccess(response) {
  localStorage.setItem('accessToken', response.accessToken);
  localStorage.setItem('refreshToken', response.refreshToken);
  localStorage.setItem('user', JSON.stringify(response.user));
}

// Adicionar token em todas as requisições
const headers = {
  'Authorization': `Bearer ${localStorage.getItem('accessToken')}`,
  'Content-Type': 'application/json'
};
```

#### ❌ Não Fazer:
```javascript
// NÃO armazenar em cookies sem httpOnly
document.cookie = `token=${token}`; // ❌ Vulnerável a XSS

// NÃO armazenar em variáveis globais
window.authToken = token; // ❌ Perde ao recarregar página

// NÃO deixar token exposto
console.log('Token:', token); // ❌ Pode vazar em logs
```

---

### 3. Fluxo de Login/Logout

#### Login
```javascript
async function login(credentials) {
  try {
    const response = await fetch('/api/auth/login/email', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(credentials)
    });
    
    if (response.ok) {
      const data = await response.json();
      
      // Armazenar tokens
      localStorage.setItem('accessToken', data.accessToken);
      localStorage.setItem('refreshToken', data.refreshToken);
      localStorage.setItem('user', JSON.stringify(data.user));
      
      // Redirecionar para dashboard
      window.location.href = '/dashboard';
    }
  } catch (error) {
    console.error('Erro no login:', error);
  }
}
```

#### Logout Manual
```javascript
async function logout() {
  const refreshToken = localStorage.getItem('refreshToken');
  
  try {
    // Opcional: Chamar endpoint de logout no backend
    await fetch('/api/auth/logout', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('accessToken')}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ refreshToken })
    });
  } catch (error) {
    // Mesmo se falhar, limpar tokens localmente
    console.error('Erro ao fazer logout no backend:', error);
  } finally {
    // Sempre limpar armazenamento local
    performLogout();
  }
}
```

---

### 4. Verificação de Autenticação nas Rotas

#### Exemplo com React Router

```javascript
import { Navigate } from 'react-router-dom';

function ProtectedRoute({ children }) {
  const token = localStorage.getItem('accessToken');
  
  if (!token) {
    return <Navigate to="/login" replace />;
  }
  
  return children;
}

// Uso
<Route 
  path="/dashboard" 
  element={
    <ProtectedRoute>
      <Dashboard />
    </ProtectedRoute>
  } 
/>
```

#### Exemplo com Vue Router

```javascript
router.beforeEach((to, from, next) => {
  const requiresAuth = to.matched.some(record => record.meta.requiresAuth);
  const token = localStorage.getItem('accessToken');
  
  if (requiresAuth && !token) {
    next('/login');
  } else {
    next();
  }
});
```

---

## 🔄 Refresh Token (Futuro)

Quando implementado no backend, o frontend deve:

1. Detectar quando o token está próximo de expirar
2. Chamar `/api/auth/refresh` com o refresh token
3. Atualizar o access token no localStorage
4. Continuar a requisição original

```javascript
async function refreshAccessToken() {
  const refreshToken = localStorage.getItem('refreshToken');
  
  try {
    const response = await fetch('/api/auth/refresh', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken })
    });
    
    if (response.ok) {
      const data = await response.json();
      localStorage.setItem('accessToken', data.accessToken);
      return data.accessToken;
    } else {
      // Refresh token inválido, fazer logout
      performLogout();
    }
  } catch (error) {
    performLogout();
  }
}
```

---

## 🚨 Cenários que Causam 401

### Situações onde o backend retorna 401:

1. **Token expirado**
   - Access token passou da data de validade
   - Ação: Tentar refresh token ou fazer logout

2. **Usuário deletado**
   - Administrador removeu o usuário do sistema
   - Ação: Logout imediato

3. **Usuário desativado**
   - Conta foi suspensa (`active = false`)
   - Ação: Logout imediato

4. **Permissões alteradas**
   - Role do usuário mudou no sistema
   - Ação: Logout e novo login para obter novo token

5. **Token inválido/malformado**
   - Token foi adulterado ou corrompido
   - Ação: Logout imediato

---

## 📝 Checklist de Implementação

### Obrigatório
- [ ] Interceptor HTTP para capturar respostas 401
- [ ] Função `performLogout()` que limpa todo armazenamento local
- [ ] Redirecionamento para `/login` em caso de 401
- [ ] Proteção de rotas que requerem autenticação
- [ ] Adicionar header `Authorization: Bearer {token}` em todas as requisições autenticadas

### Recomendado
- [ ] Mostrar mensagem ao usuário quando logout automático ocorrer
- [ ] Implementar retry de requisição após refresh token (quando disponível)
- [ ] Logging de eventos de autenticação para debug
- [ ] Tratamento de erros de rede (offline)

### Opcional
- [ ] Implementar "Manter conectado" com refresh token de longa duração
- [ ] Contador de tempo até expiração do token
- [ ] Renovação automática de token antes de expirar

---

## 🧪 Testes

### Testar Logout Automático

1. **Fazer login normalmente**
2. **Derrubar o banco de dados** (simula usuário deletado)
3. **Tentar acessar qualquer endpoint protegido**
4. **Verificar:** Deve receber 401 e ser redirecionado para login

```javascript
// Teste manual no console
fetch('http://localhost:8080/api/pessoas?limit=10', {
  headers: { 'Authorization': 'Bearer SEU_TOKEN_AQUI' }
})
.then(res => {
  console.log('Status:', res.status);
  if (res.status === 401) {
    console.log('✅ Backend está rejeitando token inválido');
  }
});
```

---

## 📚 Referências

- [JWT Best Practices](https://datatracker.ietf.org/doc/html/rfc8725)
- [OWASP Authentication Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html)
- Backend: `src/main/java/org/ipredencao/ipredencao_manager/filter/JwtAuthenticationFilter.java`

---

## 💬 Suporte

Em caso de dúvidas sobre a integração, consulte:
- Documentação da API: `/docs/` (quando disponível)
- Código do filtro JWT: `JwtAuthenticationFilter.java`
- Este documento: `docs/FRONTEND_AUTH_INTEGRATION.md`

