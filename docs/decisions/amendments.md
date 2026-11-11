# encri-api — amendments to earlier decisions

Each entry corrects a record. Its bold lead names the record possessively ("L20's …") so that
`scripts/docs/decision-index.py` copies it into that record's footer. The entries below were written on 2026-11-11
(L26) from what the later records already said; none is new.

## Amendments

- **L24's *nothing is built* was overtaken by its build (2026-11-11).** `handOffNamed`, `VERSION` 2 and the client's
  `withNames` landed in `a414543`, the day encri built D147. `AgainstEncriTest` reading a handoff's name both ways is
  written and has not run. L24 marks it in place.
- **L24's companions offered the name on a share and not on *open with*, amended by L25 (2026-11-11).** A client
  asks for `handOffNamed` on a tick the user makes, "Send its name", unticked every time, in the dialog it shows
  before a share or an *open with* alike. No list of apps reads real names. L24 marks it in place.
- **L12 and L15's empty lists are filled by L22 (2026-10-28).** `EncriTrust.RELEASE_CERTIFICATES` holds encri's app
  signing certificate, `BA:30:D5:EE…4C:CC:49`, so an app trusts a released encri by default and not only one signed by
  its own key; and the registry lists Encri Files under that certificate.
- **L20's three backup screens are two, by L21 (2026-10-27).** `ACTION_BACKUPS` and `Encri.backupsIntent()` left the
  contract before its first release, with the scheduled backup that left encri (encri D137). `ACTION_SAVE_BACKUP_FILE`
  and `ACTION_RESTORE` stay. L20 marks it in place.
- **L2 and L6's package that can never change is permanent from the first tag, by L19 (2026-10-23).** Before a
  release it could change, and did, with every name in the contract.
- **L2 and L8's owed append-only check is built by L18 (2026-10-23)**: `scripts/ci/append-only.py`, which CI runs
  against the newest `v*` tag. It holds parcelables' fields and constants as well as methods, and lets only `VERSION`
  rise.
- **L3 and L9's open question, the Kotlin an app needs, is settled by L17 (2026-10-23)**: Kotlin 2.1 or newer, with
  kotlin-stdlib 2.2.10. L3 marks it in place.
- **L5 and L8's owed guard, that no public signature in the client names the contract, is built**: `PublicSurfaceTest`
  by L12 (2026-10-23), which first had to be made to see the client's classes at all, and the API dump by L16
  (2026-10-23).
- **L2 and L8's claim that a method an older service lacks reads as 0 or null is measured on a device by L13
  (2026-10-23)**, over a real `BinderProxy` on three phones and the API 27 emulator. L8 marks it in place.
- **L3's instrumented matrix, left out for want of device tests, is added by L13 (2026-10-23)**, at API 27 and 36.
  L3 marks it in place.
- **L4's filter for generated code is by name since L11 (2026-10-22)**: every class an `.aidl` file names, with its
  inner classes, because the aidl tool's structured parcelables implement no Binder interface. L4 marks it in place.
- **L7's leaving dependency scanning out is superseded by L10 (2026-10-22)**: what ships is a gate, and what builds it
  is a report. L7 says so in place.
- **L7's finding that CodeQL needs no in-process Kotlin compiler was true of Kotlin 2.2 and is false of 2.4, by L9
  (2026-10-22)**, so the flag is in the workflow. L7 marks it in place.
- **L3's Kotlin is 2.4.21, by L9 (2026-10-22)**, ahead of the 2.2.10 AGP depends on, which moved the Kotlin an app
  needed until L17 settled it. L3 marks it in place.
