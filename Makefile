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