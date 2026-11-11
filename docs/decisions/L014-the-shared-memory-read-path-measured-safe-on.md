# L14 — The shared-memory read path: measured safe on ashmem and about three times faster per block, and not built yet
*2026-10-23 · accepted; **flagged for veto on not building it** (below). Closes `ROADMAP.md` item 1, the spike encri
D113 owed, and answers the open question of where it lives: here, beside the device tests, which already run a
service and a client in two processes.*

**The spike** (`SharedMemorySpikeTest`, `SpikeService` in a `:spike` process) is encri D113's in miniature. A service
creates a 1 MiB `SharedMemory`, keeps a read-write mapping, narrows the region to `PROT_READ` and hands it over a raw
Binder transaction; it also opens a proxy descriptor over the same bytes. Both routes serve a 64 KiB block by
copying one from memory, standing in for encri's decrypt, so what differs is the transport.

**The client cannot write through the region, on every target:**

| Attempt | G6 · 28 | S9 · 29 | A20 · 30 | emulator · 27 | emulator · 36 |
|---|---|---|---|---|---|
| `/proc/self/fd` calls it | `/dev/ashmem` | `/dev/ashmem` | `/dev/ashmem<uuid>` | `/dev/ashmem` | `/dev/ashmem<uuid>` |
| `mapReadWrite`, `map(READ or WRITE)` | EPERM | EPERM | EPERM | EPERM | EPERM |
| `setProtect` widened | refused | refused | refused | refused | refused |
| `mmap` of the raw descriptor, shared or private, writable | EPERM | EPERM | EPERM | EPERM | EPERM |
| `pwrite`, `ftruncate` | EINVAL | EINVAL | EINVAL | EINVAL | EINVAL |
| reopen `/proc/self/fd/<n>` read-write | allowed | **EACCES** | allowed | allowed | allowed |
| …then map it | EINVAL | EACCES | EINVAL | EINVAL | EINVAL |

The reopen that is allowed reaches nothing: opening ashmem's device makes a new region of no size, which no mapping
fits. And the assertion that decides it: **encri's copy checksums the same after every attempt**. encri's writes
after the hand-over reach the client's view, and zeroing the region clears it — encri D113's *zero it when a session
ends* works as planned.

**What is not measured is memfd.** Every target backs `SharedMemory` with ashmem, the API 36 emulator included
(`sys.use_memfd` is false there). A vendor ROM at API 34 or above may use memfd, sealed with `F_SEAL_FUTURE_WRITE`
rather than ashmem's protection mask, and the S24 is the phone that would say. The read path does not ship before it
has.

**What a block costs, median of 1024 blocks after a warm-up, beside a 64 KiB memcpy:**

| | Binder round trip | a block through the region | a block through AppFuse | AppFuse ÷ region | memcpy |
|---|---|---|---|---|---|
| S9 · 29 | 46 µs | 83 µs | 1.08 ms | 13× | 3.1 µs |
| A20 · 30 | 307 µs | 673 µs | 2.12 ms | 3.2× | 6.6 µs |
| G6 · 28, screen off | 767 µs | 1.55 ms | 4.68 ms | 3.0× | 3.8 µs |
| emulator · 27 | 14 µs | 44 µs | 114 µs | 2.6× | 1.8 µs |

The AppFuse reads are each block once, four blocks apart and shuffled, with the warm-up on blocks the timed reads
never touch, so none is a page-cache hit (encri D22). These are isolated reads: a sequential reader gets readahead
through AppFuse, and the region path, one round trip per block, does not. And they are transport only. **With encri's
decrypt added** — 400 µs a block on the S9 and 580 on the G6 (encri D40) — a block costs 1.5 ms against 0.48 ms on
the S9 and 5.3 ms against 2.1 ms on the G6: **about 2.5 to 3 times** per block, which is encri D113's estimate
measured. The slow round trips on the G6 and the A20 are the phones' state, a little core woken for each call, which
encri D12 and D40 both warn moves a figure more than code does.

**Not built now, and that is a judgment rather than a measurement.** Three reasons:

1. **It is additive.** A new method appended after the last (L2) breaks nothing installed, so building it later costs
   no contract version beyond the one it would cost now.
2. **Nothing reads content through the library yet.** The file manager hands documents to viewers by descriptor or by
   handoff, and its previews come through `thumbnail`. The first client that reads a document's bytes in its own
   process — an in-app viewer, a player through a `MediaDataSource` — is what the path is for, and is its trigger.
3. **memfd is unmeasured** on the one kind of ROM that would use it.

**When it is built**, it is encri D113's shape with what the spike found: lengths and offsets in the Binder message
and never in the region, encri copying out of the region once if it ever reads from it, the region zeroed when the
session ends, and the region handed out only after `setProtect(PROT_READ)` has succeeded.

**Flagged for veto:**

1. **The read path waits for a client that needs it**, though it is measured at about three times faster per block.

*Rejected:* building the path on the ashmem result alone, when the phone most likely to use memfd has not run it.
