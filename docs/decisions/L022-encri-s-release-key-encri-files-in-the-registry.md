# L22 — encri's release key: Encri Files in the registry, the client trusting it, and the fingerprints file
*2026-10-28 · accepted, and **flagged for veto on one point** (below). Fills what L12 and L15 left empty until a key
existed. encri's half is its D140, which put this before encri's release is built.*

**The keys exist.** On 2026-10-27 the maintainer made encri's app signing key, an upload key for each app and a code
transparency key (encri D115, D140), and on 2026-10-28 gave Google Play the app signing key for both encri and Encri
Files. Play shows the same certificate for both, SHA-256 `BA:30:D5:EE…4C:CC:49`, so the one-key plan of encri D115
holds, and Play lets two apps share it.

**Encri Files is the registry's first entry**, `registry/com.systemhalted.encri.files.app`: developer SystemHalted,
its source at `https://github.com/syshlted/encri-files`, that certificate, all five rights (it asks for all five), from
version 1000, its first. Without it, encri's consent screen called its own file manager *not on encri's list of
reviewed apps* (measured by encri D140 on its first minified build). One entry per app, as L15 rejected listing them by
their shared key; encri itself is not listed, because it is the one that asks.

**`EncriTrust.RELEASE_CERTIFICATES` holds the same certificate**, so an app built against this library trusts a
released encri by default, not only one signed by its own key (L12). Encri Files trusted encri by `sameSigner` alone
until now, and still does; a third-party app had to pin a certificate itself.

**`FINGERPRINTS.md` is written**, the file encri D115 asks for: each app's package and signing certificate, as a table
and as AppVerifier's lines, and the code transparency key's certificate, with how to check each. It is in the first
`v*` tag, which is GPG-signed. Upload keys are not in it: they prove nothing to a user, only to Play. Encri Files'
repository carries the same file, owed with its first release.

**Flagged for veto:**

1. **The developer is named "SystemHalted"**, the organization the repositories and the GPG key are under, rather than
   a person's name. It is what the consent screen shows, so it should match the name on the Play listing.

**Not settled here:** Encri Files' upload key. Choosing *Use the same key as another app* gave it encri's upload key as
well; a reset to its own was requested on 2026-10-28. Nothing here depends on it, because neither the registry nor
the trust names an upload key.

**Verified.** The registry's validator says the entry back; the registry's and the client's suites, ktlint, detekt and
both coverage gates pass, and both APKs build.
