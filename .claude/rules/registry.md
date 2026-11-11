---
paths:
  - "encri-registry/**"
  - "registry/**"
  - "FINGERPRINTS.md"
---

# Registry and fingerprints — component brief

The registry of recognized apps that each encri release builds in: the entries in `registry/`, with
`registry/README.md` (the format and what a reviewer checks); `encri-registry`, the pure-JVM module that reads,
validates and recognizes them and carries the snapshot in its jar; and `FINGERPRINTS.md`, the keys encri and Encri
Files are signed with. encri reads the registry at its consent screen (encri D114, D122). This brief is level 1; full
records are in `docs/decisions/`.

## Rules that hold here
- **One file per app, `registry/<package>.app`, of `key: value` lines**: `package`, `developer`, `source`, one or more
  `certificate`, `rights`, `minimum-version` and an optional `refused-versions`. That is encri D114's list, with the
  developer added because the label claims *we know who wrote this*. (L15)
- **Strict, because a mistake is a label on a stranger's app.** An unknown key is an error; every key but
  `certificate` appears once; the file is named after its package; a certificate is 64 hex digits, with or without
  `apksigner`'s colons; `rights` come from the contract's five and always include `read`; version codes are positive.
  A registry that lists a package twice is refused whole. (L15)
- **No dependency but the standard library.** No TOML or JSON parser comes into encri's verified set or the jar
  encri builds in, for seven keys a reviewer reads in a diff. (L1, L15)
- **A recognition per app asking**: reviewed, with any right asked beyond review named; signed with another key (the
  package listed and the certificate not); refused (below the floor, or a refused version); or not listed. (L15)
- **Recognition changes words; only a refused version changes access.** Being listed never grants anything, and a
  version the registry refuses is not let in at all (encri D122). It matches a certificate's exact SHA-256. (L15,
  encri D143)
- **One entry per app, never one for a shared key**, so one app can be refused without the other. encri itself is not
  listed: it is the one that asks. (L15, L22)
- **The snapshot is a resource in the module's jar**, every entry in name order, so the registry an encri recognizes
  is the one in the encri-api commit it was built from (encri D126's pin). A unit test checks that the repository's
  `registry/` is the snapshot the jar carries. (L15)
- **CI validates every pull request**, and `:encri-registry:validateRegistry` says each entry back for its author.
  Coverage gates 90% line and 85% branch. The module's runtime classpath is locked and scanned. (L4, L10, L15)
- **Its public API is `api/encri-registry.api`**, dumped and checked like the client's. (L16)
- **Encri Files is the one entry**: developer SystemHalted, its source on GitHub, the certificate `BA:30:D5:EE…4C:CC:49`,
  all five rights, from version 1000, its first. (L22)
- **`FINGERPRINTS.md` lists each app's package and signing certificate**, as a table and as AppVerifier's lines, and
  the code transparency key's certificate, with how to check each. It goes out inside the first `v*` tag, which is
  GPG-signed. Upload keys are not in it: they prove nothing to a user. Encri Files' repository carries the same file.
  (L22, encri D115)

## Traps already paid for
- While the registry was empty, nothing had read a non-empty snapshot; the coverage gate is what said so. (L15)
- The first dump of the registry listed its internal `validate`, because the bytecode does not keep Kotlin's
  visibility at the top level. Top-level functions are mapped back to their Kotlin declarations now. (L16)
- Without Encri Files' entry, encri's consent screen called its own file manager *not on encri's list of reviewed
  apps*, measured on encri's first minified build (encri D140). (L22)
- Choosing *Use the same key as another app* in Play gave Encri Files encri's upload key as well. Nothing here depends
  on it, because neither the registry nor the client's trust names an upload key. (L22)

## Measured, not assumed
- Google Play shows the same app signing certificate for encri and Encri Files: two apps can share one key, as encri
  D115 planned. (L22, encri D140)
- The validator says Encri Files' entry back, and the registry's suite and gates pass with it. (L22)

## Decisions
| | Why it matters here |
|---|---|
| L15 | The format, its strictness, the module, the snapshot; two veto flags |
| L22 | Encri Files listed; `FINGERPRINTS.md`; one veto flag (the developer's name) |
| L16 | The registry's public API dump |
| L10 | Its runtime classpath locked and scanned |
| L4 | Coverage gates 90/85 |
| L1 | The pure-JVM module L1 foresaw |
| encri D114, D115, D122 | What an entry holds; one key for encri and Encri Files; a refused version gets no session |
| encri D126, D140, D143 | encri's pin of this repository; the keys made; trust by exact digest |

## Open here
- L15's flags: a plain-text format of our own, and a refused version not let in at all. L22's: the developer named
  "SystemHalted" rather than a person, which should match the Play listing.
- Encri Backup's entry waits on its signing key, open in encri-backup's K1. Any other app is *not listed* until a
  reviewed entry lands.
- `FINGERPRINTS.md` says encri and Encri Files share one key "the same on Google Play and on GitHub". encri D142 has
  encri go to Google Play alone, with no published APK. Not corrected.
- `FINGERPRINTS.md` is released inside the first `v*` tag, which waits for the first release (encri D136).

## Read next
- Related briefs: `.claude/rules/client.md` (the trust that names the same certificate), `.claude/rules/contract.md`
- `registry/README.md`; encri's side: `../encri-core/.claude/rules/companion-api.md`
- Full records: `docs/decisions/INDEX.md`
