#!/usr/bin/env python3
"""
Script de importação de dados do IPR_Dump para a API ipredencao-manager

Fases:
1. Importar Pessoas (com contatos consolidados)
2. Upload de Fotos
2.5. Definir Chefe de Família
3. Criar Relacionamentos (Pai, Mãe, Cônjuge)

Uso:
    export API_TOKEN="seu-token-jwt-aqui"
    python import_dump.py --api-url http://localhost:8080 --dump-path ../src/main/resources/IPR_Dump
"""

import argparse
import json
import logging
import os
import sys
from pathlib import Path
from typing import Dict, List, Optional, Any, Tuple
from datetime import datetime
import time

import pandas as pd
import requests
from dateutil import parser as date_parser
import pytz

# Configuração de logging
logging.basicConfig(
    level=logging.DEBUG,  # Mudado para DEBUG para ver payloads
    format='%(asctime)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler('import_dump.log'),
        logging.StreamHandler(sys.stdout)
    ]
)
logger = logging.getLogger(__name__)


class ImportConfig:
    """Configurações de importação"""
    def __init__(self, api_url: str, token: str, dump_path: str):
        self.api_url = api_url.rstrip('/')
        self.token = token
        self.dump_path = Path(dump_path)
        self.checkpoint_file = Path('import_checkpoint.json')
        self.id_mapping_file = Path('id_mapping.json')
        
        # Arquivos do dump
        self.pessoa_csv = self.dump_path / 'Pessoa.csv'
        self.contato_csv = self.dump_path / 'Pessoa_Contato.csv'
        self.fotos_dir = self.dump_path / 'fotos'
        
    @property
    def headers(self) -> Dict[str, str]:
        return {
            'Authorization': f'Bearer {self.token}',
            'Content-Type': 'application/json'
        }


class APIClient:
    """Cliente para comunicação com a API"""
    def __init__(self, config: ImportConfig):
        self.config = config
        self.session = requests.Session()
        # Adicionar apenas Authorization aos headers padrão da sessão
        # Content-Type deve ser definido por requisição (JSON vs multipart/form-data)
        self.session.headers.update({'Authorization': f'Bearer {config.token}'})
        
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
        
    def create_pessoa(self, pessoa_data: Dict) -> Dict:
        """Cria uma pessoa via API"""
        url = f"{self.config.api_url}/api/pessoas"
        headers = {
            'Authorization': f'Bearer {self.config.token}',
            'Content-Type': 'application/json'
        }
        logger.debug(f"Enviando POST para {url}")
        logger.debug(f"Headers: Authorization=Bearer {self.config.token[:20]}...")
        response = self._retry_request('POST', url, headers=headers, json=pessoa_data)
        return response.json()
    
    def upload_foto(self, pessoa_id: int, foto_path: Path) -> Dict:
        """Faz upload de foto para uma pessoa"""
        url = f"{self.config.api_url}/api/pessoas/{pessoa_id}/foto"
        headers = {'Authorization': f'Bearer {self.config.token}'}
        
        with open(foto_path, 'rb') as f:
            files = {'foto': (foto_path.name, f, 'image/png')}
            response = self._retry_request('POST', url, headers=headers, files=files)
        return response.json()
    
    def create_relacionamento(self, pessoa_id: int, relacionamento: Dict) -> Dict:
        """Cria um relacionamento entre pessoas"""
        url = f"{self.config.api_url}/api/pessoas/{pessoa_id}/relacionamento"
        headers = {'Content-Type': 'application/json'}
        response = self._retry_request('POST', url, headers=headers, json=relacionamento)
        return response.json()


class CheckpointManager:
    """Gerenciamento de checkpoint para retomar importação"""
    def __init__(self, checkpoint_file: Path):
        self.checkpoint_file = checkpoint_file
        self.checkpoint = self._load_checkpoint()
        
    def _load_checkpoint(self) -> Dict:
        if self.checkpoint_file.exists():
            with open(self.checkpoint_file, 'r') as f:
                return json.load(f)
        return {
            'fase1_completed': False,
            'fase2_completed': False,
            'fase3_completed': False,
            'last_pessoa_id': None,
            'pessoas_created': 0,
            'fotos_uploaded': 0,
            'relacionamentos_created': 0
        }
    
    def save(self):
        with open(self.checkpoint_file, 'w') as f:
            json.dump(self.checkpoint, f, indent=2)
    
    def mark_fase_complete(self, fase: int):
        self.checkpoint[f'fase{fase}_completed'] = True
        self.save()
    
    def is_fase_complete(self, fase: int) -> bool:
        return self.checkpoint.get(f'fase{fase}_completed', False)


class IDMapper:
    """Gerenciamento de mapeamento de IDs antigos para novos"""
    def __init__(self, mapping_file: Path):
        self.mapping_file = mapping_file
        self.mapping = self._load_mapping()
        
    def _load_mapping(self) -> Dict[int, int]:
        if self.mapping_file.exists():
            with open(self.mapping_file, 'r') as f:
                data = json.load(f)
                return {int(k): int(v) for k, v in data.items()}
        return {}
    
    def save(self):
        with open(self.mapping_file, 'w') as f:
            json.dump(self.mapping, f, indent=2)
    
    def add(self, old_id: int, new_id: int):
        self.mapping[old_id] = new_id
        
    def get(self, old_id: int) -> Optional[int]:
        return self.mapping.get(old_id)
    
    def exists(self, old_id: int) -> bool:
        return old_id in self.mapping


# =============================================================================
# FUNÇÕES DE MAPEAMENTO DE DADOS
# =============================================================================

def map_sexo(sexo_str: str) -> str:
    """Mapeia sexo do CSV para enum da API"""
    mapping = {
        'M': 'MASCULINO',
        'F': 'FEMININO'
    }
    return mapping.get(sexo_str, 'MASCULINO')


def map_estado_civil(estado_civil_str: str) -> str:
    """Mapeia estado civil do CSV para enum da API"""
    if pd.isna(estado_civil_str):
        return 'SOLTEIRO_SEM_RELACIONAMENTO'
    
    estado_civil_str = estado_civil_str.strip()
    
    mapping = {
        'Casado': 'CASADO',
        'Casada': 'CASADO',
        'Solteiro': 'SOLTEIRO_SEM_RELACIONAMENTO',
        'Solteira': 'SOLTEIRO_SEM_RELACIONAMENTO',
        'Divorciado': 'DIVORCIADO_SEM_RELACIONAMENTO',
        'Divorciada': 'DIVORCIADO_SEM_RELACIONAMENTO',
        'Viúvo': 'VIUVO_SEM_RELACIONAMENTO',
        'Viúva': 'VIUVO_SEM_RELACIONAMENTO'
    }
    
    return mapping.get(estado_civil_str, 'SOLTEIRO_SEM_RELACIONAMENTO')


def get_categoria_id(categoria_id: int) -> int:
    """
    Retorna apenas o ID da categoria.
    O @JsonCreator do enum CategoriaEnum.fromId(Long id) espera receber
    o número diretamente, não um objeto.
    """
    return categoria_id


def map_categoria(categoria_str: str) -> Optional[int]:
    """
    Mapeia categoria do CSV para ID da categoria na API.
    Retorna None se a categoria indica que a pessoa não deve ser importada.
    """
    if pd.isna(categoria_str) or not str(categoria_str).strip():
        return 36  # AGREGADO_FAMILIAR
    
    categoria = str(categoria_str).strip()
    
    # Mapeamento direto de categorias
    categoria_map = {
        # Pastores
        '00.Pastores da Igreja': 1,  # PASTOR_DA_IGREJA
        
        # Membros comungantes
        '01.Membro': 3,  # MEMBRO_COMUNGANTE
        
        # Rol à parte
        '02.01.Rol à parte, membro em trânsito': 9,  # MEMBRO_EM_TRANSITO
        '02.03.Rol à parte, membro ausente (doença etc.)': 10,  # MEMBRO_AUSENTE
        '02.90.Rol à parte, membro a transferir': 11,  # MEMBRO_A_TRANSFERIR
        '02.95.Rol à parte, membro não localizado ou pedido de desligamento': 12,  # MEMBRO_NAO_LOCALIZADO
        '02.99.Rol à parte, membro sob disciplina': 13,  # MEMBRO_SOB_DISCIPLINA
        
        # Membros não comungantes
        '03.02.Membro não comungante, em catequização final': 5,  # AGUARDANDO_EXAME
        '03.96.Membro não comungante': 7,  # MEMBRO_NAO_COMUNGANTE
        '03.97.Membro não comungante, em idade para profissão de fé': 8,  # MEMBRO_NAO_COMUNGANTE_IDADE_PROFISSAO
        '03.98.Membro não comungante, rol à parte (em trânsito)': 9,  # MEMBRO_EM_TRANSITO
        '03.99.Membro não comungante, rol à parte (transf)': 11,  # MEMBRO_A_TRANSFERIR
        
        # Admissão genérica
        '04.00.Admissão, rotina de admissão': 22,  # AGUARDANDO_RESOLUCAO_PENDENCIA
        '04.98.Admissão, aguardando carta de transferência': 14,  # AGUARDANDO_CARTA_TRANSFERENCIA
        '04.99.Admissão, solicitar carta de transferência': 15,  # SOLICITAR_CARTA_TRANSFERENCIA
        
        # Admitendo
        '05.10.Admitendo, menor aguardando batismo infantil': 16,  # AGUARDANDO_BATISMO_INFANTIL
        '05.15.Admitendo, não presbiteriano aguardando votos de membresia': 17,  # AGUARDANDO_VOTOS_MEMBRESIA
        '05.51.Admitendo, adulto/jovem aguardando profissão de fé e batismo': 19,  # AGUARDANDO_PROFISSAO_FE_BATISMO
        '05.70.Admitendo, aguardando resolução pendência': 22,  # AGUARDANDO_RESOLUCAO_PENDENCIA
        '05.90.Admitendo, aguardando entrevista': 23,  # AGUARDANDO_ENTREVISTA
        
        # Possível admissão
        '06.01.Possível admissão: em catequização': 24,  # EM_CATEQUIZACAO
        '06.94.Possível admissão: filhos de pais em catequização': 24,  # EM_CATEQUIZACAO
        '06.95.Possível admissão: gestação': 34,  # GESTACAO
        '06.96.Possível admissão: admissão sobrestada (pedido, impedim. ou discord. CFW)': 25,  # ADMISSAO_SOBRESTADA
        '06.97.Possível admissão: batismo de menor sobrestado (credobatismo)': 26,  # BATISMO_MENOR_SOBRESTADO
        '06.98.Possível admissão: em avaliação': 22,  # AGUARDANDO_RESOLUCAO_PENDENCIA
        '06.99.Possível admissão: aguardando ficha cadastral': 22,  # AGUARDANDO_RESOLUCAO_PENDENCIA
        
        # Agregados
        '08.Agregado não membro (p.ex. familiar frequente)': 36,  # AGREGADO_FAMILIAR
        '80.Oficiais da IPB': 36,  # AGREGADO_FAMILIAR
        
        # Missionários
        '51.Missionários apoiados': 32,  # MISSIONARIO_APOIADO
        
        # Ex-membros
        '91. Ex-membro da Igreja': 37,  # EX_MEMBRO
        
        # Pessoa referenciada
        '99.Pessoa referenciada (sistema)': 36,  # AGREGADO_FAMILIAR
    }
    
    # Categorias que NÃO devem ser importadas (retorna None)
    # Ex-membros e visitantes serão tratados especialmente (importar apenas com relacionamentos)
    categorias_nao_importar = [
        '09.Pessoa fictícia (sistema)',
        '48.Aparece na contabilidade',
        '42.Visitante recorrente EBD/GF/3aIdade/EnglishBibleStudy/Youtube',
        '43.Visitante frequente',
        '45.Visitante frequente, mas sem intenção de admissão',
        '90.Visitante ocasional periódico',
        '95.Visitante',
    ]
    
    if categoria in categorias_nao_importar:
        return None  # Indica que não deve importar
    
    # Buscar no mapeamento
    if categoria in categoria_map:
        return categoria_map[categoria]
    
    # Se não encontrou, log warning e retorna AGREGADO_FAMILIAR como fallback
    logger.warning(f"Categoria desconhecida: '{categoria}'. Usando AGREGADO_FAMILIAR como fallback.")
    return 36  # AGREGADO_FAMILIAR


def infer_tipo_batismo(batismo_data: str, profissao_fe_data: str) -> str:
    """
    Infere o tipo de batismo baseado nas datas de batismo e profissão de fé.
    
    Lógica:
    - Se ambas as datas existem e são diferentes → INFANTIL
    - Se apenas uma das datas existe → ADULTO
    - Se ambas as datas existem e são iguais → ADULTO
    - Se nenhuma data existe → NAO_BATIZADO
    """
    has_batismo = not pd.isna(batismo_data) and str(batismo_data).strip() not in ['', '---', '???']
    has_profissao = not pd.isna(profissao_fe_data) and str(profissao_fe_data).strip() not in ['', '---', '???']
    
    if not has_batismo and not has_profissao:
        return 'NAO_BATIZADO'
    
    if has_batismo and has_profissao:
        # Tentar parsear as datas para comparar
        try:
            batismo_parsed = parse_date(batismo_data)
            profissao_parsed = parse_date(profissao_fe_data)
            
            if batismo_parsed and profissao_parsed:
                if batismo_parsed != profissao_parsed:
                    return 'INFANTIL'
                else:
                    return 'ADULTO'
        except:
            pass
        
        # Se não conseguir parsear, considerar ADULTO
        return 'ADULTO'
    
    # Se apenas uma existe
    return 'ADULTO'


def parse_date(date_str: Any) -> Optional[str]:
    """Parse de data flexível retornando formato ISO em UTC
    
    As datas no CSV estão em GMT-3 (horário de Brasília).
    Esta função converte para UTC antes de retornar.
    """
    if pd.isna(date_str):
        return None
    
    date_str = str(date_str).strip()
    
    # Valores inválidos
    if date_str in ['', '---', '???', 'prov.', '~']:
        return None
    
    # Tentar parsear ano apenas
    if len(date_str) == 4 and date_str.isdigit():
        return f"{date_str}-06-30"  # Usar meio do ano como estimativa
    
    try:
        # Tentar parsear data completa (naive, sem timezone)
        parsed = date_parser.parse(date_str, fuzzy=True)
        
        # Se a data não tem timezone, assumir que está em GMT-3 (America/Sao_Paulo)
        if parsed.tzinfo is None:
            brasilia_tz = pytz.timezone('America/Sao_Paulo')
            parsed = brasilia_tz.localize(parsed)
        
        # Converter para UTC e formatar
        parsed_utc = parsed.astimezone(pytz.UTC)
        return parsed_utc.strftime('%Y-%m-%dT%H:%M:%S.000Z')
    except:
        logger.warning(f"Não foi possível parsear data: {date_str}")
        return None


def clean_cpf(cpf: Any) -> Optional[str]:
    """Limpa e valida CPF"""
    if pd.isna(cpf):
        return None
    
    cpf_str = str(cpf).strip()
    # Remove caracteres não numéricos
    cpf_clean = ''.join(c for c in cpf_str if c.isdigit())
    
    if len(cpf_clean) == 11:
        return cpf_clean
    
    return None


def clean_phone(phone: Any) -> Optional[str]:
    """Limpa número de telefone"""
    if pd.isna(phone):
        return None
    
    phone_str = str(phone).strip()
    # Remove caracteres não numéricos
    phone_clean = ''.join(c for c in phone_str if c.isdigit())
    
    if len(phone_clean) >= 8:
        return phone_clean
    
    return None


# =============================================================================
# FASE 1: IMPORTAR PESSOAS
# =============================================================================

def consolidate_contacts(contato_df: pd.DataFrame) -> Dict[int, Dict]:
    """
    Consolida contatos por pessoa.
    
    Retorna dict com estrutura:
    {
        idPessoa: {
            'telefone': str,
            'telefonesSecundarios': [str],
            'email': str,
            'emailsSecundarios': [str],
            'profissao': [str],
            'empresa': [str],
            'endereco': {
                'cep': str,
                'logradouro': str,
                'cidade': str,
                'estado': str,
                'coordenadas': str
            },
            'usa_endereco_de': int  # ID da pessoa de quem usa o endereço
        }
    }
    """
    logger.info("Consolidando contatos por pessoa...")
    
    contacts_by_person = {}
    pessoas_com_endereco_compartilhado = {}  # pessoa_id -> pessoa_referencia_id
    
    for _, row in contato_df.iterrows():
        pessoa_id = int(row['idPessoa'])
        tipo_contato = str(row['TipoContato'])
        texto_contato = row['TextoContato']
        usa_de = row.get('UsaDe')  # Coluna que indica se usa endereço de outra pessoa
        
        if pessoa_id not in contacts_by_person:
            contacts_by_person[pessoa_id] = {
                'telefone': None,
                'telefonesSecundarios': [],
                'email': None,
                'emailsSecundarios': [],
                'profissao': [],
                'empresa': [],
                'endereco': {}
            }
        
        person_contacts = contacts_by_person[pessoa_id]
        
        # Verificar se este é um campo de endereço e tem UsaDe preenchido
        is_endereco_field = any(campo in tipo_contato for campo in ['Residência.CEP', 'Residência.CidadeEstado', 'Residência.Coords', 'Residência.Logradouro'])
        
        if is_endereco_field and not pd.isna(usa_de) and str(usa_de).strip():
            # Esta pessoa usa o endereço de outra pessoa
            try:
                pessoa_referencia_id = int(usa_de)
                pessoas_com_endereco_compartilhado[pessoa_id] = pessoa_referencia_id
                # Não processar os campos de endereço individuais, vamos copiar depois
                continue
            except (ValueError, TypeError):
                # Se não conseguir converter, processar normalmente
                pass
        
        person_contacts = contacts_by_person[pessoa_id]
        
        # Telefones
        if 'Telefone' in tipo_contato:
            phone = clean_phone(texto_contato)
            if phone:
                # Primário vai para telefone principal
                if 'Primário' in tipo_contato and not person_contacts['telefone']:
                    person_contacts['telefone'] = phone
                # Residencial e outros vão para secundários
                else:
                    if phone not in person_contacts['telefonesSecundarios']:
                        person_contacts['telefonesSecundarios'].append(phone)
        
        # Emails
        elif 'E-mail' in tipo_contato:
            email = str(texto_contato).strip() if not pd.isna(texto_contato) else None
            if email and '@' in email:
                # Email primário
                if 'Primário' in tipo_contato and not person_contacts['email']:
                    person_contacts['email'] = email
                # Email de trabalho e outros vão para secundários
                else:
                    if email not in person_contacts['emailsSecundarios']:
                        person_contacts['emailsSecundarios'].append(email)
        
        # Profissão
        elif 'Trabalho.Profissão' in tipo_contato:
            profissao = str(texto_contato).strip() if not pd.isna(texto_contato) else None
            if profissao and profissao not in person_contacts['profissao']:
                person_contacts['profissao'].append(profissao)
        
        # Empresa/Organização
        elif 'Trabalho.Organização' in tipo_contato:
            empresa = str(texto_contato).strip() if not pd.isna(texto_contato) else None
            if empresa and empresa not in person_contacts['empresa']:
                person_contacts['empresa'].append(empresa)
        
        # Endereço - CEP
        elif 'Residência.CEP' in tipo_contato:
            cep = clean_phone(texto_contato)  # Usar clean_phone para remover não-numéricos
            if cep:
                person_contacts['endereco']['cep'] = cep
        
        # Endereço - Logradouro
        elif 'Residência.Logradouro' in tipo_contato:
            logradouro = str(texto_contato).strip() if not pd.isna(texto_contato) else None
            if logradouro:
                person_contacts['endereco']['logradouro'] = logradouro
        
        # Endereço - Cidade/Estado (separado por / ou -)
        elif 'Residência.CidadeEstado' in tipo_contato:
            cidade_estado = str(texto_contato).strip() if not pd.isna(texto_contato) else None
            if cidade_estado:
                # Tentar separar por / ou -
                if '/' in cidade_estado:
                    partes = cidade_estado.split('/')
                elif '-' in cidade_estado:
                    partes = cidade_estado.split('-')
                else:
                    # Se não tem separador, usar tudo como cidade
                    partes = [cidade_estado]
                
                if len(partes) >= 1:
                    person_contacts['endereco']['cidade'] = partes[0].strip()
                if len(partes) >= 2:
                    person_contacts['endereco']['estado'] = partes[1].strip()
        
        # Endereço - Coordenadas
        elif 'Residência.Coords' in tipo_contato:
            coordenadas = str(texto_contato).strip() if not pd.isna(texto_contato) else None
            if coordenadas:
                person_contacts['endereco']['coordenadas'] = coordenadas
    
    # Segunda passagem: copiar endereços compartilhados
    logger.info(f"Processando {len(pessoas_com_endereco_compartilhado)} pessoas com endereço compartilhado...")
    for pessoa_id, pessoa_referencia_id in pessoas_com_endereco_compartilhado.items():
        if pessoa_referencia_id in contacts_by_person:
            endereco_referencia = contacts_by_person[pessoa_referencia_id].get('endereco', {})
            if endereco_referencia:
                # Copiar o endereço da pessoa referenciada
                contacts_by_person[pessoa_id]['endereco'] = endereco_referencia.copy()
                logger.debug(f"Pessoa {pessoa_id} usando endereço de pessoa {pessoa_referencia_id}")
            else:
                logger.warning(f"Pessoa {pessoa_id} deveria usar endereço de {pessoa_referencia_id}, mas a referência não tem endereço")
        else:
            logger.warning(f"Pessoa {pessoa_id} referencia endereço de {pessoa_referencia_id}, mas pessoa referência não encontrada")
    
    logger.info(f"Consolidados contatos de {len(contacts_by_person)} pessoas")
    return contacts_by_person


def has_relationships(row: pd.Series) -> bool:
    """Verifica se a pessoa tem relacionamentos (Pai, Mãe, Cônjuge, ChefeFamília)"""
    relacionamentos_cols = ['Pai', 'Mãe', 'Cônjuge', 'ChefeDeFamília']
    
    for col in relacionamentos_cols:
        valor = row.get(col)
        if not pd.isna(valor) and str(valor).strip() not in ['', '0']:
            return True
    
    return False


def should_import_pessoa(row: pd.Series) -> Tuple[bool, Optional[int]]:
    """
    Determina se a pessoa deve ser importada e qual categoria usar.
    Retorna (deve_importar, categoria_id)
    
    Para visitantes:
    - Importa apenas se houver relacionamentos
    - Se importar, usa categoria AGREGADO_FAMILIAR (36)
    
    Para ex-membros:
    - Importa apenas se houver relacionamentos
    - Mantém a categoria EX_MEMBRO (37)
    """
    categoria_id = map_categoria(row.get('Categoria'))
    
    # Se categoria retornou None (não importar por padrão)
    if categoria_id is None:
        # Verificar se tem relacionamentos
        if has_relationships(row):
            logger.info(f"Pessoa {row.get('Nome')} (ID {row.get('idPessoa')}) tem categoria não importável mas possui relacionamentos. Importando como AGREGADO_FAMILIAR.")
            return (True, 36)  # Importar como AGREGADO_FAMILIAR
        else:
            logger.info(f"Pessoa {row.get('Nome')} (ID {row.get('idPessoa')}) tem categoria não importável e sem relacionamentos. Pulando.")
            return (False, None)
    
    # Ex-membros (37) só devem ser importados se tiverem relacionamentos
    if categoria_id == 37:  # EX_MEMBRO
        if has_relationships(row):
            logger.info(f"Ex-membro {row.get('Nome')} (ID {row.get('idPessoa')}) possui relacionamentos. Importando.")
            return (True, categoria_id)
        else:
            logger.info(f"Ex-membro {row.get('Nome')} (ID {row.get('idPessoa')}) sem relacionamentos. Pulando.")
            return (False, None)
    
    return (True, categoria_id)


def create_pessoa_payload(row: pd.Series, contacts: Dict, categoria_id: int, campus: str, igreja_map: Dict[int, str]) -> Dict:
    """Cria o payload JSON para criação de pessoa"""
    
    # Resolver nome da igreja anterior
    igreja_anterior = None
    if not pd.isna(row.get('IgrejaDeOrigem')):
        igreja_origem_str = str(row['IgrejaDeOrigem']).strip()
        try:
            # Tentar converter para int e buscar no mapeamento
            igreja_id = int(float(igreja_origem_str))
            igreja_anterior = igreja_map.get(igreja_id)
            if not igreja_anterior:
                # Se não encontrar no mapa, usar o valor original
                logger.warning(f"Igreja com ID {igreja_id} não encontrada no mapeamento")
                igreja_anterior = igreja_origem_str
        except (ValueError, TypeError):
            # Se não for um número, usar o valor como está
            igreja_anterior = igreja_origem_str
    
    payload = {
        'nome': str(row['Nome']).strip() if not pd.isna(row['Nome']) else None,
        'apelido': str(row['NomeUsual']).strip() if not pd.isna(row['NomeUsual']) else None,
        'sexo': map_sexo(row.get('Sexo', 'M')),
        'cpf': clean_cpf(row.get('CPF')),
        'rg': str(row['RG']).strip() if not pd.isna(row.get('RG')) else None,
        'dataNascimento': parse_date(row.get('DataNascimento')),
        'dataFalecimento': parse_date(row.get('DataFalecimento')),
        'estadoCivil': map_estado_civil(row.get('EstadoCivil')),
        'categoria': get_categoria_id(categoria_id),
        'campus': campus,
        'igrejaAnterior': igreja_anterior,
        'dataBatismo': parse_date(row.get('BatismoData')),
        'dataProfissaoDeFe': parse_date(row.get('ProfissãoDeFéData')),
        'igrejaBatismo': str(row['BatismoLocal']).strip() if not pd.isna(row.get('BatismoLocal')) else None,
        'tipoBatismo': infer_tipo_batismo(row.get('BatismoData'), row.get('ProfissãoDeFéData')),
    }
    
    # Adicionar contatos consolidados
    if contacts:
        if contacts.get('telefone'):
            payload['telefone'] = contacts['telefone']
        if contacts.get('telefonesSecundarios'):
            payload['telefonesSecundarios'] = contacts['telefonesSecundarios']
        if contacts.get('email'):
            payload['email'] = contacts['email']
        if contacts.get('emailsSecundarios'):
            payload['emailsSecundarios'] = contacts['emailsSecundarios']
        if contacts.get('profissao'):
            payload['profissao'] = contacts['profissao']
        if contacts.get('empresa'):
            payload['empresa'] = contacts['empresa']
        
        # Endereço (precisa ter CEP E logradouro - ambos obrigatórios)
        if contacts.get('endereco'):
            endereco = contacts['endereco']
            has_cep = endereco.get('cep') and str(endereco['cep']).strip()
            has_logradouro = endereco.get('logradouro') and str(endereco['logradouro']).strip()
            if has_cep and has_logradouro:
                payload['endereco'] = endereco
    
    # Remover campos None
    return {k: v for k, v in payload.items() if v is not None}


def fase1_importar_pessoas(config: ImportConfig, api_client: APIClient, 
                           checkpoint: CheckpointManager, id_mapper: IDMapper):
    """Fase 1: Importa pessoas com contatos consolidados"""
    if checkpoint.is_fase_complete(1):
        logger.info("Fase 1 já completada. Pulando...")
        return
    
    logger.info("=" * 80)
    logger.info("FASE 1: Importando Pessoas")
    logger.info("=" * 80)
    
    # Carregar CSVs
    # Pessoa_Contato.csv usa vírgula como delimitador
    logger.info(f"Carregando {config.contato_csv}")
    contato_df = pd.read_csv(config.contato_csv, sep=',', on_bad_lines='skip', encoding='utf-8')
    
    # Pessoa.csv usa ponto-e-vírgula como delimitador
    logger.info(f"Carregando {config.pessoa_csv}")
    pessoa_df = pd.read_csv(config.pessoa_csv, sep=';', on_bad_lines='skip', encoding='utf-8')
    
    # Carregar IPVideira.csv para identificar pessoas da congregação
    ipv_csv = config.dump_path / 'IPVideira.csv'
    logger.info(f"Carregando {ipv_csv}")
    ipv_df = pd.read_csv(ipv_csv, sep=',', on_bad_lines='skip', encoding='utf-8')
    pessoas_congregacao = set(ipv_df['idPessoa'].tolist())
    logger.info(f"Identificadas {len(pessoas_congregacao)} pessoas da congregação (IPVideira)")
    
    # Carregar Igreja.csv para mapear IDs de igreja para nomes
    igreja_csv = config.dump_path / 'Igreja.csv'
    logger.info(f"Carregando {igreja_csv}")
    igreja_df = pd.read_csv(igreja_csv, sep=',', on_bad_lines='skip', encoding='utf-8')
    igreja_map = dict(zip(igreja_df['idIgreja'], igreja_df['NomeIgreja']))
    logger.info(f"Carregadas {len(igreja_map)} igrejas para mapeamento")
    
    # Consolidar contatos
    contacts_by_person = consolidate_contacts(contato_df)
    
    # Processar cada pessoa
    total = len(pessoa_df)
    success_count = 0
    error_count = 0
    skip_count = 0
    
    for idx, row in pessoa_df.iterrows():
        old_id = int(row['idPessoa'])
        
        # Verificar se já foi processada
        if id_mapper.exists(old_id):
            logger.info(f"Pessoa {old_id} já processada. Pulando...")
            continue
        
        try:
            # Verificar se a pessoa deve ser importada
            deve_importar, categoria_id = should_import_pessoa(row)
            
            if not deve_importar:
                skip_count += 1
                continue
            
            # Obter contatos consolidados
            contacts = contacts_by_person.get(old_id, {})
            
            # Determinar campus
            campus = 'CONGREGACAO' if old_id in pessoas_congregacao else 'SEDE'
            
            # Criar payload
            payload = create_pessoa_payload(row, contacts, categoria_id, campus, igreja_map)
            
            # Criar pessoa na API
            logger.info(f"[{idx + 1}/{total}] Criando pessoa: {payload.get('nome', 'N/A')} (ID antigo: {old_id})")
            logger.debug(f"Payload: {json.dumps(payload, indent=2, ensure_ascii=False, default=str)}")
            result = api_client.create_pessoa(payload)
            
            new_id = result['id']
            id_mapper.add(old_id, new_id)
            
            # Guardar nome da foto para fase 2
            foto_nome = str(row['ArquivoFoto']).strip() if not pd.isna(row.get('ArquivoFoto')) else None
            if foto_nome and foto_nome not in ['', '0', 'nan']:
                id_mapper.mapping[f"foto_{old_id}"] = foto_nome
            
            success_count += 1
            
            # Salvar progresso periodicamente
            if success_count % 10 == 0:
                id_mapper.save()
                checkpoint.checkpoint['pessoas_created'] = success_count
                checkpoint.save()
                logger.info(f"Progresso salvo: {success_count}/{total} pessoas criadas")
            
        except Exception as e:
            error_count += 1
            logger.error(f"Erro ao criar pessoa {old_id}: {e}")
            if error_count > 50:
                logger.error("Muitos erros. Abortando importação.")
                raise
    
    # Salvar progresso final
    id_mapper.save()
    checkpoint.checkpoint['pessoas_created'] = success_count
    checkpoint.mark_fase_complete(1)
    
    logger.info("=" * 80)
    logger.info(f"FASE 1 COMPLETA: {success_count} pessoas criadas, {skip_count} puladas, {error_count} erros")
    logger.info("=" * 80)


# =============================================================================
# FASE 2: UPLOAD DE FOTOS
# =============================================================================

def fase2_upload_fotos(config: ImportConfig, api_client: APIClient,
                       checkpoint: CheckpointManager, id_mapper: IDMapper):
    """Fase 2: Upload de fotos"""
    if checkpoint.is_fase_complete(2):
        logger.info("Fase 2 já completada. Pulando...")
        return
    
    logger.info("=" * 80)
    logger.info("FASE 2: Upload de Fotos")
    logger.info("=" * 80)
    
    success_count = 0
    error_count = 0
    skip_count = 0
    
    # Processar apenas pessoas que têm foto
    foto_mappings = {k: v for k, v in id_mapper.mapping.items() if isinstance(k, str) and k.startswith('foto_')}
    total = len(foto_mappings)
    
    for foto_key, foto_nome in foto_mappings.items():
        old_id = int(foto_key.replace('foto_', ''))
        new_id = id_mapper.get(old_id)
        
        if not new_id:
            logger.warning(f"ID novo não encontrado para pessoa {old_id}. Pulando foto.")
            skip_count += 1
            continue
        
        # Procurar arquivo de foto
        foto_path = config.fotos_dir / foto_nome
        
        if not foto_path.exists():
            logger.warning(f"Arquivo de foto não encontrado: {foto_path}")
            skip_count += 1
            continue
        
        try:
            logger.info(f"[{success_count + error_count + skip_count + 1}/{total}] Upload foto para pessoa {new_id}: {foto_nome}")
            api_client.upload_foto(new_id, foto_path)
            success_count += 1
            
            # Salvar progresso periodicamente
            if success_count % 10 == 0:
                checkpoint.checkpoint['fotos_uploaded'] = success_count
                checkpoint.save()
                logger.info(f"Progresso salvo: {success_count}/{total} fotos enviadas")
            
        except Exception as e:
            error_count += 1
            logger.error(f"Erro ao fazer upload de foto {foto_nome} para pessoa {new_id}: {e}")
    
    # Salvar progresso final
    checkpoint.checkpoint['fotos_uploaded'] = success_count
    checkpoint.mark_fase_complete(2)
    
    logger.info("=" * 80)
    logger.info(f"FASE 2 COMPLETA: {success_count} fotos enviadas, {skip_count} puladas, {error_count} erros")
    logger.info("=" * 80)


# =============================================================================
# FASE 2.5: DEFINIR CHEFE DE FAMÍLIA
# =============================================================================

def fase2_5_definir_chefe_familia(config: ImportConfig, api_client: APIClient,
                                   checkpoint: CheckpointManager, id_mapper: IDMapper):
    """Fase 2.5: Definir chefeDeFamiliaId nas pessoas"""
    # Usar um checkpoint diferente para esta fase
    if checkpoint.checkpoint.get('fase2_5_completed', False):
        logger.info("Fase 2.5 já completada. Pulando...")
        return
    
    logger.info("=" * 80)
    logger.info("FASE 2.5: Definindo Chefe de Família")
    logger.info("=" * 80)
    
    # Carregar CSV de pessoas (usando ponto-e-vírgula como delimitador)
    logger.info(f"Carregando {config.pessoa_csv}")
    pessoa_df = pd.read_csv(config.pessoa_csv, sep=';', on_bad_lines='skip', encoding='utf-8')
    
    success_count = 0
    error_count = 0
    skip_count = 0
    
    for idx, row in pessoa_df.iterrows():
        old_id = int(row['idPessoa'])
        new_id = id_mapper.get(old_id)
        
        if not new_id:
            skip_count += 1
            continue
        
        # Verificar se tem chefe de família
        if pd.isna(row.get('ChefeFamília')) or not str(row['ChefeFamília']).strip():
            continue
        
        try:
            chefe_old_id = int(row['ChefeFamília'])
            chefe_new_id = id_mapper.get(chefe_old_id)
            
            if not chefe_new_id:
                logger.warning(f"Chefe de família com ID antigo {chefe_old_id} não encontrado no mapeamento")
                skip_count += 1
                continue
            
            # Atualizar pessoa com chefeDeFamiliaId
            # Precisamos fazer um GET, atualizar e fazer um PUT
            logger.info(f"[{success_count + error_count + skip_count + 1}] Definindo chefe de família {chefe_new_id} para pessoa {new_id}")
            
            # GET pessoa atual
            url = f"{config.api_url}/api/pessoas/{new_id}"
            headers = {'Authorization': f'Bearer {config.token}', 'Content-Type': 'application/json'}
            response = api_client._retry_request('GET', url, headers=headers)
            pessoa = response.json()
            
            # APENAS atualizar o campo chefeDeFamiliaId (manter resto como veio do GET)
            pessoa['chefeDeFamiliaId'] = chefe_new_id
            
            # Converter categoria de objeto para ID (backend espera apenas o ID no PUT)
            if pessoa.get('categoria') and isinstance(pessoa['categoria'], dict):
                pessoa['categoria'] = pessoa['categoria']['id']
            
            # PUT pessoa atualizada (mantendo todos os outros campos como vieram do GET)
            url = f"{config.api_url}/api/pessoas/{new_id}"
            api_client._retry_request('PUT', url, headers=headers, json=pessoa)
            
            success_count += 1
            
            # Salvar progresso periodicamente
            if success_count % 10 == 0:
                checkpoint.checkpoint['chefes_familia_definidos'] = success_count
                checkpoint.save()
                logger.info(f"Progresso salvo: {success_count} chefes de família definidos")
            
        except Exception as e:
            error_count += 1
            logger.error(f"Erro ao definir chefe de família para pessoa {new_id}: {e}")
    
    # Salvar progresso final
    checkpoint.checkpoint['chefes_familia_definidos'] = success_count
    checkpoint.checkpoint['fase2_5_completed'] = True
    checkpoint.save()
    
    logger.info("=" * 80)
    logger.info(f"FASE 2.5 COMPLETA: {success_count} chefes definidos, {skip_count} pulados, {error_count} erros")
    logger.info("=" * 80)


# =============================================================================
# FASE 3: CRIAR RELACIONAMENTOS
# =============================================================================

def fase3_criar_relacionamentos(config: ImportConfig, api_client: APIClient,
                                checkpoint: CheckpointManager, id_mapper: IDMapper):
    """Fase 3: Criar relacionamentos entre pessoas"""
    if checkpoint.is_fase_complete(3):
        logger.info("Fase 3 já completada. Pulando...")
        return
    
    logger.info("=" * 80)
    logger.info("FASE 3: Criando Relacionamentos")
    logger.info("=" * 80)
    
    # Carregar CSV de pessoas novamente (usando ponto-e-vírgula como delimitador)
    logger.info(f"Carregando {config.pessoa_csv}")
    pessoa_df = pd.read_csv(config.pessoa_csv, sep=';', on_bad_lines='skip', encoding='utf-8')
    
    success_count = 0
    error_count = 0
    skip_count = 0
    
    total_relationships = 0
    
    for idx, row in pessoa_df.iterrows():
        old_id = int(row['idPessoa'])
        new_id = id_mapper.get(old_id)
        
        if not new_id:
            skip_count += 1
            continue
        
        # Processar relacionamentos
        relationships = []
        
        # Pai
        if not pd.isna(row.get('Pai')) and str(row['Pai']).strip():
            try:
                pai_old_id = int(row['Pai'])
                pai_new_id = id_mapper.get(pai_old_id)
                if pai_new_id:
                    relationships.append({
                        'pessoaRelacionadaId': pai_new_id,
                        'tipoRelacionamento': 'PAI'
                    })
            except (ValueError, TypeError):
                pass
        
        # Mãe
        if not pd.isna(row.get('Mãe')) and str(row['Mãe']).strip():
            try:
                mae_old_id = int(row['Mãe'])
                mae_new_id = id_mapper.get(mae_old_id)
                if mae_new_id:
                    relationships.append({
                        'pessoaRelacionadaId': mae_new_id,
                        'tipoRelacionamento': 'MAE'
                    })
            except (ValueError, TypeError):
                pass
        
        # Cônjuge
        if not pd.isna(row.get('Cônjuge')) and str(row['Cônjuge']).strip():
            try:
                conjuge_old_id = int(row['Cônjuge'])
                conjuge_new_id = id_mapper.get(conjuge_old_id)
                if conjuge_new_id:
                    relationships.append({
                        'pessoaRelacionadaId': conjuge_new_id,
                        'tipoRelacionamento': 'CONJUGE',
                        'inicioRelacionamento': parse_date(row.get('DataInícioRelacionamento'))
                    })
            except (ValueError, TypeError):
                pass
        
        # Chefe de Família não é um relacionamento, será tratado em fase separada
        
        # Criar cada relacionamento
        for rel in relationships:
            try:
                # Remover campos None
                rel_clean = {k: v for k, v in rel.items() if v is not None}
                
                logger.info(f"Criando relacionamento: Pessoa {new_id} -> {rel_clean['tipoRelacionamento']} -> Pessoa {rel_clean['pessoaRelacionadaId']}")
                api_client.create_relacionamento(new_id, rel_clean)
                success_count += 1
                total_relationships += 1
                
                # Salvar progresso periodicamente
                if success_count % 20 == 0:
                    checkpoint.checkpoint['relacionamentos_created'] = success_count
                    checkpoint.save()
                    logger.info(f"Progresso salvo: {success_count} relacionamentos criados")
                
            except Exception as e:
                error_count += 1
                logger.error(f"Erro ao criar relacionamento {rel}: {e}")
    
    # Salvar progresso final
    checkpoint.checkpoint['relacionamentos_created'] = success_count
    checkpoint.mark_fase_complete(3)
    
    logger.info("=" * 80)
    logger.info(f"FASE 3 COMPLETA: {success_count} relacionamentos criados, {error_count} erros")
    logger.info("=" * 80)


# =============================================================================
# MAIN
# =============================================================================

def main():
    parser = argparse.ArgumentParser(description='Importar dados do IPR_Dump para a API')
    parser.add_argument('--api-url', default='http://localhost:8080', help='URL base da API')
    parser.add_argument('--dump-path', default='../src/main/resources/IPR_Dump', help='Caminho para a pasta IPR_Dump')
    parser.add_argument('--skip-fase1', action='store_true', help='Pular fase 1 (importar pessoas)')
    parser.add_argument('--skip-fase2', action='store_true', help='Pular fase 2 (upload fotos)')
    parser.add_argument('--skip-fase3', action='store_true', help='Pular fase 3 (relacionamentos)')
    
    args = parser.parse_args()
    
    # Obter token de autenticação
    token = os.environ.get('API_TOKEN')
    if not token:
        logger.error("Variável de ambiente API_TOKEN não definida!")
        logger.error("Use: export API_TOKEN='seu-token-jwt-aqui'")
        sys.exit(1)
    
    # Inicializar configuração
    config = ImportConfig(args.api_url, token, args.dump_path)
    
    # Validar arquivos
    if not config.pessoa_csv.exists():
        logger.error(f"Arquivo não encontrado: {config.pessoa_csv}")
        sys.exit(1)
    
    if not config.contato_csv.exists():
        logger.error(f"Arquivo não encontrado: {config.contato_csv}")
        sys.exit(1)
    
    if not config.fotos_dir.exists():
        logger.error(f"Diretório não encontrado: {config.fotos_dir}")
        sys.exit(1)
    
    # Inicializar componentes
    api_client = APIClient(config)
    checkpoint = CheckpointManager(config.checkpoint_file)
    id_mapper = IDMapper(config.id_mapping_file)
    
    logger.info("Iniciando importação do IPR Dump")
    logger.info(f"API URL: {config.api_url}")
    logger.info(f"Dump Path: {config.dump_path}")
    
    try:
        # Executar fases
        if not args.skip_fase1:
            fase1_importar_pessoas(config, api_client, checkpoint, id_mapper)
        
        if not args.skip_fase2:
            fase2_upload_fotos(config, api_client, checkpoint, id_mapper)
        
        # Fase 2.5: Definir chefe de família (não tem flag skip, sempre executa se fase 1 foi executada)
        if not args.skip_fase1:
            fase2_5_definir_chefe_familia(config, api_client, checkpoint, id_mapper)
        
        if not args.skip_fase3:
            fase3_criar_relacionamentos(config, api_client, checkpoint, id_mapper)
        
        logger.info("=" * 80)
        logger.info("IMPORTAÇÃO COMPLETA!")
        logger.info(f"Pessoas criadas: {checkpoint.checkpoint.get('pessoas_created', 0)}")
        logger.info(f"Fotos enviadas: {checkpoint.checkpoint.get('fotos_uploaded', 0)}")
        logger.info(f"Chefes de família definidos: {checkpoint.checkpoint.get('chefes_familia_definidos', 0)}")
        logger.info(f"Relacionamentos criados: {checkpoint.checkpoint.get('relacionamentos_created', 0)}")
        logger.info("=" * 80)
        
    except Exception as e:
        logger.error(f"Erro durante importação: {e}", exc_info=True)
        logger.info("O progresso foi salvo. Você pode retomar a importação executando o script novamente.")
        sys.exit(1)


if __name__ == '__main__':
    main()


