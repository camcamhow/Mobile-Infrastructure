# What Mobile Cell Network Does

**Mobile Cell Network** is a comprehensive telecommunications and in-game smartphone mod for **Minecraft NeoForge 1.21.1** (NeoForge 21.1.255). 

_Last updated: October 12, 2026 — migrated the phone home screen to shared UI primitives._

It simulates a full cellular infrastructure inside Minecraft—complete with customizable cell towers, realistic radio signal propagation, multi-carrier networks, paid roaming agreements, data-driven subscription plans, and an interactive in-game smartphone equipped with 14 functional apps.

---

## Table of Contents

1. [Core Concepts](#1-core-concepts)
2. [The In-Game Smartphone](#2-the-in-game-smartphone)
   - [Operating System & Interface](#operating-system--interface)
   - [Built-In Smartphone Apps](#built-in-smartphone-apps)
   - [Battery & Power Management](#battery--power-management)
3. [Blocks & Infrastructure](#3-blocks--infrastructure)
   - [Cell Towers (Antennas)](#cell-towers-antennas)
   - [Server Block (Network Management)](#server-block-network-management)
   - [Emergency Dispatch Console](#emergency-dispatch-console)
   - [Phone Charger](#phone-charger)
   - [Banking & Commercial Blocks](#banking--commercial-blocks)
4. [Signal Propagation & Coverage Engine](#4-signal-propagation--coverage-engine)
5. [Cellular Carriers & Multi-Network System](#5-cellular-carriers--multi-network-system)
6. [Roaming & Inter-Carrier Agreements](#6-roaming--inter-carrier-agreements)
7. [Subscription Plans & Billing](#7-subscription-plans--billing)
8. [Emergency SOS & First Responder Dispatch](#8-emergency-sos--first-responder-dispatch)
9. [Economy & Banking](#9-economy--banking)
10. [Administrator Commands & Configuration](#10-administrator-commands--configuration)

---

## 1. Core Concepts

In vanilla Minecraft, players communicate via standard global chat. Mobile Cell Network introduces a realistic cellular ecosystem:
- **Radio Infrastructure:** Players or server administrators construct cell towers to broadcast coverage.
- **Signal Attenuation:** Radio waves weaken over distance and are attenuated or blocked by intervening blocks (stone, obsidian, metal, glass, etc.).
- **Smartphones:** Carrying a cellphone grants access to calls, SMS, mapping, social networking, banking, and utilities.
- **Economy & Carriers:** Players can found cellular carriers, deploy towers, sell subscription plans, and negotiate inter-network roaming agreements for revenue.
- **Emergency System:** When stranded without a plan or in dead zones, players can send emergency SOS messages that broadcast coordinates to emergency dispatch stations.

---

## 2. The In-Game Smartphone

### Operating System & Interface
Right-clicking the **Cellphone** item opens a custom, responsive mobile OS:
- **Status Bar:** Real-time indicator displaying the current network carrier (or roaming indicator), in-game time, cellular tier badge (`E`, `E+`, `2G`, `3G`, `H`, `H+`, `4G`, `4G+`, `LTE`, `5G`, `5G+`), dynamic 4-bar signal strength that drops one bar at a time and refills when the tier drops, and battery percentage.
- **On-Screen HUD:** When the phone is held in the main hand or off-hand, a compact status bar overlay is visible directly on the player HUD above the hotbar.
- **OS Styles:** Switch between a dark **Samsung One UI-inspired** layout and **Snapple** in Settings.
- **Phone item:** The existing cellphone uses an original rounded 3D model with a screen, metallic sides and rear cameras. It must still be held normally; camera and flashlight do not automatically move it into a hand.
- **Navigation:** Functional Back button, Home button, and gesture indicator.
- **Home Screen:** Snapple uses the same 3D model as the held cellphone, a camera cutout, original wallpaper, date and network widgets, app grid, search, and a dock. The lock screen shares its wallpaper and has a styled PIN keypad. Existing apps retain their current layouts.
- **App Search:** Live filter bar on the home screen to quickly search installed apps.
- Lock screen with a swipe-up gesture from the bottom to unlock and optional PIN entry. Each physical phone has a unique ID that persists with the item, so finding another player's phone grants access to their messages, contacts, and (partially) their bank app.

### Built-In Smartphone Apps

| App | Description & Features |
|---|---|
| 📞 **Phone / Dialer** | Keypad for dialing numeric phone numbers, Recents tab (incoming, outgoing, missed call logs), Contacts tab, and Voicemail tab. |
| 📲 **Call Screen** | Active call interface with call duration timer, Mute, visual Speakerphone, and End Call buttons. Integrates with Simple Voice Chat when installed (creating temporary audio rooms), with an automatic fallback to real-time text-call mode on HUD. Handles busy tones, ringtones, and out-of-service alerts. |
| 💬 **SMS / Messages** | Direct peer-to-peer texting and group conversations. Shows read/unread badges. Offline recipients receive queued texts upon logging in. Features an always-pinned **Emergency (SOS)** channel. |
| 👥 **Contacts** | Address book to save, edit, and delete player contact names and numbers, with one-tap shortcuts to call or message. |
| 📼 **Voicemail** | Allows callers to leave voicemail notes when recipients are offline, busy, or decline a call. Supports reviewing, calling back, and deleting voicemails. |
| 🗺️ **Map** | GPS satellite navigation app featuring terrain elevation shading, player position tracking, zooming, panning, waypoint markers (POIs), and one-tap waypoint sharing over SMS. Toggleable signal strength heatmap overlay shows coverage as a color gradient (green = strong, red = weak, transparent = none). |
| 🏦 **Bank** | Mobile banking client displaying current account balance, linked bank card, direct funds transfer to other players, and full transaction history. |
| 📊 **Plan** | Subscription management screen. Displays active plan, carrier name, remaining minutes, texts, and data allowance (MB), cycle countdown, and overage billing. Includes a two-step network and plan chooser modal to subscribe or cancel plans. |
| ⚙️ **Settings** | Controls OS theme style (One UI vs iOS), Low Power Mode, ringtone and notification sounds, Do Not Disturb, per-app notifications, and manual **Cell Tower selection** (choose automatic best tower or pin a specific antenna). |
| 📷 **Camera** | Live first-person camera viewfinder allowing free camera movement and player motion while framing and snapping photos. |
| 🖼️ **Gallery** | Photo album viewing pictures captured with the Camera app. |
| 🔦 **Flashlight** | Illuminates the area ahead of the player (~7 blocks) using real-time dynamic light blocks. Discharges the battery faster while active. |
| 🛍️ **Store** | Digital app catalog for installing optional applications. |
| 🌐 **Socials** | In-game social media platform with user profiles, posts, photo uploads, likes, comments, user search, and follower feeds. |

### Battery & Power Management
- Phones possess an internal battery (0 to 10,000 charge units).
- The battery gradually discharges over time (`phoneDrainPerSecond`).
- **Low Power Mode:** Reduces drain by a configurable multiplier, caps maximum signal tier at `E+`, and restricts available apps to essential services (SMS, Map, Settings).
- **Phone Charger:** Placing the phone in the Phone Charger block recharges its battery using Forge Energy (FE).
- **Dead State:** When the battery reaches 0%, the phone shuts off and displays "Phone Off" until recharged.
- **Remote Device Management:** From any phone you own, you can lock the banking app, lock all apps, or wipe the data on any other phone you've previously used.

---

## 3. Blocks & Infrastructure

### Cell Towers (Antennas)
Antennas transmit cellular signal across a spherical coverage area:
- **Copper Antenna (Tier 1):** Short range, lower maximum data tier, economical craft.
- **Iron Antenna (Tier 2):** Medium range and balanced data speeds.
- **Diamond Antenna (Tier 3):** High power, maximum transmission radius, high bandwidth (`5G+`).

Antenna features include:
- **Power States:** Operational, Low Power, or Unpowered (when unsupplied with power or damaged).
- **Ownership & Security:** Antennas are claimed by player networks using secure, salted password authentication.
- **Antenna Sharing:** An antenna owner can share their physical tower with other carrier networks, allowing multiple providers to broadcast from a single mast.
- **Mesh Linking:** Antennas within line-of-sight on the same network establish mesh data links.
- Holding an antenna shows a wireframe preview of its coverage radius on the ground. Green indicates the target is within your own network's coverage; cyan indicates a new coverage area.

### Server Block (Network Management)
The **Server Block** serves as the central control terminal for telecommunication networks:
- **Setup Workflow:** Placing a Server Block prompts the network owner to name the network and set a secure administrator password.
- **Multi-Tab Admin GUI:**
  - **Networks:** View active networks, tower counts, online/offline status, allow/disallow networks, and reset passwords.
  - **Roaming:** Manage bilateral roaming agreements, inspect outgoing/incoming proposals, counter rate proposals, and monitor accumulated roaming revenue.
  - **Antennas:** Inspect registered towers, signal states, claiming status, and shared network lists.
  - **Players:** Manage subscriber numbers, view active plans, and adjust balances.
  - **Plans:** Inspect and create subscription plans.
  - **Billing:** View recent billing renewal logs and force billing cycles.
  - **Emergency Broadcasts:** Send emergency alerts across carrier networks.

### Emergency Dispatch Console
A dedicated console (`emergency_dispatch`) reserved for operators and emergency personnel (permission level 2):
- Receives all incoming SOS messages transmitted over the emergency channel.
- Displays sender name, phone number, exact world coordinates (X, Y, Z), and dimension.
- Allows first responders to mark emergencies as resolved and archive logs.

### Phone Charger
A block that accepts a cellphone into its internal charging cradle, pulling Forge Energy (FE) to restore the phone's battery. If the charger is broken, the inserted cellphone drops safely.

### Banking & Commercial Blocks
- **ATM Block (`atm_block`):** Physical cash machine to access player bank accounts, link bank cards, and handle deposits/withdrawals.
- **Shop Terminal (`shop_terminal_block`):** Commercial payment terminal facilitating card and mobile transactions.

### Phone Locking & Device Management
Each physical cellphone receives a persistent UUID and has a server-stored device profile. The first user becomes its owner; a later holder can read the phone's saved messages, contacts, call history, social profile, and synchronized gallery photos. A found phone can display the original owner's current or last-synchronized bank balance in read-only mode, but cannot transfer from that account.

Phones open on a lock screen by default. Swipe up or press the Home control to unlock; when a PIN is configured, the server verifies it before exposing phone data. Owners can open Settings → Device → Manage devices to view known device IDs and last-seen times, remotely lock banking, lock all apps, unlock, wipe device contents, or remove a device from their known-device list. Device locks are enforced by server-side phone handlers, not only by the UI. Wiping removes the phone's messages, contacts, call/social data, and synchronized photos while retaining its phone number and original bank-account association.

---

## 4. Signal Propagation & Coverage Engine

The mod calculates signal strength dynamically:
1. **Distance Falloff:** Uses an inverse distance model where signal strength decays relative to the antenna's configured range.
2. **Line-of-Sight Raycasting:** A line-of-sight check is traced between the player and candidate towers.
3. **Block Attenuation:** Blocks between the tower and player degrade signal according to block tags:
   - `signal_attenuation/none`: Air and transparent blocks (no loss).
   - `signal_attenuation/low`: Foliage, wood, glass (minimal attenuation).
   - `signal_attenuation/medium`: Dirt, stone, bricks (moderate attenuation).
   - `signal_attenuation/high`: Concrete, deepslate, iron blocks (heavy attenuation).
   - `signal_attenuation/blocking`: Obsidian, bedrock, reinforced shielding (blocks signal completely).
4. **Signal Tier Mapping:** Combined signal strength and antenna tier determine the active network tier:
   `E` ➔ `E+` ➔ `2G` ➔ `3G` ➔ `H` ➔ `H+` ➔ `4G` ➔ `4G+` ➔ `LTE` ➔ `5G` ➔ `5G+`.

---

## 5. Cellular Carriers & Multi-Network System

- Anyone with appropriate permissions can establish independent cellular networks.
- Networks possess a unique ID, custom display name, owner UUID, server block coordinates, and a salted password hash.
- Carriers operate independently: players without an active subscription plan on a network (or an approved roaming agreement) experience **"No Service"**, disabling calling and data apps.

---

## 6. Roaming & Inter-Carrier Agreements

When a player travels outside their home carrier's tower coverage:
- If a foreign carrier's tower is detected, the phone checks if the player's plan has `allowRoaming: true`.
- Roaming requires an **active bilateral agreement** between the home network and the host network.
- **Proposal System:**
  - Network A sends a proposal to Network B with specific rates:
    - Voice Rate (per minute)
    - SMS Rate (per text)
    - Data Rate (per megabyte)
  - Network B can **Accept**, **Reject**, or **Counter** with updated rates.
  - When accepted, an active roaming agreement is synchronized between both carriers.
- **Revenue Sharing:** Roaming charges are deducted from the subscriber and split between the home network and visited (tower owner) network. `roamingRevenueSplitHome` (default `0.5`) controls the home network fraction; the host receives the remainder. Revenue this cycle resets at the end of each plan billing cycle and whenever a plan is changed or cancelled. Lifetime revenue is tracked separately and only resets via `/mcn network revenue reset`.

---

## 7. Subscription Plans & Billing

Plans are data-driven JSON files stored in `data/mobilecellnetwork/plans/`:
- **Attributes:**
  - `price`: Cost deducted every billing cycle.
  - `billingCycleDays`: Duration of each cycle in Minecraft in-game days.
  - `callMinutes`: Voice call minutes allowance (`-1` for unlimited).
  - `textMessages`: SMS message allowance (`-1` for unlimited).
  - `dataAllowanceMb`: Cellular data allowance (`-1` for unlimited).
  - `maxSignalTier`: Caps the highest tier achievable on this plan.
  - `carrierNetworkId`: Carrier network providing the plan.
  - `allowRoaming`: Whether roaming on foreign towers is permitted.
  - `roamingMode`: `CHARGED` or `UNCHARGED`.
  - `allowOverage`: Whether usage past allowances is permitted.
  - `overageRatePerMinute`, `overageRatePerText`, `overageRatePerMb`: Rates applied when over limits.
- **Hard Stop:** When `allowOverage: false` and allowances are exhausted, outgoing calls, SMS, and data apps are blocked until the next renewal.
- **Currency System:** Configurable currency formatting (defaulting to `$`, `dollar`, and `dollars`) formatted consistently via `CurrencyUtil`.

---

## 8. Emergency SOS & First Responder Dispatch

- The **Emergency** SMS channel is accessible regardless of coverage status.
- Even if the player has no active plan, exhausted credits, zero signal, or a dead battery (if enabled in config), emergency messages can still be transmitted.
- Emergency messages:
  - Do not consume text quotas.
  - Record exact coordinates and dimension.
  - Alert online operators with sound notifications.
  - Display directly inside the **Emergency Dispatch Console**.

---

## 9. Economy & Banking

- Every player has a persistent bank account capability.
- **Bank Card:** Item holding player account credentials.
- **Transactions:** Tracks transfers, plan subscription fees, overage charges, and roaming charges. Overage entries appear as `Overage: call minutes`, `Overage: text messages`, or `Overage: data MB`.
- Plan subscription payments and overage charges are credited to the plan's carrier network. Roaming charges are split between the subscriber's home network and the tower owner network (configurable via `roamingRevenueSplitHome`, default 50/50).
- Each network has a persistent `revenueBalance` accumulator, viewable in the Server Block → Networks tab and managed via `/mcn network revenue`.
- Automatic recurring billing debits accounts periodically and alerts players to insufficient funds.

---

## 10. Administrator Commands & Configuration

### Commands (`/mcn` - Permission Level 2)
- `/mcn signal here`: Diagnostics for signal strength, nearest towers, attenuation, and line-of-sight.
- `/mcn chatlock <true|false>`: Toggles interception of vanilla chat (encouraging smartphone SMS usage).
- `/mcn plan list|assign|reload`: Inspect, assign, or reload subscription plans.
- `/mcn network list|create|delete|allow|disallow|owner|roaming|password|server`: Manage cellular carriers.
- `/mcn network revenue <networkId>`: Show a network's lifetime revenue balance. `/mcn network revenue reset <networkId>` clears the lifetime balance; per-cycle revenue resets with plan billing cycles, changes, and cancellations.
- `/mcn antenna list|state|owner|forceclaim|share|unshare`: Inspect and manage physical cell towers.
- `/mcn roaming list|propose|force-agreement|clear`: Roaming agreement administration.
- `/mcn emergency list|clear`: View or archive emergency SOS logs.
- `/mcn billing force|history`: Run immediate billing cycles or review transaction histories.
- `/mcn player list|number|plan|lowpower|charge`: Manage player phone numbers, plans, power states, and battery charge.
- `/mobilecellnumber view|set`: Quick lookup or assignment of phone numbers.

Phone locking and device management are performed in Settings → Device; no additional `/mcn` commands are added.

### Configuration (`mobilecellnetwork-common.toml`)
- Configurable ranges for Copper, Iron, and Diamond antennas.
- `towerPlacementPreviewEnabled` and `towerPlacementPreviewMaxDistance` control the antenna placement radius preview and its aim range.
- Falloff formulas, line-of-sight sampling rates, and raycast step distances.
- Battery drain rates, Low Power Mode multipliers, and flashlight power consumption.
- Currency symbols and naming.
- Default roaming rates, `roamingRevenueSplitHome`, and automatic proposal behavior.
- Vanilla chat lock options and bypass permissions.
- Dead phone emergency call toggles.
