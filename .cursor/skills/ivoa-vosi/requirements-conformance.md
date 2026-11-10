# VOSI 1.1 requirements and conformance

Normative keywords follow [RFC 2119](https://www.ietf.org/rfc/rfc2119.txt). **Source** is VOSI 1.1 unless noted.

## REST binding (all VOSI functions)

| ID | Level | Requirement | VOSI § |
|----|-------|-------------|--------|
| VOSI-REST-01 | MUST | REST binding required even when the data protocol is not RESTful | §2 |
| VOSI-REST-02 | MUST | Support interfaces accessible via HTTP **GET** | §2 |
| VOSI-REST-03 | MUST | Capabilities and availability bindings accessible to **anonymous** requests | §2 |
| VOSI-REST-04 | — | POST/PUT/DELETE undefined; default **405 Method Not Allowed** if unhandled | §2 |
| VOSI-REST-05 | — | Clients discover URLs from registry/capabilities; must not assume URL patterns except where a service standard constrains them | §2 |

## Capabilities metadata

| ID | Level | Requirement | VOSI § |
|----|-------|-------------|--------|
| VOSI-CAP-01 | MUST | All VO services **shall** provide the capabilities interface | §3.1 |
| VOSI-CAP-02 | MUST | Root element `capabilities` in namespace `http://www.ivoa.net/xml/VOSICapabilities/v1.0` | §3.1, App. A |
| VOSI-CAP-03 | MUST | One or more child `capability` elements (VOResource `Capability` type) | §3.1 |
| VOSI-CAP-04 | MUST | Protocol capabilities use `standardID`; extensions use `xsi:type` on `capability` | §3.1 |
| VOSI-CAP-05 | SHOULD | Include self-describing VOSI capabilities per §4 in the document | §3.1, §4 |
| VOSI-CAP-06 | MUST | `Last-Modified` on GET/HEAD for change tracking | §3.1 |
| VOSI-CAP-07 | SHOULD | Static template + runtime merge, or fully dynamic generation consistent with deployment URLs | §3.1, App. D |

## Availability metadata

| ID | Level | Requirement | VOSI § |
|----|-------|-------------|--------|
| VOSI-AVL-01 | MUST | All VO services **shall** provide the availability interface | §3.2 |
| VOSI-AVL-02 | MUST | Root `availability` per `http://www.ivoa.net/xml/Availability/v1.0` (VOSIAvailability schema) | §3.2, App. B, §5.5 |
| VOSI-AVL-03 | MUST | Child `available` (boolean) | §3.2 |
| VOSI-AVL-04 | MAY | `upSince`, `downAt`, `backAt`, `note` (multiple `note` allowed) | §3.2 |
| VOSI-AVL-05 | SHOULD | Health check dependencies (DB, queues), not only web container up | §3.2 |
| VOSI-AVL-06 | SHOULD | `available=false` when draining queue / not accepting work | §3.2 |

## Table metadata (VOSI-tables)

| ID | Level | Requirement | VOSI § |
|----|-------|-------------|--------|
| VOSI-TBL-01 | SHOULD | Tabular services expose a tables VOSI endpoint | §3.3 |
| VOSI-TBL-02 | MUST | TableSet root type `VODataService` `TableSet`; single-table root type `Table` | §3.3, App. C |
| VOSI-TBL-03 | SHOULD | `detail=min|max` query parameter as suggestion; may return either level | §3.3 |
| VOSI-TBL-04 | MUST | Child resource per table: `GET .../tables/{name}` matches `Table` name | §3.3 |
| VOSI-TBL-05 | — | Register `ivo://ivoa.net/std/VOSI#tables-1.1` for VOSI 1.1 tables binding | §4, App. E.1 |

## Registration (in capabilities document & registry)

| ID | Level | Requirement | VOSI § |
|----|-------|-------------|--------|
| VOSI-REG-01 | MUST | Availability: `standardID="ivo://ivoa.net/std/VOSI#availability"` | §4 |
| VOSI-REG-02 | MUST | Capabilities endpoint: if declared, `standardID` **must** be `ivo://ivoa.net/std/VOSI#capabilities` | §4 |
| VOSI-REG-03 | SHOULD | Tables: `ivo://ivoa.net/std/VOSI#tables` or `#tables-1.1` | §4 |
| VOSI-REG-04 | MUST | REST: `accessURL@use="full"` on VOSI interface entries | §4 |
| VOSI-REG-05 | SHOULD | REST: `interface@xsi:type="vs:ParamHTTP"` (VODataService 1.1) | §4, §5.1 |
| VOSI-REG-06 | MUST | VOSI capabilities and availability interface entries: **no** `securityMethod` | §4 |

## Errors and HTTP semantics

VOSI does not define TAP-style overflow or VOTable error documents. For support interfaces:

| Situation | Expected behaviour |
|-----------|-------------------|
| Success | **200** + XML entity body |
| Unsupported write verb | **405** (recommended per §2) |
| Auth on capabilities/availability GET | Must not be required (§2, §4) — misconfiguration if **401/403** for anonymous |
| Invalid table name (child resource) | Service-appropriate **404** (consistent with deployment) |
| Malformed `detail` | Ignore or treat as no param per §3.3 (no normative error XML) |

Document any service-specific extensions (caching headers, compression) without contradicting §3.1 `Last-Modified` expectations.

## VOSI 1.1 deltas (vs 1.0)

| Feature | VOSI 1.1 | § / App. |
|---------|----------|----------|
| `tables-1.1` standardID | New | §4, E.1 |
| `detail` param + per-table child URLs | New | §3.3, E.1 |
| `table` root element | Alternate root in VOSITables schema | E.1 |
| SOAP | Removed | E.1 |

## Compliance statements

Claim **VOSI 1.1** only when:

1. Capabilities and availability **MUST** rows are implemented and tested.
2. Table metadata rows are satisfied if the service is tabular (TAP, etc.) or explicitly documented as N/A.
3. The capabilities document’s VOSI self-entries match live endpoint URLs and §4 security rules.

List unsupported optional features (e.g. no `/tables`, no scheduled `downAt`/`backAt`).
