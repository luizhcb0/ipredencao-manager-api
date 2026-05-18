-- =============================================================================
-- Relatório dos atos de uma Ata, no formato exigido pelo
-- "Regulamento para Confecção de Atas dos Concílios da IPB" (Manual Presbiteriano,
-- Art. 12, §2º), seguindo o uso adotado pela Igreja Presbiteriana Redenção.
--
-- Lê os atos da tabela person_note, cujo conteúdo segue o padrão:
--   "DD/MM/AAAA | <Tipo de ato> | <Forma do ato> | Ata: NNN [| <igreja>]"
-- (para "Emissão de carta de transferência" e "Solicitação", o "Ata: NNN"
--  fica no campo 3 e a igreja no campo 4).
--
-- Como usar:
--   1) Trocar o número da ata no CTE `params` (ou via variável psql).
--   2) Executar:  psql ... -f scripts/relatorio_ata.sql
-- =============================================================================

WITH params AS (
    SELECT '298' AS ata_numero
),
notes AS (
    SELECT
        pn.id                                       AS note_id,
        pn.person_id,
        pn.content,
        TRIM(SPLIT_PART(pn.content, '|', 1))        AS data_ato,
        TRIM(SPLIT_PART(pn.content, '|', 2))        AS tipo_ato,
        TRIM(SPLIT_PART(pn.content, '|', 3))        AS campo3,
        TRIM(SPLIT_PART(pn.content, '|', 4))        AS campo4
    FROM person_note pn, params
    WHERE pn.content ~ ('Ata:\s*' || params.ata_numero || '(\D|$)')
),
notes_norm AS (
    SELECT
        n.*,
        CASE
            WHEN n.campo3 ILIKE 'Ata:%' THEN NULLIF(n.campo4, '')
            ELSE NULLIF(n.campo3, '')
        END AS forma_ato
    FROM notes n
),
pais AS (
    SELECT
        pr.pessoa_id,
        MAX(CASE WHEN pr.tipo_relacionamento = 'PAI' THEN p.nome END) AS pai,
        MAX(CASE WHEN pr.tipo_relacionamento = 'MAE' THEN p.nome END) AS mae
    FROM pessoa_relacionamento pr
    JOIN pessoa p ON p.pessoa_id = pr.pessoa_relacionada_id
    WHERE pr.tipo_relacionamento IN ('PAI', 'MAE')
    GROUP BY pr.pessoa_id
),
estado_civil_texto AS (
    SELECT
        p.pessoa_id,
        CASE
            WHEN p.estado_civil IS NULL THEN NULL
            WHEN p.sexo = 'FEMININO' THEN
                CASE
                    WHEN p.estado_civil = 'CASADO'                THEN 'casada'
                    WHEN p.estado_civil::text LIKE 'SOLTEIRO%'    THEN 'solteira'
                    WHEN p.estado_civil::text LIKE 'DIVORCIADO%'  THEN 'divorciada'
                    WHEN p.estado_civil::text LIKE 'VIUVO%'       THEN 'viúva'
                END
            ELSE
                CASE
                    WHEN p.estado_civil = 'CASADO'                THEN 'casado'
                    WHEN p.estado_civil::text LIKE 'SOLTEIRO%'    THEN 'solteiro'
                    WHEN p.estado_civil::text LIKE 'DIVORCIADO%'  THEN 'divorciado'
                    WHEN p.estado_civil::text LIKE 'VIUVO%'       THEN 'viúvo'
                END
        END AS texto
    FROM pessoa p
),
endereco_fmt AS (
    SELECT
        e.id,
        CONCAT_WS(', ',
            NULLIF(TRIM(CONCAT_WS(' ',
                NULLIF(e.logradouro, ''),
                NULLIF(e.numero, ''),
                NULLIF(e.complemento, '')
            )), ''),
            NULLIF(e.bairro, ''),
            NULLIF(CONCAT_WS('/', NULLIF(e.cidade, ''), NULLIF(e.estado, '')), ''),
            CASE WHEN NULLIF(e.cep, '') IS NOT NULL THEN 'CEP ' || e.cep END
        ) AS texto
    FROM endereco e
),
linhas AS (
    SELECT
        n.tipo_ato,
        n.forma_ato,
        n.data_ato,
        p.pessoa_id,
        p.nome,
        p.sexo,
        p.igreja_anterior,
        pa.pai,
        pa.mae,
        ec.texto AS estado_civil_texto,
        COALESCE(NULLIF(ef.texto, ''), '[endereço não cadastrado]') AS endereco_texto,
        -- "filha de PAI/MAE" com placeholders quando não cadastrado
        CASE WHEN p.sexo = 'FEMININO' THEN 'filha de ' ELSE 'filho de ' END
            || COALESCE(pa.pai, '[pai não cadastrado]')
            || '/'
            || COALESCE(pa.mae, '[mãe não cadastrada]')                   AS pais_texto,
        -- forma_ato sem o prefixo "Demissão por " / "Admissão por "
        REGEXP_REPLACE(LOWER(COALESCE(n.forma_ato, '')),
                       '^(demissão|admissão) por ', '')                   AS forma_norm,
        TO_CHAR(p.data_nascimento, 'DD/MM/YYYY')                          AS data_nasc_str
    FROM notes_norm n
    JOIN pessoa p             ON p.pessoa_id = n.person_id
    LEFT JOIN pais pa         ON pa.pessoa_id = p.pessoa_id
    LEFT JOIN estado_civil_texto ec ON ec.pessoa_id = p.pessoa_id
    LEFT JOIN endereco_fmt ef ON ef.id = p.endereco_id
)
SELECT
    l.tipo_ato,
    l.forma_ato,
    l.data_ato,
    l.pessoa_id AS id,
    l.nome,
    -- Linha formatada conforme tipo de ato
    CASE
        -- A) Admissão de membro comungante por batismo
        WHEN l.tipo_ato ILIKE 'Admissão de membro comungante%'
             AND l.forma_ato ILIKE '%batismo%' THEN
            'em ' || l.data_ato || ', '
            || UPPER(l.nome) || ' (' || l.pessoa_id || '), '
            || l.pais_texto
            || ', ' || CASE WHEN l.sexo = 'FEMININO' THEN 'nascida' ELSE 'nascido' END
                   || ' em ' || COALESCE(l.data_nasc_str, '[data de nascimento não cadastrada]')
            || ', ' || COALESCE(l.estado_civil_texto, '[estado civil não cadastrado]')
            || ', com residência e domicílio em ' || l.endereco_texto
            || ', por batismo celebrado pelo [celebrante];'

        -- B) Admissão de membro comungante por jurisdição/transferência
        WHEN l.tipo_ato ILIKE 'Admissão de membro comungante%' THEN
            'em ' || l.data_ato || ', '
            || UPPER(l.nome) || ' (' || l.pessoa_id || '), '
            || l.pais_texto
            || ', ' || CASE WHEN l.sexo = 'FEMININO' THEN 'nascida' ELSE 'nascido' END
                   || ' em ' || COALESCE(l.data_nasc_str, '[data de nascimento não cadastrada]')
            || ', ' || COALESCE(l.estado_civil_texto, '[estado civil não cadastrado]')
            || ', com residência e domicílio em ' || l.endereco_texto
            || ', comungante, proveniente da '
                   || COALESCE(NULLIF(l.igreja_anterior, ''), '[igreja anterior não cadastrada]')
            || ', por ' || l.forma_norm || ';'

        -- C) Admissão de menor não-comungante (sempre por batismo)
        WHEN l.tipo_ato ILIKE 'Admissão de menor não comungante%' THEN
            'em ' || l.data_ato || ', '
            || UPPER(l.nome) || ' (' || l.pessoa_id || '), '
            || l.pais_texto
            || ', ' || CASE WHEN l.sexo = 'FEMININO' THEN 'nascida' ELSE 'nascido' END
                   || ' em ' || COALESCE(l.data_nasc_str, '[data de nascimento não cadastrada]')
            || ', ' || COALESCE(l.estado_civil_texto, 'solteiro(a)')
            || ', com residência e domicílio em ' || l.endereco_texto
            || ', por batismo, celebrado pelo [celebrante];'

        -- D) Demissões (com ou sem destino)
        WHEN l.tipo_ato ILIKE 'Demissão%' THEN
            'em ' || l.data_ato || ', '
            || UPPER(l.nome) || ' (' || l.pessoa_id || '), '
            || 'por ' || l.forma_norm || ';'

        -- E) Emissão de carta de transferência
        WHEN l.tipo_ato ILIKE 'Emissão de carta de transferência%' THEN
            'em ' || l.data_ato || ', '
            || INITCAP(l.nome) || ' (' || l.pessoa_id || ')'
            || COALESCE(' → ' || l.forma_ato, '')
            || ';'

        -- F) Solicitação de carta de transferência
        WHEN l.tipo_ato ILIKE 'Solicitação de carta de transferência%' THEN
            'em ' || l.data_ato || ', '
            || INITCAP(l.nome) || ' (' || l.pessoa_id || ');'

        ELSE
            'em ' || l.data_ato || ', '
            || UPPER(l.nome) || ' (' || l.pessoa_id || ') — '
            || COALESCE(l.tipo_ato, '[?]') || ' / '
            || COALESCE(l.forma_ato, '[?]') || ';'
    END AS linha_relatorio
FROM linhas l
ORDER BY
    l.tipo_ato,
    l.forma_ato NULLS FIRST,
    TO_DATE(l.data_ato, 'DD/MM/YYYY'),
    l.nome;
