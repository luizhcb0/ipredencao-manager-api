#!/usr/bin/env python3
"""
Script para atualizar pessoas transferidas para EX_MEMBRO

Lê o arquivo transferidos.xlsx e para cada pessoa:
1. Busca pelo nome no endpoint de search
2. Se encontrar, atualiza para EX_MEMBRO com campus VIDEIRA
3. Adiciona informação de demissão nas informações adicionais

Uso:
    export API_TOKEN="seu-token-jwt-aqui"
    python atualizar_transferidos.py --api-url http://localhost:8080 --xlsx-path ../src/main/resources/IPR_Dump/transferidos.xlsx
"""

import argparse
import json
import logging
import os
import sys
from pathlib import Path
from typing import Dict, Optional, Any
import time

import pandas as pd
import requests

# Configuração de logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler('atualizar_transferidos.log'),
        logging.StreamHandler(sys.stdout)
    ]
)
logger = logging.getLogger(__name__)


class APIClient:
    """Cliente para comunicação com a API"""
    def __init__(self, api_url: str, token: str):
        self.api_url = api_url.rstrip('/')
        self.token = token
        self.session = requests.Session()
        self.session.headers.update({'Authorization': f'Bearer {token}'})
    
    def _retry_request(self, method: str, url: str, max_retries: int = 3, **kwargs) -> requests.Response:
        """Executa requisição com retry em caso de falha"""
        for attempt in range(max_retries):
            try:
                response = self.session.request(method, url, **kwargs)
                
                # Log detalhado em caso de erro
                if response.status_code >= 400:
                    logger.error(f"Erro HTTP {response.status_code}: {response.text[:500]}")
                
                response.raise_for_status()
                return response
            except requests.exceptions.RequestException as e:
                if attempt == max_retries - 1:
                    raise
                logger.warning(f"Tentativa {attempt + 1} falhou: {e}. Tentando novamente...")
                time.sleep(2 ** attempt)  # Backoff exponencial
    
    def search_pessoa(self, nome: str) -> list:
        """Busca pessoa por nome"""
        url = f"{self.api_url}/api/pessoas/search"
        headers = {
            'Authorization': f'Bearer {self.token}',
            'Content-Type': 'application/json'
        }
        payload = {
            "nome": nome,
            "pagination": {
                "limit": 50,
                "offset": 0
            }
        }
        
        logger.debug(f"Buscando pessoa: {nome}")
        response = self._retry_request('POST', url, headers=headers, json=payload)
        result = response.json()
        return result.get('data', [])
    
    def get_pessoa(self, pessoa_id: int) -> Dict:
        """Busca pessoa por ID"""
        url = f"{self.api_url}/api/pessoas/{pessoa_id}"
        headers = {
            'Authorization': f'Bearer {self.token}',
            'Content-Type': 'application/json'
        }
        response = self._retry_request('GET', url, headers=headers)
        return response.json()
    
    def update_pessoa(self, pessoa_id: int, pessoa_data: Dict) -> Dict:
        """Atualiza uma pessoa via PUT"""
        url = f"{self.api_url}/api/pessoas/{pessoa_id}"
        headers = {
            'Authorization': f'Bearer {self.token}',
            'Content-Type': 'application/json'
        }
        logger.debug(f"Atualizando pessoa {pessoa_id}")
        logger.debug(f"Payload: {json.dumps(pessoa_data, indent=2, ensure_ascii=False)}")
        response = self._retry_request('PUT', url, headers=headers, json=pessoa_data)
        return response.json()


def determinar_tipo_demissao(tipo_membro: str) -> str:
    """
    Determina o tipo de demissão baseado no tipo de membro informado no xlsx.
    
    Args:
        tipo_membro: Tipo de membro da coluna do xlsx (ex: "Comungante", "Não Comungante", etc)
    
    Returns:
        String com a informação de demissão formatada
    """
    tipo_lower = str(tipo_membro).lower().strip()
    
    # Variações possíveis para identificar não comungantes
    nao_comungante_keywords = ['não comungante', 'nao comungante', 'menor', 'não-comungante']
    
    eh_nao_comungante = any(keyword in tipo_lower for keyword in nao_comungante_keywords)
    
    if eh_nao_comungante:
        return "30/11/2025 | Demissão de menor não comungante | Demissão por transferência | Ata: 292"
    else:
        # Default para membro comungante
        return "30/11/2025 | Demissão de membro comungante | Demissão por transferência | Ata: 292"


def processar_transferidos(api_client: APIClient, xlsx_path: Path, dry_run: bool = False):
    """Processa a lista de transferidos do arquivo xlsx"""
    
    logger.info("=" * 80)
    logger.info("PROCESSANDO TRANSFERIDOS")
    logger.info("=" * 80)
    
    # Carregar xlsx
    logger.info(f"Carregando {xlsx_path}")
    
    try:
        df = pd.read_excel(xlsx_path)
    except Exception as e:
        logger.error(f"Erro ao carregar arquivo xlsx: {e}")
        sys.exit(1)
    
    logger.info(f"Arquivo carregado com {len(df)} linhas")
    logger.info(f"Colunas encontradas: {list(df.columns)}")
    
    nome_column = 'nome'
    tipo_membro_column = 'status'
    
    logger.info(f"Usando coluna '{nome_column}' para nomes")
    logger.info(f"Usando coluna '{tipo_membro_column}' para tipo de membro")
    
    # Estatísticas
    success_count = 0
    not_found_count = 0
    multiple_found_count = 0
    error_count = 0
    skipped_count = 0
    
    total = len(df)
    
    for idx, row in df.iterrows():
        nome = str(row[nome_column]).strip() if not pd.isna(row[nome_column]) else None
        tipo_membro = str(row[tipo_membro_column]).strip() if not pd.isna(row[tipo_membro_column]) else 'Comungante'
        
        if not nome or nome == 'nan':
            logger.warning(f"[{idx + 1}/{total}] Nome vazio ou inválido na linha {idx + 1}")
            skipped_count += 1
            continue
        
        logger.info(f"[{idx + 1}/{total}] Processando: {nome} ({tipo_membro})")
        
        try:
            # 1. Buscar pessoa por nome
            resultados = api_client.search_pessoa(nome)
            
            if not resultados:
                logger.warning(f"Pessoa não encontrada: {nome}")
                not_found_count += 1
                continue
            
            if len(resultados) > 1:
                logger.warning(f"Múltiplas pessoas encontradas para '{nome}': {len(resultados)} resultados")
                logger.warning(f"IDs encontrados: {[p['id'] for p in resultados]}")
                multiple_found_count += 1
                
                # Tentar encontrar match exato (case-insensitive)
                matches_exatos = [p for p in resultados if p['nome'].strip().lower() == nome.lower()]
                if len(matches_exatos) == 1:
                    logger.info(f"Match exato encontrado: ID {matches_exatos[0]['id']}")
                    pessoa_encontrada = matches_exatos[0]
                else:
                    # Se não tem match exato único, pular
                    logger.warning(f"Não foi possível determinar pessoa única. Pulando.")
                    continue
            else:
                pessoa_encontrada = resultados[0]
            
            pessoa_id = pessoa_encontrada['id']
            logger.info(f"Pessoa encontrada: ID {pessoa_id} - {pessoa_encontrada['nome']}")
            
            # 2. Fazer GET completo da pessoa
            pessoa = api_client.get_pessoa(pessoa_id)
            
            # Verificar se já é ex-membro
            categoria_atual = pessoa['categoria']
            if isinstance(categoria_atual, dict):
                categoria_id = categoria_atual['id']
            else:
                categoria_id = categoria_atual
            
            if categoria_id == 33:  # EX_MEMBRO
                logger.info(f"Pessoa {pessoa_id} já é EX_MEMBRO. Pulando.")
                skipped_count += 1
                continue
            
            # 3. Determinar tipo de demissão baseado no tipo de membro do xlsx
            info_demissao = determinar_tipo_demissao(tipo_membro)
            logger.debug(f"Tipo de membro: {tipo_membro} -> {info_demissao}")
            
            # 4. Preparar payload de atualização
            # Converter categoria para ID se necessário
            if isinstance(pessoa.get('categoria'), dict):
                pessoa['categoria'] = pessoa['categoria']['id']
            
            # Remover relacionamentos do payload (não são aceitos no PUT)
            pessoa.pop('relacionamentos', None)
            
            # Atualizar campos
            pessoa['categoria'] = 33  # EX_MEMBRO
            pessoa['campus'] = 'VIDEIRA'
            
            # Adicionar informação de demissão às informações adicionais
            info_adicional_atual = pessoa.get('informacoesAdicionais', '')
            if info_adicional_atual:
                pessoa['informacoesAdicionais'] = f"{info_adicional_atual}\n{info_demissao}"
            else:
                pessoa['informacoesAdicionais'] = info_demissao
            
            # 5. Atualizar pessoa
            if dry_run:
                logger.info(f"[DRY RUN] Atualizaria pessoa {pessoa_id} ({tipo_membro}):")
                logger.info(f"  - Categoria: {categoria_id} -> 33 (EX_MEMBRO)")
                logger.info(f"  - Campus: {pessoa.get('campus', 'N/A')} -> VIDEIRA")
                logger.info(f"  - Info adicional: +{info_demissao}")
            else:
                api_client.update_pessoa(pessoa_id, pessoa)
                logger.info(f"✓ Pessoa {pessoa_id} ({tipo_membro}) atualizada com sucesso!")
            
            success_count += 1
            
        except Exception as e:
            error_count += 1
            logger.error(f"Erro ao processar '{nome}': {e}")
            if error_count > 10:
                logger.error("Muitos erros. Abortando.")
                raise
    
    # Relatório final
    logger.info("=" * 80)
    logger.info("PROCESSAMENTO CONCLUÍDO")
    logger.info(f"Total de registros: {total}")
    logger.info(f"Atualizados com sucesso: {success_count}")
    logger.info(f"Não encontrados: {not_found_count}")
    logger.info(f"Múltiplos resultados (sem match exato): {multiple_found_count}")
    logger.info(f"Pulados (já ex-membro ou inválido): {skipped_count}")
    logger.info(f"Erros: {error_count}")
    logger.info("=" * 80)


def main():
    parser = argparse.ArgumentParser(description='Atualizar pessoas transferidas para EX_MEMBRO')
    parser.add_argument('--api-url', default='http://localhost:8080', help='URL base da API')
    parser.add_argument('--xlsx-path', required=True, help='Caminho para o arquivo transferidos.xlsx')
    parser.add_argument('--dry-run', action='store_true', help='Modo dry-run (não faz alterações)')
    
    args = parser.parse_args()
    
    # Obter token de autenticação
    token = os.environ.get('API_TOKEN')
    if not token:
        logger.error("Variável de ambiente API_TOKEN não definida!")
        logger.error("Use: export API_TOKEN='seu-token-jwt-aqui'")
        sys.exit(1)
    
    # Validar arquivo
    xlsx_path = Path(args.xlsx_path)
    if not xlsx_path.exists():
        logger.error(f"Arquivo não encontrado: {xlsx_path}")
        sys.exit(1)
    
    # Inicializar cliente
    api_client = APIClient(args.api_url, token)
    
    logger.info("Iniciando processamento de transferidos")
    logger.info(f"API URL: {args.api_url}")
    logger.info(f"Arquivo: {xlsx_path}")
    if args.dry_run:
        logger.info("MODO DRY-RUN: Nenhuma alteração será feita")
    
    try:
        processar_transferidos(api_client, xlsx_path, args.dry_run)
        logger.info("Processamento finalizado com sucesso!")
        
    except Exception as e:
        logger.error(f"Erro durante processamento: {e}", exc_info=True)
        sys.exit(1)


if __name__ == '__main__':
    main()

