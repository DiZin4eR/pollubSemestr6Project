# Kontekst wykorzystania LLM

## Narzedzie

W pracy nad konteneryzacja i dokumentacja projektu wykorzystano model LLM ChatGPT/Codex.

## Zakres rozmowy

Uzytkownik przekazal wymagania projektowe dotyczace:

- konteneryzacji aplikacji,
- uruchamiania przez Docker CLI i docker-compose,
- rozdzielenia backendu i bazy danych na oddzielne kontenery,
- opisania wykorzystania narzedzi LLM,
- przygotowania dokumentacji README.

Na podstawie istniejacej struktury repozytorium sprawdzono, ze projekt jest aplikacja Spring Boot budowana przez Gradle, korzysta z Java 17, MariaDB, Thymeleaf, Spring Data JPA, Spring Security oraz integracji z API GUS i News API.

## Fragmenty wygenerowane lub wspoltworzone przez LLM

- `Dockerfile` - wieloetapowy obraz Docker budujacy aplikacje przez Gradle i uruchamiajacy plik JAR na obrazie JRE.
- `.dockerignore` - lista katalogow i plikow pomijanych w kontekscie budowania obrazu.
- `docker-compose.yaml` - konfiguracja dwoch uslug: `backend` i `db`, z baza MariaDB, wolumenem danych, healthcheckiem oraz zmiennymi srodowiskowymi Spring Boot.
- `docker/mariadb/init/01-gus-initial-data.sql` - przefiltrowany dump SQL zawierajacy tylko dane GUS ladowane automatycznie przez obraz MariaDB przy starcie pustej bazy.
- `README.md` - opis aplikacji, instrukcje uruchomienia przez Docker CLI, instrukcje uruchomienia przez docker-compose, opis struktury projektu, konfiguracja i informacja o wykorzystaniu LLM.
- `LLM_CONTEXT.md` - niniejsze podsumowanie rozmowy.
- modul `auth` - logowanie, rejestracja, JWT w ciasteczku HttpOnly oraz obsluga naglowka `Authorization: Bearer <token>`.
- `SecurityConfig` - konfiguracja dostepu wymagajaca zalogowania dla stron aplikacji, endpointow API i dokumentacji Swagger, z wyjatkiem logowania, rejestracji, statycznych zasobow i endpointu `POST /api/auth/login`.

## Modyfikacje i dopasowanie wygenerowanych tresci

Wygenerowane tresci zostaly dopasowane do istniejacego projektu:

- uzyto Java 17 zgodnie z `build.gradle`,
- wybrano MariaDB zgodnie z zaleznoscia `org.mariadb.jdbc:mariadb-java-client` i konfiguracja `spring.datasource.*`,
- ustawiono port aplikacji `8080`,
- uzyto nazwy bazy `economy_db`, uzytkownika `eco_user` i hasla `eco_pass` zgodnie z `application.properties`,
- uwzgledniono zmienne `APP_ADMIN_*`, `APP_JWT_SECRET` i `NEWS_API_KEY`,
- przefiltrowano pelny dump bazy tak, aby paczka startowa zawierala tylko tabele `gus_*` i nie importowala aktualnosci ani starych tabel testowych,
- kod zostal uporzadkowany w pakiety feature-oriented `gus`, `news`, `config` i `controller`,
- dodano pakiet `auth` oraz ustawiono reguly Spring Security tak, aby anonimowy dostep byl ograniczony do formularzy logowania/rejestracji, plikow statycznych i endpointu wydajacego token JWT,
- opis struktury projektu przygotowano na podstawie rzeczywistych katalogow `auth`, `controller`, `gus`, `news`, `config`, `templates` i `static`.

## Weryfikacja

- `docker compose config` zakonczyl sie poprawnie i potwierdzil poprawnosc skladni `docker-compose.yaml`.
- `./gradlew test --no-daemon` zakonczyl sie wynikiem `BUILD SUCCESSFUL`.
- `docker build -t ecodata-hub:verify .` nie mogl zostac wykonany w srodowisku roboczym, poniewaz demon Docker nie byl uruchomiony: `Cannot connect to the Docker daemon`.
