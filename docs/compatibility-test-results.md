# Easy Adventure Compatibility Test Results

Test date: 2026-04-24  
Environment:

- Minecraft `1.20.1`
- Forge `47.4.18`
- Easy Adventure dev workspace
- Create `6.0.8`

## Status Legend

- `PENDING`
- `PASS`
- `FAIL`
- `BLOCKED_BY_MISSING_MOD`
- `BLOCKED_BY_MISSING_OBJECT`
- `NEEDS_NEW_TAG`
- `NEEDS_SPECIAL_COMPAT`

## Environment

| ID | Item | Status | Notes |
|---|---|---|---|
| ENV-1 | Packaged Easy Adventure jar builds successfully | PASS | `build/libs/easyadventure-1.6.1.jar` is generated and ready for normal Forge-client testing. |
| ENV-2 | Create compatibility pass prepared against `create-1.20.1-6.0.8.jar` | PASS | Safety-layer tags and Create entity skip rules are present in current source. |

## A. Ownership / Recovery Security

| ID | Item | Status | Notes |
|---|---|---|---|
| A1 | Recall table only shows the current player's bases | PENDING |  |
| A2 | Normal players cannot recall another player's base | PENDING |  |
| A3 | OP can recover a registered key | PENDING |  |
| A4 | OP can recover a packed orphan structure | PENDING |  |
| A5 | OP can claim an already placed unregistered base | PENDING |  |

## B. Dynamic Light / Fake Block Safety

| ID | Item | Status | Notes |
|---|---|---|---|
| B1 | Immersive Lanterns waist lantern no longer blocks packing | BLOCKED_BY_MISSING_MOD | Immersive Lanterns is not installed in this round. |
| B2 | Vanilla `minecraft:light` is ignored during pack/deploy checks | PENDING |  |

## C. Touhou Little Maid

| ID | Item | Status | Notes |
|---|---|---|---|
| C1 | Maid entity is not captured | PENDING |  |
| C2 | High-risk TLM decorative block entities are blocked before packing | PENDING | Test at least altar, picnic_mat, cchess/wchess, gomoku, shrine, model_switcher, maid_beacon. |
| C3 | Non-block-entity TLM decor either restores cleanly or gets promoted to dangerous tag | PENDING |  |
| C4 | No TLM ghost block remains after blocked or failed packing | PENDING |  |

## D. Mekanism

| ID | Item | Status | Notes |
|---|---|---|---|
| D1 | Dynamic Tank is blocked before packing | PENDING |  |
| D2 | Thermal Evaporation structure is blocked before packing | PENDING |  |
| D3 | Boiler is blocked before packing | PENDING | Main Mekanism jar present, but some objects may depend on addon jars. |
| D4 | Turbine is blocked before packing | PENDING | Main Mekanism jar present, but some objects may depend on addon jars. |
| D5 | Fission / Fusion reactor structures are blocked before packing | BLOCKED_BY_MISSING_MOD | Requires Mekanism Generators, not present in this round. |
| D6 | Induction Matrix and SPS are blocked before packing | PENDING |  |
| D7 | Single-block Mekanism tile entities preserve data after pack/redeploy | PENDING | Start with Energy Cube, Bin, Fluid Tank, Chemical Tank, Metallurgic Infuser, Enrichment Chamber. |
| D8 | Pipe disconnect state does not briefly leak under low-update restore | PENDING | Start with mechanical pipe / pressurized tube / logistical transporter. |
| D9 | Mekanism multiblock storage loss is prevented by blocking | PENDING |  |

## E. Create

| ID | Item | Status | Notes |
|---|---|---|---|
| E1 | Contraption anchor blocks are blocked before packing | PENDING | Bearings, pistons, pulleys, gantries, cart assembler. |
| E2 | Chassis and moving-control blocks are blocked before packing | PENDING | Chassis, contraption controls, minecart anchor, track station. |
| E3 | Portable interfaces and logistics movers are blocked before packing | PENDING | Portable interfaces, chain conveyor, package frogport, packager, repackager. |
| E4 | Create multiblock storages are conservatively blocked | PENDING | Item Vault, Fluid Tank, Creative Fluid Tank. |
| E5 | Create contraption entities are skipped from entity capture | PENDING | Contraption, stationary contraption, gantry contraption, carriage contraption, seat, super glue. |
| E6 | Simple decorative Create blocks still pack and restore normally | PENDING | Start with nixie tube, seat block, valve handle, postbox, table cloth. |

## F. Regression

| ID | Item | Status | Notes |
|---|---|---|---|
| F1 | Vanilla base pack/unpack still works | PENDING |  |
| F2 | Base key recall remains owner-only after compat changes | PENDING |  |
| F3 | OP recovery commands still work after compat changes | PENDING |  |

## Recommended Order For Live Round

1. `B2`
2. `E1`
3. `E2`
4. `E3`
5. `E4`
6. `E5`
7. `E6`
8. `A1` to `A5`
9. `F1` to `F3`

## Notes Format

When recording a result, keep notes in this style:

- world/setup used
- exact object tested
- observed game behavior
- expected behavior
- follow-up action

Example:

`Attempted to pack TLM altar inside a 5x5x5 core. Precheck failed with unsafe-block message before capture. Matches current safe-downgrade design.`
