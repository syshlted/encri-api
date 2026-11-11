# encri-api — conventions

> Moved out of `AGENTS.md` on 2026-11-11 (L26), unchanged but for L26's strike-through: that file's *Repo conventions* and *Relationship to encri* sections.

## Repo conventions

**Three files, and they do not overlap**, as in encri. ~~This one is the decision log~~ **`docs/decisions/` is the
decision log, one record a file (amended by L26)**: why a thing is
the way it is, what was rejected, what was measured. `ROADMAP.md` is what is next and in what order.
`CHANGELOG.md` is what happened. When they disagree, ~~**this file wins**~~ **the decision records win (amended by
L26)** and the other two are the bug.

**Default branch is `master`.** A SystemHalted organization-wide policy, not a per-repo accident. Do
not rename it to `main`.

**Commits are signed and Conventional.** Repo-local config, the same as encri's:

| Setting | Value |
|---|---|
| `user.signingkey` | `E483D747728C1C9EA213FB29615692825276E947` (RSA-4096, expires 2028-09-14) |
| `commit.gpgsign` | `true` |
| `tag.gpgsign` | `true` |
| `gpg.format` | `openpgp` |

`core.hooksPath` is deliberately unset: lefthook installs its own shims into `.git/hooks/` and refuses
to install while `core.hooksPath` points elsewhere. The versioned part is `lefthook.yml` and
`scripts/hooks/`.

| Hook | Job | Enforces |
|---|---|---|
| `pre-commit` | `signing-preflight` | `commit.gpgsign` true, `user.signingkey` set, key usable |
| `pre-commit` | `ktlint`, `detekt` | style and static analysis, over the working tree rather than the index |
| `commit-msg` | `conventional-commit` | Conventional Commits, subject <= 72 chars |
| `pre-push` | `verify-signatures` | every commit in the range has `%G?` in {G, U} |
| `pre-push` | `unit-tests` | `allUnitTests` |

Fresh clones bootstrap with `lefthook install`. Hooks are a fast local mirror of CI, never a
substitute: `--no-verify` and `LEFTHOOK=0` always exist, so every rule enforced locally is re-enforced
in `.github/workflows/`.

**Every commit builds and passes its own tests, not just the branch tip** — encri's rule, for
encri's reason: `verify-signatures` walks every commit in a push and `unit-tests` runs once, at the
tip, so a red commit in the middle is pushed silently and a `git bisect` later blames the wrong one.

**Merge with a merge commit or a rebase, never a squash** (encri D7). GitHub's squash re-signs with
its own web-flow key, so `commit-policy` would be verifying GitHub rather than the author.

**Regenerating the dependency verification metadata**, after adding or bumping anything:

```sh
rm gradle/verification-metadata.xml
./gradlew --write-verification-metadata sha256 --refresh-dependencies --rerun-tasks \
  ktlintCheck detekt allUnitTests koverVerify koverXmlReport assemble
```

**Start from an empty file, which is two steps past encri's note.** `--write-verification-metadata`
merges into the file it finds and never removes an entry, so regenerating over it keeps everything
the build has stopped using. Measured at L9: after the move to Kotlin 2.4.21 a merged file still held
all 31 components of the Kotlin 2.2.10 toolchain, which a scan of the file then reports as though the
build still ran them. The header Gradle writes into a new file is the one this file had.

**Changing what an app receives** means updating the lockfiles too, which STRICT mode demands (L10):

```sh
./gradlew assembleRelease --write-locks
```

That also writes an empty `settings-gradle.lockfile` for the version catalog, which is ignored.

**`--rerun-tasks` is not optional, which is one step past encri's note.** A task that is up to date
resolves nothing, so the first metadata written here missed `aapt2` — which only an *executing*
resource compile asks for — and the next clean build failed verification on it. The file is `sha256`
with signature verification off, so it freezes the dependency set rather than attesting it, as
encri's does (encri D7). All 624 of its artifacts are in encri's file too, with the same hashes —
541 until Robolectric came in for L8, then 619 before the file was regenerated from empty at L9.

## Relationship to encri

- **Decisions here are numbered L1, L2, …, and encri's are cited as *encri D113*.** Two logs both
  numbered from 1 would make "D3" mean two things in every conversation that spans both.
- ~~**About the contract, this file wins.**~~ **About the contract, this repository's decision records win (amended
  by L26).** encri D115 moved the contract's version line here — it is
  encri D6's plugin API line — so a change to the contract is recorded here and encri's service
  follows it. About encri's internals, ~~encri's file wins and this one links rather than restates~~ encri's decision
  records win and these link rather than restate (amended by L26, as encri D127 moved encri's).
- **The toolchain is encri's, version for version** (L3). encri compiles its service against the
  contract, so a bump lands in both repositories or in neither.
