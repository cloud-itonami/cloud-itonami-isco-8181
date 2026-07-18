# Security Policy

This project handles glass and ceramics plant operator plant operating
workflows. Treat vulnerabilities as potentially high impact even when
the demo data is synthetic — this domain's failure modes include
physical worker-safety risk from glass-melting furnace or ceramics
kiln burn exposure and heavy-machinery hazard from forming/handling
equipment.

## Do Not Disclose Publicly

Report privately before opening public issues for:

- credential exposure
- real operator, plant or operator data exposure
- authorization bypass
- Glass and Ceramics Plant Coordination Governor bypass
- audit-ledger tampering
- over-disclosure in reports or exports
- unsafe robot action dispatch
- any path that lets a proposal reach a plant-operation-execution
  decision, a plant-safety-clearance decision, or a
  plant-safety-officer-override decision

## Reporting

Use GitHub private vulnerability reporting when available for the repository.
If that is unavailable, contact the repository maintainers through the
cloud-itonami organization before publishing details.

Include:

- affected commit or version
- reproduction steps
- expected and actual behavior
- impact on operator/plant data, policy enforcement or audit logging
- suggested fix, if known

## Production Guidance

- Store secrets outside Git.
- Keep real operator/plant/operator data outside this repository.
- Run policy tests before deployment.
- Export and review audit logs regularly.
- Use least privilege for operators and service accounts.
