---
name: ivoa-vosi
description: Guides implementation, extension, testing, and documentation of shared IVOA Virtual Observatory Support Interface (VOSI) endpoints for Java VO services per VOSI 1.1. Use when building or reviewing /capabilities, /availability, VOSI-tables, capabilities.xml, registry harvesting, AvailabilityPlugin health checks, or VosiCapabilitiesTest/VosiAvailabilityTest integration tests across TAP, SIA, SODA, and other DAL services.
---

# IVOA VOSI (shared, Java)

Primary normative spec: [VOSI 1.1 REC (2017-05-24)](https://www.ivoa.net/documents/VOSI/20170524/REC-VOSI-1.1.html).

VOSI is **shared infrastructure** across concrete service standards (TAP, SIA, SODA, Datalink, etc.). Implement the support interface once per deployable; wire service-specific capabilities into the same `capabilities` document.

Before changing protocol behaviour, read the cited VOSI section (and VOResource / VODataService where capability XML is defined). Do not invent URLs, parameters, or document shapes.

## Quick start

1. Read [requirements-conformance.md](requirements-conformance.md) for MUST/SHOULD/MAY with § refs.
2. For HTTP binding and XML shapes, use [endpoints.md](endpoints.md).
3. For VOResource, VODataService, DALI cross-refs, see [related-standards.md](related-standards.md).
4. For this monorepo’s servlet and test patterns, see [opencadc-java.md](opencadc-java.md).
5. For tests, see [testing.md](testing.md).

## Areas of responsibility

| Area | Guidance |
|------|----------|
| Protocol endpoints | `/capabilities`, `/availability` (all services); `/tables` when tabular (see [endpoints.md](endpoints.md)) |
| Recommended metadata | Self-describing VOSI capabilities inside the capabilities document (§4); TAP tables via VOSI 1.1 |
| Examples | Not defined by VOSI — implement per **DALI** on services that define `/examples` (e.g. TAP); document separately |
| Results & errors | Valid XML per appendix schemas; GET/HEAD only; other methods → **405** if no handler (§2) |
| Documentation | Operator docs: endpoint URLs, harvest behaviour, availability semantics |
| Testing | [testing.md](testing.md); extend `CapabilitiesTest` / `AvailabilityTest` |

## Implementation principles

- Distinguish mandatory requirements from recommended and optional functionality.
- Do not claim standards compliance without supporting implementation and tests.
- Consult the relevant specification sections before implementing protocol behaviour.
- Do not invent endpoint behaviour, parameters, or response formats.
- Follow the existing Java project architecture and conventions.
- Keep protocol-specific behaviour separate from application and database concerns where appropriate.
- Document implementation limitations and unsupported optional capabilities.

## Conformance gate (all VO services)

- [ ] Anonymous `GET /capabilities` → valid `VOSICapabilities` root, lists service + VOSI self-capabilities
- [ ] Anonymous `GET /availability` → `available` element present; reflects real dependency health (§3.2)
- [ ] VOSI capability entries in capabilities doc: `ivo://ivoa.net/std/VOSI#capabilities` and `#availability`, `accessURL use="full"`, **no** `securityMethod` on those interfaces (VOSI §4)
- [ ] `HEAD /capabilities` supports `Last-Modified` for harvesters (§3.1)
- [ ] `POST|PUT|DELETE` on support URLs: **405** or explicit handler — document choice
- [ ] Service-specific capabilities use correct `standardID`, `xsi:type` extensions, `ParamHTTP` where appropriate
- [ ] If `/tables` provided: `tables-1.1` registration, `detail` parameter, per-table child resources (§3.3)
- [ ] Integration tests subclass `ca.nrc.cadc.vosi.CapabilitiesTest` and/or `AvailabilityTest`

## Relationship to service-specific skills

| Service skill | VOSI owned here | Delegated to service standard |
|---------------|-----------------|-------------------------------|
| `ivoa-tap` | Embed VOSI caps in TAP capabilities doc; availability anonymous | `/sync`, `/async`, TAP capability `version="1.1"` |
| SIA / SODA / others | Same VOSI endpoints pattern | Protocol capability extensions in `capabilities.xml` |

## Additional resources

- [requirements-conformance.md](requirements-conformance.md)
- [endpoints.md](endpoints.md)
- [related-standards.md](related-standards.md)
- [opencadc-java.md](opencadc-java.md)
- [testing.md](testing.md)
