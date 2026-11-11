# encri-api — open questions

> Moved out of `AGENTS.md` on 2026-11-11 (L26), unchanged: that file's *Open questions* section.

## Open questions

- **How the artifacts are signed for Maven Central** (L1): the repository's signing key, or one of
  their own.
- ~~**The Kotlin floor for consumers** (L3).~~ **Kotlin 2.1 (L17).**
- ~~**Where encri D113's spike lives** — a read-only `SharedMemory` handoff and a Binder round trip on
  the four phones. It needs a service and a client in two processes, and either repository could hold
  it.~~ **Here, beside the device tests (L14).**
