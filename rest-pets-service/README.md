# REST Pets Service

Spring Boot microservice that exposes a modern JSON REST API over the legacy
[`soap-pets-service`](../soap-pets-service/) RPC/encoded SOAP endpoint.

## Mapping

| REST | SOAP |
|------|------|
| `GET /api/v1/pets/dogs` | `ListDogs` |
| `GET /api/v1/pets/cats` | `ListCats` |
| `GET /api/v1/pets?type=dog\|cat` | `ListDogs` / `ListCats` |
| `POST /api/v1/pets` `{ "name", "type" }` | `AddPet` |

Also available:

- OpenAPI UI: `http://localhost:8081/swagger-ui.html`
- Health: `http://localhost:8081/actuator/health`

## Prerequisites

1. Run the SOAP service on port `8080` (see [`soap-pets-service/README.md`](../soap-pets-service/README.md)).
2. Java 21 and Maven 3.8+.

## Run

```bash
cd rest-pets-service
mvn -q spring-boot:run
```

Optional env overrides:

| Variable | Default |
|----------|---------|
| `SOAP_PETS_BASE_URL` | `http://localhost:8080/pets` |
| `SOAP_PETS_USERNAME` | `admin` |
| `SOAP_PETS_PASSWORD` | `secret` |
| `SERVER_PORT` | `8081` |

## Sample requests

```bash
# List dogs
curl -s http://localhost:8081/api/v1/pets/dogs | jq .

# List cats
curl -s 'http://localhost:8081/api/v1/pets?type=cat' | jq .

# Add a dog
curl -s -X POST http://localhost:8081/api/v1/pets \
  -H 'Content-Type: application/json' \
  -d '{"name":"Rex","type":"dog"}' | jq .
```

## Tests

```bash
cd rest-pets-service
mvn -q test
```
