# cloud-itonami-isco-8181

Open Occupation Blueprint for **ISCO-08 8181**: Glass and Ceramics Plant Operators.

This repository designs a forkable OSS business for a glass and ceramics plant scheduling and logistics coordination practice: a plant scheduling and supply-coordination robot manages crew/task records under a governor-gated actor, so a glass and ceramics plant-operator crew keeps its own operating records instead of renting a closed workforce-management SaaS.

**Maturity: `:implemented`.** `src/glassceramics/` implements the
`GlassCeramicsPlantCoordActor` as a `langgraph.graph/state-graph`
(`glassceramics.actor`) wired to a `Glass and Ceramics Plant
Coordination Advisor` (`glassceramics.advisor`) and an independent
`GlassCeramicsPlantCoordGovernor` (`glassceramics.governor`),
following the itonami actor pattern (ADR-2607121000): `:intake ->
:advise -> :govern -> :decide -+-> :commit (:ok?) +-> :request-approval
(:escalate?, human-in-the-loop interrupt) +-> :hold (:hard?)`. HARD
invariants (always hold, never overridable): operator provenance,
plant provenance, no-actuation (`:effect` must be `:propose`), a
closed op-allowlist (`:log-work-record`, `:schedule-crew-operation`,
`:flag-safety-concern`, `:coordinate-supply-order` — nothing else may
ever be proposed), and a permanent, unconditional block on any
proposal that would directly finalize a plant-operation-execution
decision (e.g. deciding to run or proceed with a specific
glass-melting furnace or ceramics kiln cycle) or a
plant-safety-clearance decision (e.g. declaring the plant floor
safety-cleared), or override a plant safety officer's judgment.
Always-escalate paths (human sign-off regardless of confidence,
mapping this repo's Trust Controls in
[`docs/business-model.md`](docs/business-model.md)):
`:flag-safety-concern` (always) and `:coordinate-supply-order` above
the registered cost threshold.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot performs
the physical domain work**. Here a plant scheduling/logistics coordination robot performs crew scheduling, production-run/materials-usage/progress-record logging and raw-material/refractory-materials supply-order coordination for a glass and ceramics plant-operator crew, under an actor that proposes actions and an independent **Glass and Ceramics Plant Coordination Governor** that gates them. The governor never dispatches hardware itself, never operates glass-melting furnace or ceramics kiln equipment on the plant floor, and never finalizes a plant-operation-execution decision or a plant-safety-clearance decision, or overrides a plant safety officer's judgment; `:high`/`:safety-critical` actions (such as a flagged burn-hazard, furnace-condition or equipment-condition concern, or an above-threshold supply order) require human sign-off. **This actor coordinates PLANT SCHEDULING/LOGISTICS ONLY — it never operates furnace/kiln equipment or makes safety-clearance decisions itself.**

## Core Contract

```text
crew roster + plant registration + safety-reporting policy
        |
        v
Glass and Ceramics Plant Coordination Advisor -> Glass and Ceramics Plant Coordination Governor -> log/schedule/coordinate, or human sign-off
        |
        v
robot actions (gated) + operating records + audit ledger
```

No automated advice can dispatch a robot action the governor refuses, finalize
a plant-operation-execution decision, finalize a plant-safety-clearance
decision, override a plant safety officer's judgment, suppress an operating
record, or disclose sensitive data without governor approval and audit
evidence.

## Capability layer

Resolves via [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation)
(ISCO-08 `8181`). Required capabilities:

- :robotics
- :identity
- :audit-ledger

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## License

AGPL-3.0-or-later.
