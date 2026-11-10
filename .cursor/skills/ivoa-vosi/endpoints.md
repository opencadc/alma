# VOSI endpoints and expected behaviour

Paths shown are conventional in this monorepo (`/capabilities`, `/availability`). VOSI does not mandate path names globally — **registered `accessURL` values in the capabilities document are authoritative** (VOSI §2).

## `/capabilities`

| HTTP | Behaviour |
|------|-----------|
| GET | Return capabilities XML document |
| HEAD | Same resource; include **Last-Modified** (VOSI §3.1) |
| POST, PUT, DELETE | Undefined → **405** if not implemented (VOSI §2) |

**Document structure**

- Namespace: `xmlns:vosi="http://www.ivoa.net/xml/VOSICapabilities/v1.0"`
- Root: `<vosi:capabilities>`
- Children: one `<capability>` per supported standard or custom interface

**Required content patterns**

1. **Service protocol capabilities** — e.g. TAP `standardID="ivo://ivoa.net/std/TAP"` with `ParamHTTP` `accessURL` (`use="base"` per TAP 1.1).
2. **VOSI self-description** (SHOULD, expected in production):

```xml
<capability standardID="ivo://ivoa.net/std/VOSI#capabilities">
  <interface xsi:type="vs:ParamHTTP" role="std">
    <accessURL use="full">https://example.net/srv/capabilities</accessURL>
  </interface>
</capability>
<capability standardID="ivo://ivoa.net/std/VOSI#availability">
  <interface xsi:type="vs:ParamHTTP" role="std">
    <accessURL use="full">https://example.net/srv/availability</accessURL>
  </interface>
</capability>
```

VOSI §4: **no `securityMethod`** on those two interface blocks.

3. **Tables** (when applicable):

```xml
<capability standardID="ivo://ivoa.net/std/VOSI#tables-1.1">
  <interface xsi:type="vs:ParamHTTP" role="std" version="1.1">
    <accessURL use="base">https://example.net/srv/tables</accessURL>
  </interface>
</capability>
```

Use `use="full"` vs `use="base"` per the relevant service standard (TAP uses base for data endpoints; VOSI entries use **full**).

**Extension capabilities**

- Set `xsi:type` to the protocol extension type (e.g. `sia:SimpleImageAccess`).
- Populate protocol-specific elements defined in that extension schema.

See VOSI §5.1 for a full SIA example including VOSI self-capabilities.

---

## `/availability`

| HTTP | Behaviour |
|------|-----------|
| GET | Availability XML |
| HEAD | Allowed if supported |
| POST, PUT, DELETE | **405** if unhandled |

**Document structure** (VOSI §5.5):

```xml
<vosi:availability xmlns:vosi="http://www.ivoa.net/xml/VOSIAvailability/v1.0">
  <vosi:available>true</vosi:available>
  <vosi:note>optional human-readable status</vosi:note>
</vosi:availability>
```

| Element | Required | Semantics |
|---------|----------|-----------|
| `available` | MUST | `true` if accepting requests; `false` if up but not usable or dependencies down |
| `upSince` | Optional | xs:dateTime |
| `downAt` | Optional | Scheduled downtime start |
| `backAt` | Optional | Scheduled return |
| `note` | Optional | Repeatable free text |

**Implementation note:** Implement via a health plugin that probes DB, storage, or internal queues — not a static `true` (VOSI §3.2).

**Auth:** Anonymous only (VOSI §2, §4).

---

## `/tables` (VOSI-tables, when applicable)

Not every service exposes tables. When it does (mandatory for TAP clients in practice):

| Resource | GET behaviour |
|----------|----------------|
| `/tables` | `tableset` document (`VODataService` `TableSet`) |
| `/tables?detail=min` | Suggest minimum detail (table names, optional descriptions) |
| `/tables?detail=max` | Suggest full column/FK metadata |
| `/tables/{qualifiedTableName}` | Single `table` document, full detail |

Rules (VOSI §3.3):

- Child path segment **must** match the `name` of the corresponding `Table` in the tableset.
- Service may always return `min` detail at the tableset level for scale.
- `version="1.1"` on `tableset` / `table` where using VOSI 1.1 features.

If a service standard requires tables but none is deployed, follow that standard (e.g. TAP: **404** on `/tables`).

---

## DALI `/examples` (not VOSI)

The **examples** endpoint is defined by **DALI**, not VOSI. Services that implement it (e.g. TAP) should:

- Register `ivo://ivoa.net/std/DALI#examples` in the **capabilities** document.
- Return HTML/XHTML per DALI/TAP.

When working on examples, use the service-specific skill (e.g. `ivoa-tap`) — keep only the capability advertisement in VOSI scope here.

---

## URL consistency checklist

- [ ] `accessURL` hosts in `capabilities.xml` match deployment (replace placeholders like `@app_name@` at build/deploy).
- [ ] VOSI `#capabilities` and `#availability` URLs match servlet mappings.
- [ ] Registry record (if any) agrees with live GET responses (harvest use case, App. D).
