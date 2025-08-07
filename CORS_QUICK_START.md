# CORS - Guia Rápido

## 🚀 Para rodar localmente (com frontend na porta 3000)

```bash
# Subir containers e rodar aplicação
make run
```

A aplicação já está configurada para aceitar requisições de:
- `http://localhost:3000`
- `http://localhost:3001` 
- `http://localhost:4200`
- `http://127.0.0.1:3000`
- `http://127.0.0.1:4200`

## 🔧 Para frontend em outra porta

Edite `src/main/resources/application.properties`:

```properties
app.cors.allowed-origins=http://localhost:3000,http://localhost:SUA_PORTA
```

## 🌐 Para produção

```bash
# Definir domínios permitidos
export ALLOWED_ORIGINS="https://meuapp.com,https://www.meuapp.com"
export SPRING_PROFILES_ACTIVE=prod

# Rodar
make run-prod
```

## ✅ Testar CORS

No console do navegador:

```javascript
fetch('http://localhost:8080/formulario-pessoa')
  .then(r => console.log('CORS OK!', r))
  .catch(e => console.error('CORS erro:', e));
```

## 📚 Documentação completa

Ver: `docs/CORS_CONFIGURATION.md`