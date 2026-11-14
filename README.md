# Mobile Cell Network

A comprehensive telecommunications and in-game smartphone simulation mod for **Minecraft NeoForge 1.21.1** (NeoForge 21.1.255).

Mobile Cell Network introduces a realistic cellular ecosystem into Minecraft—complete with physical cell towers, realistic radio signal propagation and block attenuation, multi-carrier networks, paid roaming agreements, data-driven subscription plans, and an interactive in-game smartphone equipped with 14 functional apps.

---

## Table of Contents

1. [Overview & Core Concept](#overview--core-concept)
2. [The In-Game Smartphone](#the-in-game-smartphone)
   - [Interface & Operating System](#interface--operating-system)
   - [Smartphone Apps](#smartphone-apps)
   - [Battery & Power Management](#battery--power-management)
3. [Blocks & Infrastructure](#blocks--infrastructure)
   - [Cell Towers (Antennas)](#cell-towers-antennas)
   - [Server Block (Network Administration)](#server-block-network-administration)
   - [Emergency Dispatch Console](#emergency-dispatch-console)
   - [Phone Charger](#phone-charger)
   - [Banking & Commercial Blocks](#banking--commercial-blocks)
4. [Signal Propagation & Coverage Engine](#signal-propagation--coverage-engine)
5. [Cellular Carriers & Multi-Network System](#cellular-carriers--multi-network-system)
6. [Roaming & Inter-Carrier Agreements](#roaming--inter-carrier-agreements)
7. [Subscription Plans & Billing](#subscription-plans--billing)
8. [Emergency SOS System](#emergency-sos-system)
9. [Economy & Banking](#economy--banking)
10. [Administrator Commands & Configuration](#administrator-commands--configuration)

---

## Overview & Core Concept

Vanilla Minecraft relies on global chat for player communication. Mobile Cell Network replaces or supplements this with a realistic telecommunications system:

- **Radio Towers:** Players and admins place cell towers (antennas) in the world to transmit coverage.
- **Physical Signal Simulation:** Radio waves diminish with distance and are attenuated or completely blocked by world terrain and blocks (glass, wood, stone, deepslate, obsidian, etc.).
- **Smartphones:** Carrying a cellphone provides an interactive device with voice calling, messaging, digital mapping, online banking, social media, and system utilities.
- **Player-Run Carriers:** Players can establish their own cellular companies, build towers, set subscription plans, and negotiate inter-network roaming agreements for revenue.
- **Emergency Lifeline:** Stranded players without an active plan or cell signal can transmit emergency SOS texts that pinpoint their coordinates to first responders.

---

## The In-Game Smartphone

### Interface & Operating System
Right-clicking the **Cellphone** item opens a custom in-game smartphone interface:
- **Status Bar:** Displays the connected carrier (or roaming indicator), in-game time, 4-bar signal strength indicator, cellular tier badge (`E`, `E+`, `2G`, `3G`, `H`, `H+`, `4G`, `4G+`, `LTE`, `5G`, `5G+`), and battery percentage.
- **On-Screen HUD:** When the phone is held in either hand, a compact status bar overlay appears directly above the hotbar without needing to open the phone UI.
- **Phone Styles:** Switch between a dark **Samsung One UI-inspired** style and a light **Apple iOS-inspired** style in Settings.
- **Navigation Controls:** Working Back button, Home button, and gesture indicator.
- **App Search:** A live filter bar at the bottom of the home screen lets players search apps by name.

### Smartphone Apps

| App | Description & Features |
|---|---|
| 📞 **Phone / Dialer** | Number keypad for dialing, Recents call log (incoming, outgoing, missed), Contacts tab, and Voicemail tab. |
| 📲 **Call Screen** | Active call screen with call duration timer, Mute, visual Speakerphone, and End Call buttons. Integrates with Simple Voice Chat (creates temporary voice call groups) or falls back to live text-calls on HUD. Supports ringtones, busy tones, and out-of-service alerts. |
| 💬 **SMS / Messages** | Direct messaging and group chats with unread indicators. Queues messages for offline players to deliver on login. Includes an always-pinned **Emergency (SOS)** conversation. |
| 👥 **Contacts** | Contact book to create, edit, and delete contacts, with one-tap shortcuts to call or message. |
| 📼 **Voicemail** | Allows callers to leave messages when recipients are offline, busy, or decline. Supports playing/reading, callback, and deletion. |
| 🗺️ **Map** | GPS satellite navigation app featuring terrain elevation shading, player position tracking, zooming, panning, waypoint markers (POIs), and one-tap waypoint sharing over SMS. |
| 🏦 **Bank** | Mobile banking client displaying current account balance, linked bank card, direct funds transfer to other players, and full transaction history. |
| 📊 **Plan** | Subscription management screen. Displays active plan, carrier name, remaining minutes, texts, and data allowance (MB), cycle countdown, and overage billing. Includes a two-step network and plan chooser modal to subscribe or cancel plans. |
| ⚙️ **Settings** | Controls OS theme style (One UI vs iOS), Low Power Mode, ringtone and notification sounds, Do Not Disturb, per-app notifications, and manual **Cell Tower selection** (choose automatic best tower or pin a specific antenna). |
| 📷 **Camera** | Live first-person camera viewfinder allowing free camera movement and player motion while framing and snapping photos. |
| 🖼️ **Gallery** | Photo album viewing pictures captured with the Camera app. |
| 🔦 **Flashlight** | Illuminates the area ahead of the player (~7 blocks) using real-time dynamic light blocks. Discharges the battery faster while active. |
| 🛍️ **Store** | Digital app catalog for installing optional applications. Extra Ringtones currently uses the glyph fallback because it has no PNG icon. |
| 🌐 **Socials** | In-game social media platform with user profiles, posts, photo uploads, likes, comments, user search, and follower feeds. |

### Battery & Power Management
- Phones hold an internal charge between 0 and 10,000 units.
- Battery drains over time (`phoneDrainPerSecond`).
- **Low Power Mode:** Reduces drain, caps the maximum signal tier at `E+`, and restricts available apps to essential services (SMS, Map, Settings). The Map renders at a reduced scale (`mapResolutionDivisor`) to save data.
- **Phone Charger:** Recharges phones using Forge Energy (FE). Dropping or breaking the charger safely drops the inserted phone.
- **Dead Phone:** At 0% charge, the screen turns black and displays "Phone Off" until recharged.

---

## Blocks & Infrastructure

### Cell Towers (Antennas)
Antennas transmit cellular signals across a spherical coverage area:
- **Copper Antenna (Tier 1):** Short-range, lower data tier cap, economical crafting recipe.
- **Iron Antenna (Tier 2):** Medium-range, balanced data speeds.
- **Diamond Antenna (Tier 3):** Long-range, maximum signal bandwidth (`5G+`).

Antenna mechanics:
- **Power States:** Powered, Low Power, or Unpowered.
- **Ownership & Password Claiming:** Antennas are claimed by networks using salted password verification.
- **Antenna Sharing:** Tower owners can share their physical antenna mast with other carriers, broadcasting multiple networks from one block.
- **Mesh Links:** Nearby antennas on the same network automatically establish mesh connections.

### Server Block (Network Administration)
The **Server Block** acts as the central management terminal for cellular networks:
- **Setup Workflow:** Placing a server block opens the administration interface; if the assigned network has no password, the owner is prompted to set the network name and password.
- **Management Tabs:**
  - **Networks:** Inspect registered networks, antenna counts, online/offline status, allow/disallow flags, and password management.
  - **Roaming:** Manage bilateral roaming agreements, propose rates, counter proposals, and track collected roaming revenue.
  - **Antennas:** View registered towers, signal states, claiming status, and shared network lists.
  - **Players:** Manage subscriber numbers, active plans, and bank balances.
  - **Plans:** Inspect and create subscription plans.
  - **Billing:** View recent billing renewal logs and force billing cycles.
  - **Emergency Broadcasts:** Broadcast server-wide emergency alerts to carrier phones.

### Emergency Dispatch Console
A dedicated block (`emergency_dispatch`) for operators and emergency personnel (permission level 2):
- Receives incoming SOS messages sent over the emergency channel.
- Displays sender name, phone number, exact coordinates (X, Y, Z), and dimension.
- Allows first responders to mark emergencies as resolved and archive logs.

### Phone Charger
Accepts a cellphone into its cradle and recharges its battery using Forge Energy (FE).

### Banking & Commercial Blocks
- **ATM Block (`atm_block`):** Physical terminal for player banking, card linking, deposits, and withdrawals.
- **Shop Terminal (`shop_terminal_block`):** Commercial payment terminal facilitating card and mobile transactions.

---

## Signal Propagation & Coverage Engine

The coverage engine calculates signal strength dynamically every second for active phones:
1. **Distance Falloff:** Uses an `INVERSE` distance model where signal strength decays relative to the antenna's configured range: `1 / (1 + 4 * distance / range)`. A cutoff at `inverseMaxRangeMultiplier * range` prevents infinite range.
2. **Line-of-Sight Raycasting:** A ray is sampled between the player and candidate towers. Sampling cost is tuned via `losSampleInterval` and `losMaxSamples`.
3. **Block Attenuation:** Blocks along the line of sight degrade signal according to block tags:
   - `signal_attenuation/none`: Air and transparent blocks (0 attenuation).
   - `signal_attenuation/low`: Foliage, wood, glass (minimal attenuation).
   - `signal_attenuation/medium`: Dirt, stone, bricks (moderate attenuation).
   - `signal_attenuation/high`: Concrete, deepslate, iron blocks (heavy attenuation).
   - `signal_attenuation/blocking`: Obsidian, bedrock, reinforced shielding (blocks signal completely).
4. **Signal Bars:** The 4-bar indicator uses continuous signal strength thresholds (defaults: 0.80, 0.55, 0.30, and >0.00).
5. **Signal Tier Mapping:** Combined signal strength and antenna tier determine the active network tier:
   `E` ➔ `E+` ➔ `2G` ➔ `3G` ➔ `H` ➔ `H+` ➔ `4G` ➔ `4G+` ➔ `LTE` ➔ `5G` ➔ `5G+`.

---

## Cellular Carriers & Multi-Network System

- Players and admins can create independent carrier networks.
- Networks feature a unique ID, custom name, owner UUID, server block coordinates, and salted password hash.
- **No Service Gating:** Players without an active plan or roaming connection experience "No Service", which cleanly gates calling, SMS, and data apps while keeping exempt utility apps (Settings, Camera, Gallery, Flashlight) accessible.

---

## Roaming & Inter-Carrier Agreements

When a player leaves their home carrier's tower coverage:
- If a foreign carrier's antenna is detected, the phone checks if the player's plan has `allowRoaming: true`.
- Roaming requires an **active bilateral agreement** between the home network and the host network.
- **Proposal System:**
  - Network A sends a proposal to Network B with specified rates:
    - Voice Rate (per minute)
    - SMS Rate (per text)
    - Data Rate (per megabyte)
  - Network B can **Accept**, **Reject**, or **Counter** with updated rates.
  - When accepted, the agreement is activated and synchronized between both networks.
- **Revenue Sharing:** Roaming charges are deducted from the subscriber and split between the plan's home network and visited tower network. `roamingRevenueSplitHome` controls the home-network fraction (default 0.5); the visited network receives the remainder. Plan subscription and overage revenue goes to the plan carrier.

---

## Subscription Plans & Billing

Plans are data-driven JSON files stored in `data/mobilecellnetwork/plans/` (reloadable with `/mcn plan reload`):
- **Plan Parameters:**
  - `price`: Cost deducted every billing cycle.
  - `billingCycleDays`: Cycle duration in in-game days.
  - `callMinutes`: Voice allowance (`-1` for unlimited).
  - `textMessages`: SMS allowance (`-1` for unlimited).
  - `dataAllowanceMb`: Data allowance (`-1` for unlimited).
  - `maxSignalTier`: Maximum cellular tier permitted on the plan.
  - `carrierNetworkId`: Carrier network operating the plan.
  - `allowRoaming`: Whether roaming on foreign towers is allowed.
  - `roamingMode`: `CHARGED` or `UNCHARGED`.
  - `allowOverage`: Whether usage past allowances is permitted.
  - `overageRatePerMinute`, `overageRatePerText`, `overageRatePerMb`: Rates applied when exceeding allowances.
- **Hard Stop:** When `allowOverage: false` and allowances are exhausted, outgoing calls, SMS, and data apps are blocked until renewal.
- **Recurring Billing:** Automated billing runs every 1,200 server ticks. If an account cannot pay, service is suspended or downgraded to the default plan.
- **Currency System:** Currency formatting (defaulting to `$`, `dollar`, and `dollars`) is handled consistently across all screens via `CurrencyUtil`.

---

## Emergency SOS System

- The **Emergency** SMS channel is always accessible, even without an active plan, with zero balance, or in dead zones.
- If enabled in config (`deadPhoneAllowsEmergency`), emergency SOS can even be sent from a dead phone.
- Emergency messages:
  - Do not consume text quotas or bank funds.
  - Record exact player coordinates (X, Y, Z) and dimension.
  - Play an alert sound to online operators.
  - Display directly inside the **Emergency Dispatch Console**.

---

## Economy & Banking

- Every player possesses a persistent bank account capability.
- **Bank Card:** Item holding account credentials for ATM access.
- **Transactions:** Tracks transfers, plan fees, overage charges, roaming fees, and carrier revenue payouts.
- Offline players receive pending revenue and billing reconciliations upon logging in.

---

## Administrator Commands & Configuration

### Commands (`/mcn` - Permission Level 2)
- `/mcn signal here`: Diagnostics for signal strength, nearest antenna, attenuation, and line-of-sight.
- `/mcn chatlock <true|false>`: Toggles interception of vanilla chat (encouraging smartphone SMS usage).
- `/mcn plan list|assign|reload`: Inspect, assign, or reload subscription plans.
- `/mcn network list|create|delete|allow|disallow|owner|roaming|password|server`: Manage cellular carriers.
- `/mcn antenna list|state|owner|forceclaim|share|unshare`: Inspect and manage physical cell towers.
- `/mcn roaming list|propose|force-agreement|clear`: Roaming agreement administration.
- `/mcn emergency list|clear`: View or archive emergency SOS logs.
- `/mcn billing force|history`: Run immediate billing cycles or review transaction histories.
- `/mcn player list|number|plan|lowpower|charge`: Manage player phone numbers, plans, power states, and battery charge.
- `/mobilecellnumber view|set`: Quick lookup or assignment of phone numbers.

### Configuration (`mobilecellnetwork-common.toml`)
- Configurable ranges for Copper, Iron, and Diamond antennas.
- Falloff formulas, line-of-sight sampling rates, and raycast step distances.
- Battery drain rates, Low Power Mode multipliers, and flashlight power consumption.
- Currency symbols and naming.
- Default roaming rates and automatic proposal behavior.
- Vanilla chat lock options and bypass permissions.
- Dead phone emergency call toggles.
