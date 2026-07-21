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

### The fallback is deliberate

With no keystore configured, `assembleRelease` still succeeds — it signs with the debug key and logs
a warning. That keeps a fresh clone buildable. **A debug-signed APK must never be distributed**: it
cannot be updated in place from any other machine. Check the build log for the warning before
handing an APK to anyone.

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

Play Store distribution is not in use yet. It would additionally need an upload key, a Play Console
listing, a privacy policy, a content rating and a data-safety declaration.
