# Backup → clean reinstall → restore — device verification

**Date:** 2026-08-28  
**Device:** Samsung SM-A176B (`RZCYA1VBQ2H`), Android 16 / API 36, 1080 × 2340  
**Build:** `zinely-0.9.0-beta.2-debug.apk` from `feat/zine-backup-v2`

## Scope

This is a real production-flow recovery pass. It does not use a hand-authored, corrupt, or newer-version
archive, and it does not claim coverage of the intentionally deferred corrupt/newer-document scenario.

## Procedure and result

1. On a clean debug installation, created an A4 zine and added text with the marker `Restore-verify`.
2. Returned to the shelf and confirmed one zine.
3. Used **Backups → Back up this shelf**. Android's save flow wrote
   `Downloads/zinely-backup-2026-08-28.zine`; device inspection measured 2,097 bytes.
4. Uninstalled `com.aritr.zinely`, then installed and launched the same current debug APK.
5. Used onboarding **Restore a backup**, selected that exact Downloads archive, and completed restore.
6. Zinely reported **“1 zine added to your shelf”**. The restored shelf showed one zine. Opening it in the
   editor exposed the saved element as `Text: Restore-verify`.

**Result: PASS for the bounded text-only clean-reinstall recovery path.** The exported archive remained
outside app storage, the fresh installation restored it through the public UI, and the restored content was
openable and retained its committed text.

## Deliberately not claimed

- Photo assets, cover fidelity, timestamps, and preview/PDF/print-output parity were not present in this
  one-zine fixture and remain open in the torture matrix.
- Another-device/API, provider failure, storage exhaustion, cancellation/process death, repeated restore,
  and offline scenarios remain pending.
- No accessibility or device settings were changed during this pass, so no setting restoration was needed.
