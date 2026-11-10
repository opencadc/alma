---
name: ivoa-tap
description: Guides implementation, extension, testing, and documentation of a standards-aligned Java Table Access Protocol (TAP) service per IVOA TAP 1.1 in OpenCADC-style deployments. Use when building or reviewing TAP /sync and /async, ADQL execution, UWS jobs, TAP_SCHEMA, VOTable results, DALI parameters, or Gradle integration tests; use with the ivoa-vosi skill for /capabilities, /availability, and /tables.
---

# IVOA TAP (Java)

Primary normative spec: [TAP 1.1 REC (2019-09-27)](https://www.ivoa.net/documents/TAP/20190927/REC-TAP-1.1.html).

**Shared VOSI:** `/capabilities`, `/availability`, and `/tables` follow [VOSI 1.1](../ivoa-vosi/SKILL.md). Read that skill (and its [opencadc-java.md](../ivoa-vosi/opencadc-java.md)) before changing servlet wiring, `capabilities.xml`, or `Vosi*Test` classes.

Before changing TAP protocol behaviour, read the cited TAP section (and the delegated standard named there). Do not invent parameters, URLs, MIME types, or response shapes.

## Quick start

1. Read [requirements-conformance.md](requirements-conformance.md) for MUST/SHOULD/MAY and section refs.
2. For HTTP surfaces, use [endpoints.md](endpoints.md).
3. For ADQL, UWS, DALI, VOTable boundaries, use [related-standards.md](related-standards.md).
4. For VOSI endpoints and CADC servlets, use [ivoa-vosi](../ivoa-vosi/SKILL.md).
5. For tests, use [testing.md](testing.md); run Gradle `intTest` in the `tap` module.

## Areas of responsibility

| Area | Guidance |
|------|----------|
| Protocol endpoints | `endpoints.md` — `/sync`, `/async`; VOSI siblings per DALI (see `ivoa-vosi`) |
| Query processing | ADQL mandatory (`LANG=ADQL`, `QUERY`); validate at execution time for async |
| Async execution | UWS job lifecycle on `/async`; result at `.../results/result` |
| Metadata | `TAP_SCHEMA` tables required; `/tables` (VOSI) recommended — [ivoa-vosi/endpoints.md](../ivoa-vosi/endpoints.md) |
| Results & errors | VOTable default; DALI overflow/error rules; TAP §3 |
| Documentation | Service README + optional `/examples` (DALI vocabulary) |
| Testing | [testing.md](testing.md); VOSI via [ivoa-vosi/testing.md](../ivoa-vosi/testing.md) |

## Implementation principles

- Distinguish mandatory requirements from recommended and optional functionality.
- Do not claim standards compliance without supporting implementation and tests.
- Consult the relevant specification sections before implementing protocol behaviour.
- Do not invent endpoint behaviour, parameters, or response formats.
- Follow the existing Java project architecture and conventions.
- Prefer minimal, maintainable implementations over unnecessary abstractions.
- Keep protocol-specific behaviour separate from application and database concerns where appropriate.
- Document implementation limitations and unsupported optional capabilities.

## Suggested Java module boundaries

Match this monorepo (`tap/` Gradle WAR + shared CADC libraries). Keep TAP protocol code separate from ALMA-specific SQL/plugins:

```
tap/src/main/java/org/opencadc/tap/
  ws/                 # TAPWebService, QueryJobManager, QueryRunnerImpl
  impl/               # AdqlQueryImpl, uploads, MAXREC, result store
  schema/             # TAP_SCHEMA DAO / metadata
  parser/             # ADQL converters (dialect-specific)

tap/src/main/webapp/
  capabilities.xml    # TAP + VOSI capabilities (ivoa-vosi)
  WEB-INF/web.xml     # servlets: sync/async, VOSI, UWS

tap/src/main/resources/sql/
  *_tap_schema_*.sql  # TAP_SCHEMA content

alma-lib/             # shared logging, UWS helpers as needed
```

Reuse **cadc-tap**, **cadc-uws**, and **cadc-vosi** components where possible instead of reimplementing UWS or VOSI.

## Conformance checklist (minimal TAP 1.1)

Map each item to `src/test` or `src/intTest` tests:

- [ ] `/sync` and `/async` under the TAP capability `accessURL` (use="base")
- [ ] VOSI: [ivoa-vosi](../ivoa-vosi/SKILL.md) conformance gate for capabilities, availability, tables
- [ ] Exactly one capability `standardID="ivo://ivoa.net/std/TAP"` with `interface@version="1.1"`
- [ ] ADQL via `LANG` + `QUERY`; unknown language → TAP error (§3.3)
- [ ] Default and mandatory VOTable output; `FORMAT` ≡ `RESPONSEFORMAT` (not both)
- [ ] `TAP_SCHEMA.schemas`, `.tables`, `.columns`, `.keys`, `.key_columns` queryable
- [ ] Async: UWS phases, parameters accumulated until `PHASE=RUN`, errors at `.../error`
- [ ] Successful async ADQL result named `result`
- [ ] Document unsupported optional features (upload, CSV/TSV, `/tables`, `/examples`, extra LANG)

## Workflow patterns

**Sync ADQL**

```
POST /sync
LANG=ADQL&QUERY=SELECT ...
→ VOTable body (or 303 → result URL per DALI)
```

**Async ADQL**

```
POST /async → 303 Location: .../async/{id}
POST .../async/{id}/parameters (optional, may accumulate)
POST .../async/{id}/phase  PHASE=RUN
GET  .../async/{id}/phase (poll or UWS 1.1 ?WAIT=)
GET  .../async/{id}/results/result
```

Parameter requirements for TAP apply when the job **runs**, not necessarily when the job is created (TAP §2.7 intro).

## Documentation deliverables

When adding or changing behaviour, update:

1. `tap/README.md`: base URL, DB config, async storage, limits
2. Developer notes: ADQL subset, upload limits, optional TAP features not implemented
3. `capabilities.xml` + TAPRegExt where used (production expectation)

## Additional resources

- [requirements-conformance.md](requirements-conformance.md) — TAP requirement matrix
- [endpoints.md](endpoints.md) — TAP paths, parameters, status codes
- [related-standards.md](related-standards.md) — ADQL, UWS, DALI, VOTable
- [testing.md](testing.md) — Gradle unit and integration tests
- [ivoa-vosi](../ivoa-vosi/SKILL.md) — shared capabilities, availability, tables
