# Mobile Cell Network

Bring smartphones and player-run cellular networks to Minecraft. Build the infrastructure, create a carrier, and stay connected through in-game phones.

**Minecraft 1.21.1** · **NeoForge 21.1.255** · **Java 21**

> **Work in progress:** Some interfaces and integrations are still being developed. Recent features have been built but need further in-game and multiplayer testing.

Tower section lighting captures its tier before Minecraft constructs block states, avoiding a startup registration crash. The build checks actual section construction and cached light values for every state of all four tiers.

Tower selection outlines cache their exact edge lists per shared section shape. This avoids repeating Minecraft's expensive voxel-edge scan every frame while aiming at a tower, especially Tier 4. Each shape's edge list is calculated once on first highlight and reused across towers.

## Features

- **Two phones:** Snapple's SnapPhone 16 and LanDriod's G24, with distinct interfaces, battery management, and customizable home screens.
- **Phone apps:** Calls, messages, contacts, voicemail, maps, camera and selfie photos, gallery, Socials, banking, plans, settings, and an app store.
- **Cellular infrastructure:** Network servers, antennas, signal boosters, chargers, and four creative-only tower variants.
- **Player-run carriers:** Create networks and plans, manage subscriptions, and configure roaming between carriers.
- **Signal and power:** Coverage varies with distance and terrain; infrastructure accepts compatible Forge Energy (FE).
- **Hands-on installation:** Fit radio electronics, run visible power wires with a cable spool, and commission new towers and boosters with a technician tester.
- **Emergency services:** Send an SOS with your location and review reports from an operator's dispatch console.

## Installation

1. Install Minecraft **1.21.1** and NeoForge **21.1.255**.
2. Place the mod JAR in your Minecraft `mods` folder.
3. Start Minecraft using the NeoForge profile.

For multiplayer, install the same mod build on the server and each client. Simple Voice Chat is optional for voice-call integration; without it, calls use the existing text-call fallback.

## Quick start

1. Find the **Mobile Cell Network** creative tab and take a phone, Network Server, and antenna.
2. Place the server, open **Networks**, and create a network. In **Plans**, create a plan for that carrier.
3. Place an antenna, install a Radio Electronics Module, wire it to an FE output with a Power Cable Spool, and use a Technician Tester to commission it. Assign it to your network.
4. Right-click your phone, complete setup, and choose the network and plan in the **Plan** app.
5. Stay within coverage of an eligible antenna to use cellular services.

Servers, antennas, and boosters accept FE from compatible mods. Operators can temporarily bypass their power requirements for testing:

```text
/mcn powertest true
/mcn powertest false
```

The bypass does not power the phone charger or grant a plan, coverage, or service. The `requireEnergy` configuration can also disable infrastructure power requirements.

### Create: Electro Energetics power

All powered equipment uses the standard NeoForge FE capability: copper, iron, diamond, and test-tower antennas, servers, signal boosters, phone chargers, and the Tower Power Tap. Create: Electro Energetics can supply them through its **FE converter**; no mandatory dependency or change to other FE-mod support is needed.

- Set the converter to **electrical grid -> FE**, with a working Electro Energetics supply. Orient its FE side toward the receiving block (the converter supplies the block opposite its `FACING` direction).
- **Towers and boosters:** Put a Tower Power Tap beside the converter, with one of the tap's sides or bottom at the converter's FE output. Spool-connect the tap's **top** to the equipment, install the radio module, then commission with the tester. The tower's work lights share this supply.
- **Servers and phone chargers:** Put the block directly at the converter's FE output, or use a compatible FE cable/battery network between them. Their FE capability accepts power on any face. Chargers also accept a spool wire from the tap's top terminal; no radio module or commissioning is needed for chargers. Insert a phone to charge it.
- The converter **pushes** FE into its neighbor. Its exposed capability reports an extraction interface but does not allow FE to be pulled out, so **do not spool-connect directly to the converter**. Use the tap or an extractable FE battery as the buffer.

The tap's top is output-only; converter input must use another face. Tower section blocks and decorative cabinet surfaces are not separate FE inputs: use the tap-and-spool route for the wide tower. Phones charge in the charger; unpowered decorative blocks and technician tools do not need FE. Installation, ownership, commissioning, and normal service requirements remain unchanged.

This connection method is based on the upstream [converter implementation](https://github.com/george8188625/Create-Electro-Energetics/blob/1.21.1/src/main/java/com/george_vi/electroenergetics/content/converter/ConverterDevice.java). Automated checks cover the push-only converter contract, tap buffering, and wire transfer; testing both mods together in-game is still required.

### Technician installation

The creative tab contains these tower variants. Tier 1 is a slim monopole with two three-sector antenna arrays, an attached ladder, and a combined ground-level power/radio cabinet and computer. Tiers 2-4 retain their accessible 5-by-5 foundations and two full-height service platforms:

The monopole ladder runs along the west side, clear of the north-facing cabinet door and computer. The cabinet and computer sit forward of the pole, leaving the electronics visible when the door opens, with a rear mounting bracket and an extended foundation. Their click targets and cable socket follow the new position. Reconnect any previously installed monopole cable to move its saved endpoint to the new socket. Its ladder standoffs tie into the pole above the computer. The pole ends below the top beacon lens so its cap cannot flicker through the light.

The monopole inventory/held miniature uses explicit sprite-bounded UVs to prevent tall parts sampling unrelated atlas textures. Its compact hand scale and centered hotbar view keep the full tower visible; the miniature includes the forward cabinet, computer, and red tip beacon.

| Tower | Height | Palette | Base range | Highest signal tier |
|---|---:|---|---:|---|
| Tier 1 | 6 blocks | Weathered gray / ivory monopole | 128 blocks | 2G |
| Tier 2 | 10 blocks | Copper / orange | 256 blocks | 3G |
| Tier 3 | 15 blocks | Silver / blue | 512 blocks | 4G+ (no 5G) |
| Tier 4 | 20 blocks | Existing steel / white | 1024 blocks | 5G+ |

Tiers 2 and 3 place their antenna panels and mounts on the upper platform; the lower platform holds the computer with its original cabinet box underneath, plus safety railings and lights. The thin computer pedestal has been removed. Their ground power cabinets remain unchanged. Install their radio modules in the upper cabinet, not the lower computer cabinet. On the monopole, install the module in the combined ground cabinet.

These are separate creative-only items, not replacements for the existing small copper/iron/diamond antennas. Existing tower registry IDs and saved installation/network state are preserved: previous Tiers 1-3 are now labeled 2-4 without changes to their ranges or signal limits. Tier 4 retains the original tower registry ID. The monopole requires a clear 3-by-3 area, six blocks tall. Terrain, power state, and subscription limits still affect reception. Static tower geometry is cached in GPU buffers; cabinet doors, indicators, and lighting remain dynamic. Headless checks verify geometry and reduced per-frame vertex construction, not an in-game FPS figure. Update both client and server to the same JAR and fully restart after adding the new tower registry entries.

New antennas (including the creative test tower) and boosters start offline:

1. Use a **Radio Electronics Module** on the equipment. On Tier 1, open the combined ground cabinet; on Tiers 2-3, climb to the top platform and open its blue-label radio cabinet; on Tier 4, either platform's blue-label cabinet works. Right-click with an empty hand to open the door, then use the module inside. The exposed cabinet shows the radio board, terminals, and wiring.
2. Use a **Power Cable Spool** on the output face of a compatible FE generator or battery. A live cable preview appears at the attachment. Connect straight to the antenna/booster, or click solid block faces to add up to **16 snap points** along the route, then click the equipment to finish. On wide test towers, open the **yellow-label power cabinet** at the base with an empty hand and use the spool inside it; other tower-frame faces can be used as cable snaps.
3. Each completed cable route can be up to **32 blocks** long and transfers up to **256 FE/tick**, limited by the source output and equipment input. A spool contains **256 cable units**; each connection consumes its full routed length rounded up, including detours. The preview turns red when over the length limit. Clicked endpoints, snap clips, and straight cable sections are saved and synchronized to clients.
4. Supply FE, then use a **Technician Tester** on the equipment to print electrical/radio diagnostics and attempt commissioning. On wide test towers, aim inside any open cabinet. Commissioning requires electronics, a connected wire, and stored power (or power bypass); it does not require network assignment or upstream radio coverage.

**Equipment cable sockets:** New spool connections automatically snap to the Tower Power Tap's top copper terminal, the rear sockets on boosters and chargers, the small antennas' base sockets, and the fixed side gland on each tower's ground power cabinet. The preview and saved connection use the same socket position; clicking elsewhere on a small device still finishes at its socket. Open the tower's base cabinet before connecting it, as before. Metal collars stay attached to the housing, and the tower gland does not move with its door. Straight connections and manually routed block-face snaps are both supported. Chargers can now receive spool power without a radio module or tester; direct FE charging still works. Existing saved cable routes retain their original clicked endpoints; disconnect and reconnect them to adopt the fixed sockets.

The base cabinet door faces north (toward decreasing Z); the upper cabinets and computer screens face west, inward toward the mast-side walkway rather than the railing. Empty-hand right-click toggles each door; door state is saved and synchronized to other players. The doors open inward without blocking the walkway. Cabinet opening works on existing wide towers too, including grandfathered equipment. On legacy narrow towers, use the previous tower-section interactions. The tower-tip beacon uses a clean red lens, the feed mast stops below its support, and crossing lattice braces use separate depth layers to avoid overlapping surfaces.

**Physical access and tower controls:** On lattice Tiers 2-4, enter through the framed doorway on the east side of the foundation to reach the yellow power cabinet; the front lattice remains intact. At either platform, climb above deck height, step right/east from the north-side ladder onto the transfer landing, then move forward onto the platform. Radio cabinets stay in the northeast corner, clear of the antenna panels, with their fronts facing the mast-side walkway. Stand just west to work on them; cable branches run underneath the deck to the rear, not across the work area. The ladder rungs remain solid, so do not try to walk through them. Tier 4 has a computer above each cabinet. Tiers 2-3 instead have their platform computer above a restored lower-deck cabinet box and their radio installation cabinet on the upper deck; their ground computers are unchanged. The Tier 1 monopole's computer is above its combined ground cabinet. Empty-hand right-click a screen to open tower controls, including carrier assignment. Platform terminals keep the menu open relative to the terminal, not the tower base. The screens remain available for setup and diagnostics while the transmitter is offline.

**Tower work lights:** On lattice Tiers 2-4, the original **eight fixtures** remain below the platform corners, four per platform. The Tier 1 monopole has no platform lights. The extra railing and lower-mast floodlights have been removed, including their visual geometry, collision, and light emission. Existing wide towers clear the removed fixtures' saved light states within one second while loaded. The original lamps emit real **level-15** Minecraft block light (the vanilla maximum) while the tower is enabled and has stored FE, an active power state, or the infrastructure power bypass. They can light installation work before the radio is commissioned; they share the tower supply rather than adding a separate FE draw. Their lenses face downward, but vanilla block lighting spreads in all directions rather than projecting shader-style spotlight cones.

The east service doorway has a continuous threshold under both jambs, foundation-mounted base shoes, and header ties to the tapered mast legs. Access openings omit whole obstructing diagonals instead of leaving cut-off brace stubs; full-width headers remain above the platform openings. Antenna support bars terminate behind the panels and meet recessed rear brackets at two heights, rather than poking through their faces. The handheld tower model has four continuous supports under its top cap.

Work lights and collision geometry use shared, initialization-independent dimensions. Automated checks load both geometry-first and lights-first in separate JVMs to guard against startup failures during block registration.

**Custom technician models:** The Power Cable Spool is a three-dimensional reel with insulated windings, metal hub, and exposed copper cable end. The Radio Electronics Module is a three-dimensional circuit board with chips, heat-sink fins, gold traces, and connector sockets; its item-frame view faces the components outward. Both have inventory, held-item, dropped-item, and item-frame views. Installed external cables are solid-looking insulated leads with attachment collars and snap clips, not thin line primitives. They run straight between selected attachments rather than sagging through supporting surfaces. Open cabinets show heat sinks, chips, traces, terminals, wiring, and breakers matching the installed equipment. Ladder standoffs meet full-width mast crossmembers, and every safety-cage hoop is braced to both tapered mast legs without obstructing the climbing lane or transfer landings.

Static tower panels render only exposed surfaces: overlapping deck/landing sections, railing joints, and foundation plates no longer draw duplicate coplanar faces. Raised deck grip strips have their undersides embedded in the deck rather than sharing its top plane, preventing depth flicker without changing collision or access.

Sneak-use the spool on equipment to cut its wire (no cable refund), or sneak-use it in the air to clear the selected starting point and all pending snaps. To choose another source, clear the current selection first. Rewiring requires commissioning again. Wires and clips are non-solid visual connections, not placeable cable blocks or a general-purpose power grid; they do not chain through other antennas or boosters. Snaps guide the route but are not electrical relays. A source must be loaded to deliver power, and buffered energy can keep equipment running when supply stops.

All three technician items have survival crafting recipes and appear in the creative tab. Only an equipment owner or an operator can modify its installation. Breaking equipment loses its installed electronics and wire.

Existing equipment keeps its previous behavior. The `requireInstallation` common configuration (in the coverage section) defaults to `true`; setting it to `false` skips installation requirements for subsequent placements, not equipment already awaiting installation. Power bypass skips only the FE check, not electronics, wiring, or commissioning. Existing decorative tower cable runs remain decorative; the new external power spans carry FE.

### Electrical and signal tester

The **Technician Tester** uses an original rounded 3D mesh, like the phones: a beveled yellow protective boot, dark molded face, recessed LCD, circular selector with raised diagonal grip, labeled buttons, four recessed input jacks, and red/black probes with curved leads. Its MCN markings identify the in-game FE/RF functions rather than pretending to measure real voltage. The detailed model appears in inventory, held, dropped, and framed views. Hold it in either hand to read its backlit physical screen; it samples once per second without spamming chat:

Both probes are held against the boot by two retaining clips, with their curved leads connected to plugs seated in the COM and FE/RF jacks. The live charge gauge reacquires its render buffer after drawing LCD text, avoiding the `Not building!` crash when aiming at equipment with an energy buffer.

HOLD, RANGE, and LIGHT are printed on the raised button faces, not behind them. Selector legends sit outside the dial, and jack labels sit below the sockets; the texture generator rejects labels that would be hidden by the dial or buttons.

- Aim inside an open tower cabinet, or at an antenna/booster, for stored/max FE, signal percentage/tier, installation or power fault, and simulated available wire-source output (up to 256 FE/tick).
- Aim at a power tap, server, charger, or another exposed FE port for its energy buffer. Tap readings also show available top-port output.
- Aim elsewhere or into the air for a physical signal survey. Boosters show direct upstream radio signal at the booster; towers show signal at your location.
- Closed cabinets and unauthorized installations display explicit instructions/errors instead of private equipment readings. The screen distinguishes idle and stale samples; stale readings are not live measurements. FE and normalized RF percentage are real game values, not invented voltage, resistance, or dBm readings.
- The screen is part of the held meter in first person, not a separate GUI. Right-click testing still prints a detailed, persistent report in chat and attempts commissioning; sneak-use remains inspect-only.

Detailed tester controls:

- Use it inside an open tower cabinet (or on a regular antenna/booster) to inspect radio-module installation, commissioning state, stored/max FE, power bypass, wire validity, source availability, source buffer, and simulated available output. Tower reports include enabled/network/power state and FE draw. Normal use still attempts commissioning after the report; **sneak-use** only inspects and does not change installation state.
- Tower reports show that tower's received signal strength **at the tester's location**, including any eligible booster relay. Booster reports show the strongest **direct upstream** tower signal **at the booster**, without chaining other boosters.
- Use it in the air or on an unrelated block to survey the strongest loaded, enabled tower's physical radio coverage at your location. Reports show normalized **0-100% strength**, radio tier, network ID, and source tower coordinates; no signal is explicitly reported as `NO_SERVICE`. These are game-engine radio readings, not invented real-world voltage or dBm values.
- Use it on a **Tower Power Tap**, or empty-hand right-click the tap, to inspect its energy buffer and port instructions.

The meter does not require a charged phone or subscription and does not grant service, plans, or billing exemptions. Signal percentage measures physical coverage, not carrier authorization. Power-source probing simulates extraction without consuming FE; an empty/unloaded/missing source is distinguished from equipment still running on buffered energy. All radio readings still require the relevant chunks and towers to be loaded.

### Tower power tap

Place the craftable **Tower Power Tap** beside the tower as a small distribution cabinet:

1. Feed a compatible FE generator, battery, or energy cable into its **sides or bottom**. The tap actively pulls from adjacent extractable FE outputs and also accepts pushed FE. Configure the external source's facing/output mode as needed.
2. With the **Power Cable Spool**, select the tap's **top copper terminal**, then connect inside the tower's open base power cabinet (or to a regular antenna/booster).
3. Use the tester to check power and commission the equipment.

The tap buffers **100,000 FE**, persists energy across world reloads, and limits each input/output operation to **1,024 FE**. Tower spool wires retain their **256 FE/tick** per-wire limit and maximum **32-block** span. Multiple equipment wires can draw from a tap's shared buffer. It is not a generator: it creates no free energy, receives no energy from the power bypass, and does not pull from other taps, antennas, or boosters. Top is output-only; other faces are input-only. External sources still need compatible FE support (non-FE adapters are not included). Existing direct generator-to-tower connections remain supported. Breaking a tap drops the cabinet but loses its stored FE.

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
| **Signal Booster** | Right-click with an empty hand to select a receivable carrier/network and view power, installation, and direct upstream signal. Automatically relays that carrier's strongest receivable tower within 32 blocks; it cannot create signal or relay another booster. |
| **Tower Power Tap** | Buffered FE distribution cabinet: generator/cable inputs on sides and bottom, tower spool output at the top copper terminal. |
| **Cell Signal Test Antenna** | Creative-only 20-block tapered steel lattice tower on a 5×5 foundation. Solid cross-braces, walkable railed platforms with widened north entrances, a climbable ladder and safety-cage detailing, bolted footings, cable runs, mounts and equipment cabinets. Reserve a clear 5×5 area, 20 blocks tall, centered on the base. Existing narrow towers keep their dimensions; replace them for the wider version. Existing wide towers receive these geometry improvements automatically. |
| **Phone Charger** | Recharges a phone using FE; the infrastructure test bypass does not power it. |
| **Emergency Dispatch** | Operator console for reviewing and resolving SOS reports. |

Server and booster indicators show power and low-energy status; the rack's blue light indicates a managed network, while the booster's blue light indicates power bypass. See the [user guide](USER_GUIDE.md) for all indicator meanings and setup details.

The booster menu lists receivable carriers with their strongest direct signal at the booster. Click a carrier to tune it; **Rescan** refreshes the list. The selection survives restarts and remains selected if its towers become unavailable (no fallback to a different carrier). **Automatic (all carriers)** relays any eligible carrier and is the default for new and existing boosters. The menu is available without power or commissioning, but relaying still requires both. Owners and operators can configure owned installations; normal build permissions and an eight-block interaction limit apply. Phone plans, roaming, network availability, signal attenuation, and tier caps still apply. The tester follows the selected carrier when inspecting upstream signal.

The booster menu adapts to the GUI-scaled window size, uses compact bounded text, and keeps internal carrier IDs in hover details rather than the carrier rows. A powered, commissioned booster with direct tower reception regenerates a strong local signal that falls off over 32 blocks and through obstructions; its output strength is no longer limited to the incoming percentage. Tower hardware, plan, and low-power speed caps still apply. The phone HUD and phone status use this post-booster reception, while the booster menu and tester show the incoming signal before regeneration. An incomplete installation must still be commissioned with the tester before it can boost.

Tower placement checks the actual foundation collision and item-use permissions throughout the footprint. The ladder stops at its physical top; its collision shapes are computed once and reused. Geometry improvements apply to existing wide towers after restarting Minecraft.

The handheld tower miniature uses steel, gray/light-gray concrete, and white panel textures matching the placed tower, rather than the older cyan diamond-panel palette.

Tower geometry and collision-box caches are prepared during common mod setup on clients and servers, before entering a world. This moves the expensive first-use geometry construction out of the first tower placement/render instead of weakening collision or spreading an incomplete tower over multiple ticks. Placing the 500-block structure still performs its normal clearance, permission, and neighbor updates.

Balcony antenna support arms and mounting crossbars use the same beam definitions for rendering and collision, like the mast braces; they are not pass-through decoration.

The cable bundle enters the base cabinet through bent leads and a gland plate, branches into both balcony equipment cabinets, and terminates inside an upper junction box.

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

The build includes `verifyTowerGeometry`: headless checks for climbing and descending, off-center side-landing entry and return, east-side base access, ladder-to-cabinet routes, terminal sightlines and interaction range, rendered antenna-panel clearance from cabinets and screens, landing floor support, eight corner-light emission rules, cross-brace collision, cached block-local shapes, solid ladder rungs and limits, and tower/cable renderer bounds. These checks do not replace in-game or multiplayer testing.

The build also includes `verifyInstallations`, covering installation state, commissioning, legacy saves, persistence, rewiring, FE transfer limits, energy conservation, power-tap directional ports/buffer/simulation, booster carrier tuning/persistence/strongest-source selection and menu data serialization, tester percentage formatting, and custom 3D item model bounds, faces, texture references, and display contexts. Visual wires, item interactions, actual world-light propagation, and third-party generator compatibility still need in-game and multiplayer testing.

Client event registration and screen-related packet handling are isolated from common mod initialization so dedicated servers do not load Minecraft screen classes. The `verifyDedicatedServerClassloading` build check verifies the common entry point, payload registration class, phone-opening packet, and booster menu/entity with Minecraft client/rendering classes blocked. It does not replace a full dedicated-server startup or multiplayer test.

## Documentation

Read the [user guide](USER_GUIDE.md) for detailed phone controls, apps, infrastructure, plans, roaming, emergency features, and administrator options.

The bundled Lato font is distributed under the [SIL Open Font License](src/main/resources/assets/mobilecellnetwork/font/lato-ofl.txt).
