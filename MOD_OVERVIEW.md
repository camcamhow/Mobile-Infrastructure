# Mobile Cell Network - Mod Overview

NeoForge 1.21.1 (21.1.255) mod. Mod ID `mobilecellnetwork`, version 1.0.0, MIT license.
This document describes what the mod currently does. It is updated whenever the mod changes.

_Last updated: after Batch 9 packet, admin-command, roaming, and setup-flow wiring._

---

## 1. Concept

Players build cell towers (antennas) that give nearby players a phone signal. A player carries a
cellphone item that opens an in-game smartphone UI (apps for calling, texting, maps, banking,
social posts, camera, and so on). Signal, plans, billing and roaming are simulated on the server.

---

## 2. Blocks

| Block | Purpose |
|---|---|
| `copper_antenna` (tier 1), `iron_antenna` (tier 2), `diamond_antenna` (tier 3) | Cell towers. Higher tiers give more range and a higher maximum signal tier. |
| `server_block` | Admin/network-management GUI (networks, plans, antennas, players). |
| `atm_block` | Banking access. |
| `shop_terminal_block` | Shop terminal. |
| `phone_charger` | Recharges a phone. |

Antennas have an owner network, an enabled flag, a power state (normal / low power / unpowered),
a roaming policy, and mesh links to other antennas on the same network.

## 3. Items

- **Cellphone** - stack size 1. Holds a charge (`mobilecellnetwork:phone_charge` data component, 0-10000).
  Right-click opens the phone.
- **Bank card** - banking item.
- A creative tab "Mobile Cell Network" holds all blocks and items.

---

## 4. Signal and coverage

- Every second the server finds the strongest usable tower in range for each player who carries a phone.
- Signal strength depends on distance, the tower's range and a line-of-sight check. Blocks along the
  line reduce strength according to the signal attenuation block tags
  (`none`, `low`, `medium`, `high`, `blocking`).
- Strength maps to a tier: E, E+, 2G, H, H+, 3G, LTE, 4G, 4G+, 5G, 5G+. The tier is capped by the
  tower's tier, the player's plan, Low Power Mode and the tower's power state.
- Signal bars (0-4) are derived from strength and tier.
- Network labels:
  - On the phone's own network the label is the tower's network name.
  - On another network's tower it shows `<home network> (Roaming)`. The home network is the plan's
    carrier, or, if the plan has no carrier, the first network the phone connected to (saved with the phone data).
- **Cell Tower selection:** Settings -> Cell Tower lists towers in range (name, tier, strength, roaming tag).
  The player can pick Automatic (strongest) or pin a specific tower. If the pinned tower goes out of
  range, the phone falls back to Automatic. The choice is not saved across logout.
- **Mesh sharing** (`allowMeshSharing`): antennas on the same network within mesh range store links to
  each other. The links are currently only stored; nothing else reads them yet.

## 5. Roaming and billing

- A tower from another network can be used only when the plan allows roaming and both networks have an
  active roaming agreement. Network owners can propose, accept, reject, counter, and revoke agreements.
- Roaming agreements carry separate per-minute, per-text, and per-MB rates. Charges are deducted from
  the player's bank account and credited to the visited network's revenue balance.
- Plans define price, billing cycle days, call minutes, texts, data allowance, maximum signal tier,
  carrier network, roaming rules and optional overage rates. Disabled overage enforces hard service limits.
- Currency displays use the configurable currency symbol and names via `CurrencyUtil`.

---

## 6. The phone UI

Phone frame with status bar (provider, time, signal bars + tier label such as `5G+`, battery),
Back and Home buttons, and a home screen with an app grid. The bottom of the home screen has a working
app search box. The phone has two styles (Samsung One UI and iOS). The phone reopens the last app used
before it was closed (kept in memory only; Home clears it; falls back to Home when the phone is dead or
the app is not allowed in Low Power Mode).

### Apps

| App | What it does |
|---|---|
| **Phone** | Dialer with Keypad / Recents / Contacts / Voicemail tabs. Calling opens a call screen. |
| **Call screen** | Mute, speaker (visual only) and End buttons, call timer. Unknown numbers play a "not in service" tone. If the callee is offline or unavailable (or declines / times out) the caller can leave a voicemail. |
| **SMS** | Direct and group chats, unread tracking, uses the plan's text allowance. |
| **Contacts** | Add/edit/delete contacts. |
| **Voicemail** | List, call back and delete voicemails. Voicemails for offline players are queued and delivered on login. |
| **Map** | Terrain map with height shading, points of interest, player marker, panning/zooming, POI sharing. Needs a plan with sufficient data tier. |
| **Store** | Install optional apps. |
| **Socials** | Profiles, posts with images, likes, following, people search. |
| **Bank** | Account balance, transfers and transaction history. |
| **Plan** | Current plan, usage this cycle, change plan. |
| **Settings** | Low Power Mode, ringtone and tone, notification sounds, Do Not Disturb, phone style, per-app notifications, Cell Tower selection. |
| **Camera** | Live first-person viewfinder; the player can still move and look; saves photos. |
| **Gallery** | View and import photos. |
| **Flashlight** | Toggle. While on, light blocks are placed in front of the player (about 7 blocks) and the battery drains faster (`flashlightDrainMultiplier`, default 4x). Turns off when the battery dies, in Low Power Mode, or on logout. |

### Battery and Low Power Mode

- The charge drains continuously (`phoneDrainPerSecond`). A dead phone shows "Phone Off" and can't be used.
- Low Power Mode reduces drain, caps signal tier, and limits apps to the configured allow-list.
- The server pushes the charge to the client regularly, so the UI battery updates live.
- A prompt offers Low Power Mode when the battery falls below a configured threshold.

### On-screen status HUD

When a phone is in the player's inventory or offhand and no screen is open, a one-line bar is shown above
the hotbar: network label, signal bars, tier and strength %, battery icon and %, and time of day.
It is hidden while a screen is open, with F1 or with the F3 debug screen.

---

## 7. Calls and voicemail

- Calls go through `CallManager`, with proximity-independent voice routing via `VoiceManager`.
- Failure reasons: not in service (number doesn't exist), unavailable (offline, dead phone, low power,
  busy, no service, no minutes), declined, timeout, caller-side failure.
- Call history (recents) is stored per phone, up to 200 entries (incoming, outgoing, missed).
- Voicemail notes can be left by the caller; offline recipients get it when they next log in.

## 8. Admin tools

- **Server block GUI** for managing networks, roaming proposals and agreements, antennas, plans, players,
  billing, and the emergency dispatch console. Placing a server block opens its management screen; an
  unprotected assigned network prompts for a name and password.
- **Commands** (permission level 2): `/mcn signal`, `/mcn reload`, `/mcn chatlock`,
  `/mcn plan`, `/mcn network`, `/mcn antenna`, `/mcn roaming`, `/mcn emergency`,
  `/mcn billing`, and `/mcn player` management commands, plus `/mobilecellnumber view|set`.

## 9. Configuration (selected options)

`allowMeshSharing`, roaming default rates and proposal behavior, serverless-network policy,
minimum network-password length, roaming notification cooldown, emergency-message length,
currency symbol/name/plural, `phoneDrainPerSecond`, `lowPowerDrainMultiplier`,
`flashlightDrainMultiplier`, `lowPowerAllowedApps`, signal thresholds, antenna ranges (copper / iron / diamond),
phone max charge, debug signal info, and more in `MobileCellNetworkConfig`.

## 10. Project layout

Package `com.example.mobilecellnetwork`: `blockentity`, `block`, `capabilities`, `chat`, `command`,
`config`, `data`, `event`, `gui` (+ `gui.map`), `item`, `network` (packets, caches, coverage engine),
`plans`, `billing`, `registry`, `voice`.
Build with `.\gradlew.bat build`. The jar is `build\libs\mobilecellnetwork-1.0.0.jar`.

## 11. Known limits

- Speaker button is visual only.
- Mesh links are stored but have no gameplay effect yet.
- Selected tower, last-opened app and home-network fallback are not all persisted (tower choice and last app are session-only).
- Recent features (call screen, camera movement, flashlight, tower selection, status HUD) are built but have
  not all been tested in-game.

## Roaming overhaul

**Batches 1-8:** Added persistent network/roaming data, plan selection and no-service gating, billing and overage enforcement, server and antenna administration, and emergency messaging/dispatch.


**Batch 2 (server-side logic, no GUI/packets yet):**
**Batch 3 (plan app redesign + server handler):**# Mobile Cell Network - Mod Overview

NeoForge 1.21.1 (21.1.255) mod. Mod ID `mobilecellnetwork`, version 1.0.0, MIT license.
This document describes what the mod currently does. It is updated whenever the mod changes.

_Last updated: after the on-screen status bar (HUD) and tower-selection / roaming-label changes._

---

## 1. Concept

Players build cell towers (antennas) that give nearby players a phone signal. A player carries a
cellphone item that opens an in-game smartphone UI (apps for calling, texting, maps, banking,
social posts, camera, and so on). Signal, plans, billing and roaming are simulated on the server.

---

## 2. Blocks

| Block | Purpose |
|---|---|
| `copper_antenna` (tier 1), `iron_antenna` (tier 2), `diamond_antenna` (tier 3) | Cell towers. Higher tiers give more range and a higher maximum signal tier. |
| `server_block` | Admin/network-management GUI (networks, plans, antennas, players). |
| `atm_block` | Banking access. |
| `shop_terminal_block` | Shop terminal. |
| `phone_charger` | Recharges a phone. |

Antennas have an owner network, an enabled flag, a power state (normal / low power / unpowered),
a roaming policy, and mesh links to other antennas on the same network.

## 3. Items

- **Cellphone** - stack size 1. Holds a charge (`mobilecellnetwork:phone_charge` data component, 0-10000).
  Right-click opens the phone.
- **Bank card** - banking item.
- A creative tab "Mobile Cell Network" holds all blocks and items.

---

## 4. Signal and coverage

- Every second the server finds the strongest usable tower in range for each player who carries a phone.
- Signal strength depends on distance, the tower's range and a line-of-sight check. Blocks along the
  line reduce strength according to the signal attenuation block tags
  (`none`, `low`, `medium`, `high`, `blocking`).
- Strength maps to a tier: E, E+, 2G, H, H+, 3G, LTE, 4G, 4G+, 5G, 5G+. The tier is capped by the
  tower's tier, the player's plan, Low Power Mode and the tower's power state.
- Signal bars (0-4) are derived from strength and tier.
- Network labels:
  - On the phone's own network the label is the tower's network name.
  - On another network's tower it shows `<home network> (Roaming)`. The home network is the plan's
    carrier, or, if the plan has no carrier, the first network the phone connected to (saved with the phone data).
- **Cell Tower selection:** Settings -> Cell Tower lists towers in range (name, tier, strength, roaming tag).
  The player can pick Automatic (strongest) or pin a specific tower. If the pinned tower goes out of
  range, the phone falls back to Automatic. The choice is not saved across logout.
- **Mesh sharing** (`allowMeshSharing`): antennas on the same network within mesh range store links to
  each other. The links are currently only stored; nothing else reads them yet.

## 5. Roaming and billing

- A tower from a different network can be used only if the plan allows roaming and both networks have
  approved a matching roaming agreement (same fee, equal to the plan's `roamingFeePerMinute`).
- Roaming fees are charged for roaming calls (per minute) and roaming data (per MB), when
  `roamingFeeEnabled` is on. Fees go to the tower owner network as revenue. Texts are not charged separately.
- Plans define price, billing cycle days, call minutes, texts, data allowance, maximum signal tier,
  carrier network, allowed networks and roaming fees. Plan usage is tracked per player and billed per cycle.

---

## 6. The phone UI

Phone frame with status bar (provider, time, signal bars + tier label such as `5G+`, battery),
Back and Home buttons, and a home screen with an app grid. The bottom of the home screen has a working
app search box. The phone has two styles (Samsung One UI and iOS). The phone reopens the last app used
before it was closed (kept in memory only; Home clears it; falls back to Home when the phone is dead or
the app is not allowed in Low Power Mode).

### Apps

| App | What it does |
|---|---|
| **Phone** | Dialer with Keypad / Recents / Contacts / Voicemail tabs. Calling opens a call screen. |
| **Call screen** | Mute, speaker (visual only) and End buttons, call timer. Unknown numbers play a "not in service" tone. If the callee is offline or unavailable (or declines / times out) the caller can leave a voicemail. |
| **SMS** | Direct and group chats, unread tracking, uses the plan's text allowance. |
| **Contacts** | Add/edit/delete contacts. |
| **Voicemail** | List, call back and delete voicemails. Voicemails for offline players are queued and delivered on login. |
| **Map** | Terrain map with height shading, points of interest, player marker, panning/zooming, POI sharing. Needs a plan with sufficient data tier. |
| **Store** | Install optional apps. |
| **Socials** | Profiles, posts with images, likes, following, people search. |
| **Bank** | Account balance, transfers and transaction history. |
| **Plan** | Current plan, usage this cycle, change plan. |
| **Settings** | Low Power Mode, ringtone and tone, notification sounds, Do Not Disturb, phone style, per-app notifications, Cell Tower selection. |
| **Camera** | Live first-person viewfinder; the player can still move and look; saves photos. |
| **Gallery** | View and import photos. |
| **Flashlight** | Toggle. While on, light blocks are placed in front of the player (about 7 blocks) and the battery drains faster (`flashlightDrainMultiplier`, default 4x). Turns off when the battery dies, in Low Power Mode, or on logout. |

### Battery and Low Power Mode

- The charge drains continuously (`phoneDrainPerSecond`). A dead phone shows "Phone Off" and can't be used.
- Low Power Mode reduces drain, caps signal tier, and limits apps to the configured allow-list.
- The server pushes the charge to the client regularly, so the UI battery updates live.
- A prompt offers Low Power Mode when the battery falls below a configured threshold.

### On-screen status HUD

When a phone is in the player's inventory or offhand and no screen is open, a one-line bar is shown above
the hotbar: network label, signal bars, tier and strength %, battery icon and %, and time of day.
It is hidden while a screen is open, with F1 or with the F3 debug screen.

---

## 7. Calls and voicemail

- Calls go through `CallManager`, with proximity-independent voice routing via `VoiceManager`.
- Failure reasons: not in service (number doesn't exist), unavailable (offline, dead phone, low power,
  busy, no service, no minutes), declined, timeout, caller-side failure.
- Call history (recents) is stored per phone, up to 200 entries (incoming, outgoing, missed).
- Voicemail notes can be left by the caller; offline recipients get it when they next log in.

## 8. Admin tools

- **Server block GUI** for managing networks, plans, antennas and players.
- **Commands** (permission level 2): `/mcn signal here`, `/mcn reload`, `/mcn chatlock`,
  `/mcn plan list|assign|reload|create|delete`, `/mcn network list|create|delete|allow|disallow|owner|roaming`,
  `/mcn antenna list|state|owner`, `/mcn billing force|history`,
  `/mcn player list|number|plan|lowpower|charge`, balance commands (`add|remove|set`),
  and `/mobilecellnumber view|set`.

## 9. Configuration (selected options)

`allowMeshSharing`, `roamingFeeEnabled`, `phoneDrainPerSecond`, `lowPowerDrainMultiplier`,
`flashlightDrainMultiplier`, `lowPowerAllowedApps`, signal thresholds, antenna ranges (copper / iron / diamond),
phone max charge, debug signal info, and more in `MobileCellNetworkConfig`.

## 10. Project layout

Package `com.example.mobilecellnetwork`: `blockentity`, `block`, `capabilities`, `chat`, `command`,
`config`, `data`, `event`, `gui` (+ `gui.map`), `item`, `network` (packets, caches, coverage engine),
`plans`, `billing`, `registry`, `voice`.
Build with `.\gradlew.bat build`. The jar is `build\libs\mobilecellnetwork-1.0.0.jar`.

## 11. Known limits

- Speaker button is visual only.
- Mesh links are stored but have no gameplay effect yet.
- Selected tower, last-opened app and home-network fallback are not all persisted (tower choice and last app are session-only).
- Recent features (call screen, camera movement, flashlight, tower selection, status HUD) are built but have
  not all been tested in-game.

## Roaming overhaul (in progress)

**Batch 1 (data model only, no behaviour change yet):** per-network settings (`NetworkSettings`: salted password hash, server block position, online flag, allowRoaming, default roaming rates 5/1/1 per minute/text/MB, auto-send requests, roaming agreements, outgoing/incoming proposals) saved in `NetworkSavedData`; `RoamingAgreement`, `RoamingProposal`, `ProposalState`; phone tower preference (`TowerPreferenceMode` AUTOMATIC_FASTEST/AUTOMATIC_PRIMARY/PINNED + pinned tower) and `PhoneData.hasActivePlan()`. The old per-minute fee map is still what roaming uses until Batch 5.


**Batch 2 (server-side logic, no GUI/packets yet):**
**Batch 3 (plan app redesign + server handler):**

**Batch 4 (no-service gating):** Client-side gating added to network-dependent screens: PhoneScreen, CallScreen, SmsScreen, ContactsScreen, MapScreen, StoreScreen, SocialsScreen, VoicemailScreen. Each screen checks ClientPhoneCache.planId().isBlank() at the top of render() and shows NoServiceOverlay if no active plan and screen is not exempt. Exempt screens (PlanScreen, SettingsScreen, CameraScreen, GalleryScreen, FlashlightScreen) skip the gating check. BankingScreen remains viewable with balance/transactions rendered but Transfer button and action buttons disabled. Server-side gating added to CallManager, DataUsageService, and SmsService with FAILURE_REASON_NO_PLAN return code. PhoneRenderer.drawStatusBar updated to hide signal bars + tier label when hasActivePlan() is false and show "No Service" in error color. Emergency thread in SMS app is always accessible as placeholder conversation named "Emergency" at top of list, blocking sending with "coming soon" toast.

**Batch 6 (currency, roaming and overage charging):** Added config-backed currency symbol/name values and `CurrencyUtil`; added active-plan `PlanUsage.isOver`/`getOverage`; charges are computed incrementally and deducted for call minutes, SMS, roaming, and data overage. Disabled overage now blocks calls, SMS (including global), and data at their plan limits. Roaming charges update the saved visited-network revenue balance and continue to use the configured host/carrier payout split.

**Batch 7 (server management and antenna sharing):** The Server block GUI has an operator Roaming tab showing each network's policy, default rates, agreements count, and saved revenue balance, with per-network rate settings and confirmed revenue reset actions. Network password changes use the masked password modal, minimum length validation, salted hash storage, and owner/operator checks. Antenna management now supports password-protected network claims, owner/operator unclaim, network selection for sharing/unsharing, and displays shared networks. Admin action logging omits password and claim credentials. Full Gradle build succeeds.

**Batch 8 (emergency messaging and dispatch):** Added a pinned Emergency conversation that remains accessible without an active plan, with the phone's No Service view providing a direct route to it. Emergency messages bypass regular plan, service and text-allowance checks, are stored in `NetworkSavedData` with sender and location details, and appear in the admin-only Emergency Dispatch screen. Admin emergency broadcasts are recorded in the dispatch history as well. The `emergency_dispatch` block can only be placed and used by operators; the block entity itself has no additional saved state.

**Batch 9:** Added typed payloads and handlers for network passwords, server reconnection, antenna ownership/sharing, roaming proposals and agreements, plan selection/cancellation, tower preferences, and emergency messages. Added `/mcn` password/server assignment, antenna force-claim/share, roaming, and emergency list/archive administration. Roaming defaults, automatic proposals, notification cooldowns, serverless-network behavior, emergency-message length, and currency formatting are configurable. Server-block placement opens its management screen and prompts for a name and password when the assigned network has no password.
