-- Script para criar database e tabelas no RDS
-- Executar via CloudShell ou psql

-- 1. Criar database (conectar primeiro ao 'postgres' default)
CREATE DATABASE ipredencao_manager;

-- 2. Conectar ao novo database
\c ipredencao_manager

-- 3. Verificar
SELECT current_database();

