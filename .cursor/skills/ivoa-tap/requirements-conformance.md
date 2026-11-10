# TAP 1.1 requirements and conformance

Normative keywords follow [RFC 2119](https://www.ietf.org/rfc/rfc2119.txt) as used in TAP. **Source** is TAP 1.1 unless noted.

Legend: **MUST** = mandatory for TAP 1.1 conformance claim; **SHOULD** = recommended; **MAY** = optional.

## Core service

| ID | Level | Requirement | TAP § |
|----|-------|-------------|-------|
| TAP-CORE-01 | MUST | Expose REST resources under a single TAP base URL | §2 |
| TAP-CORE-02 | MUST | Provide `/sync` (TAP-sync) | §2.1, §2 table |
| TAP-CORE-03 | MUST | Provide `/async` (TAP-async, UWS job list) | §2.2, §2 table |
| TAP-CORE-04 | MUST | Provide VOSI-availability (anonymous); location per DALI | §2.3, §2 table |
| TAP-CORE-05 | MUST | Provide `/capabilities` (VOSI-capabilities, anonymous) | §2.4, §2 table |
| TAP-CORE-06 | SHOULD | Provide `/tables` (VOSI-tables) | §2.5, §2 table |
| TAP-CORE-07 | SHOULD | Provide `/examples` (DALI-examples) | §2.6, §2 table |
| TAP-CORE-08 | MUST | Fixed-name resources (`async`, `sync`, `tables`, `examples`) are siblings of `capabilities` except availability rules per DALI | §2 |
| TAP-CORE-09 | MAY | Return human-readable HTML at service root; clients MUST NOT depend on it | §2 |

## Query languages and execution

| ID | Level | Requirement | TAP § |
|----|-------|-------------|-------|
| TAP-QL-01 | MUST | Support ADQL queries | §1.3.1, §1 intro |
| TAP-QL-02 | MUST | Support `LANG` parameter; client MUST supply value; standard value `ADQL` | §2.7.1 |
| TAP-QL-03 | MUST | Support `QUERY` parameter (required because ADQL is required) | §2.7.2 |
| TAP-QL-04 | MAY | Support other query languages; describe via TAPRegExt | §1.3.2, §2.7.1 |
| TAP-QL-05 | MUST | Return query results as VOTable (mandatory output format) | §1 intro, §2.7.3 |
| TAP-QL-06 | SHOULD | Return "unknown query language" error for unsupported LANG/version | §2.7.1, §3.3 |
| TAP-QL-07 | MUST | Enforce TAP parameter rules when query **executes** (async: at RUN, not at job CREATE) | §2.7 intro |
| TAP-QL-08 | MUST | Ignore spurious parameters; MUST NOT error on spurious parameters alone | §2.7 intro |

## DALI parameters (via TAP)

| ID | Level | Requirement | TAP § |
|----|-------|-------------|-------|
| TAP-PARAM-01 | MUST | Accept `RESPONSEFORMAT`; MUST accept `FORMAT` as equivalent (not both) | §2.7.3 |
| TAP-PARAM-02 | MUST | Default output VOTable when FORMAT and RESPONSEFORMAT omitted | §2.7.3 |
| TAP-PARAM-03 | SHOULD | Support CSV and TSV output | §2.7.3 |
| TAP-PARAM-04 | MUST | Implement `MAXREC` per DALI; overflow indicator when truncated (§3.4) | §2.7.4, §3.4 |
| TAP-PARAM-05 | — | `MAXREC=0`: metadata-only semantics; validation/execution optional | §2.7.4 |
| TAP-PARAM-06 | — | `MAXREC` interacts with ADQL `TOP` as specified | §2.7.4 |
| TAP-PARAM-07 | — | `RUNID` per DALI | §2.7.5 |
| TAP-PARAM-08 | SHOULD | Support VOTable upload via `UPLOAD` per DALI | §2.7.6 |
| TAP-PARAM-09 | MUST | If upload supported: tables as `TAP_UPLOAD.<name>`; not in TAP_SCHEMA | §2.7.6 |
| TAP-PARAM-10 | SHOULD | DALI geometry/timestamp serialization when those types used | §2.7.2 |

## VOSI-capabilities

| ID | Level | Requirement | TAP § |
|----|-------|-------------|-------|
| TAP-CAP-01 | MUST | Exactly one capability `standardID="ivo://ivoa.net/std/TAP"` | §2.4 |
| TAP-CAP-02 | MUST | TAP interface `version="1.1"` (missing version treated as 1.0 by clients) | §2.4 |
| TAP-CAP-03 | MUST | TAP `accessURL` use="base"; clients append `/sync`, `/async`, etc. | §2.4, §2 |
| TAP-CAP-04 | SHOULD | TAPRegExt metadata in production registries | §2.4 (TAPRegExt) |

## VOSI-tables and examples

| ID | Level | Requirement | TAP § |
|----|-------|-------------|-------|
| TAP-TBL-01 | SHOULD | `/tables` implements VOSI-tables; equivalent metadata to TAP_SCHEMA | §2.5 |
| TAP-TBL-02 | MUST | If `/tables` not implemented: HTTP 404 | §2.5 |
| TAP-EX-01 | SHOULD | `/examples` returns XHTML/HTML with DALI examples vocabulary | §2.6 |
| TAP-EX-02 | MUST | If no examples: HTTP 404 on `/examples` | §2.6 |
| TAP-EX-03 | MUST | Each example has unique `property="query"` child | §2.6 |

## UWS / async semantics

| ID | Level | Requirement | TAP § |
|----|-------|-------------|-------|
| TAP-UWS-01 | MUST | `/async` conforms to DALI-async / UWS job list | §2.2 |
| TAP-UWS-02 | MUST | Failed query: error document at `.../error` | §2.2 |
| TAP-UWS-03 | MUST | Success: results under `.../results/`; ADQL single result named `result` | §2.2 |
| TAP-UWS-04 | MUST | Empty result: result resource exists, no data rows | §2.2 |
| TAP-UWS-05 | MAY | 303 redirect to result URL (sync or async) | §2.1, §2.2, §5.2 |
| TAP-UWS-06 | — | UWS child resources: phase, quote, executionduration, destruction, error, parameters, results, owner | §2.2, §5.1 |
| TAP-UWS-07 | SHOULD | Support UWS 1.1+ (e.g. blocking `?WAIT=` on job GET) | §1.1, §5.1.3 |

## VOTable results, errors, overflow

| ID | Level | Requirement | TAP § |
|----|-------|-------------|-------|
| TAP-VO-01 | MUST | Errors: error document + appropriate HTTP status (async error resource often 200) | §3.3 |
| TAP-VO-02 | MUST | Overflow: valid partial result, success HTTP/MIME; indicator per DALI | §3.4 |
| TAP-VO-03 | MUST | ADQL result: one table; columns match SELECT order/names | §3.2 |
| TAP-VO-04 | MUST | FIELD metadata from TAP_SCHEMA where applicable (`datatype`, `arraysize`, `xtype`) | §3.2 |
| TAP-VO-05 | MUST | Generate unique names for unnamed select expressions | §3.2 |
| TAP-VO-06 | SHOULD | Minimum VOTable 1.2 features (overflow, `xtype`) | §1.1 deps |

## TAP_SCHEMA metadata

| ID | Level | Requirement | TAP § |
|----|-------|-------------|-------|
| TAP-SCH-01 | MUST | Schema `TAP_SCHEMA` with required tables queryable via TAP | §4 |
| TAP-SCH-02 | MUST | `TAP_SCHEMA.schemas` columns per §4.1 | §4.1 |
| TAP-SCH-03 | MUST | `TAP_SCHEMA.tables` columns per §4.2; `table_type` ∈ {table, view} | §4.2 |
| TAP-SCH-04 | MUST | `TAP_SCHEMA.columns` columns per §4.3 | §4.3 |
| TAP-SCH-05 | MUST | `principal`, `indexed`, `std` are 0 or 1 | §4.3 |
| TAP-SCH-06 | MUST | `"size"` column lowercase delimited identifier; deprecated but populated | §4.3 |
| TAP-SCH-07 | MUST | `TAP_SCHEMA.keys` and `.key_columns` per §4.4 | §4.4 |
| TAP-SCH-08 | SHOULD | Enforce/describe FK relationships listed in §4.4 | §4.4 |
| TAP-SCH-09 | MAY | Extra TAP_SCHEMA tables/columns for local extensions (self-described) | §4 |

## TAP 1.1 vs 1.0 (implementation notes)

| Topic | TAP 1.1 change | § |
|-------|----------------|---|
| REQUEST / VERSION params | Removed | App. A.3 |
| FORMAT | Alias for RESPONSEFORMAT | §2.7.3 |
| TAP_SCHEMA | `arraysize`, `xtype`, `*_index` columns | App. A.5–A.6 |
| VOSI-tables | Explicit `/tables`; capabilities `tables-1.1` | §2.4, App. A.7 |

## Compliance statements

Only assert "TAP 1.1 compliant" when all **MUST** rows above that apply to the deployed feature set are implemented **and** covered by automated tests. List explicitly:

- Optional features not implemented (upload, `/tables`, extra formats, non-ADQL LANG)
- ADQL subset or unsupported ADQL constructs
- UWS version (1.0 polling-only vs 1.1 `WAIT`)
- Auth methods (SSO) if not anonymous-only
