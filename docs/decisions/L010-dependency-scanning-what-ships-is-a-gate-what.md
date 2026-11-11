# L10 — Dependency scanning: what ships is a gate, what builds it is a report
*2026-10-22 · accepted, at the user's direction; supersedes L7 on leaving it out*

**Two questions with different answers**, so `dependencies.yml` has two jobs.

**What an app receives fails the build on any advisory.** Gradle's dependency locking holds each
module's `releaseRuntimeClasspath` in a `gradle.lockfile` beside it, in STRICT mode, and OSV-Scanner
reads the two lockfiles with no triage file at all: nothing that ships is accepted. Today that is
`kotlin-stdlib` 2.4.21 and `annotations` 13.0, with no advisories. Measured both ways: strict mode
refuses to build with a lockfile missing, and a lockfile with `commons-lang3` 3.16.0 planted in it fails
the scan on GHSA-j288-q9x7-2f5v. The lock also makes a new runtime dependency a diff somebody reads:
it changes only by `--write-locks`, like the verification metadata changes only by regenerating it.

**What builds it is reported to the Security tab, and does not fail the job.** OSV-Scanner reads every
artifact `gradle/verification-metadata.xml` pins — which is why that file is now regenerated from empty
(L9) — against `gradle/osv-scanner.toml`, which accepts the 12 advisories left after L9. Each entry
says why the flaw cannot be reached from a library build; every one was checked against the advisory's
own details, not its summary line. Each expires on 2027-01-22, so the alert comes back for review
rather than staying ignored. Measured: with the file, 0 results; with freemarker's entry removed, that
advisory reappears. **A report rather than a gate** because an advisory published this week is not
something a pull request did, and failing unrelated pull requests teaches people to stop reading the
check. A pull request that brings a vulnerable tool in is still caught: it is a new alert, and code
scanning merge protection refuses one (`ROADMAP.md`).

**The scanner is a release binary checked against Google's published SHA-256**, not
`osv-scanner-action`, which runs a Docker image by tag: pinning the action's SHA does not pin the image
(encri D7). The job fails if the scanner fails, so a scan that did not run cannot pass as a clean one.
OSV-Scanner writes absolute paths into its SARIF, and the pinned `upload-sarif` sends the checkout path
as `checkout_uri`, which is what maps them back onto the repository; read from the action's source at
that SHA. Every step of the workflow was run locally as written before it was committed.

*Rejected:* forcing newer versions under AGP, Kover and ktlint, which would clear the list by swapping a
combination their authors tested for one nobody did, to patch code a library build never runs; it would
also break the property that every artifact here is in encri's verified set. *Rejected:* gating the
whole set, for the reason above. *Rejected:* Dependabot through a submitted dependency graph, which needs
a job holding `contents: write` to run the build, and a Gradle plugin the verified set would have to
admit.
