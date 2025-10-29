# Logout Automático - Guia Rápido para Frontend

## 🎯 Objetivo

Implementar logout automático quando o backend retorna **401 Unauthorized**.

---

## ⚡ Implementação Mínima (5 minutos)

### 1. Interceptor HTTP

Adicione este código no seu arquivo principal (ex: `main.js`, `App.js`, `index.js`):

```javascript
// Com Axios
import axios from 'axios';

axios.interceptors.response.use(
  response => response,
  error => {
    if (error.response?.status === 401) {
      // Limpar tokens
      localStorage.clear();
      sessionStorage.clear();
      
      // Redirecionar para login
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);
```

```javascript
// Com Fetch (criar wrapper)
const originalFetch = window.fetch;

window.fetch = async (...args) => {
  const response = await originalFetch(...args);
  
  if (response.status === 401) {
    localStorage.clear();
    sessionStorage.clear();
    window.location.href = '/login';
  }
  
  return response;
};
```

### 2. Adicionar Token nas Requisições

```javascript
// Exemplo de requisição
const token = localStorage.getItem('accessToken');

fetch('/api/pessoas', {
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  }
});
```

---

## 🔍 Como Testar

1. Faça login normalmente
2. Pare o banco de dados
3. Tente acessar qualquer página
4. Deve ser redirecionado para `/login` automaticamente

---

## ❓ Por que é necessário?

Quando o usuário é:
- ✅ Deletado do sistema
- ✅ Desativado pelo admin
- ✅ Teve permissões alteradas

O backend **imediatamente** rejeita o token com **401**, mesmo que ainda não tenha expirado.

O frontend precisa detectar isso e fazer logout automático.

---

## 📋 Checklist

- [ ] Interceptor HTTP implementado
- [ ] Limpeza de localStorage/sessionStorage no logout
- [ ] Redirecionamento para página de login
- [ ] Token adicionado em todas as requisições autenticadas

---

## 🚀 Próximos Passos (Opcional)

- [ ] Adicionar mensagem "Sua sessão expirou"
- [ ] Implementar refresh token
- [ ] Adicionar loading/skeleton enquanto valida token

---

Documento completo: [FRONTEND_AUTH_INTEGRATION.md](./FRONTEND_AUTH_INTEGRATION.md)

