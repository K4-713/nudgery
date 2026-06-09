# Security Notes — Protecting Sensitive User Data

**Status: working notes, not binding decisions.** This document is a threat-modeling
scratchpad for how Nudgery protects the data users put into it. Nothing here is a
commitment yet. When a control is actually chosen, it graduates to the appropriate
home: a binding internal rule → `ENGINEERING_DECISIONS.md` (with a `TDD_` test);
user-visible behavior → `README.md`; UX → `DESIGN.md`. Until then, this is where the
reasoning lives so it can be picked up later.

Last substantive update: 2026-06-08.

---

## Why this matters

Nudgery is positioned as a general-purpose logger, **not** a health/medical app, and we
keep that framing for the Play Store and docs. But we should design honestly: **people
will enter health and other highly sensitive data in it** — medication adherence, mood
and mental-health check-ins, substance use and recovery, reproductive/cycle data, sexual
activity, and abuse documentation were all foreseeable from day one (health monitoring was
the original use case). The product framing does not change the data reality, so our
security posture should assume sensitive content is present.

### What an exposure could cost a user

Depending on content and who obtains it: stigma, employment/insurance harm, custody
disputes, outing (orientation/gender) in a hostile family or country, legal exposure
(e.g. reproductive data in some jurisdictions), and — in abuse situations — physical
danger. These are real harms, concentrated on the users least able to absorb them, and we
cannot tell in advance which user is at risk.

---

## Assets to protect

1. **Answer content** — free-text answers, chosen options, scale values. The payload.
2. **Question / nudge text** — often reveals the subject by itself ("Did I take my
   antidepressant?"). Note this is what a reminder notification would normally display.
3. **Patterns & metadata** — `scheduledAt` / `answeredAt` timestamps and schedule config
   leak routine, presence, sleep/wake, travel, and gaps, *even when every answer is
   innocuous.* Inference risk is a first-class asset, not an afterthought.
4. **Existence of personal data itself** — that the user is logging *something*, and that it is
   hidden. In a monitored/coercive situation, salience is the threat: a notification (even
   a generic one) firing tells an observer there is something here worth looking at.

---

## Adversaries (be specific)

| ID | Adversary | Access | Notes |
|----|-----------|--------|-------|
| **A1** | Stranger with a lost/stolen device; repair technician | Physical, device **locked/off**, offline | Analyzes data away from the phone. |
| **A2** | Known person with physical + **unlocked** access (abusive partner, family) | Knows the PIN / device is unlocked | Highest-likelihood acute harm for a personal log. |
| **A3** | Coercive authority (border, law enforcement) | Can **compel unlock** in many jurisdictions; forensic tools | Image-and-retain is routine for the forensic subset; unlock is the precondition. |
| **A4** | Malicious app on the device (no root) | Sandbox-limited | OS already blocks cross-app file reads. |
| **A5** | Rooted device / live on-device extraction / capable forensic on an unlocked phone | Root or code-exec on a live, unlocked device | Can scrape decrypted data from memory or use the app as a decryption oracle. |
| **A6** | Recipient of an exported backup file | The file has **left** the device (email, Drive, wrong recipient) | OS protections do not apply to exported files. |

### The lock-state distinction that drives everything (BFU vs AFU)

- **BFU (Before First Unlock)** — device locked/off, not unlocked since boot. Android's
  file-based encryption already keeps user data encrypted; a raw image yields little.
- **AFU (After First Unlock) / unlocked-in-hand** — OS keys are in memory; a forensic
  extraction can yield ~95% of a full filesystem dump, *and* any device-bound auto-unwrap
  key is also available to anything running on the unlocked device.

Implication: encryption with a **device-bound, auto-unwrapping** key only adds protection
in the BFU/offline case (A1) — exactly where the OS already helps. It does **nothing**
once the device is unlocked (A2, A3, A5). Meaningfully defending the unlocked-device cases
requires a secret the adversary does **not** have — i.e. a **user passphrase**, not a
device-bound key.

---

## Candidate controls

- **C0 — OS baseline (already in place).** File-based encryption + app sandbox +
  `allowBackup="false"` (set in `AndroidManifest.xml`). Free, already done.
- **C1 — At-rest DB encryption, device-bound key.** SQLCipher via the Android driver
  factory; key in the Android Keystore (TEE/StrongBox), never stored beside the DB; no
  login, auto-unwraps when the app runs.
- **C2 — Optional passphrase "Protected mode," dormant when locked.** Whole DB encrypted
  with a key derived from a user passphrase (Argon2/scrypt/PBKDF2 → SQLCipher). **No
  outside account.** While locked: key is not in memory, no background work, **no
  notifications**, app inert. Unlock with the passphrase to use; reuse the existing
  `CatchUpMissedFiresUseCase` to reconcile fires missed while sealed. Going fully dark (vs
  generic notifications) is *both* more private and *simpler to build* — it removes the
  need to split timing metadata from content.
- **C3 — Encrypted backup files.** Passphrase-derived key (PBKDF2/Argon2) + AES-GCM
  envelope around the existing JSON/ZIP backup. Uses platform `javax.crypto` — **no new
  dependency.** Can reuse C2's passphrase/KDF so the two become one "lock with a password"
  capability.
- **C4 — App-lock / biometric gate.** Keystore key marked
  `setUserAuthenticationRequired(true)`; ties access to device credential/biometric.
  Convenience option; weaker than C2 against an adversary who controls the device unlock.
- **C5 — Notification content minimization.** Generic lock-screen text so question content
  never renders when the device is locked. (Subsumed by C2 when locked = no notifications.)

---

## Coverage matrix

✓ = meaningfully protects · ~ = partial / defense-in-depth / conditional · ✗ = does not protect

| Adversary / scenario | C0 OS | C1 device-key | C2 passphrase+dormant | C3 enc. backup | C4 app-lock |
|---|---|---|---|---|---|
| **A1** lost/stolen/repair, locked, offline | ✓ (BFU) | ✓ (DiD; also AFU file-copy) | ✓ | — | ✓ |
| **A2** known person, unlocked phone | ✗ | ✗ (auto-unwraps) | ✓ | — | ~ ¹ |
| **A3** compelled unlock (border/LE) | ✗ | ✗ | ~ ² | — | ~ ² |
| **A4** other app, no root | ✓ (sandbox) | ~ (DiD) | ✓ | — | ✓ |
| **A5** rooted / live unlocked extraction | ✗ | ✗ | ~ ³ | — | ✗ |
| **A6** exported backup file leaves device | ✗ | ✗ | ✗ | ✓ | ✗ |
| **A7** ambient "something is being recorded" | ✗ | ✗ | ✓ ⁴ | — | ~ ⁴ |

1. Helps only if the lock is **not** auto-released by a credential the adversary controls.
2. Resists a compelled **device** unlock, but **not** a compelled **passphrase** if the
   adversary knows it exists and coerces it. No software defeats coercion.
3. If locked, the key is absent → protected. If unlocked on a rooted device, plaintext can
   be scraped from memory → not protected.
4. C2 dormant-mode fires **no** notifications, removing the ambient tell. It does **not**
   hide that the app is installed (launcher / Settings → Apps / Play "your apps" still show
   it). C5 reduces lock-screen content leakage but the notification still appears.

---

## Key tensions & honest limits

- **Reminders vs. protection.** Timely nudging needs background DB access; strong
  protection needs the DB sealed. **Dormant-when-locked** resolves this firmly toward
  protection: Protected mode becomes a *vault you open deliberately when it's safe*, not an
  ambient reminder. Acceptable — arguably ideal — for the "I put something sensitive here"
  persona; a real behavior change to name for users.
- **No recovery.** No account ⇒ a forgotten passphrase = unrecoverable data. That is the
  point (no backdoor), but it demands a blunt setup warning and a nudge to make an
  (encrypted) backup.
- **Convenience vs. strictness.** Biometric unlock (C4) reduces typing but re-ties access
  to the device credential an abuser/border may already control. Make it the user's choice;
  offer password-only as the strict option.
- **Deniability is limited.** We can remove *ambient* tells (no notifications, no on-open
  content), defeating casual/over-the-shoulder discovery. We **cannot** cleanly hide that
  the app is installed, and we **cannot** beat a compelled passphrase. A neutral/empty
  presentation or decoy vault is a *further* level — substantially more complex, easy to
  get wrong, and **dangerous if it gives an at-risk user false confidence.** Keep it out of
  any first version.
- **Bimodal sensitivity.** Most users log low-stakes things; a minority log
  genuinely dangerous content and can't be distinguished in advance → favor **opt-in**
  controls (C2/C3) that concentrate cost on the users who ask for it.

---

## Current lean (2026-06-08)

For protection *proportional to effort and matched to the real threats*:

1. **C3 (encrypted backups)** — highest value per cost: covers the worst exposure (A6,
   sensitive content sitting in email/Drive), opt-in, **no new dependency**.
2. **C2 (optional passphrase + dormant-when-locked)** — the only control that meaningfully
   addresses A2 (known person, unlocked phone), the worst acute harm for a personal
   log. Bigger build, but the right shape; reuses C3's passphrase/KDF.
3. **C1 (device-bound at-rest encryption)** — modest, narrow value (A1 only, much of which
   the OS already covers). Worth it mainly as defense-in-depth and to truthfully tick
   "encrypted at rest" on the Play Data Safety form. *Currently being reconsidered* — see
   `TODO.md` "Encrypt the Database at Rest."

C1 and C2 are partly redundant (both encrypt the DB); if C2 is built, C1's device-bound
key may be subsumed or offered only as the no-passphrase default.

---

## Open questions to settle later

- Whole-app Protected mode vs. per-Nudge protection? (Per-Nudge lets unlocked nudges still
  fire — which partially defeats the ambient-tell goal.)
- Is "no reminders while locked" acceptable as the documented behavior of Protected mode?
- One shared passphrase for C2 + C3, or independent secrets?
- KDF choice and parameters (Argon2id preferred; document cost params as an ED).
- Migration paths: enabling/disabling Protected mode on an existing (possibly populated)
  database, including the plaintext → encrypted one-time migration and its rollback/safety.

## Relevant existing code

- `shared/src/androidMain/.../db/DatabaseDriverFactory.kt` — where SQLCipher would attach.
- `androidApp/.../AndroidManifest.xml` — `allowBackup="false"` already set.
- Backup flow: `ExportAnswersUseCase` (shared) → `NudgeDetailScreen` / `SettingsScreen`
  (write + share) → `NudgeBackupParser` (import). Encryption hooks at the byte boundary.
- `CatchUpMissedFiresUseCase` — reuse to reconcile fires missed while a vault was locked.
