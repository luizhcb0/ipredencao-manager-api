#!/bin/bash

# Script auxiliar para executar a importação do IPR Dump
# 
# Uso:
#   ./run_import.sh [token]
#
# Se o token não for fornecido como argumento, o script tentará usar
# a variável de ambiente API_TOKEN

set -e

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Função para imprimir mensagens coloridas
print_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Verificar se está no diretório correto
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR"

print_info "Script de importação IPR Dump"
echo ""

# Verificar Python
if ! command -v python3 &> /dev/null; then
    print_error "Python 3 não encontrado. Instale Python 3.8 ou superior."
    exit 1
fi

PYTHON_VERSION=$(python3 --version | cut -d' ' -f2)
print_info "Python encontrado: $PYTHON_VERSION"

# Verificar e instalar dependências
if [ ! -d "venv" ]; then
    print_info "Criando ambiente virtual Python..."
    python3 -m venv venv
fi

print_info "Ativando ambiente virtual..."
source venv/bin/activate

print_info "Instalando/atualizando dependências..."
pip install -q --upgrade pip
pip install -q -r requirements.txt

# Obter token
if [ -n "$1" ]; then
    export API_TOKEN="$1"
    print_info "Token fornecido como argumento"
elif [ -z "$API_TOKEN" ]; then
    print_error "Token de autenticação não fornecido!"
    echo ""
    echo "Uso:"
    echo "  ./run_import.sh YOUR_JWT_TOKEN"
    echo "ou"
    echo "  export API_TOKEN='YOUR_JWT_TOKEN'"
    echo "  ./run_import.sh"
    exit 1
else
    print_info "Usando token da variável de ambiente API_TOKEN"
fi

# Verificar se API está acessível
API_URL="${API_URL:-http://localhost:8080}"
print_info "Verificando API em $API_URL..."

if ! curl -s -f "$API_URL/api/auth/health" > /dev/null 2>&1; then
    print_warning "API não está respondendo em $API_URL"
    print_warning "Certifique-se de que a API está rodando antes de continuar."
    echo ""
    read -p "Deseja continuar mesmo assim? (s/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Ss]$ ]]; then
        print_info "Abortando importação."
        exit 1
    fi
else
    print_info "API está respondendo ✓"
fi

# Verificar arquivos do dump
DUMP_PATH="../src/main/resources/IPR_Dump"
if [ ! -f "$DUMP_PATH/Pessoa.csv" ]; then
    print_error "Arquivo $DUMP_PATH/Pessoa.csv não encontrado!"
    exit 1
fi

if [ ! -f "$DUMP_PATH/Pessoa_Contato.csv" ]; then
    print_error "Arquivo $DUMP_PATH/Pessoa_Contato.csv não encontrado!"
    exit 1
fi

if [ ! -d "$DUMP_PATH/fotos" ]; then
    print_error "Diretório $DUMP_PATH/fotos não encontrado!"
    exit 1
fi

print_info "Arquivos do dump verificados ✓"
echo ""

# Mostrar resumo
print_info "Resumo da importação:"
echo "  - API URL: $API_URL"
echo "  - Dump Path: $DUMP_PATH"
echo "  - Pessoas no CSV: $(wc -l < "$DUMP_PATH/Pessoa.csv" | xargs)"
echo "  - Contatos no CSV: $(wc -l < "$DUMP_PATH/Pessoa_Contato.csv" | xargs)"
echo "  - Fotos disponíveis: $(ls -1 "$DUMP_PATH/fotos" | wc -l | xargs)"
echo ""

# Perguntar confirmação
read -p "Iniciar importação? (s/N): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Ss]$ ]]; then
    print_info "Importação cancelada."
    exit 0
fi

# Executar importação
print_info "Iniciando importação..."
echo ""

python3 import_dump.py --api-url "$API_URL" --dump-path "$DUMP_PATH"

# Verificar resultado
if [ $? -eq 0 ]; then
    echo ""
    print_info "Importação concluída com sucesso! ✓"
    
    if [ -f "import_checkpoint.json" ]; then
        echo ""
        print_info "Estatísticas:"
        cat import_checkpoint.json | python3 -m json.tool | grep -E "pessoas_created|fotos_uploaded|relacionamentos_created"
    fi
else
    echo ""
    print_error "Importação falhou. Verifique o log em import_dump.log"
    print_info "Você pode retomar a importação executando o script novamente."
    exit 1
fi


