# Unravel — Human Testing Flight Manual & Hands-On Audit Protocol

> **Document Version:** 1.0.0  
> **Release Target:** `v0.6.0-beta.0` (Beta Distribution for Human Testing)  
> **Package Name:** `com.unravel.app`  
> **Target Audience:** Internal QA Engineers, DSA Educators, Tactile UX Specialists, Android Device Testers  
> **Core Philosophy:** *"Show, Don't Tell"* — Zero passive prose, 100% direct manipulation, 60/120 FPS tactile interaction, physical haptic choreography.

---

## 1. Executive Overview

Welcome to the **Unravel Beta (`v0.6.0-beta.0`) Flight Test**.

Unravel is an interactive Android DSA learning laboratory engineered from the ground up to replace static textbooks, pseudo-code bullet points, and passive video lectures with **direct touch manipulation physics**. Every data structure is a living machine; every algorithm is an active physical transformation you can touch, stretch, scrub, and race.

### Purpose of this Beta Audit
Automated unit tests (248+ tests) and automated emulator instrumentation verify bytecode stability, R8 full-mode shrinking, ProGuard keep rules, and algorithm invariants. However, **tactile feel, gesture friction, haptic feedback cadence, gesture disambiguation, and frame timing** require real human hands on physical silicon.

As a flight tester, your mission is to evaluate:
1. **Mechanical Responsiveness:** Does touching an array bar, dragging a tree node, or painting a grid wall feel immediate, physical, and predictable?
2. **Gesture Disambiguation:** Does 2D panning interfere with drag-to-swap? Does velocity scrubbing conflict with system edge navigation?
3. **Haptic Choreography:** Do tick, click, and buzz haptics feel crisp, distinct, and synchronized with visual events without fatiguing the hand?
4. **Visual Telemetry & Ergonomics:** Is the UI legible, high-contrast, and free of clutter against the obsidian void (`#0B0E14`) on OLED and high-refresh-rate displays?
5. **Form Factor Adaptability:** How gracefully does the layout adapt to standard smartphones, high-refresh-rate flagships, and foldable form factors (e.g. Pixel Fold / Pixel 9 Pro Fold)?

---

## 2. Pre-Flight Sideloading Instructions

### 2.1 Artifact Metadata & Integrity
Verify the release package prior to sideloading:

| Property | Value |
| :--- | :--- |
| **Package File** | `release-artifacts/unravel-v0.6.0-beta.0.apk` |
| **Package Size** | `2,249,041 bytes` (~2.14 MB) |
| **SHA-256 Hash** | `5e4372b888f90d0abeb848730811bee024d1e897176590ec7505d04ce8861fef` |
| **Version Code / Name** | `versionCode 6` / `versionName 0.6.0-beta.0` |
| **Min SDK / Compile SDK**| Android 8.0 (API 26) / Android 16 (API 36) |
| **Target Signature** | v1 + v2 + v3 deterministic release signature (`unravelBeta`) |

### 2.2 Sideload via Android Debug Bridge (ADB)
Connect your physical device or boot an emulator with USB debugging enabled.

1. **Verify ADB Connection:**
   ```bash
   adb devices
   ```
   *Expected output: Your device serial listed as `device`.*

2. **Uninstall Prior Debug Builds (if applicable):**
   *Note: Android enforces signature compatibility. If an earlier debug-signed build is installed, uninstall it first:*
   ```bash
   adb uninstall com.unravel.app
   ```

3. **Install the Signed Beta APK:**
   ```bash
   adb install -r release-artifacts/unravel-v0.6.0-beta.0.apk
   ```
   *Expected output: `Success`.*

4. **Verify SHA-256 Checksum (Optional Pre-Flight Check):**
   *PowerShell:*
   ```powershell
   (Get-FileHash -Algorithm SHA256 release-artifacts/unravel-v0.6.0-beta.0.apk).Hash -eq (Get-Content release-artifacts/unravel-v0.6.0-beta.0.apk.sha256).Trim()
   ```
   *Bash:*
   ```bash
   sha256sum -c release-artifacts/unravel-v0.6.0-beta.0.apk.sha256
   ```

5. **Launch the Application via Intent:**
   ```bash
   adb shell am start -n com.unravel.app/com.unravel.app.MainActivity
   ```

### 2.3 Direct Sideloading on Physical Device (Non-ADB)
1. Transfer `release-artifacts/unravel-v0.6.0-beta.0.apk` to your device storage via USB cable, Google Drive, or local transfer.
2. Open your device **Files** app and tap `unravel-v0.6.0-beta.0.apk`.
3. If prompted: **Settings $\to$ Allow from this source** (Enable sideloading permission for your file manager or browser).
4. Tap **Install** and open **Unravel**.

---

## 3. Flight Test Missions (Tactile Audit Protocols)

Perform each mission sequentially on your testing device. Score each mission against the tactile rubric provided in Section 4.

---

### Mission 1: The Infinite DAG Curriculum
**Target Module:** Skill Map / Curriculum DAG  
**Focus Area:** 2D canvas navigation, inertial boundary physics, cluster legibility, and state persistence.

#### Test Protocol:
1. **Fluid 2D Panning:**
   - Place one finger on the obsidian canvas and pan smoothly in all 4 cardinal directions and diagonals.
   - *Audit Check:* Canvas translation must follow finger touch 1:1 with zero jank or frame hitching (solid 60/120 FPS).
2. **Multi-Touch Zoom:**
   - Pinch with two fingers to zoom out to minimum bounds ($0.5\times$) and zoom in to maximum magnification ($2.0\times$).
   - *Audit Check:* Node labels and spline paths must remain sharp, anti-aliased, and geometrically aligned during scaling.
3. **Inertial Rubber-Banding:**
   - Fling the canvas aggressively past its boundary edges.
   - *Audit Check:* Canvas must exhibit an elastic resistance curve (rubber-banding) and snap back smoothly to boundary limits without clipping or visual tearing.
4. **Cluster Headers & Spline Transitions:**
   - Observe cluster group boundaries (Sorting, Trees, Graphs, Dynamic Programming).
   - Trace the cubic Bézier curves connecting prerequisite nodes.
   - *Audit Check:* Active unlock paths must glow with an energetic gradient; locked prerequisites must remain subtly dimmed without obstructing background grid geometry.
5. **"Jump Back In" State Resume:**
   - Tap any module node (e.g. QuickSort).
   - Press the Android Home button or swipe to switch apps.
   - Relaunch Unravel.
   - *Audit Check:* The bottom "Jump Back In" telemetry card must accurately display your last active algorithm and restore execution state instantly from Proto DataStore.

---

### Mission 2: Tactile Array & Sorting Mechanics
**Target Module:** Sorting Sandbox (BubbleSort, QuickSort, MergeSort)  
**Focus Area:** Direct touch bar modification, drag-to-swap Bézier trajectories, sliding window overlays, and scrubbing timeline.

#### Test Protocol:
1. **Drag-Height Bar Resizing:**
   - In an unsorted array view, place your thumb directly on an individual vertical bar.
   - Drag vertically upward and downward to manually resize the bar's numerical value.
   - *Audit Check:* Bar height must track vertical touch coordinate continuously. Haptic tick must trigger on crossing height increments.
2. **Drag-to-Swap Parabolic Bézier Motion:**
   - Touch and hold an array bar, then drag horizontally toward an adjacent bar.
   - *Audit Check:* Selected bar must elevate slightly along a parabolic Bézier curve, casting a luminous shadow over lower bars. Upon release over a destination index, the two elements must swap places with a tactile "snap" sound/vibration and fluid spring transition.
3. **Sliding Window Overlay:**
   - Select a partitioned algorithm (e.g. QuickSort partition window or two-pointer partition).
   - Observe the translucent glass highlight bracket surrounding active pivot and compare windows.
   - *Audit Check:* Glass overlay must refract underlying bars with crisp boundary markers and zero artifacting.
4. **Variable Speed Scrubbing ($0.25\times \dots 4.0\times$):**
   - Tap Play to run the algorithm animation.
   - Grab the playback timeline scrubber slider at the bottom.
   - Scrub backward and forward across algorithm history.
   - Adjust speed toggle from $0.25\times \to 0.5\times \to 1.0\times \to 2.0\times \to 4.0\times$.
   - *Audit Check:* Animation steps must scrub forward and backward deterministically. Step counters, pointer markers (`L`, `R`, `pivot`), and code highlight line indicators must stay 100% synchronized with the array state.

---

### Mission 3: Non-Linear Machine Manipulation
**Target Module:** Tree & Graph Sandbox (Binary Search Tree, Floyd Cycle, Grid Pathfinding)  
**Focus Area:** Non-linear canvas topologies, orbital rotations, wall painting, and shortest path tightening.

#### Test Protocol:
1. **Floyd Cycle Detection Orbital Ring:**
   - Open Linked List Cycle module.
   - Observe the loop topology rendered as an orbital ring.
   - Step through the fast (`2x`) and slow (`1x`) pointers.
   - *Audit Check:* Pointers must glide around the orbital track with smooth angular velocity. The moment of collision (meeting point) must trigger a distinct haptic pulse.
2. **AVL Curved Subtree Rotation:**
   - In the BST / AVL module, insert a sequence of nodes that triggers an unbalance condition (e.g. `10 -> 20 -> 30` for Left Rotation, or `30 -> 10 -> 20` for Right-Left Rotation).
   - *Audit Check:* Watch the tree rebalance. Subtrees must swing along curved radial arcs rather than jumping linearly. Node connector lines must stretch and contract elastically.
3. **2D Grid Wall Painting:**
   - Open Dijkstra / A* Pathfinding module.
   - Drag your finger across the 2D grid matrix in continuous strokes.
   - *Audit Check:* Touched grid cells must toggle into impenetrable obstacle walls instantly with zero dropped touches. Swiping back over a wall must erase it.
4. **Elastic Path Tightening:**
   - Place start node $S$ and target node $T$ on opposite sides of the painted obstacle wall.
   - Press Run / Step.
   - *Audit Check:* Visited wavefront cells must ripple outward with matrix emerald (`#00FF9D`) glow. Once the target is reached, the resolved shortest path line must tighten elastically like a pulled ribbon around wall corners.

---

### Mission 4: "Be the Algorithm" Challenge Mode
**Target Module:** Challenge Mode (Predictive Decision Mini-Games)  
**Focus Area:** Touch decision gestures, haptic confirm/buzz sensation, streak counters, and symmetric swap tolerance.

#### Test Protocol:
1. **Decision Pause Points:**
   - Launch Challenge Mode on QuickSort or Two Pointers.
   - The engine automatically plays forward and halts at a critical branching point (e.g. *"Should elements at index 3 and index 7 swap?"*).
   - *Audit Check:* UI must clearly highlight the decision target without explanatory wall-of-text prompts.
2. **Touch Gesture Input:**
   - Execute the action manually via gesture:
     - Drag to swap the two target bars.
     - Or tap the **No Swap / Advance** action button.
   - *Audit Check:* Input response must be immediate (< 16 ms latency).
3. **Haptic Feedback Differentiation:**
   - **Correct Prediction:** Execute the correct algorithm action.
     - *Audit Check:* Crisp affirmative haptic click (`HapticFeedbackType.Confirm`) accompanied by a Matrix emerald bloom (`#00FF9D`).
   - **Incorrect Prediction:** Deliberately execute an invalid move.
     - *Audit Check:* Heavy tactile rejection buzz (`HapticFeedbackType.Reject`) accompanied by a subtle alert coral shake (`#FF3366`).
4. **Streak Multiplier Telemetry:**
   - Complete 3 correct predictions in succession.
   - *Audit Check:* The kinetic score ticker must increment with an accelerating multiplier badge (`2x`, `3x`, `5x`) rendered in neon monospace font.
5. **Symmetric Swap Tolerance:**
   - In operations where swapping order does not matter (e.g. symmetric pair selection), drag element $A \to B$ vs $B \to A$.
   - *Audit Check:* Both directional gestures must be recognized as equivalent correct actions.

---

### Mission 5: Split-Screen Race Mode & Foldable Posture
**Target Module:** Dual Engine Race Mode  
**Focus Area:** Parallel simulation determinism, operation delta HUD, Glass Prism divider, and foldable posture adaptation.

#### Test Protocol:
1. **Synchronized Parallel Execution:**
   - Launch Race Mode (e.g. QuickSort vs MergeSort on identical 20-element random seed).
   - Press Start.
   - *Audit Check:* Both engines must tick simultaneously with independent, non-blocking render loops. Neither engine may cause frame drops in the companion viewport.
2. **Operation Delta HUD Badges:**
   - Observe the live telemetry HUD at the top/center of the screen.
   - Compare Operation Counter (`CMP` comparison delta, `MUT` array mutation delta).
   - *Audit Check:* The leading algorithm must be dynamically highlighted with Matrix emerald; the trailing algorithm must display a real-time lag badge (e.g. `+14 CMP`).
3. **Glass Prism Refraction Divider:**
   - Locate the central frosted glass divider separating the two algorithm panes.
   - Touch and drag the divider handle across the screen.
   - *Audit Check:* Viewport sizes must scale smoothly in real time. Canvas graphics must clip cleanly at the divider boundary without overlapping or artifacting.
4. **Orientation & Foldable Posture Adaptation:**
   - Rotate the device between Portrait and Landscape.
   - *On Foldables (Pixel Fold, Galaxy Fold):* Open device from outer cover screen to inner unfolded tablet screen; fold half-way into tabletop/flex posture.
   - *Audit Check:*
     - Portrait: Top/Bottom split-screen stack.
     - Landscape / Unfolded: Left/Right dual-pane workstation.
     - State, elapsed steps, and random seed must persist seamlessly across orientation changes with zero reload flickers.

---

### Mission 6: Dark Mode Aesthetics & Telemetry Ergonomics
**Target Module:** Global System UI  
**Focus Area:** Visual hierarchy, typography, contrast ratios, and absence of visual clutter.

#### Test Protocol:
1. **Obsidian Void Contrast (`#0B0E14`):**
   - Inspect background deep-space canvas in low-ambient lighting conditions.
   - *Audit Check:* Canvas base `#0B0E14` must provide true deep black immersion without washed-out gray backplates.
2. **Neon Palette Contrast Hierarchy:**
   - Verify readability of key telemetry colors:
     - Neon Cyan (`#00F0FF`): Primary interactive pointers and active focus nodes.
     - Matrix Emerald (`#00FF9D`): Completed operations, sorted elements, and affirmative states.
     - Warning Amber (`#FFB800`): Pivot candidates, comparison windows, and threshold alerts.
     - Alert Coral (`#FF3366`): Partition bounds, barrier walls, and collision markers.
   - *Audit Check:* Text and numeric badges must maintain high legibility (compliant with WCAG AAA / APCA readability) against the dark background.
3. **Monospace Telemetry Alignment:**
   - Inspect step numbers, Big-O complexity badges ($O(n \log n)$), memory telemetry, and code highlight lines.
   - *Audit Check:* Monospace typography (JetBrains Mono) must maintain strict tabular figure alignment without jittering when digit values increase.
4. **Absence of Decorative Fluff:**
   - Inspect all screens for unnecessary illustrations, filler prose, or cartoon mascots.
   - *Audit Check:* The UI must strictly embody a high-precision, tactile scientific instrument.

---

## 4. Tactile Quality Scorecard & Rubric

Evaluate your overall testing session against the following 5-point mechanical rubric:

| Score | Category | Target Standard | Observed Quality |
| :---: | :--- | :--- | :--- |
| **[ /5]** | **1. Touch Latency & Tracking** | Direct 1:1 finger tracking, < 16ms gesture response, zero drag latency. | |
| **[ /5]** | **2. Framerate Stability** | Consistent 60 FPS (or 120 FPS on high-refresh displays), zero micro-stuttering during concurrent animations. | |
| **[ /5]** | **3. Haptic Precision** | Crisp tactile ticks and distinct rejection buzzes; no hollow or delayed vibration motor firing. | |
| **[ /5]** | **4. Gesture Disambiguation**| Clean separation of canvas pan vs element drag; no unintended gesture triggering. | |
| **[ /5]** | **5. Visual Precision** | Crisp vector splines, high-contrast monospace telemetry, zero clipping at display cutouts/folds. | |

---

## 5. Defect Reporting & Feedback Intake Protocol

If you encounter any tactile friction, gesture ambiguity, animation defect, or visual glitch during your flight test:

1. **Log Entry Location:** Record your findings in the beta defect intake holding pen:
   - Primary Repository Tracker: `docs/beta_feedback_intake.md`
   - Active Working Notepad: `local-notes/active/outstanding_issues_notepad.md` (`### Beta Human Testing Defect Intake`)
2. **Defect Template:** Use the standardized intake format:
   ```markdown
   #### [DEFECT-BETA-XXX] [Concise Title]
   - **Tester Device / Model:** e.g. Pixel 9 Pro Fold / Galaxy S24 / Pixel 7
   - **Android OS Version:** e.g. Android 15 (API 35) / Android 14 (API 34)
   - **Subsystem:** Skill Map / Array Canvas / Tree Canvas / Grid Canvas / Race Mode / Challenge Mode / Persistence
   - **Severity:** Critical (Blocks testing) / Major (Broken mechanic) / Friction (Tactile feel flaw) / Aesthetic (Visual polish)
   - **Observed Behavior:** What physically happened under touch manipulation.
   - **Expected Mechanical Behavior:** What the tactile mechanics should feel like.
   - **Reproduction Steps:**
     1. Open module ...
     2. Touch and drag ...
     3. Observe ...
   ```
3. **Post-Beta Triage:** All logged entries will be reviewed, prioritized, and converted into post-beta engineering work packages prior to the final production release.
