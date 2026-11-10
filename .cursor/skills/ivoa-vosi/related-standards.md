# Related standards (VOSI context)

VOSI **wraps** VOResource capability metadata and VODataService table metadata for live HTTP retrieval.

## VOResource 1.03+

- **Role:** Defines the `Capability` and `Interface` types serialized inside `VOSICapabilities`.
- **Use when:** Adding or extending `<capability>` elements, `standardID` values, `accessURL@use`, `securityMethod`.
- **Doc:** [VOResource](http://www.ivoa.net/xml/VOResource/v1.0) (Plante et al., 2008).

## VODataService 1.1

- **Role:** `ParamHTTP` interface type; `TableSet`, `Table`, `Column`, `ForeignKey` for VOSI-tables.
- **Use when:** Building `/tables` output; TAPRegExt and TAP capabilities often reference the same types.
- **Doc:** [VODataService](http://www.ivoa.net/xml/VODataService/v1.1) (Plante et al., 2010).

## Service-specific capability extensions

| Standard | `standardID` (typical) | Notes |
|----------|----------------------|--------|
| TAP 1.1 | `ivo://ivoa.net/std/TAP` | See `ivoa-tap` skill; embed in same capabilities doc |
| SIA 1.x | `ivo://ivoa.net/std/SIA` | `xsi:type="sia:SimpleImageAccess"` |
| SODA | SODA standardID in service spec | Soda `capabilities.xml` in this repo |
| Datalink | Datalink standardID | Custom capabilities in `datalink` module |

Always use the **service REC** for extension schema elements beyond VOSI’s generic shell.

## DALI 1.1

- **Role:** Not a VOSI endpoint, but often co-located (e.g. TAP `examples`, parameter rules).
- **VOSI intersection:** DALI describes availability placement for some DAL services; TAP 1.1 cites DALI for availability alongside VOSI.

## Registry interfaces

- **Role:** Harvesting VOSI URLs into registry records (non-normative use case VOSI App. D).
- **Doc:** [Registry Interfaces 1.0](http://www.ivoa.net/documents/RegistryInterface/) (Benson et al., 2009).

## Responsibility split

```mermaid
flowchart LR
  subgraph vosi [VOSI layer]
    CAP[/capabilities/]
    AVL[/availability/]
    TBL[/tables/ optional]
  end
  subgraph svc [Service protocol]
    API[sync query / SIA / etc.]
  end
  subgraph dom [Domain]
    DB[(Database)]
    STORE[Storage]
  end

  CAP --> API
  AVL --> DB
  AVL --> STORE
  TBL --> DB
  API --> DB
```

- **VOSI layer:** XML generation, servlet wiring, anonymous access, schema validation.
- **Service protocol:** Standard-specific capability extensions and interfaces.
- **Domain:** Availability plugins probe domain dependencies; table metadata may read DB catalog.

## Reading order for a change

1. VOSI §2 binding + §3 metadata class affected
2. Appendix schema (A, B, or C)
3. VOResource / VODataService for XML details
4. Service standard (TAP, SIA, …) for additional capability requirements
5. This monorepo’s `capabilities.xml` and `web.xml` for local conventions (`opencadc-java.md`)
