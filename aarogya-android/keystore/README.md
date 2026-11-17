# `keystore/`

This directory is `.gitignore`d (except this file) — nothing in it other than
this README is ever meant to be committed.

`patient-rc.jks` is a **throwaway signing key, generated locally on
2026-11-17 purely to let `bundleProdRelease`/`assembleProdRelease` be verified
end-to-end on this machine.** It is not, and must never become, the real
production signing key for this app:

- It was generated with a placeholder password (`rc-keystore-change-me`) and a
  placeholder distinguished name, never meant to be kept secret.
- Nobody rotates or backs this up. If it's lost, nothing is lost that matters —
  it only ever signed release candidate builds on this one machine.

**Before this app's first real Play Store upload**, generate a real production
keystore separately (`keytool -genkeypair`, a strong unique password, stored in
a password manager and/or Play App Signing), and:
1. Never check it into this repo (`.jks`/`.keystore` are already gitignored).
2. Back it up somewhere durable and access-controlled — once a release has been
   uploaded to the Play Console signed with a given key, that key cannot be
   swapped out for most existing apps without Play App Signing's own key-upgrade
   process, and losing it entirely can mean losing the ability to ship an update
   to that app under its existing listing, ever.
3. Point `keystore.properties` (local) or the CI secrets (see
   `.github/workflows/release.yml` and `keystore.properties.example`) at the
   real file/credentials instead of this one.
