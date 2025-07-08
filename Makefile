db-up:
	docker compose up -d
	./gradlew update

migrate:
	./gradlew update

jooq:
	./gradlew generateJooq

run: db-up jooq
	./gradlew bootRun 