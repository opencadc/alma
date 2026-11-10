# TAP endpoints and expected behaviour

Base URL: value of TAP capability `accessURL` with `use="base"` in VOSI-capabilities (TAP §2.4). All paths below are relative to that base unless noted.

## Resource summary

| Path | TAP type | Required | If unsupported |
|------|----------|----------|----------------|
| `/sync` | TAP-sync | MUST | — |
| `/async` | TAP-async (UWS list) | MUST | — |
| `/capabilities` | VOSI-capabilities | MUST | — |
| availability | VOSI-availability | MUST | Per DALI (sibling; anonymous) |
| `/tables` | VOSI-tables | SHOULD | **404** |
| `/examples` | DALI-examples | SHOULD | **404** |
| `/` (root) | — | — | HTML MAY; not normative |

DALI: all resources except availability are siblings of `/capabilities` (TAP §2).

---

## `/sync` (TAP-sync)

**Spec:** TAP §2.1; query parameters TAP §2.7; DALI-sync rules.

| Method | Use |
|--------|-----|
| GET | Allowed; may return cached representation — volatile data clients should POST |
| POST | Primary query submission |

**Parameters (form body or query string per DALI):**

| Parameter | Required | Notes |
|-----------|----------|-------|
| `LANG` | MUST at execution | `ADQL` or `ADQL-2.0` style version suffix |
| `QUERY` | MUST for ADQL | ADQL text |
| `RESPONSEFORMAT` / `FORMAT` | Optional | Default `votable`; MUST NOT send both |
| `MAXREC` | Optional | Truncation + overflow (§3.4) |
| `RUNID` | Optional | DALI |
| `UPLOAD` | Optional | If supported; repeatable, accumulating |

**Success:**

- Response body contains result **or** HTTP **303** `Location` to result resource (DALI POST-redirect-GET; TAP §5.2).
- ADQL: single VOTable (or chosen format).

**Errors:** TAP §3.3 — error document; sync may use 4xx/5xx or 200 with error VOTable depending on case.

---

## `/async` (TAP-async)

**Spec:** TAP §2.2; UWS ([REC-UWS-1.1](http://www.ivoa.net/documents/UWS/)); DALI-async.

### Job list `POST /async`

Creates a job. Same TAP parameters as sync may be sent on create or later via `.../parameters`.

- Typical response: **303** with `Location: .../async/{job-id}` (TAP §5.1).
- TAP parameter validation when job runs (`PHASE=RUN`), not necessarily on create.

### Job `GET /async/{id}`

UWS job representation (XML). UWS 1.1+: optional `WAIT` seconds for blocking state change (TAP §5.1.3).

### Parameters `POST /async/{id}/parameters`

- Parameter values **accumulate** (multi-valued); no replace/remove in PENDING (TAP §5.1.2).
- Response often **303** back to job URL.

### Phase `GET|POST /async/{id}/phase`

| POST body | Effect |
|-----------|--------|
| `PHASE=RUN` | Start execution; TAP checks LANG/QUERY/etc. here |
| `PHASE=ABORT` | Abort running job |

Poll `GET .../phase` (UWS 1.0) or use blocking job GET (UWS 1.1+).

### Results `GET /async/{id}/results/result`

- **MUST** be named `result` for ADQL (TAP §2.2).
- May be direct body or **303** to stored result.
- Empty query: resource exists, zero rows.

### Error `GET /async/{id}/error`

- Failed or aborted jobs: error document (TAP §3.3).
- HTTP status **200** when retrieving async error resource (TAP §3.3).

### Other UWS children

`quote`, `executionduration`, `destruction`, `results` (list), `owner` — semantics per UWS; TAP §5.1 cites negotiable `EXECUTIONDURATION`, `DESTRUCTION`.

**HTTP 404:** Unknown job id on `/async/{id}` (TAP §3.3).

---

## `/capabilities` (VOSI-capabilities)

**Spec:** TAP §2.4 (TAP capability content); HTTP/XML binding per **VOSI** — see [ivoa-vosi/endpoints.md](../ivoa-vosi/endpoints.md) and [opencadc-java.md](../ivoa-vosi/opencadc-java.md).

| Requirement | Detail |
|-------------|--------|
| Access | Anonymous MUST |
| TAP capability | `ivo://ivoa.net/std/TAP`, interface `version="1.1"`, `ParamHTTP`, base `accessURL` |
| Count | Exactly one TAP capability |
| VOSI self-desc | SHOULD include capabilities, availability, tables interfaces in same document until DALI provides alternatives (TAP §2.4) |

Clients build: `{base}/sync`, `{base}/async`, `{base}/tables`, etc.

---

## VOSI-availability

**Spec:** TAP §2.3 cites DALI; document shape and anonymous GET per [ivoa-vosi/endpoints.md](../ivoa-vosi/endpoints.md).

- MUST allow anonymous access.
- Endpoint URL declared in capabilities (not fixed by path name in TAP 1.1).

---

## `/tables` (VOSI-tables)

**Spec:** TAP §2.5 (content vs TAP_SCHEMA); VOSI 1.1 binding in [ivoa-vosi/endpoints.md](../ivoa-vosi/endpoints.md) (`detail`, child table resources).

| State | Behaviour |
|-------|-----------|
| Implemented | Metadata equivalent to TAP_SCHEMA; VOTableType recommended over TAPType in output |
| Not implemented | **404** (required) |

Auth: may mirror TAP (anonymous and/or authenticated per capability).

---

## `/examples` (DALI-examples)

**Spec:** TAP §2.6; DALI examples vocabulary.

| State | Behaviour |
|-------|-----------|
| Present | GET returns `application/xhtml+xml` or `text/html` |
| Absent | **404** (required) |

Each `typeof="example"` block: MUST have `property="query"`; MAY have `property="table"` text children. Do not wrap table names in `<a>` for RDFa (TAP §2.6).

---

## Authentication

TAP §2.4: single base URL; same `/sync` and `/async` for anonymous and authenticated methods declared via `securityMethod` on the TAP interface. Availability and capabilities remain anonymous. Implement 401/403 per TAP §3.3 where appropriate.

---

## Reference exchange (async)

See TAP §5.1 for normative message sequences: create → (optional parameter POSTs) → `PHASE=RUN` → poll → `results/result` or `error`.
