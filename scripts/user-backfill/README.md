# Backfill — usuários inativos vinculados a pessoas

One-shot: cria ou vincula `usuario` inativo para pessoas dos agregadores **2** (membro comungante), **3** (não comungante) e **5** (admitendo) que têm `pessoa.email`. Não envia convite. Não altera perfil/status de usuário já existente — só preenche `person_id` quando o e-mail casa de forma unívoca.

Requer a migration **V014** (`usuario.person_id` + perfis `MEMBER` / `MEMBERSHIP_CANDIDATE`).

## Pré-requisitos

| Item | Valor |
|------|--------|
| V014 aplicada | `\d usuario` mostra `person_id` |
| Python 3.11+ | `python3 -m venv .venv && .venv/bin/pip install -r requirements.txt` |
| Postgres | `DATABASE_URL` (obrigatório fora do Docker local) |
| Firebase Admin | `FIREBASE_SERVICE_ACCOUNT_KEY_PATH` (só no `--apply`) |

Não carrega `scripts/.env.local` sozinho — esse arquivo tem host de produção. Sem `DATABASE_URL`, o script usa o Postgres local (`localhost:54329`). Para produção, exporte `DATABASE_URL` explicitamente.

Local (Docker):

```bash
export DATABASE_URL="postgresql://ipredencao_manager:ipredencao_manager@localhost:54329/ipredencao_manager"
```

## Ordem

### 1. Testes das regras

```bash
cd ipredencao-manager-api/scripts/user-backfill
python3 -m unittest test_classify.py
```

### 2. Dry-run (padrão)

Não toca Firebase nem grava `usuario`. Gera CSV.

```bash
python3 backfill.py --csv user_backfill_dry_run.csv
```

Revise `skipped` (`email_collision`, `person_collision`, `no_email`) antes do apply.

### 3. Apply

Cria identidade Firebase **habilitada**, com senha inicial = CPF só dígitos quando a pessoa tem CPF utilizável (6+ dígitos). Sem CPF, a conta nasce sem senha e ainda habilitada. Insere `usuario` `active=false` `provider=EMAIL`, ou só faz `UPDATE person_id` quando o e-mail já pertence a um usuário livre. E-mail já existente no Firebase é reaproveitado **sem** gravar senha e **sem** desativar. Retomável: `already_done` / `linked` não duplicam e não reescrevem senha. Se o INSERT falhar após criar o Firebase, o script tenta apagar a identidade nova. O CSV traz `password_set=yes` quando a senha foi definida; nunca grava o CPF.

```bash
export FIREBASE_SERVICE_ACCOUNT_KEY_PATH=/caminho/ipredencao-manager-api-firebase-adminsdk.json
python3 backfill.py --apply --csv user_backfill_apply.csv
```

Não dispare convites em massa. Ativação no app = admin liga `active`. A senha inicial no Firebase é o CPF (números); a pessoa troca depois por **Esqueci minha senha**. Um apply anterior que já criou contas disabled/sem senha **não** é reparado.

### 4. Conferência

```sql
SELECT access_profile, active, count(*)
FROM usuario
WHERE access_profile IN ('MEMBER', 'MEMBERSHIP_CANDIDATE')
GROUP BY 1, 2;

SELECT count(*) FROM usuario WHERE person_id IS NOT NULL;
SELECT person_id, count(*) FROM usuario WHERE person_id IS NOT NULL GROUP BY 1 HAVING count(*) > 1;
```

Todo `created` deve ter `active=false`, `person_id` único e `firebase_uid` preenchido.

## Rollback

Não há down automático do enum Postgres. Para desfazer o apply:

1. Apagar os `usuario` criados neste run (`added_at` / CSV `created`).
2. `UPDATE usuario SET person_id = NULL WHERE id IN (...)` para os `linked`.
3. Apagar no Firebase só UIDs **criados** neste run (não os reaproveitados).

Usuários staff pré-existentes devem permanecer.
