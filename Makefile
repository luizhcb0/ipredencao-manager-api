db-up:
	docker compose up -d
	./gradlew update

migrate:
	./gradlew update

jooq:
	./gradlew generateJooq

run: db-up jooq
	./gradlew bootRun

run-prod: db-up jooq
	SPRING_PROFILES_ACTIVE=prod ./gradlew bootRun

restart:
	docker compose down
	docker compose up -d
	./gradlew update

clean:
	docker compose down
	docker volume prune -f
	docker compose up -d
	./gradlew update 