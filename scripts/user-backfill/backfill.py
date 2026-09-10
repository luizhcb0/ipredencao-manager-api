#!/usr/bin/env python3
"""Backfill idempotente: cria/vincula usuários inativos para pessoas elegíveis.

Padrão: dry-run (só CSV). Use --apply para gravar Firebase + Postgres.
Não envia e-mail de convite.
"""

from __future__ import annotations

import argparse
import csv
import os
import sys
from pathlib import Path

import psycopg

from classify import (
    ALREADY_DONE,
    CREATED,
    LINKED,
    SKIPPED,
    Action,
    PersonRow,
    UserRow,
    classify,
)

PEOPLE_SQL = """
SELECT p.pessoa_id, p.nome, p.email, c.agregador_categoria_id
FROM pessoa p
JOIN categoria c ON c.id = p.categoria_id
WHERE c.agregador_categoria_id IN (2, 3, 5)
"""

USERS_SQL = """
SELECT id, email, person_id, access_profile::text, active, firebase_uid
FROM usuario
"""

INSERT_SQL = """
INSERT INTO usuario (firebase_uid, email, name, access_profile, active, provider, person_id)
VALUES (%s, %s, %s, %s::perfil_acesso, false, 'EMAIL'::provider_autenticacao, %s)
RETURNING id
"""

LINK_SQL = "UPDATE usuario SET person_id = %s WHERE id = %s AND person_id IS NULL"


def database_url() -> str:
    if os.environ.get("DATABASE_URL"):
        return os.environ["DATABASE_URL"]
    return "postgresql://ipredencao_manager:ipredencao_manager@localhost:54329/ipredencao_manager"


def fetch_rows(conn) -> tuple[list[PersonRow], list[UserRow]]:
    with conn.cursor() as cur:
        cur.execute(PEOPLE_SQL)
        people = [PersonRow(r[0], r[1], r[2], r[3]) for r in cur.fetchall()]
        cur.execute(USERS_SQL)
        users = [UserRow(r[0], r[1], r[2], r[3], r[4], r[5]) for r in cur.fetchall()]
    return people, users


def write_csv(actions: list[Action], path: Path) -> None:
    with path.open("w", newline="") as fh:
        writer = csv.writer(fh)
        writer.writerow(["outcome", "pessoa_id", "nome", "email", "profile", "reason", "usuario_id"])
        for action in actions:
            writer.writerow([
                action.outcome,
                action.pessoa_id,
                action.nome,
                action.email or "",
                action.profile or "",
                action.reason,
                action.usuario_id or "",
            ])


def init_firebase():
    import firebase_admin
    from firebase_admin import credentials

    if firebase_admin._apps:
        return
    key_path = os.environ.get("FIREBASE_SERVICE_ACCOUNT_KEY_PATH") or os.environ.get(
        "GOOGLE_APPLICATION_CREDENTIALS"
    )
    if not key_path:
        raise SystemExit("Defina FIREBASE_SERVICE_ACCOUNT_KEY_PATH ou GOOGLE_APPLICATION_CREDENTIALS")
    firebase_admin.initialize_app(credentials.Certificate(key_path))


def firebase_uid_for(email: str, display_name: str) -> tuple[str, bool]:
    """Retorna (uid, created). created=True só quando a identidade foi criada agora."""
    from firebase_admin import auth

    try:
        user = auth.create_user(email=email, display_name=display_name, disabled=True)
        return user.uid, True
    except auth.EmailAlreadyExistsError:
        existing = auth.get_user_by_email(email)
        if not existing.disabled:
            auth.update_user(existing.uid, disabled=True)
        return existing.uid, False


def compensate_firebase(uid: str) -> None:
    from firebase_admin import auth

    try:
        auth.delete_user(uid)
    except Exception as exc:  # noqa: BLE001
        print(f"aviso: não foi possível compensar Firebase {uid}: {exc}", file=sys.stderr)


def apply_actions(conn, actions: list[Action]) -> list[Action]:
    init_firebase()
    applied: list[Action] = []
    with conn.cursor() as cur:
        for action in actions:
            if action.outcome == ALREADY_DONE or action.outcome == SKIPPED:
                applied.append(action)
                continue
            if action.outcome == LINKED:
                cur.execute(LINK_SQL, (action.pessoa_id, action.usuario_id))
                applied.append(action)
                continue
            uid, created = firebase_uid_for(action.email, action.nome)
            try:
                cur.execute(INSERT_SQL, (uid, action.email, action.nome, action.profile, action.pessoa_id))
                usuario_id = cur.fetchone()[0]
            except Exception:
                if created:
                    compensate_firebase(uid)
                raise
            applied.append(Action(
                CREATED, action.pessoa_id, action.nome, action.email, action.profile, action.reason, usuario_id
            ))
    conn.commit()
    return applied


def summarize(actions: list[Action]) -> None:
    counts: dict[str, int] = {}
    for action in actions:
        counts[action.outcome] = counts.get(action.outcome, 0) + 1
    for outcome in (CREATED, LINKED, ALREADY_DONE, SKIPPED):
        print(f"{outcome}: {counts.get(outcome, 0)}")


def main() -> int:
    parser = argparse.ArgumentParser(description="Backfill de usuários inativos vinculados a pessoas")
    parser.add_argument("--apply", action="store_true", help="Grava Firebase e Postgres (sem isto, só dry-run)")
    parser.add_argument("--csv", default="user_backfill_report.csv", help="Caminho do CSV de relatório")
    args = parser.parse_args()

    url = database_url()
    host = url.split("@")[-1]
    print(f"banco: {host}")

    with psycopg.connect(url) as conn:
        people, users = fetch_rows(conn)
        actions = classify(people, users)
        csv_path = Path(args.csv)
        if args.apply:
            actions = apply_actions(conn, actions)
            write_csv(actions, csv_path)
            print(f"apply gravado em {csv_path}")
        else:
            write_csv(actions, csv_path)
            print(f"dry-run gravado em {csv_path} (passe --apply para executar)")
        summarize(actions)
    return 0


if __name__ == "__main__":
    sys.exit(main())
