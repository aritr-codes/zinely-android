# Releasing Zinely

Mechanics of cutting and distributing a build. **What** ships is owned by [ROADMAP.md](ROADMAP.md)
and [zinely-v1.md](zinely-v1.md); **whether** it may ship is owned by the Release Agent review in
[CLAUDE.md](../CLAUDE.md#release-review-release-agent). This document owns only the how.

---

## 1. The release key

Zinely is distributed as a side-loaded APK. Android decides whether a build may be installed *over*
an existing one by comparing signing keys — so the key is the app's identity, not a formality.

**If the key is lost, no future build can update an installed Zinely.** Every tester would have to
uninstall, and uninstalling deletes their zines, because backup/restore does not exist yet
([zinely-v1.md §7](zinely-v1.md) blocker 2). Treat the keystore as irreplaceable.

### One-time setup

Generate the keystore **outside the repository** — this command is run by the person who will own
the credential, not by an agent, so the passwords never enter a transcript or a log:

```bash
keytool -genkeypair -v \
  -keystore ~/zinely-release.jks \
  -alias zinely \
  -keyalg RSA -keysize 4096 -validity 10000 \
  -dname "CN=Zinely, O=Zinely, C=IN"
```

Then create `keystore.properties` at the repository root (git-ignored):

```properties
storeFile=C:/Users/<you>/zinely-release.jks
storePassword=<store password>
keyAlias=zinely
keyPassword=<key password>
```

CI reads the same four values from `ZINELY_KEYSTORE_FILE`, `ZINELY_KEYSTORE_PASSWORD`,
`ZINELY_KEY_ALIAS`, `ZINELY_KEY_PASSWORD` instead.

**Back up the `.jks` file and both passwords to somewhere that survives this machine dying**
(password manager + one offline copy). This is the single highest-consequence artifact in the
project.

All four values are required together. Supplying only some of them is a configuration error and
fails the build naming the missing ones — half-configured signing used to fail deep inside AGP with
a message that named neither the missing credential nor this file.

### The fallback, and the gate on it

With no keystore configured, the release build falls back to the debug key so a fresh clone stays
buildable — but `packageRelease` then **fails**, and no APK is produced:

> `zinely: refusing to package a <version> release APK signed with the debug key.`

To get an undistributable debug-signed release build on purpose (CI smoke, perf profiling), opt out
explicitly:

```bash
./gradlew :app:assembleRelease -PallowDebugSignedRelease
```

**This gate runs at execution time, deliberately.** The first version of it was a configuration-phase
`logger.warn`, and with `org.gradle.configuration-cache=true` a cached configuration is not re-run —
so the warning silently never printed and the build produced a debug-signed APK under the release
filename with a clean log. A human reading build output is not a gate; a failing task is.

## 2. Cutting a build

1. **Version** — bump `zinelyVersionName` **and** `versionCode` in [app/build.gradle.kts](../app/build.gradle.kts).
   `versionCode` must increase on every distributed build; Android compares only that when deciding
   whether an APK is an upgrade.
2. **Changelog** — move `[Unreleased]` into a dated version section in [CHANGELOG.md](../CHANGELOG.md),
   with an honest **Known limitations** list. Overpromising wording is a Release-Agent finding.
3. **Tests** — `bash tools/grun.sh <module>:test` per module; goldens must be verified, not just recorded.
4. **Assemble** —
   ```bash
   bash tools/grun.sh :app:assembleRelease --no-daemon \
     -Porg.gradle.java.installations.auto-detect=false \
     -Porg.gradle.java.installations.paths="C:\Program Files\Microsoft\jdk-21.0.10.7-hotspot"
   ```
   Output: `app/build/outputs/apk/release/zinely-<versionName>-release.apk` — the artifact name
   carries the version so testers never see an anonymous `app-release.apk`.
5. **Verify on a clean device** — install the artifact you are about to send, on a phone that does
   not already have Zinely, and complete the full journey. A build that has only ever been verified
   as an upgrade has not been verified.
6. **Tag** — `git tag -a v<version> -m "<version> — <headline>"` on the exact commit the artifact was
   built from, and push both.

## 3. Beta distribution (side-load)

Testers install an APK directly, which Android treats as an unknown source. The tester note must say,
in plain words: what to tap to allow the install, that the app never touches the network, and — until
backup exists — **that uninstalling deletes their zines, so export anything they care about first**.

### The one-time break at 0.9.0-beta.1

Every build up to and including 0.8.0 was signed with a *debug* key. `0.9.0-beta.1` is the first
signed with the real release key, and Android will not install it over a differently-signed app —
anyone still holding `0.6.0-alpha.1` gets `INSTALL_FAILED_UPDATE_INCOMPATIBLE`.

Those testers must **export anything they want to keep (Save PDF to Downloads), then uninstall,
then install the beta**. Their zines do not survive; there is no backup/restore yet. This was
already flagged as a known limitation of the alpha, and it is a one-time cost that ends here — every
build from the beta onward installs cleanly over its predecessor. The tester note must say so
explicitly rather than let someone discover it as an install error.

Play Store distribution is not in use yet. It would additionally need an upload key, a Play Console
listing, a privacy policy, a content rating and a data-safety declaration.
