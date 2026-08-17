.PHONY: help
help: ## Mostrar ajuda
	@echo "Comandos disponíveis:"
	@echo ""
	@echo "Desenvolvimento:"
	@echo "  make run          - Subir DB, gerar JOOQ e rodar app"
	@echo "  make db-up        - Subir DB e rodar migrations"
	@echo "  make jooq         - Gerar código JOOQ (sobe containers)"
	@echo "  make jooq-only    - Gerar código JOOQ (DB já rodando)"
	@echo "  make migrate      - Rodar migrations"
	@echo "  make restart      - Reiniciar DB"
	@echo "  make clean        - Limpar DB e volumes"
	@echo ""
	@echo "Docker:"
	@echo "  make build        - Build imagem Docker"
	@echo "  make test-docker  - Testar imagem localmente"
	@echo ""
	@echo "Deploy AWS:"
	@echo "  make aws-login    - Login AWS SSO"
	@echo "  make create-ecr   - Criar repositório ECR"
	@echo "  make deploy       - Deploy para App Runner"

# Detectar o sistema operacional
ifeq ($(OS),Windows_NT)
    GRADLEW = gradlew.bat
else
    GRADLEW = ./gradlew
endif

db-up:
	docker compose up -d
	$(GRADLEW) update

migrate:
	$(GRADLEW) update

jooq:
	$(GRADLEW) generateJooq

jooq-only:
	$(GRADLEW) generateJooq -x composeUp

run: db-up jooq
	$(GRADLEW) bootRun

run-prod: db-up jooq
ifeq ($(OS),Windows_NT)
	set SPRING_PROFILES_ACTIVE=prod && $(GRADLEW) bootRun
else
	SPRING_PROFILES_ACTIVE=prod $(GRADLEW) bootRun
endif

restart:
	docker compose down
	docker compose up -d
	$(GRADLEW) update

clean:
	docker compose down
	docker volume prune -f
	docker compose up -d
	$(GRADLEW) update

# Comandos específicos para Windows (backup)
win-run:
	docker compose up -d
	gradlew.bat update
	gradlew.bat generateJooq
	gradlew.bat bootRun

win-restart:
	docker compose down
	docker compose up -d
	gradlew.bat update

# Comandos específicos para Linux/Mac (backup)
unix-run:
	docker compose up -d
	./gradlew update
	./gradlew generateJooq
	./gradlew bootRun

unix-restart:
	docker compose down
	docker compose up -d
	./gradlew update

# Docker commands
build:
	$(GRADLEW) generateJooq
	docker build -t ipredencao-manager-api .

deploy:
	./scripts/quick-rebuild.sh --yes

test-docker:
	docker run -p 8080:8080 \
		-e SPRING_PROFILES_ACTIVE=local \
		-e DATABASE_URL=jdbc:postgresql://host.docker.internal:54329/ipredencao_manager \
		-e DATABASE_USERNAME=ipredencao_manager \
		-e DATABASE_PASSWORD=ipredencao_manager \
		ipredencao-manager-api

# AWS commands
aws-login:
	aws sso login

create-ecr:
	aws ecr create-repository --repository-name ipredencao-manager-api --region us-east-1 