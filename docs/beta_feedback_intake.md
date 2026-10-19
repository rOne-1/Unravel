# Unravel — Beta Human Testing Defect Intake Pipeline & Template

> **Document Version:** 1.0.0  
> **Target Release:** `v0.6.0-beta.0` (Beta Release for Human Testing)  
> **Custodian:** QA Specialist & Lead Engineer  
> **Companion Document:** [`docs/human_testing_flight_manual.md`](human_testing_flight_manual.md)  
> **Holding Pen (Local):** `local-notes/active/outstanding_issues_notepad.md`

---

## 1. Overview & Intake Policy

The **Beta Human Testing Defect Intake Pipeline** is the official mechanism for capturing, classifying, and converting qualitative and quantitative feedback gathered from human testers during the `v0.6.0-beta.0` flight test.

### Scope of Beta Feedback
Testers evaluate physical interactions across:
- **Skill Map:** 2D canvas pan, zoom, rubber-banding, prerequisite splines, "Jump Back In" resume.
- **Sorting Canvas:** Vertical bar resizing, drag-to-swap Bézier motion, sliding window highlights, scrubber.
- **Tree & Graph Canvas:** Floyd cycle orbital motion, AVL curved subtree rotation, 2D grid wall painting, path tightening.
- **Challenge Mode:** Decision pause points, touch gestures, haptic confirm/reject distinction, streak counter.
- **Race Mode:** Dual ticker synchronization, operation delta HUD badges (`CMP`, `MUT`), Glass Prism divider, foldable posture.
- **System Telemetry:** Monospace typography alignment, dark mode contrast on `#0B0E14`, haptic latency.

---

## 2. Standard Defect Intake Template

When filing an issue, copy and paste the block below into the active intake log:

```markdown
#### [DEFECT-BETA-XXX] [Short Title Describing Friction / Defect]
- **Tester Device / Model:** (e.g. Pixel 9 Pro Fold, Pixel 8, Galaxy S24 Ultra, Nothing Phone 2)
- **Android OS Version:** (e.g. Android 15 / API 35, Android 14 / API 34)
- **Subsystem:** (Skill Map / Array Canvas / Tree Canvas / Grid Canvas / Race Mode / Challenge Mode / Persistence)
- **Severity:** (Critical / Major / Friction / Aesthetic)
- **Observed Behavior:** 
  [Describe exactly what happened on the physical display or under your fingers]
- **Expected Mechanical / Tactile Behavior:** 
  [Describe what the interaction should feel like according to the Flight Manual]
- **Reproduction Steps:**
  1. Launch Unravel v0.6.0-beta.0
  2. Navigate to [Module / Screen]
  3. Perform [Gesture / Action]
  4. Observe [Anomalous Behavior]
- **Logcat / Crash Trace (if applicable):**
  [Include any error traces or adb logcat output]
```

### Severity Definitions
- **Critical:** Application crash, ANR, unrecoverable black screen, or corrupted DataStore/Room state.
- **Major:** Core algorithmic mechanic broken (e.g., drag-to-swap fails to mutate array, race mode locks up).
- **Friction:** Gesture ambiguity (e.g., vertical scroll accidentally triggers canvas pan, scrubber slider thumb difficult to target), haptic timing mismatch (> 16ms lag), or rubber-banding jerkiness.
- **Aesthetic:** Monospace font clipping, contrast ratio edge cases on specific display panels, slight visual artifact at fold seam.

---

## 3. Post-Beta Triage & Engineering Workflow

All filed reports proceed through a systematic 4-step triage pipeline before engineering modifications:

```
┌─────────────────────────────────┐
│   Human Feedback Intake Entry   │
└────────────────┬────────────────┘
                 │
                 ▼
┌─────────────────────────────────┐
│ 1. Verification & Deduplication │  (Verify reproduction steps on physical device / emulator)
└────────────────┬────────────────┘
                 │
                 ▼
┌─────────────────────────────────┐
│ 2. Root Cause Analysis (RCA)    │  (Pinpoint subsystem: Canvas, Engine, Compose, Haptics, DataStore)
└────────────────┬────────────────┘
                 │
                 ▼
┌─────────────────────────────────┐
│ 3. Severity Classification      │
│    ├─ Critical / Major          │ ──► Immediate blocking hotfix or blocking release gate
│    └─ Friction / Aesthetic      │ ──► Scheduled for post-beta hardening pass
└────────────────┬────────────────┘
                 │
                 ▼
┌─────────────────────────────────┐
│ 4. Work Package Conversion      │  (Formulate WP-60+ with verifiable acceptance criteria for v1.0.0)
└─────────────────────────────────┘
```

1. **Step 1 (Receipt & Verification):** The QA Specialist checks the report against existing items in `local-notes/active/outstanding_issues_notepad.md` to avoid duplication, then replicates the issue on physical hardware or the `Pixel_9_Pro_Fold` emulator.
2. **Step 2 (RCA):** The Lead Engineer identifies the exact file, class, and method responsible (e.g. `PointerInputScope.detectDragGestures`, `ArraySortingCanvasRenderer.kt`, `ActiveSessionSerializer.kt`).
3. **Step 3 (Classification):** Categorize defect severity and determine whether it blocks the testing flight or feeds the post-beta hardening milestone.
4. **Step 4 (Work Package Authoring):** Convert verified defects into isolated engineering work packages (`WP-60+`) following standard project sprint governance (granular commits, verifiable quality gates, PR reviews).
