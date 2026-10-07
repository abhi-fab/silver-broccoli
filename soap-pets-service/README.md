# SOAP Pets Service

Local **RPC/encoded** SOAP sample with HTTP Basic Auth.

The WSDL binding uses `style="rpc"` and `use="encoded"` with
`encodingStyle="http://schemas.xmlsoap.org/soap/encoding/"`.

## Operations

| Operation | Parameters | Returns |
|-----------|------------|---------|
| `ListDogs` | — | Dog names |
| `ListCats` | — | Cat names |
| `AddPet` | `name` (string), `type` (`dog` or `cat`) | Added pet name |

## Run

```bash
cd soap-pets-service
mvn -q compile exec:java
```

- Endpoint: `http://localhost:8080/pets`
- WSDL: `http://localhost:8080/pets?wsdl`
- Username: `admin`
- Password: `secret`

## Sample request (ListDogs)

```bash
curl -u admin:secret \
  -H 'Content-Type: text/xml; charset=utf-8' \
  -H 'SOAPAction: ""' \
  -d '<?xml version="1.0" encoding="UTF-8"?>
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:pets="http://example.com/pets">
  <soapenv:Header/>
  <soapenv:Body>
    <pets:ListDogs/>
  </soapenv:Body>
</soapenv:Envelope>' \
  http://localhost:8080/pets
```

Use `ListCats` the same way for cat names.

## Sample request (AddPet)

```bash
curl -u admin:secret \
  -H 'Content-Type: text/xml; charset=utf-8' \
  -H 'SOAPAction: ""' \
  -d '<?xml version="1.0" encoding="UTF-8"?>
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:pets="http://example.com/pets"
                  xmlns:xsd="http://www.w3.org/2001/XMLSchema"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <soapenv:Header/>
  <soapenv:Body>
    <pets:AddPet soapenv:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
      <name xsi:type="xsd:string">Rex</name>
      <type xsi:type="xsd:string">dog</type>
    </pets:AddPet>
  </soapenv:Body>
</soapenv:Envelope>' \
  http://localhost:8080/pets
```

`type` must be `dog` or `cat`. The new name is appended to the matching in-memory list and is returned by the next `ListDogs` / `ListCats` call.
