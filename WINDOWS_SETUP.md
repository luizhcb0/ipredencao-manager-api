# Configuração para Windows

## 🚀 Guia Rápido

### Opção 1: Usando o script batch (MAIS FÁCIL)

```cmd
# Rodar a aplicação
run.bat run

# Reiniciar containers
run.bat restart

# Limpeza completa
run.bat clean
```

### Opção 2: Usando Make (se der problema)

```cmd
# Deve funcionar automaticamente
make run

# Se não funcionar, use os comandos específicos:
make win-run
make win-restart
```

### Opção 3: Comandos manuais

```cmd
# Subir containers
docker compose up -d

# Aplicar migrations
gradlew.bat update

# Regenerar JOOQ
gradlew.bat generateJooq

# Rodar aplicação
gradlew.bat bootRun
```

## ⚠️ Requisitos

- **Docker Desktop** instalado e rodando
- **Java 17+** instalado
- **Make** (opcional) - pode instalar via Chocolatey: `choco install make`

## 🔧 Se der erro de permissão

```cmd
# Dar permissão ao script
icacls run.bat /grant Everyone:F
```

## 🐛 Troubleshooting

### Erro: 'gradlew' não é reconhecido
**Solução:** Use `gradlew.bat` em vez de `./gradlew`

### Erro: Docker não está rodando
**Solução:** Abrir Docker Desktop e aguardar inicializar

### Erro: Porta já em uso
**Solução:** 
```cmd
# Parar containers
docker compose down

# Verificar se algo está na porta 8080
netstat -ano | findstr :8080

# Matar processo se necessário
taskkill /PID [número_do_processo] /F
```

### Erro: Make não funciona
**Solução:** Use o script `run.bat` ou comandos manuais

## 📋 Comandos Completos

### Script Batch
```cmd
run.bat run       # Rodar aplicação
run.bat restart   # Reiniciar
run.bat clean     # Limpeza
run.bat migrate   # Migrations
run.bat jooq      # Regenerar JOOQ
run.bat prod      # Produção
```

### Make (se funcionar)
```cmd
make run          # Rodar aplicação
make restart      # Reiniciar
make clean        # Limpeza
make migrate      # Migrations
make jooq         # Regenerar JOOQ
```

### Manual
```cmd
# Passo a passo manual
docker compose up -d
gradlew.bat update
gradlew.bat generateJooq
gradlew.bat bootRun
```

## ✅ Teste Final

Após rodar, acessar:
- http://localhost:8080/pessoas
- Deve retornar JSON (mesmo que vazio)

Se funcionar, está tudo ok! 🎉
