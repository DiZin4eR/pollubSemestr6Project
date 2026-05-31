# EcoData Hub

Aplikacja webowa Spring Boot integrujaca dane gospodarcze z API GUS oraz artykuly z News API. Interfejs HTML jest renderowany po stronie backendu z uzyciem Thymeleaf, a dane aplikacji sa przechowywane w bazie MariaDB.

## Funkcje

- przegladanie tematow i wskaznikow GUS,
- pobieranie danych dla wybranych zmiennych,
- lista aktualnosci gospodarczych,
- eksport wybranych widokow do JSON,
- REST API dla zasobow bazodanowych z dokumentacja Swagger UI.

## Wymagania

- Docker,
- Docker Compose,
- opcjonalnie Java 17, jezeli aplikacja ma byc uruchamiana lokalnie bez kontenerow.

## Uruchomienie z Docker CLI

1. Zbuduj obraz aplikacji:

```bash
docker build -t ecodata-hub .
```

2. Utworz siec dla kontenerow:

```bash
docker network create ecodata-network
```

3. Uruchom kontener bazy danych:

```bash
docker run -d \
  --name ecodata-db \
  --network ecodata-network \
  -e MARIADB_DATABASE=economy_db \
  -e MARIADB_USER=eco_user \
  -e MARIADB_PASSWORD=eco_pass \
  -e MARIADB_ROOT_PASSWORD=root \
  -v ecodata-mariadb-data:/var/lib/mysql \
  -v "$(pwd)/docker/mariadb/init:/docker-entrypoint-initdb.d:ro" \
  mariadb:11.4
```

Port bazy nie jest mapowany na hosta, aby uniknac konfliktu z lokalna MariaDB na porcie `3306`. Backend laczy sie z baza przez siec Dockera: `ecodata-db:3306`. Jezeli potrzebny jest dostep do bazy z hosta, dodaj mapowanie na wolny port, np. `-p 3307:3306`.

4. Uruchom kontener backendu:

```bash
docker run -d \
  --name ecodata-backend \
  --network ecodata-network \
  -e SPRING_DATASOURCE_URL=jdbc:mariadb://ecodata-db:3306/economy_db \
  -e SPRING_DATASOURCE_USERNAME=eco_user \
  -e SPRING_DATASOURCE_PASSWORD=eco_pass \
  -e APP_ADMIN_USERNAME=admin \
  -e APP_ADMIN_PASSWORD=admin \
  -e APP_JWT_SECRET=change-this-secret \
  -e NEWS_API_KEY=2142cb7cc8ba4a829e73887f56de987d \
  -p 8080:8080 \
  ecodata-hub
```

5. Otworz aplikacje:

- strona glowna: `http://localhost:8080/`
- Swagger UI po zalogowaniu: `http://localhost:8080/swagger-ui.html`

Konta i autoryzacja:

- konto uzytkownika mozna utworzyc przez `http://localhost:8080/signup`,
- logowanie jest dostepne pod `http://localhost:8080/login`,
- wszystkie strony aplikacji, endpointy API i dokumentacja Swagger wymagaja zalogowania poza `/login`, `/signup`, plikami CSS/JS i `POST /api/auth/login`,
- administrator jest seedowany przy starcie z `APP_ADMIN_USERNAME` / `APP_ADMIN_PASSWORD`,
- po zalogowaniu aplikacja zapisuje JWT w ciasteczku HttpOnly `AUTH_TOKEN`,
- API przyjmuje tez naglowek `Authorization: Bearer <token>`.

## Uruchomienie z docker-compose

Uruchom aplikacje i baze danych:

```bash
docker compose up --build
```

Aplikacja bedzie dostepna pod adresem `http://localhost:8080/`.
Kontener bazy danych nie publikuje portu `3306` na hoście, więc Compose moze dzialac rownolegle z lokalna MariaDB.

Zatrzymanie kontenerow:

```bash
docker compose down
```

Zatrzymanie kontenerow i usuniecie wolumenu bazy danych:

```bash
docker compose down -v
```

## Dane startowe GUS

Import tematow GUS jest wolny, bo aplikacja ogranicza czestotliwosc zapytan do API GUS. Z tego powodu projekt zawiera startowa paczke danych `docker/mariadb/init/01-gus-initial-data.sql`.

`docker-compose.yaml` uzywa dwoch mechanizmow:

- wolumen `ecodata-mariadb-data` przechowuje baze pomiedzy restartami kontenerow,
- katalog `docker/mariadb/init` jest montowany jako `/docker-entrypoint-initdb.d`, dzieki czemu MariaDB automatycznie importuje pliki `.sql` przy pierwszym starcie pustej bazy.

Obecna paczka startowa zawiera tylko dane GUS:

- `gus_data_attributes`,
- `gus_subjects`,
- `gus_subject_children`,
- `gus_subject_levels`,
- `gus_subject_import_states`.

Nie zawiera danych aktualnosci ani starych tabel testowych. Aby odswiezyc paczke po ponownym imporcie tematow GUS, wykonaj:

```bash
mkdir -p docker/mariadb/init
docker exec ecodata-db mariadb-dump \
  -uroot \
  -proot \
  economy_db \
  gus_subjects \
  gus_subject_children \
  gus_subject_levels \
  gus_subject_import_states \
  > docker/mariadb/init/01-gus-initial-data.sql
```

Przy kolejnym tworzeniu bazy od zera MariaDB automatycznie wykona plik `docker/mariadb/init/01-gus-initial-data.sql`. Mechanizm init dziala tylko dla pustego katalogu danych, wiec aby go przetestowac, trzeba usunac wolumen:

```bash
docker compose down -v
docker compose up --build
```

## Struktura projektu

```text
.
|-- Dockerfile
|-- docker-compose.yaml
|-- docker/
|   |-- mariadb/
|       |-- init/             # opcjonalne dumpy SQL ladowane do pustej bazy
|-- build.gradle
|-- settings.gradle
|-- gradlew
|-- gradle/
|-- src/
|   |-- main/
|   |   |-- java/com/ecodatahub/
|   |   |   |-- config/        # konfiguracja Spring, OpenAPI, Security i klientow HTTP
|   |   |   |-- controller/    # kontrolery MVC i REST
|   |   |   |-- gus/           # modul danych GUS: API, DTO, encje, repozytoria, serwisy
|   |   |   |-- news/          # modul aktualnosci: API NewsAPI, encje, repozytoria, serwisy
|   |   |   |-- EcoDataHubApplication.java
|   |   |-- resources/
|   |       |-- application.properties
|   |       |-- static/        # CSS i JavaScript
|   |       |-- templates/     # szablony Thymeleaf
|   |-- test/                 # testy automatyczne
```

## Konfiguracja

Najwazniejsze zmienne srodowiskowe:

- `SPRING_DATASOURCE_URL` - adres JDBC bazy MariaDB,
- `SPRING_DATASOURCE_USERNAME` - uzytkownik bazy danych,
- `SPRING_DATASOURCE_PASSWORD` - haslo bazy danych,
- `NEWS_API_KEY` - klucz do News API,
- `APP_ADMIN_USERNAME`, `APP_ADMIN_PASSWORD` - konto administratora.
- `APP_JWT_SECRET` - sekret do podpisywania tokenow JWT.

## Wykorzystanie sztucznej inteligencji

Podczas przygotowania konteneryzacji i dokumentacji wykorzystano model LLM ChatGPT/Codex. Wygenerowane lub wspoltworzone przez LLM fragmenty obejmuja:

- `Dockerfile` - wieloetapowy obraz aplikacji Spring Boot,
- `.dockerignore` - ograniczenie kontekstu budowania obrazu,
- `docker-compose.yaml` - uruchomienie backendu i bazy MariaDB w osobnych kontenerach,
- `README.md` - opis aplikacji, instrukcje uruchamiania i opis struktury projektu.

Wygenerowane tresci zostaly dopasowane do istniejacej konfiguracji projektu: Gradle, Java 17, MariaDB, port `8080`, zmienne Spring Boot oraz aktualna struktura pakietow. Link do rozmowy z LLM nalezy dolaczyc w systemie oddawania projektu albo zastapic te sekcje eksportem/podsumowaniem rozmowy, jezeli link nie jest dostepny.

Podsumowanie kontekstu rozmowy znajduje sie w pliku `LLM_CONTEXT.md`.
