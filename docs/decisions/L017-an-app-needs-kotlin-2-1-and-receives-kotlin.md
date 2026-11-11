# L17 — An app needs Kotlin 2.1, and receives kotlin-stdlib 2.2.10
*2026-10-23 · accepted; **flagged for veto** (below). Settles L3's open question.*

**Every published module compiles its main sources at language and API version 2.2 and declares kotlin-stdlib
2.2.10**, where the compiler's defaults were 2.4 and 2.4.21. What an app needs follows from the metadata: a compiler
reads one version ahead, so 2.2 metadata wants Kotlin 2.1 or newer, where 2.4 wanted 2.3.

**Measured with `kotlinc`, three ways**, compiling one consumer file that connects, opens a session and lists a
folder:

| Compiler | Against | Result |
|---|---|---|
| 2.1.21 | the client with the floor | compiles |
| 2.0.21 | the client with the floor | refused: *metadata is 2.2.0, expected version is 2.0.0* |
| 2.1.21 | the client before the floor | refused: *metadata is 2.4.0, expected version is 2.1.0* |

**2.2 rather than 2.1**, because 2.1 compiles and the compiler warns that 2.1 is deprecated and its support will be
removed; a floor that breaks at the next compiler bump is one the next bump has to move. 2.1 would have bought
consumers on Kotlin 2.0, which kotlinx-coroutines 1.10.2, the client's `api` dependency, can serve but the 2.4
compiler cannot be made to target without the warning.

**kotlin-stdlib 2.2.10, the oldest the 2.2 API allows that the verified set already holds**, from AGP's toolchain:
the metadata did not change. An app resolves the newer of its own and this. The three release-classpath lockfiles
move to it, and OSV knows no advisory for 2.2.10 or 2.4.21. Main sources only: the tests use what they like.

**Flagged for veto:**

1. **Kotlin 2.1 as the floor**, which an app older than that meets as a metadata error at its first build.
