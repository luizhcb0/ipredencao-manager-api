#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
API_DIR="$(dirname "$SCRIPT_DIR")"
DUMP_DIR="$SCRIPT_DIR/.dump"
DUMP_FILE="$DUMP_DIR/prod.dump"
ENV_FILE="$SCRIPT_DIR/.env.local"

LOCAL_DB_HOST="localhost"
LOCAL_DB_PORT="54329"
LOCAL_DB_NAME="ipredencao_manager"
LOCAL_DB_USER="ipredencao_manager"
LOCAL_DB_PASSWORD="ipredencao_manager"

LOCALSTACK_ENDPOINT="http://localhost:4566"
PROD_S3_BUCKET="ipredencao-prod-storage"
LOCAL_S3_BUCKET="ipredencao-storage"

REQUIRED_PG_VERSION=17

find_pg_bin() {
    local cmd="$1"

    # 1. Environment variable override (PG_BIN_DIR)
    if [ -n "$PG_BIN_DIR" ] && [ -x "$PG_BIN_DIR/$cmd" ]; then
        echo "$PG_BIN_DIR/$cmd"
        return
    fi

    # 2. Check if the version in PATH is compatible
    if command -v "$cmd" &> /dev/null; then
        local version
        version=$("$cmd" --version 2>/dev/null | grep -oE '[0-9]+' | head -1)
        if [ -n "$version" ] && [ "$version" -ge "$REQUIRED_PG_VERSION" ]; then
            command -v "$cmd"
            return
        fi
    fi

    # 3. Try well-known paths per OS
    local candidates=()
    case "$(uname -s)" in
        Darwin)
            candidates=(
                "/opt/homebrew/opt/postgresql@${REQUIRED_PG_VERSION}/bin/$cmd"
                "/usr/local/opt/postgresql@${REQUIRED_PG_VERSION}/bin/$cmd"
            )
            ;;
        Linux)
            candidates=(
                "/usr/lib/postgresql/${REQUIRED_PG_VERSION}/bin/$cmd"
                "/usr/pgsql-${REQUIRED_PG_VERSION}/bin/$cmd"
            )
            ;;
        MINGW*|MSYS*|CYGWIN*)
            candidates=(
                "/c/Program Files/PostgreSQL/${REQUIRED_PG_VERSION}/bin/$cmd"
                "C:/Program Files/PostgreSQL/${REQUIRED_PG_VERSION}/bin/${cmd}.exe"
            )
            ;;
    esac

    for path in "${candidates[@]}"; do
        if [ -x "$path" ]; then
            echo "$path"
            return
        fi
    done

    return 1
}

resolve_pg_tools() {
    PG_DUMP=$(find_pg_bin pg_dump) || true
    PG_RESTORE=$(find_pg_bin pg_restore) || true
    DROPDB=$(find_pg_bin dropdb) || true
    CREATEDB=$(find_pg_bin createdb) || true
}

usage() {
    echo "Safely sync prod data to local DB."
    echo ""
    echo "Usage: $0 [option]"
    echo ""
    echo "Options:"
    echo "  dump       Dump prod DB and save to $DUMP_DIR"
    echo "  restore    Restore saved dump to local DB"
    echo "  s3         Sync S3 files from prod to local LocalStack"
    echo "  sync       Dump + restore + s3 sync (default)"
    echo "  help       Show this message"
    echo ""
    echo "Requires: PostgreSQL $REQUIRED_PG_VERSION+ client tools, docker"
    echo "Prod credentials must be in $ENV_FILE"
    echo ""
    echo "Set PG_BIN_DIR to override PostgreSQL binary location."
}

check_docker() {
    if ! command -v docker &> /dev/null; then
        echo "ERROR: 'docker' not found."
        exit 1
    fi
}

check_pg_prerequisites() {
    check_docker
    resolve_pg_tools

    local missing=0
    for var in PG_DUMP PG_RESTORE DROPDB CREATEDB; do
        if [ -z "${!var}" ]; then
            echo "ERROR: ${var,,} (PostgreSQL $REQUIRED_PG_VERSION+) not found."
            missing=1
        fi
    done
    if [ $missing -eq 1 ]; then
        echo ""
        echo "Install PostgreSQL $REQUIRED_PG_VERSION client tools, or set PG_BIN_DIR to their location."
        exit 1
    fi

    echo "Using: $PG_DUMP ($($PG_DUMP --version | head -1))"
}

load_prod_credentials() {
    if [ ! -f "$ENV_FILE" ]; then
        echo "ERROR: Credentials file not found: $ENV_FILE"
        echo "Create from template: cp $SCRIPT_DIR/.env.local.example $ENV_FILE"
        exit 1
    fi
    source "$ENV_FILE"

    if [ -z "$PROD_DB_HOST" ] || [ -z "$PROD_DB_USER" ] || [ -z "$PROD_DB_PASSWORD" ]; then
        echo "ERROR: Incomplete credentials in $ENV_FILE"
        echo "Required variables: PROD_DB_HOST, PROD_DB_USER, PROD_DB_PASSWORD"
        exit 1
    fi

    PROD_DB_PORT="${PROD_DB_PORT:-5432}"
    PROD_DB_NAME="${PROD_DB_NAME:-ipredencao_manager}"
}

ensure_local_db() {
    echo "Checking local DB..."
    if ! docker compose -f "$API_DIR/compose.yaml" ps postgres --status running -q 2>/dev/null | grep -q .; then
        echo "Starting local PostgreSQL..."
        docker compose -f "$API_DIR/compose.yaml" up -d --wait postgres
    fi
    echo "Local DB available at $LOCAL_DB_HOST:$LOCAL_DB_PORT"
}

do_dump() {
    load_prod_credentials
    mkdir -p "$DUMP_DIR"

    echo "Dumping prod DB ($PROD_DB_HOST)..."
    PGPASSWORD="$PROD_DB_PASSWORD" "$PG_DUMP" \
        -h "$PROD_DB_HOST" \
        -p "$PROD_DB_PORT" \
        -U "$PROD_DB_USER" \
        -d "$PROD_DB_NAME" \
        -Fc \
        --no-owner \
        --no-privileges \
        -f "$DUMP_FILE"

    local size
    size=$(du -h "$DUMP_FILE" | cut -f1)
    echo "Dump saved: $DUMP_FILE ($size)"
}

do_restore() {
    if [ ! -f "$DUMP_FILE" ]; then
        echo "ERROR: Dump not found: $DUMP_FILE"
        echo "Run '$0 dump' first."
        exit 1
    fi

    ensure_local_db

    echo "Restoring to local DB..."
    PGPASSWORD="$LOCAL_DB_PASSWORD" psql \
        -h "$LOCAL_DB_HOST" \
        -p "$LOCAL_DB_PORT" \
        -U "$LOCAL_DB_USER" \
        -d postgres \
        -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '$LOCAL_DB_NAME' AND pid <> pg_backend_pid();" \
        --quiet 2>/dev/null || true

    PGPASSWORD="$LOCAL_DB_PASSWORD" "$DROPDB" \
        -h "$LOCAL_DB_HOST" \
        -p "$LOCAL_DB_PORT" \
        -U "$LOCAL_DB_USER" \
        --if-exists \
        "$LOCAL_DB_NAME"

    PGPASSWORD="$LOCAL_DB_PASSWORD" "$CREATEDB" \
        -h "$LOCAL_DB_HOST" \
        -p "$LOCAL_DB_PORT" \
        -U "$LOCAL_DB_USER" \
        "$LOCAL_DB_NAME"

    PGPASSWORD="$LOCAL_DB_PASSWORD" "$PG_RESTORE" \
        -h "$LOCAL_DB_HOST" \
        -p "$LOCAL_DB_PORT" \
        -U "$LOCAL_DB_USER" \
        -d "$LOCAL_DB_NAME" \
        --no-owner \
        --no-privileges \
        "$DUMP_FILE"

    echo "Restore complete. Local DB updated with prod data."
}

ensure_localstack() {
    echo "Checking LocalStack..."
    if ! docker compose -f "$API_DIR/compose.yaml" ps localstack --status running -q 2>/dev/null | grep -q .; then
        echo "Starting LocalStack..."
        docker compose -f "$API_DIR/compose.yaml" up -d --wait localstack
    fi
    echo "LocalStack available at $LOCALSTACK_ENDPOINT"
}

do_s3_sync() {
    load_prod_credentials

    if [ -z "$AWS_ACCESS_KEY_ID" ] || [ -z "$AWS_SECRET_ACCESS_KEY" ]; then
        echo "ERROR: AWS credentials not found in $ENV_FILE"
        echo "Required variables: AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY"
        exit 1
    fi

    if ! command -v aws &> /dev/null; then
        echo "ERROR: 'aws' CLI not found. Install it: https://aws.amazon.com/cli/"
        exit 1
    fi

    ensure_localstack

    local S3_LOCAL_DIR="$DUMP_DIR/s3"
    mkdir -p "$S3_LOCAL_DIR"

    echo "Downloading from prod S3 ($PROD_S3_BUCKET)..."
    AWS_ACCESS_KEY_ID="$AWS_ACCESS_KEY_ID" \
    AWS_SECRET_ACCESS_KEY="$AWS_SECRET_ACCESS_KEY" \
    aws s3 sync "s3://$PROD_S3_BUCKET" "$S3_LOCAL_DIR" --region us-east-1

    echo "Ensuring local bucket exists..."
    aws s3 mb "s3://$LOCAL_S3_BUCKET" \
        --endpoint-url "$LOCALSTACK_ENDPOINT" \
        --no-sign-request 2>/dev/null || true

    echo "Uploading to local LocalStack ($LOCAL_S3_BUCKET)..."
    aws s3 sync "$S3_LOCAL_DIR" "s3://$LOCAL_S3_BUCKET" \
        --endpoint-url "$LOCALSTACK_ENDPOINT" \
        --no-sign-request

    echo "S3 sync complete."
}

# --- Main ---

case "${1:-sync}" in
    dump)
        check_pg_prerequisites
        do_dump
        ;;
    restore)
        check_pg_prerequisites
        do_restore
        ;;
    s3)
        check_docker
        do_s3_sync
        ;;
    sync)
        check_pg_prerequisites
        do_dump
        do_restore
        do_s3_sync
        ;;
    help|--help|-h)
        usage
        ;;
    *)
        echo "Unknown option: $1"
        usage
        exit 1
        ;;
esac
