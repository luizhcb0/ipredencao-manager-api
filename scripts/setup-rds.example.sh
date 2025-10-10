#!/bin/bash

# ============================================
# Exemplo de uso do script setup-rds.sh
# ============================================
# 
# Este arquivo mostra como executar o script setup-rds.sh
# com sua infraestrutura existente
#
# PASSOS:
# 1. Copie este arquivo e remova o .example
# 2. Preencha os valores das suas variáveis AWS
# 3. Execute: ./setup-rds-config.sh
# ============================================

# ⚠️ PREENCHA COM SEUS VALORES REAIS:

export DB_PASSWORD="SUA_SENHA_FORTE_AQUI"        # Senha do banco (mínimo 8 caracteres)

export VPC_ID="vpc-xxxxxxxxxxxxxxxxx"            # ID da sua VPC
export SUBNET_ID_1="subnet-xxxxxxxxxxxxxxxxx"    # ID da primeira subnet (AZ diferente)
export SUBNET_ID_2="subnet-yyyyyyyyyyyyyyyyy"    # ID da segunda subnet (AZ diferente)
export SECURITY_GROUP_ID="sg-xxxxxxxxxxxxxxxxx"  # ID do security group do RDS

# Execute o script
./setup-rds.sh
