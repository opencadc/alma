# TAP testing guidance (Java / OpenCADC)

Tests live in the `tap` Gradle module. Prefer mapping cases to TAP requirement IDs in [requirements-conformance.md](requirements-conformance.md).

| Source set | Location | Role |
|------------|----------|------|
| Unit | `tap/src/test/java` | ADQL parsing, validators, converters |
| Integration | `tap/src/intTest/java/org/opencadc/tap/integration` | Live service: sync, async, VOSI, ObsCore |

Run from `tap/`: `gradle clean build test` and integration tests per `int_test.gradle` / `runIntegrationTests.sh` when containers are up.

## VOSI (delegate to ivoa-vosi)

Do **not** duplicate VOSI test guidance here. Use [ivoa-vosi/testing.md](../ivoa-vosi/testing.md) and extend:

| Class | Extends |
|-------|---------|
| `VosiCapabilitiesTest` | `ca.nrc.cadc.vosi.CapabilitiesTest` — TAP + `VOSI_TABLES_11` in `validateContent` |
| `VosiAvailabilityTest` | `ca.nrc.cadc.vosi.AvailabilityTest` |
| `VosiTablesTest` | VOSI 1.1 tables (`detail`, child table URLs) |

## TAP sync / async integration

Property-file driven tests under `tap/src/intTest/resources/` and `tap/src/test/resources/` (e.g. `SyncResultTest-*.properties`, `SyncErrorTest-*.properties`, `AsyncResultTest-*.properties`).

**Patterns in this repo:**

- `ObsCoreTapSyncQueryTest`, `ObsCoreTapSyncErrorTest` — sync ADQL against `ivoa.obscore`
- Similar classes for `tap_schema.*` metadata queries
- Error cases: missing `LANG`/`QUERY`, bad ADQL, DML attempts, bad `LANG`

| Case | Assert |
|------|--------|
| Valid ADQL | 200 or 303→result; VOTable parseable; columns match SELECT |
| Missing LANG / QUERY | TAP error document |
| Bad LANG | Unknown language error |
| Invalid ADQL / DML | Query failure per §3.3 |
| MAXREC truncation | Row count ≤ MAXREC; overflow INFO per DALI |
| MAXREC=0 | Metadata-focused behaviour per §2.7.4 |
| GET vs POST on `/sync` | POST for non-cached/volatile data (TAP §2.1) |

**Async**

1. `POST /async` → job URL (303).
2. `PHASE=RUN` — parameter validation at run time.
3. Terminal phase → `.../results/result` or `.../error`.
4. Unknown job → 404; `ABORT` → `ABORTED` + error resource.
5. Parameter accumulation on `.../parameters` before RUN.

## TAP_SCHEMA

Sync queries against `tap_schema.schemas`, `.tables`, `.columns`, `.keys`, `.key_columns`:

- Column presence per TAP §4.
- Sample FK relationships per §4.4.
- `"size"` only with delimited identifier in ADQL.

SQL fixtures: `tap/src/main/resources/sql/alma_tap_schema_content.sql`.

## VOTable and formats

- Default VOTable when `RESPONSEFORMAT` omitted.
- `FORMAT` vs `RESPONSEFORMAT` equivalence; both set is undefined — assert documented behaviour.
- CSV/TSV if enabled: MIME types per TAP §3.2.

XSD under `tap/src/intTest/resources/` (VOTable 1.2/1.3) for parse validation where used.

## UPLOAD (if enabled)

`UploadTest-TAP-*.xml`, `TAPUploadTest-*.xml`; `ALMAUploadManager` and upload DB pool in README.

- `TAP_UPLOAD.*` in queries.
- Oversize → §3.3 error.
- Upload tables not listed in TAP_SCHEMA.

## Unit tests

Examples: `AdqlQueryImplTest`, `OracleObsCoreSelectListConverterTest`, `MaxRecValidatorImpl` coverage in `src/test`.

Keep pure protocol logic (parameter validation, VOTable building) testable without a running Tomcat.

## Compliance report

Map `TAP-*` IDs from [requirements-conformance.md](requirements-conformance.md) to Java test class names (and property files where applicable). VOSI rows map via `VOSI-*` in ivoa-vosi.
