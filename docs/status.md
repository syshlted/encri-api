# encri-api — status history

> Moved out of `AGENTS.md` on 2026-11-11 (L26), unchanged: that file's *Status* and *Build order* sections.

## Status

**Scaffolded 2026-10-22.** Two modules build, the gates run, and the hooks are installed. **The contract's
first surface is written (L11)**: a service, a session, three listeners and the parcelables they carry. **The
client is written (L12)**: connecting, trusting encri, opening a session, every request the contract has, and the
intents for the consent and unlock screens. encri implements the service as its D120. **They have run against
each other on two phones (L13)**, from a package that is not encri's, and the run found a preview encri made
larger than the contract allows, which encri now honours (encri D121). The shared-memory read path encri D113 asked about
is measured and left unbuilt (L14). **The registry exists (L15)** and encri builds it in, empty until an app has a
release key. **What a first release needs is checked, but for publishing**: the public API is dumped (L16), an app
needs Kotlin 2.1 (L17), the contract can only grow from the last tag and a consumer needs no R8 rules (L18). **Pushed 2026-10-23** to `git@syshlt-github:syshlted/encri-api.git`, replacing
GitHub's initial commit (L6). The first runs of `ci`, `commit-policy`, `codeql` and `dependencies` all passed, with
no code-scanning alert.

**Renamed with the suite on 2026-10-23 (L19, encri D124).** Every package, artifact, wire name and repository
name says encri; the earlier name is only in the history before that.

**The contract names encri's backup screens as of 2026-10-26 (L20, encri D123)**, so a client can open them; the
work and its passphrases stay on encri's screens. **Two of them since 2026-10-27 (L21)**: the automatic backups
screen left encri for two backup apps of their own (encri D137), and `ACTION_BACKUPS` left the contract with it.

**What a consent and a deleted folder do to a session is promised as of 2026-11-03 (L23)**, from what encri was
seen to do; encri owes ending the session whose grant's folder is deleted.

**A handoff names its document only when the client asks, as of 2026-11-11 (L24, encri D147)**, by a method that
comes with contract version 2. Nothing is built: the method lands with encri's half. **The user asks, by a tick in the dialog before a share or an *open
with* (L25, encri D148)**, and no list of apps is kept.

## Build order

1. ~~Scaffold: the Gradle project, both modules, the gates and the hooks.~~ **Done 2026-10-22** — L1
   to L5.
2. Everything after is in `ROADMAP.md`.

## After encri-api

> Moved from `ROADMAP.md` on 2026-11-11 (L26), where it was the last section, because it is done: Encri Files is
> built, in its own repository.

**The file manager**, in `../encri-files`, built against this library as any other app is
(encri D112, D115).
