# OpenCADC / ALMA Java conventions for VOSI

Patterns used across modules in this repository (`tap`, `sia`, `soda`, `datalink`, `data`, `cone`, `reg`). Prefer reusing **cadc-vosi** components rather than bespoke servlets.

## Capabilities

**Static template:** `src/main/webapp/capabilities.xml`

- Lists VOSI self-capabilities + service `standardID` blocks.
- Replace deployment placeholders (`https://replace.me.com/@app_name@/...`) at build or container config time.

**Servlet wiring** (`web.xml` pattern):

| Setting | Typical value |
|---------|----------------|
| Servlet | `ca.nrc.cadc.rest.RestServlet` |
| `init` | `ca.nrc.cadc.vosi.CapInitAction` |
| `get` | `ca.nrc.cadc.vosi.CapGetAction` |
| `head` | `ca.nrc.cadc.vosi.CapHeadAction` |
| `input` | `/capabilities.xml` |
| `authHeaders` | `false` |
| Mapping | `/capabilities` |

Example: `data/src/main/webapp/WEB-INF/web.xml` (CapabilitiesServlet block).

**Customization:** Subclass integration `VosiCapabilitiesTest` and override `validateContent(Capabilities caps)` to assert service-specific capabilities (e.g. TAP `Standards.TAP_10`, `Standards.VOSI_TABLES_11` in `tap/.../VosiCapabilitiesTest.java`).

## Availability

| Setting | Typical value |
|---------|----------------|
| Servlet | `ca.nrc.cadc.vosi.AvailabilityServlet` |
| `ca.nrc.cadc.vosi.AvailabilityPlugin` | Service-specific class (e.g. `org.opencadc.alma.data.fits.ServiceAvailability`) |
| Mapping | `/availability` |

Plugin implements dependency checks; servlet emits VOSIAvailability XML.

## Tables (TAP and other tabular services)

Implemented in shared TAP/CADC libraries for TAP modules; integration test `VosiTablesTest` in `tap/src/intTest/`. For new tabular services, align with VOSI 1.1 `detail` and child table URLs before duplicating logic.

## Documentation in repo

- `data/README.rst` — notes standard VOSI endpoints.
- Per-module README — document public base URL and registry `ivo` identifier used in tests.

## Checklist when adding a new WAR module

1. Add `capabilities.xml` with VOSI `#capabilities` and `#availability` (`use="full"`, no `securityMethod` on those two).
2. Register service protocol capability(ies) with correct `standardID`.
3. Wire `CapabilitiesServlet` and `AvailabilityServlet` in `web.xml`.
4. Implement `AvailabilityPlugin` with real health checks.
5. Add `VosiCapabilitiesTest` / `VosiAvailabilityTest` with module `ivo://` resource id.
6. Document endpoints in module README.
