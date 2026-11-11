# L26 — The decision log leaves AGENTS.md, as encri's did (encri D127)
*2026-11-11 · accepted on the user's direction ("all of the AGENTS.md files … need to be evaluated to place the task maps, to-do lists, bugs, artifacts, etc. in their appropriate places"; "make sure the documentation … for all of the apps is written and formatted similarly"), and **flagged for veto on five points** (below).*

#### What moved where

`AGENTS.md` was the whole decision log, L1 to L25, with the status, the conventions, the development environment,
the open questions and the build order: 86,087 bytes and 1,199 lines, loaded into every session. It now follows the
suite's `docs/documentation.md`, which is encri D127's layout.

| Was, in `AGENTS.md` | Is |
|---|---|
| `### L1` to `### L25` | `docs/decisions/L001-*.md` to `L025-*.md`, word for word but for the heading level (`###` to `#`) |
| *Status* and *Build order* | `docs/status.md` |
| *Repo conventions* and *Relationship to encri* | `docs/conventions.md` |
| *Local development environment* | `docs/dev-environment.md` |
| *Open questions* | `docs/decisions/open-questions.md` |
| *What this is* | `AGENTS.md`, which is now the level-0 guide: 173 lines and 11,845 bytes |

New beside them: `docs/decisions/INDEX.md` and each record's footer, written by `scripts/docs/decision-index.py`;
`docs/decisions/amendments.md`, whose thirteen entries are corrections the later records had already made, now
copied into the footers of the records they correct; `scripts/docs/brief-coverage.py`; and five briefs in
`.claude/rules/`. Both scripts are encri-backup's copies of encri's, reading L numbers.

This amends no numbered record. It amends the conventions, which named `AGENTS.md` as the decision log and said it
wins: struck through in place in `docs/conventions.md`.

#### The move is lossless, and that was checked

A script assigned each of the old file's 1,199 lines to exactly one destination, wrote them, then rebuilt the
original from the written files and compared: identical, byte for byte. The lines not carried are the old title block
(5 lines), the bare `## Decisions` heading (2) and four `---` rules between sections (8). Two edits were then made on
purpose, and a second rebuild differs from the original in them and nothing else: three pointers in *What this is*
("this file", encri's `AGENTS.md`), and the strike-throughs in `docs/conventions.md`.

#### The components

Five, by feature as encri's are (encri D127), each under 12 KB:

- `contract`: the wire, with `PROTOCOL.md` and the append-only check. encri compiles against it and its rules are
  the permanent ones.
- `client`: what an app declares, with the README. Its rules are about what an app sees.
- `registry`: `encri-registry`, `registry/` and `FINGERPRINTS.md`. It decides what encri's consent screen says, and
  is pure JVM.
- `build-ci-release`: one toolchain, shared with encri, with the scanning and what a release will need.
- `device-testing`: the suites that run in processes of their own, and the shared-memory spike, which shares their
  harness (L14).

`brief-coverage.py` finds every tracked file covered by at least one brief, documentation and generated files
excepted, and no glob that matches nothing.

#### Placed on the way

`ROADMAP.md`, `CHANGELOG.md`, the README and the registry's README take the suite's format and cite records rather
than `AGENTS.md`. The roadmap's severity line, which said it was encri's and was not, is the suite's table. encri's
half of L23 is encri B143. The backup app's surface in the contract is an item of its own. The roadmap's last section,
the file manager, is done and went to `docs/status.md`. Lines the briefs found stale against a later record are in
each brief's *Open here*, and are not corrected here.

#### Flagged for veto

1. **`.claude/rules/` is committed, in a public repository** (L6, L19), by narrowing `.gitignore`'s `.claude/` to
   `.claude/*` with an exception, as encri D127 did. The briefs hold nothing the records do not. The rest of
   `.claude/` stays per-machine.
2. **Code and older documents keep citing `AGENTS.md Lnn`**: nine comments in code and configuration, from
   `IEncriService.aidl` to `osv-scanner.toml`. `AGENTS.md` says how to resolve them, as encri's does.
3. **Three pointers in *What this is* were changed**, where encri D127 kept that section word for word.
4. **`CHANGELOG.md`'s older headings take the new form**, `### <title> (YYYY-MM-DD)`, where the suite's guide says
   entries written before 2026-11-11 keep theirs. Their bodies are unchanged.
5. **`amendments.md` starts with thirteen entries**, read out of the records' own text, rather than empty.

*Rejected:* leaving encri-api's log in one file while every sibling moves, which would leave one repository a reader
must learn separately.
