# merchandise-inventory

## Executar com Gradle

Requer JDK 21.

```bash
./gradlew bootRun
```

## Executar com Docker Compose

Não precisa de JDK instalado, o build é feito dentro do container.

```bash
docker compose up --build
```

Para parar: `Ctrl+C` ou `docker compose down`.

Se a porta 8080 estiver ocupada, use outra:

```bash
PORT=9090 docker compose up --build
```

## Usar JDBC em vez de JPA

Por padrão a aplicação usa JPA. Para usar JDBC, defina a variável `PERSISTENCE_TYPE=jdbc`:

```bash
PERSISTENCE_TYPE=jdbc ./gradlew bootRun
```

```bash
PERSISTENCE_TYPE=jdbc docker compose up --build
```

## Testes

```bash
./gradlew test
```

Relatório: `build/reports/tests/test/index.html`

## Swagger

http://localhost:8080/swagger-ui.html
