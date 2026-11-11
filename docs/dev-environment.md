# encri-api — development environment

> Moved out of `AGENTS.md` on 2026-11-11 (L26), unchanged: that file's *Local development environment* section.

## Local development environment

The same machine as encri, and encri's notes on it apply: the JDKs, the devices, the emulators and
Waydroid are recorded there under *Local development environment*. Three facts matter here:

- **Set `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64`.** `gradle/gradle-daemon-jvm.properties` pins the
  daemon to 21 and `jvmToolchain(21)` pins the compiler; they are separate settings (encri D11).
- **`~/.config/git/template` installs hooks of its own** into every repository `git init` or
  `git clone` makes. `lefthook install` renames them `.old`, and lefthook 2.1.14 does not run a `.old`
  hook (checked in a scratch repository, where only lefthook's job ran), so they are inert. They were
  deleted here so `.git/hooks/` matches encri's.
- **`local.properties` is gitignored** and holds `sdk.dir=/home/coder/Android/Sdk`.
