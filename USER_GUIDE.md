# Mobile Cell Network: User Guide

A comprehensive telecommunications and in-game smartphone simulation mod for **Minecraft NeoForge 1.21.1** (NeoForge 21.1.255).

Mobile Cell Network introduces a realistic cellular ecosystem into Minecraft—complete with physical cell towers, realistic radio signal propagation and block attenuation, multi-carrier networks, paid roaming agreements, data-driven subscription plans, and an interactive in-game smartphone equipped with 14 functional apps.

> **Technician installation:** Newly placed antennas and boosters require a Radio Electronics Module, a spool-connected FE power wire, and commissioning with a Technician Tester before they provide service. Install the module in Tier 1's combined ground cabinet, the upper-platform radio cabinet on Tiers 2-3, or either platform's blue-label cabinet on Tier 4. Empty-hand right-click to open the door, then use the module inside. Open the ground cabinet for power wiring. The base door faces north; platform doors and screens face west toward the mast-side walkway, not the railing. Right-click again with an empty hand to close them. Use the spool on a generator/battery output face first, then inside the base cabinet (maximum 32 blocks, 256 FE/tick, 256 cable units per spool). Supply FE before using the tester inside an open cabinet. Sneak-use the spool there to cut its wire without a refund; rewiring requires another test. Existing installations are grandfathered but their wide tower cabinets can still be opened. `requireInstallation` controls future placements; disabling FE requirements does not skip installation. See [Technician installation](README.md#technician-installation) for full controls and limitations.

> **Tower service access and lighting:** On lattice Tiers 2-4, enter through the framed east-side doorway to reach the base power cabinet. At either platform, climb above deck height, step right/east onto the ladder transfer landing, then move forward onto the platform. Radio cabinets stay in the northeast corner, clear of the antenna panels; stand just west on the mast-side walkway to access the inward-facing door. Cable branches run beneath the deck and up the rear, not across the work area. The solid ladder stays beside the route. Empty-hand right-click a computer screen to open tower controls: Tiers 2-3 separate their lower-deck computer from the top-deck radio cabinet; Tier 4 has screens above both platform cabinets. All tiers have a ground-level screen. Terminals work while offline, and menus stay usable on the platform. Each lattice platform has four downward-facing corner lamps that emit real block light when the enabled tower has power (including bypass); commissioning is not needed. The monopole has no platform lamps. Vanilla lighting spreads outward, not as a directional cone. The cable spool and electronics module have custom 3D item models, installed cables have modeled insulation and collars, and open cabinets expose detailed circuit boards and power hardware.

> **Electrical tester and power tap:** The Technician Tester is a yellow-and-black digital multimeter with a selector dial and two probes. Its backlit LCD displays live server-measured FE, RF strength/tier, installation faults, and available wire-source output while held in either hand (one sample per second). Aim inside an open cabinet or at equipment to inspect it; aim elsewhere to survey radio coverage. Servers, chargers, and power taps also expose their FE buffers to the meter. Closed/unauthorized cabinets show instructions or access errors. Right-click still prints the detailed component/wire/signal report and attempts commissioning; sneak-use only inspects. Radio readings are independent of phone charge and plans; booster readings measure direct upstream signal at the booster. Place a Tower Power Tap beside the tower, feed compatible FE into its sides/bottom, and spool-connect its top copper terminal to the tower base cabinet. The tap stores 100,000 FE but generates none; wires retain their 32-block/256-FE-per-tick limits. See [Electrical and signal tester](README.md#electrical-and-signal-tester) and [Tower power tap](README.md#tower-power-tap) for details.

> **Cable routing and visual updates:** Selecting a power output with the spool starts a visible cable preview. Connect directly to equipment, or click solid block faces to add up to 16 snap points before finishing at the equipment. The full route, including detours, must fit within 32 blocks; cable consumption rounds up that routed length. Sneak-use in the air cancels the pending route. Completed attachments and clips persist across reloads. The tester has a rounded yellow boot, molded face, circular selector, four input jacks, raised button-face labels, and clipped probes with curved leads while retaining its live LCD. The radio module displays its component face outward in item frames. Tower doorway jambs share a continuous threshold and tie into the mast; access braces have no cut-off stubs, and panel mounts stay behind their panels. The held tower cap has four visible supports. Only the original eight level-15 under-platform work lights remain; the extra railing and lower-mast floodlights have been removed. Existing towers clear their saved light states automatically. The original lamps retain the normal enabled/powered checks. Tower ladder standoffs and safety-cage hoops are braced to the frame; overlapping platform, railing, and foundation faces are removed to prevent depth flicker.

> **Create: Electro Energetics:** Use its FE converter in electrical-grid-to-FE mode. Feed its FE output into a **side or bottom of a Tower Power Tap**, then spool-connect the tap's **top** to any antenna/tower or booster and commission normally. Do not select the converter itself as the spool source: it pushes FE but cannot be drained by our wire. Servers and phone chargers accept FE on any face directly from the converter or through a compatible FE cable/battery network. Phones charge in the charger. No Electro Energetics dependency is required, and other FE mods remain supported. See [Create: Electro Energetics power](README.md#create-electro-energetics-power) for orientation and limitations.

> **Fixed cable sockets:** Spool cables now snap to dedicated connectors on the power tap (top terminal), booster and charger (rear sockets), small antennas (base), and all tower variants (fixed side of the ground power cabinet). The preview uses the same connector as the finished cable. Open the tower's ground cabinet before connecting; its cable gland stays fixed when the door closes. Straight cables and manually placed routing snaps remain available. Chargers can also use spool power and need no radio module or commissioning; direct FE input still works. Reconnect older saved wires to move their original clicked endpoints onto the new sockets.

> **Four tower variants:** Tier 1 is a slim 6-block monopole with a 128-block range and a 2G cap. Its attached ladder leads to two three-sector antenna arrays; install the radio module and connect power in the combined ground cabinet, with the computer directly above it. Placement needs a clear 3-by-3 area six blocks tall. Tier 2 is 10 blocks tall, copper/orange, with a 256-block range and a 3G cap. Tier 3 is 15 blocks tall, silver/blue, with a 512-block range and a 4G+ cap (no 5G). On both Tiers 2-3, antenna panels and mounts remain on the upper platform, while the lower platform has the computer above its restored cabinet box, railings, and lights, rather than a thin pedestal. Install radio modules in the upper cabinet; ground power cabinets remain in place. Tier 4 retains the 20-block steel/white tower, 1024-block range, and 5G+ capability. Tiers 2-4 retain their 5-by-5 foundations and full-height service platforms. All towers are creative-only; existing small antennas are unchanged. Previous towers retain their saved IDs, networks, power, ranges, and radio capabilities, with display tiers shifted up by one. Static geometry uses cached GPU meshes while doors and lights remain dynamic. Actual appearance and FPS still need in-game verification. Install the same updated JAR on client and server and fully restart both.

---

Tower highlighting reuses cached outline edges, avoiding the repeated voxel scan that caused aiming lag, especially on the largest tower (now Tier 4). The first highlight of a previously uncached section shape still calculates its edges once.

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
- **Signal booster:** A powered, commissioned booster regenerates strong local reception from an existing tower's signal within 32 blocks, with local distance and obstruction losses. It consumes 1 FE per tick, holds 10,000 FE, and accepts FE on any side. Empty-hand right-click opens its responsive tuning menu, showing stored power, installation status, incoming strength before boost, and receivable carriers. Choose a carrier to relay its strongest receivable tower, or leave **Automatic (all carriers)** to relay any eligible carrier. Carrier rows use compact names; hover for the full name and ID. Use **Rescan** to refresh the list. Selection is saved across restarts; if the selected carrier loses reception, the booster does not silently switch to another carrier. It cannot amplify an absent signal or relay another booster. Owners/operators can configure owned installations within eight blocks, including when unpowered. The tester measures the chosen carrier's upstream signal; the phone HUD shows post-booster reception. The tower's network, plan, roaming and speed-tier restrictions still apply. If the menu says **Installation incomplete**, finish wiring/module installation and commission with the tester before boosting can work.
- **Server racks:** Sneak-place racks on top of one another to form a connected-looking stack. Each block remains a separate server with its own administration. A rack accepts FE on any side, stores 10,000 FE and consumes 1 FE per tick. Its network is offline without power or while its chunk is unloaded; the administration menu stays accessible.
- **Infrastructure power test:** Operators can enable `/mcn powertest true` to bypass FE for all server racks, antennas (including the test tower), and boosters in this world, or use `/mcn powertest false` to restore normal power requirements. The choice persists across restarts. The existing `requireEnergy = false` configuration also bypasses these requirements; it must be true to require power. This setting does not charge phones or bypass network, plan, or roaming rules.
- **Phone charger:** Connect FE, then right-click the pad with a SnapPhone or G24 to place it on the charger. Its current charge percentage appears above the phone. Right-click with an empty hand to retrieve it, or sneak-right-click with an empty hand to open the charger menu. The menu also opens when the empty pad is clicked without a phone.
- **Power indicators:** Each server rack repeats three indicators on its bays: green means powered, blue means at least one network is managed, and blinking amber warns that stored FE is below 20%. The booster's green means powered, blue means power bypass is enabled, and blinking amber warns of less than 20% stored FE. All rack and booster indicators are dark without power; bypass counts as power and suppresses the low-FE warning. Antenna beacons blink when operational. The charger's indicator lights when it has stored FE, independently of the infrastructure bypass. These are visual indicators and do not illuminate nearby blocks.
- **Physical Signal Simulation:** Radio waves diminish with distance and are attenuated or completely blocked by world terrain and blocks (glass, wood, stone, deepslate, obsidian, etc.).
- **Smartphones:** Carrying a cellphone provides an interactive device with voice calling, messaging, digital mapping, online banking, social media, and system utilities.
- **Player-Run Carriers:** Players can establish their own cellular companies, build towers, set subscription plans, and negotiate inter-network roaming agreements for revenue.
- **Emergency Lifeline:** Stranded players without an active plan or cell signal can transmit emergency SOS texts that pinpoint their coordinates to first responders.

---

## The In-Game Smartphone

### Interface & Operating System
Right-clicking the **Cellphone** item opens a custom in-game smartphone interface:
- **Status Bar:** Displays the connected carrier (or roaming indicator), in-game time, 4-bar signal strength indicator, cellular tier badge (`E`, `E+`, `2G`, `3G`, `H`, `H+`, `4G`, `4G+`, `LTE`, `5G`, `5G+`), and battery percentage.
- **On-Screen HUD:** While a phone is in your inventory, a compact status bar appears above the hotbar and action-bar messages without needing to open the phone UI.
- **Phone Styles:** The SnapPhone and G24 are separate items with Snapple and Samsung One UI-inspired interfaces, respectively.
- **Without a plan:** The home screen, Settings, contacts, camera, gallery, flashlight, and plan selection remain available. Calls, ordinary messages, app downloads, and other cellular services require an active plan and service. Emergency SMS remains available separately.
- **Unlock:** Drag upward from the bottom of the lock screen; phones with a PIN then show the PIN keypad.
- **Control Center:** On the Snapple home screen, drag down from the top-right status area for a translucent panel with flashlight, Low Power Mode, ringtone and Settings controls, plus phone status. Unsupported media and brightness controls are not shown.
- **Snapple Settings:** The Snapple style uses a light, grouped settings layout; scroll to reach notification preferences. Other styles retain their existing settings layout.
- **Appearance:** In Settings, select Appearance to switch between Light and Dark. The preference is saved with the phone in the world and synchronizes when that phone is opened. Wallpapers and camera imagery keep their own colors.
- **Phone profile photo:** Select the profile card or Profile photo setting to open your computer's file picker and choose a PNG/JPEG, or use the Profile gallery setting to select an existing phone photo. The avatar uses a centered square crop, scaled into a circle; the original photo remains unchanged. You can also browse or enter a full path from the import screen. The image must be at most 1,000,000 bytes. The selected photo and gallery are stored with the phone in the world, so they follow the device when connecting to the same world from another computer. The server must accept the photo before it can be selected.
- **Phone setup and reset:** New phones start with a setup flow for the device name, appearance, optional PIN and optional profile photo. Previously registered phones keep their existing settings. Under Settings > Device, factory reset erases the phone's local profile and gallery while retaining its number and plan, then starts setup again; it does not erase the original imported files or remote Socials posts.
- **Delete phone photos:** Select an image in the phone gallery and choose Delete selected, then confirm. This removes the image from the phone's saved gallery and clears it as the phone profile photo if selected; it does not delete the original file imported from your computer or images already posted to Socials.
- **Snapple Appearance:** The open phone uses the same 3D cellphone model as the held item, with a polished metallic frame, camera cutout, original wallpaper and icons. Phone screens use Lato typography, including input fields: medium-weight and shadow-free on light phone surfaces, and bold in dark mode, with Minecraft's font as a fallback for unsupported glyphs. Existing apps continue to use their current layouts. Lato is distributed under the SIL Open Font License; its license is included with the mod.
- **Snapple icons:** Phone, Messages, Contacts, App Store and Settings use original designs inspired by smartphone icon references. The store catalog also uses the matching Snapple icons.
- **Snapple apps:** App Store features a curated app card, category filters, searchable catalog, install buttons, and a draggable scrollbar when more apps are available. Contacts has a searchable alphabetical list and My Card links to phone settings. The Phone app's Calls screen includes a searchable call history and a link to voicemail. Messages has a searchable inbox, unread filter, and options for managing chats. These screens follow the phone's light or dark appearance; contacts and voicemail are also available from the Phone app.
- **Phone item:** The original rounded 3D model uses the proportions of a 147.6 × 71.6 × 7.8 mm handset and a 6.1-inch display. The Snapple open-phone interface also matches the handset's front aspect ratio while retaining its usable height; it is not shown at physical world scale. It does not automatically move to the player's hand for camera or flashlight use.
- **Navigation Controls:** Working Back button, Home button, and gesture indicator.
- **Held phone view:** Hold a cellphone and press **P** (configurable in Minecraft controls) to open a compact phone interface at the edge of the screen. Press P again to close it. Movement keys remain available except while typing in a phone text field. Hold right-click outside the phone and move the mouse to look around; release it to use the cursor again. Right-click still opens the larger view. The held model is hidden while either interface is open to prevent duplicate phones; the interface is an overlay, not a texture on the held model.
- **Held lock display:** In first person, a charged held phone shows the current time and date, plus signal and battery indicators on its screen before opening the phone.
- **App Search:** A compact live filter at the bottom of the Snapple home screen lets players search apps by name. Hold or right-click an app to edit the home screen, drag apps to rearrange them, and select Done to finish.
- **Animated wallpaper and home editing:** The home wallpaper has a subtle moving glow and drifting particles. With Low Power Mode off, press and hold an app for half a second, then drag it onto another app to swap their positions, including Snapple dock slots. Select Done or press Escape to finish. Tap an optional app's minus badge twice to uninstall Maps, Socials, or Bank; essential apps cannot be removed. Reinstall optional apps from the store when service is available. A paid app already purchased on that phone is free to reinstall, and its purchase and home layout are saved per phone.
- **Multiplayer compatibility:** Client and server must run matching mod versions; this update uses phone network protocol 10.

### Smartphone Apps

| App | Description & Features |
|---|---|
| 📞 **Phone / Dialer** | Number keypad for dialing, Recents call log (incoming, outgoing, missed), Contacts tab, and Voicemail tab. Snapple uses a circular keypad and bottom navigation. |
| 📲 **Call Screen** | Incoming calls appear on the held phone when one is in use, with Answer and Decline controls; otherwise, use the in-game call overlay. The active call remains visible in the overlay after closing the phone. The call screen includes a duration timer, Mute, visual Speakerphone, and End Call buttons. Integrates with Simple Voice Chat (creates temporary voice call groups) or falls back to live text-calls on HUD. Supports ringtones, busy tones, and out-of-service alerts. |
| 📹 **In-game video calls** | On the SnapPhone, open FaceTime and select a contact or enter a number. FaceTime video starts when the call connects and fills the phone screen; when both cameras are on, the other participant appears full-screen with your own preview in the corner. Either participant can turn their camera off/on. Cameras stop when leaving the call screen, changing perspective, or ending the call. Video sends up to four frames per second and uses both players' plan data in addition to call minutes; audio uses the existing call system. The server and both clients must use the same mod version. |
| 💬 **SMS / Messages** | Direct messaging and group chats with unread indicators. Incoming direct, group, global and queued SMS messages show a five-second notification banner even with the phone closed or another screen open. Banners identify the sender without exposing message contents. The SMS notification toggle controls banners and sounds; notification-sound settings and Do Not Disturb control sounds. Queues messages for offline players to deliver on login. Includes an always-pinned **Emergency (SOS)** conversation. |
| 👥 **Contacts** | Contact book to create, edit, and delete contacts, with one-tap shortcuts to call or message. |
| 📼 **Voicemail** | Lists voicemail entries with caller and time, supports personal notes and callbacks, and lets you delete entries from the Phone app's Voicemail tab. |
| 🗺️ **Map** | GPS satellite navigation app featuring terrain elevation shading, player position tracking, zooming, panning, waypoint markers (POIs), and one-tap waypoint sharing over SMS. |
| 📡 **Signal testing** | Newly placed creative-only Cell Signal Test Antennas occupy a 5×5 footprint, 20 blocks tall, centered on the base. The detailed model includes solid tapered steel legs and cross-braces, a framed east-side service doorway, a climbable north-side ladder with safety-cage detailing, two walkable railed service platforms with right/east transfer landings, antenna panel pairs and mounts, bolted footings, cable bundles and clamps, vented equipment cabinets and a blinking beacon. Walk into the ladder to climb, hold jump to keep climbing, and sneak to hold your position. At either platform, climb above deck height, step right/east onto the transfer landing, then forward through the north opening; do not walk through the solid ladder rungs. Braces and cage poles leave headroom at the exits. Cross-brace collision uses small segments to approximate the diagonal beams. The cage, rungs, panel mounts and deck-grating lines include visual detailing. Placement requires the entire 5×5 area to be clear up to 19 blocks above the base; breaking any structural section removes the tower. Existing wide towers receive these geometry improvements automatically. Existing narrow 10- and 20-block towers keep their dimensions; break and replace them for the wider tower. Their north-side ladders are also climbable. Right-click the central base for status and network assignment. The antenna uses the diamond antenna's configured range, tier and FE requirements; connect power to the central base or enable the operator power-test bypass. Normal service requires an active plan and an assigned, online network. An operator can run `/mcn signaltest <player> true` to display nearby physical tower coverage without a plan or network selection; `/mcn signaltest <player> false` turns it off. Test readings do not grant calls, SMS, data, or bypass billing. `/mcn signal here` shows distance and sampled obstruction. Signal weakens with distance and sampled materials, but this is an approximation, not a radio simulation. |
| 🏦 **Bank** | Mobile banking client displaying current account balance, linked bank card, direct funds transfer to other players, and full transaction history. |
| 📊 **Plan** | Subscription management screen. Displays active plan, carrier name, remaining minutes, texts, and data allowance (MB), cycle countdown, and overage billing. Includes a two-step network and plan chooser modal to subscribe or cancel plans. |
| ⚙️ **Settings** | Controls appearance, Low Power Mode, ringtone and notification sounds, Do Not Disturb, per-app notifications, and manual **Cell Tower selection** (choose automatic best tower or pin a specific antenna). |
| 📷 **Camera** | Move with the normal movement controls; hold right-click outside the phone and move the mouse, drag inside the viewfinder, or use arrow keys to aim. Tap **Rear** or **Front** in the viewfinder to switch between the world-facing view and a front-facing selfie of your Minecraft character. Front starts at 2x for arm's-length selfie framing; zoom settings are remembered separately for each camera. Rear uses first-person view if you open the app while already in front-facing perspective. Your original perspective is restored when you leave the camera. Choose 0.5x wide, 1x, 2x, 5x, or 10x zoom on the viewfinder. Zoom changes the world view while the camera is open and restores it on exit. Snapple has a shutter, Gallery, Import, and Socials controls. |
| 🖼️ **Gallery** | Browse captured or imported photos and tap a photo to view the full image inside the phone. Return to the gallery to use the selected photo for Socials or a profile, or delete it. Snapple offers a recent collection and a scrollable library grid. |
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
- **Copper Antenna (Tier 1):** Short-range with a lower data tier cap.
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
- **Creating service:** In the server's Networks tab, choose **New Network**, enter a unique name and password, and save. The new network is attached to that server. In Plans, choose **New Plan**, enter a lowercase ID and name, set the price and allowances, and select your carrier under **Carrier and access** (a single owned network is selected automatically). Save and check the confirmation. At an antenna, select the network and save its settings so it can broadcast. A server may manage more than one network.
- **Joining service:** Open the phone's Plan app and choose a network and one of its plans (use **Change plan** if a plan is already active). The subscription price is charged from the player's bank balance. Choosing a plan does not require existing service; calls and data still require a matching online network and antenna coverage. If a network or plan is missing, reopen or refresh the relevant screen and confirm that the carrier and plan were saved.
- **Removing infrastructure:** Removing an antenna unregisters it and updates the network's tower count. Removing a server takes its network offline and hides it from the normal server network and roaming lists without deleting its plans or settings. Replacing a server at its previous position restores the matching network; when several unassigned networks exist and none matches the position, the owner can select **Restore** on the newly placed server. A single unassigned network is restored automatically. Older removed servers may not have their former positions recorded, so choose **Restore** if needed.
- **Management Tabs:**
  - **Networks:** Inspect registered networks, antenna counts, online/offline status, allow/disallow flags, and password management.
  - **Networks and Roaming:** The server menu lists networks owned by the player. Edit their names and roaming availability, or change roaming rates and automatic requests in the Roaming tab. These actions require ownership, including for operators.
  - **Antennas, Players, and Billing:** These server-menu sections remain unavailable. Configure antennas at their block; use available server commands for other administration.
  - **Plans:** Network owners can search, create, edit, and delete plans belonging to their networks. Create a carrier network first, then choose **New Plan**, enter a unique lowercase plan ID and display name, scroll to **Carrier and access**, and select an owned carrier before saving. Default plans cannot be deleted or changed to a different default here. Server confirmation or errors appear in chat.
  - **Emergency Broadcasts:** Broadcast server-wide emergency alerts to carrier phones.

### Emergency Dispatch Console
A dedicated block (`emergency_dispatch`) for operators and emergency personnel (permission level 2):
- Receives incoming SOS messages sent over the emergency channel.
- Displays sender name, phone number, exact coordinates (X, Y, Z), and dimension.
- Allows first responders to mark emergencies as resolved and archive logs.

### Phone Charger
Accepts a cellphone into its cradle and recharges its battery using Forge Energy (FE).

### Banking & Commercial Blocks
- **ATM Block (`atm_block`) and Shop Terminal (`shop_terminal_block`):** These blocks are placeholders; their physical banking and payment interfaces are not yet implemented.

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
- **Bank Card:** A registered item; the physical ATM interface is not yet implemented.
- **Transactions:** Tracks transfers, plan fees, overage charges, roaming fees, and carrier revenue payouts.
- Offline players receive pending revenue and billing reconciliations upon logging in.

---

## Administrator Commands & Configuration

### Commands (`/mcn` - Permission Level 2)
- `/mcn signal here`: Diagnostics for signal strength, nearest antenna, attenuation, and line-of-sight.
- `/mcn signaltest <player> <true|false>`: Toggle physical coverage testing without granting service.
- `/mcn powertest <true|false>`: Toggle the world-wide infrastructure power bypass.
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
