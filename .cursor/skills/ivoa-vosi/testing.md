# VOSI testing guidance (Java / OpenCADC)

## Base classes (cadc-vosi)

| Test base | Package | Purpose |
|-----------|---------|---------|
| `CapabilitiesTest` | `ca.nrc.cadc.vosi` | GET capabilities, schema/content validation |
| `AvailabilityTest` | `ca.nrc.cadc.vosi` | GET availability, `available` element |

Each deployable module provides:

```java
public class VosiCapabilitiesTest extends CapabilitiesTest {
    public VosiCapabilitiesTest() {
        super(URI.create("ivo://almascience.org/tap")); // module-specific
    }
    @Override
    protected void validateContent(Capabilities caps) throws Exception {
        super.validateContent(caps);
        // assert service standardID, interfaces, security methods
    }
}
```

Existing examples: `tap/`, `sia/`, `soda/`, `datalink/`, `reg/` under `src/intTest/java/.../Vosi*Test.java`.

## Requirement → test mapping

| ID | Test ideas |
|----|------------|
| VOSI-REST-02 | `GET` capabilities and availability return **200** |
| VOSI-REST-03 | Unauthenticated client (no credentials in test harness) |
| VOSI-REST-04 | `POST /capabilities` → **405** (if default servlet) |
| VOSI-CAP-01–03 | Base `CapabilitiesTest` + XSD / parser validation |
| VOSI-CAP-05 | `findCapability(VOSI_CAPABILITIES)` and `VOSI_AVAILABILITY` present |
| VOSI-CAP-06 | `HEAD /capabilities` → `Last-Modified` non-null after change |
| VOSI-AVL-03 | Parse XML; assert `available` is valid boolean |
| VOSI-AVL-05 | Integration test with DB down → `available=false` (environment test) |
| VOSI-REG-04–06 | Assert VOSI interfaces use `use="full"`; no security methods on VOSI entries |
| VOSI-TBL-* | `VosiTablesTest` (TAP); `detail=min|max`; child table GET |

## Capabilities content assertions

For each service module, extend `validateContent` to verify:

- Primary `standardID` (TAP, SIA, SODA, …) exists.
- Expected `ParamHTTP` (or `WebBrowser`) interfaces and `accessURL` patterns.
- Auth: data interfaces may list SSO methods; VOSI self-capabilities must not require auth (§4).
- TAP: `interface@version="1.1"` on TAP capability; `VOSI_TABLES_11` when `/tables` deployed.

## Availability assertions

- `available=true` in healthy CI environment.
- Optional: unit-test `AvailabilityPlugin` with mocked dependency failures.
- If scheduling metadata used: validate `downAt`/`backAt` parse as `xs:dateTime`.

## Tables (when present)

- GET `/tables` returns valid `tableset`.
- GET `/tables?detail=min` vs `max` — document actual behaviour; assert schema validity.
- GET `/tables/{name}` matches a table listed in tableset; column count > 0 for max detail fixtures.

## Interoperability

- Validate against VOSICapabilities / VOSIAvailability XSD (resources under `tap/src/intTest/resources/` include VOTable XSD; add VOSI schemas if not already on classpath).
- Compare harvested XML with registry record when publishing (manual or reg intTest).

## Compliance artefact

Maintain a table linking `VOSI-*` requirement IDs to test class/method names in module README or `intTest` package doc. Update when claiming VOSI 1.1 compliance for a new service.

## What VOSI tests do **not** cover

- Service protocol query execution (TAP sync, SIA search, etc.) — use service-specific skills/tests.
- DALI `/examples` HTML — separate TAP/DALI tests.
- Overflow / VOTable errors — not applicable to VOSI XML endpoints.
