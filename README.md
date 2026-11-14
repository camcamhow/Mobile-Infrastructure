# Mobile Cell Network

Bring smartphones and player-run cellular networks to Minecraft. Build the infrastructure, create a carrier, and stay connected through in-game phones.

**Minecraft 1.21.1** · **NeoForge 21.1.255** · **Java 21**

> **Work in progress:** Some interfaces and integrations are still being developed. Recent features have been built but need further in-game and multiplayer testing.

## Features

- **Two phones:** Snapple's SnapPhone 16 and LanDriod's G24, with distinct interfaces, battery management, and customizable home screens.
- **Phone apps:** Calls, messages, contacts, voicemail, maps, camera and selfie photos, gallery, Socials, banking, plans, settings, and an app store.
- **Cellular infrastructure:** Network servers, antennas, signal boosters, chargers, and a creative-only test tower.
- **Player-run carriers:** Create networks and plans, manage subscriptions, and configure roaming between carriers.
- **Signal and power:** Coverage varies with distance and terrain; infrastructure accepts compatible Forge Energy (FE).
- **Emergency services:** Send an SOS with your location and review reports from an operator's dispatch console.

## Installation

1. Install Minecraft **1.21.1** and NeoForge **21.1.255**.
2. Place the mod JAR in your Minecraft `mods` folder.
3. Start Minecraft using the NeoForge profile.

For multiplayer, install the same mod build on the server and each client. Simple Voice Chat is optional for voice-call integration; without it, calls use the existing text-call fallback.

## Quick start

1. Find the **Mobile Cell Network** creative tab and take a phone, Network Server, and antenna.
2. Place the server, open **Networks**, and create a network. In **Plans**, create a plan for that carrier.
3. Place an antenna, supply power, and assign it to your network.
4. Right-click your phone, complete setup, and choose the network and plan in the **Plan** app.
5. Stay within coverage of an eligible antenna to use cellular services.

Servers, antennas, and boosters accept FE from compatible mods. Operators can temporarily bypass their power requirements for testing:

```text
/mcn powertest true
/mcn powertest false
```

The bypass does not power the phone charger or grant a plan, coverage, or service. The `requireEnergy` configuration can also disable infrastructure power requirements.

## Phone controls

| Action | Control |
|---|---|
| Open the phone | Right-click while holding it |
| Toggle the compact held-phone view | Press **P**; configurable in Minecraft controls |
| Unlock | Swipe up and enter your PIN if configured |
| Edit the home screen | Press and hold an app, then drag to rearrange |
| Switch cameras | In Camera, select **Rear** or **Front** in the viewfinder |
| Charge a phone | Right-click a powered charger while holding it; use an empty hand to retrieve it |

Sneak-right-click a charger with an empty hand to open its menu. The charge percentage appears above an inserted phone.

## Infrastructure at a glance

| Block | Purpose |
|---|---|
| **Network Server** | Manage networks and plans. Sneak-place racks to stack them visually; each remains a separate server. |
| **Copper, Iron, and Diamond Antennas** | Broadcast progressively higher-tier service. Range and terrain affect coverage. |
| **Signal Booster** | Relays an existing eligible tower signal within 32 blocks; it cannot create signal or relay another booster. |
| **Cell Signal Test Antenna** | Creative-only ten-block tower for testing coverage. Leave nine clear blocks above the base. |
| **Phone Charger** | Recharges a phone using FE; the infrastructure test bypass does not power it. |
| **Emergency Dispatch** | Operator console for reviewing and resolving SOS reports. |

Server and booster indicators show power and low-energy status; the rack's blue light indicates a managed network, while the booster's blue light indicates power bypass. See the [user guide](USER_GUIDE.md) for all indicator meanings and setup details.

## Useful operator commands

| Command | Description |
|---|---|
| `/mcn signal here` | Inspect local signal and obstructions. |
| `/mcn signaltest <player> <true\|false>` | Toggle test-only coverage readings for a player. |
| `/mcn powertest <true\|false>` | Toggle the infrastructure power bypass. |
| `/mcn plan list` | List available plans. |
| `/mcn plan reload` | Reload data-driven plans. |
| `/mcn network list` | List networks. |

These commands require operator permission. Test-only signal readings do not grant calls, messages, data, or billing exemptions. Additional commands and configuration options are in the [user guide](USER_GUIDE.md).

## Current limitations

- The server's Antennas, Players, and Billing sections are not yet available.
- The physical ATM and Shop Terminal interfaces are not implemented.
- Custom app importing and non-FE power adapters are not implemented.
- Some recent features still need in-game and multiplayer verification.

## Build from source

With Java 21 installed, run the Gradle wrapper from the project directory:

```powershell
.\gradlew.bat build
```

On macOS or Linux, use `./gradlew build`. The resulting JAR is written to `build/libs/`.

## Documentation

Read the [user guide](USER_GUIDE.md) for detailed phone controls, apps, infrastructure, plans, roaming, emergency features, and administrator options.

The bundled Lato font is distributed under the [SIL Open Font License](src/main/resources/assets/mobilecellnetwork/font/lato-ofl.txt).
