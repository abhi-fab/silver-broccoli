# silver-broccoli

## SOAP Pets Service

Java sample under [`soap-pets-service/`](soap-pets-service/): RPC/encoded SOAP with basic auth and `ListDogs` / `ListCats` / `AddPet` operations.

```bash
cd soap-pets-service
mvn -q compile exec:java
```

## REST Pets Service

Spring Boot microservice under [`rest-pets-service/`](rest-pets-service/): JSON REST facade over the SOAP pets service.

```bash
# Terminal 1 — SOAP backend
cd soap-pets-service && mvn -q compile exec:java

# Terminal 2 — REST wrapper (port 8081)
cd rest-pets-service && mvn -q spring-boot:run
```

| REST | SOAP |
|------|------|
| `GET /api/v1/pets/dogs` | `ListDogs` |
| `GET /api/v1/pets/cats` | `ListCats` |
| `GET /api/v1/pets?type=dog\|cat` | `ListDogs` / `ListCats` |
| `POST /api/v1/pets` | `AddPet` |
