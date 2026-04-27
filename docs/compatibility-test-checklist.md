# Easy Adventure Compatibility Test Checklist

This checklist is for the current `Easy Adventure` codebase after the first compatibility safety pass.

It is meant to validate two things:

1. The current safe-downgrade rules behave as designed.
2. TLM, Mekanism, and Create objects that are still risky are identified with concrete pass/fail notes instead of vague reports.

## Current Scope

This round focuses on:

- `ignored_during_pack`
- `dangerous_to_pack`
- block entity NBT relocation during restore
- low-neighbor-update restore flow
- Create safe-downgrade coverage for moving contraptions and contraption entities
- owner-only recall behavior
- OP-only recovery flow

This round does **not** claim full deep compatibility with:

- Touhou Little Maid complex entities
- Touhou Little Maid special decorative systems
- Mekanism multiblock contents
- Mekanism network-perfect pipe restoration
- Create moving contraption persistence, glue persistence, or train system fidelity

## Important Code Basis

These files define the current expected behavior:

- `src/main/java/com/eddy1/easyadventure/block/core/CoreCompat.java`
- `src/main/java/com/eddy1/easyadventure/block/core/CorePreflight.java`
- `src/main/java/com/eddy1/easyadventure/block/core/CorePhaseProcessor.java`
- `src/main/java/com/eddy1/easyadventure/block/core/CoreEntityTransport.java`
- `src/main/java/com/eddy1/easyadventure/util/BlockPlacementUtil.java`
- `src/main/resources/data/easyadventure/tags/block/ignored_during_pack.json`
- `src/main/resources/data/easyadventure/tags/block/dangerous_to_pack.json`
- `src/main/resources/data/easyadventure/tags/entity_type/skip_entity_capture.json`

## Test Environment Setup

Current verified installed mods in `run/mods`:

- `Create 6.0.8 (Forge + MC 1.20.1)`

Preliminary startup result:

- The current Create compatibility pass is prepared against the published `create` jar.
- Live validation should be done in a normal packaged Forge client, not `runClient`, because dev-userdev runs can disagree with published mod jars.

Test baseline:

- Forge `1.20.1`
- Current `Easy Adventure` dev sources
- No `Mekanism Generators` jar installed in this round
- No `Immersive Lanterns` jar installed in this round

Suggested world setup:

- New creative test world
- One flat area for Create mechanism tests
- One multiplayer or LAN test pass for owner/permission checks

Suggested test items:

- Base Core
- Base Scroll
- Key Recall Table
- Building blocks for simple control structures
- Create bearings, pistons, pulleys, chassis, logistics blocks, and a few decorative blocks

---

## Section A: Ownership / Recovery Security

### A1. Recall table only shows the current player's bases

- Setup:
  - Player A registers at least 2 bases.
  - Player B registers at least 1 base.
  - Both players use the recall table.
- Expected:
  - Player A only sees Player A records.
  - Player B only sees Player B records.
- Current code basis:
  - `KeyRecallTableBlock` calls `recordsForOwner(player.getUUID())`.
- Result:
  - [ ] Pass
  - [ ] Fail
- Notes:

### A2. Player cannot recall another player's base by normal UI

- Setup:
  - Player A knows Player B's core UUID by any means.
  - Try to use the normal recall flow.
- Expected:
  - Recall fails.
  - No key is issued.
- Current code basis:
  - `BaseRecallService.recall(...)` checks owner UUID match.
- Result:
  - [ ] Pass
  - [ ] Fail
- Notes:

### A3. OP can recover a registered key

- Command:
  - `/easyadventure admin recover_key <player> <core_uuid>`
- Expected:
  - A valid current key is reissued for the target player.
  - The player can use the reissued key normally.
- Result:
  - [ ] Pass
  - [ ] Fail
- Notes:

### A4. OP can recover a packed orphan if storage still exists

- Command:
  - `/easyadventure admin recover_packed <player> <core_uuid> <base_name>`
- Expected:
  - A packed key is rebuilt from storage data.
  - It becomes the target player's registered current key.
- Result:
  - [ ] Pass
  - [ ] Fail
- Notes:

### A5. OP can claim an already placed unregistered base

- Command:
  - `/easyadventure admin claim_placed <player> <pos> <base_name>`
- Expected:
  - The placed core becomes registered to the specified player.
- Result:
  - [ ] Pass
  - [ ] Fail
- Notes:

---

## Section B: Dynamic Light / Fake Block Safety

### B1. Immersive Lanterns waist lantern no longer blocks packing

- Setup:
  - Equip a waist lantern from Immersive Lanterns.
  - Stand inside a base area and attempt to pack.
- Expected:
  - The base does not fail precheck because of fake light.
  - No "light source blocking" false positive appears.
- Current code basis:
  - `ignored_during_pack` skips fake light style blocks during precheck and pack scan.
- Result:
  - [ ] Pass
  - [ ] Fail
  - [ ] BLOCKED_BY_MISSING_MOD
- Notes:

### B2. Vanilla `minecraft:light` no longer blocks deployment or pack scans

- Setup:
  - Place invisible light blocks around the base test area.
- Expected:
  - They are ignored by precheck and deployment occupancy checks.
- Result:
  - [ ] Pass
  - [ ] Fail
- Notes:

---

## Section C: TLM Compatibility Safety Pass

### C1. Maid entity is not captured

- Setup:
  - Place one or more TLM maids inside the base area.
  - Pack the base.
- Expected:
  - Maids are skipped from entity capture.
  - They remain in the world rather than being serialized into the structure.
- Current code basis:
  - `touhou_little_maid:maid` is inside `skip_entity_capture`.
- Result:
  - [ ] Pass
  - [ ] Fail
- Notes:

### C2. TLM decorative block entities are conservatively blocked

- Objects to test:
  - Garden lantern
  - Altar
  - Picnic mat
  - Chessboard
  - Any other TLM decorative object that uses a block entity
- Setup:
  - Put each object inside the base area.
  - Try packing one-by-one and then as a mixed setup.
- Expected:
  - Packing is blocked with an unsafe block message instead of silently producing ghost blocks.
- Current code basis:
  - `CoreCompat.isDangerousToPack(...)` treats TLM namespace + block entity as dangerous.
- Result:
  - [ ] Pass
  - [ ] Fail
- Notes:

### C3. TLM non-block-entity decor does not silently corrupt on restore

- Setup:
  - Identify any TLM decorative object that is not blocked by the previous test.
  - Pack and redeploy it.
- Expected:
  - Either:
    - it restores cleanly and drops correctly, or
    - it is identified as another object that should be added to `dangerous_to_pack`.
- Result:
  - [ ] Pass
  - [ ] Fail
- Notes:

### C4. No TLM "ghost block" remains after failed or blocked packing

- Setup:
  - Reproduce C2 with BoccHUD or other block-outline helpers if available.
- Expected:
  - No transparent non-functional ghost block remains after the blocked action.
- Result:
  - [ ] Pass
  - [ ] Fail
- Notes:

---

## Section D: Mekanism Safety Pass

### D1. Dynamic Tank is blocked before packing

- Objects:
  - `dynamic_tank`
  - `dynamic_valve`
- Expected:
  - Precheck refuses to pack.
  - The player gets a dangerous block style message.
- Result:
  - [ ] Pass
  - [ ] Fail
- Notes:

### D2. Thermal Evaporation structure is blocked before packing

- Objects:
  - controller
  - valve
- Expected:
  - Precheck refuses to pack.
- Result:
  - [ ] Pass
  - [ ] Fail
- Notes:

### D3. Boiler is blocked before packing

- Objects:
  - casing
  - valve
  - superheating element
  - pressure disperser
- Expected:
  - Precheck refuses to pack.
- Result:
  - [ ] Pass
  - [ ] Fail
- Notes:

### D4. Turbine is blocked before packing

- Objects:
  - casing
  - valve
  - vent
  - rotational complex
  - saturating condenser
  - electromagnetic coil
- Expected:
  - Precheck refuses to pack.
- Result:
  - [ ] Pass
  - [ ] Fail
- Notes:

### D5. Fission / Fusion reactor structures are blocked before packing

- Objects:
  - fission reactor casing
  - fission reactor port
  - control rod assembly
  - reactor glass
  - laser focus matrix
  - fusion reactor controller
  - fusion reactor frame
  - fusion reactor port
  - fusion reactor logic adapter
- Expected:
  - Precheck refuses to pack.
- Result:
  - [ ] Pass
  - [ ] Fail
- Notes:

### D6. Induction Matrix and SPS are blocked before packing

- Objects:
  - induction casing
  - induction port
  - induction cells
  - induction providers
  - SPS casing
  - SPS port
  - supercharged coil
- Expected:
  - Precheck refuses to pack.
- Result:
  - [ ] Pass
  - [ ] Fail
- Notes:

### D7. Single-block Mekanism tile entities still survive pack and redeploy

- Suggested objects:
  - Energy Cube
  - Bin
  - Fluid Tank
  - Chemical Tank
  - Metallurgic Infuser
  - Enrichment Chamber
- Setup:
  - Put contents inside each block where relevant.
  - Pack and redeploy a small test base.
- Expected:
  - Block entity data restores at the new coordinates.
  - Contents remain intact for single-block machines and storage.
- Current code basis:
  - `BlockPlacementUtil.loadBlockEntity(...)` rewrites `x/y/z`.
- Result:
  - [ ] Pass
  - [ ] Fail
- Notes:

### D8. Pipe disconnect state does not briefly leak under low-update restore

- Suggested objects:
  - Fluid pipe
  - Pressurized tube
  - Mechanical pipe
  - Logistical transporter if relevant to your pack
- Setup:
  - Create two networks that are manually disconnected with tool-side settings.
  - Fill both sides with visibly different contents.
  - Pack and redeploy.
- Expected:
  - No obvious reconnection burst or content leak on the first tick after deploy.
- Current code basis:
  - Structure blocks are now restored with low neighbor update flags, then neighbor refresh happens later.
- Result:
  - [ ] Pass
  - [ ] Fail
- Notes:

### D9. Mekanism multiblock storage loss is prevented by blocking, not by risky partial support

- Setup:
  - Attempt to pack a built multiblock with contents.
- Expected:
  - The action is blocked before data loss can happen.
  - No "partial success" state occurs.
- Result:
  - [ ] Pass
- [ ] Fail
- Notes:

---

## Section E: Create Safety Pass

### E1. Contraption anchor blocks are blocked before packing

- Objects:
  - Mechanical Bearing
  - Windmill Bearing
  - Clockwork Bearing
  - Mechanical Piston / Sticky Mechanical Piston
  - Rope Pulley / Hose Pulley / Elevator Pulley
  - Gantry Shaft / Gantry Carriage
  - Cart Assembler
- Expected:
  - Precheck refuses to pack with an unsafe-block message.
- Result:
  - [ ] Pass
  - [ ] Fail
- Notes:

### E2. Chassis and moving-control blocks are blocked before packing

- Objects:
  - Linear Chassis
  - Secondary Linear Chassis
  - Radial Chassis
  - Contraption Controls
  - Minecart Anchor
  - Track Station
- Expected:
  - Precheck refuses to pack with an unsafe-block message.
- Result:
  - [ ] Pass
  - [ ] Fail
- Notes:

### E3. Portable interfaces and logistics movers are blocked before packing

- Objects:
  - Portable Storage Interface
  - Portable Fluid Interface
  - Chain Conveyor
  - Package Frogport
  - Packager / Repackager
- Expected:
  - Precheck refuses to pack with an unsafe-block message.
- Result:
  - [ ] Pass
  - [ ] Fail
- Notes:

### E4. Create multiblock storages are conservatively blocked

- Objects:
  - Item Vault
  - Fluid Tank
  - Creative Fluid Tank
- Expected:
  - Precheck refuses to pack before any data-loss scenario.
- Result:
  - [ ] Pass
  - [ ] Fail
- Notes:

### E5. Create contraption entities are skipped from entity capture

- Objects:
  - Contraption
  - Stationary Contraption
  - Gantry Contraption
  - Carriage Contraption
  - Seat
  - Super Glue
- Setup:
  - Move a Create contraption or glue setup through/inside the base area.
- Expected:
  - These entities are ignored by entity capture instead of being serialized into the base snapshot.
- Result:
  - [ ] Pass
  - [ ] Fail
- Notes:

### E6. Simple decorative Create blocks still pack and restore normally

- Suggested objects:
  - Nixie Tube
  - Seat block
  - Valve Handle
  - Postbox
  - Table Cloth
- Expected:
  - Non-dangerous decorative Create content still works.
- Result:
  - [ ] Pass
  - [ ] Fail
- Notes:

---

## Section F: Regression Checks After Compat Changes

### F1. Normal vanilla base pack/unpack still works

- Setup:
  - Standard chest, furnace, crops, bed, signs.
- Expected:
  - No regression from compatibility changes.
- Result:
  - [ ] Pass
  - [ ] Fail
- Notes:

### F2. Base key recall still remains owner-only

- Setup:
  - Repeat with two players after compat layer changes.
- Expected:
  - No cross-owner recall becomes possible.
- Result:
  - [ ] Pass
  - [ ] Fail
- Notes:

### F3. OP recovery commands still work after compat changes

- Expected:
  - `list_bases`
  - `recover_key`
  - `recover_packed`
  - `claim_placed`
  all behave as expected.
- Result:
  - [ ] Pass
  - [ ] Fail
- Notes:

---

## Recommended Outcome Labels

Use one of these per test:

- `PASS`
- `FAIL`
- `BLOCKED_BY_MISSING_MOD`
- `BLOCKED_BY_MISSING_OBJECT`
- `NEEDS_NEW_TAG`
- `NEEDS_SPECIAL_COMPAT`

## What Counts as Success for This Round

This round is successful if:

- fake/dynamic light false positives are gone
- dangerous Mekanism multiblocks are blocked before data loss
- TLM maid is safely skipped from capture
- TLM risky decorative block entities are blocked instead of silently corrupting
- dangerous Create contraption anchors and moving-storage blocks are blocked before data loss
- Create contraption entities are skipped instead of being serialized into base snapshots
- owner-only recall is still enforced
- OP recovery remains usable as a support tool

## Next Actions After Test Run

If a test fails, classify it immediately:

- add to `ignored_during_pack`
- add to `dangerous_to_pack`
- add to `skip_entity_capture`
- needs a dedicated compat adapter
- acceptable safe downgrade

Do not jump straight to "full support" unless the object clearly survives pack and redeploy without side effects.
