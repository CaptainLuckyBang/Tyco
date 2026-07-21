# Tyco — KubeJS & Datapack Reference

Everything in Tyco is data-driven. Every recipe type below works identically whether defined as a JSON file in a datapack (`data/tyco/recipe/<type>/*.json`) or added through KubeJS's `ServerEvents.recipes`. This document covers every recipe type, the config file, and how to safely add, override, or remove Tyco's shipped defaults.

---

## Recipe Types Overview

| Type | Used by | Purpose |
|---|---|---|
| `tyco:generating` | Miner, Lumberjack | Defines what a generator produces from a block below it, and its coin cost |
| `tyco:selling` | Seller | Defines what the Seller converts an item into (coins) |
| `tyco:banking` | Banker | Defines custom currency conversions (built-in Coal→Netherite tiers are config-driven, not recipe-driven — see Config section) |
| `tyco:shop_entry` | Shop | Defines an item for sale, its price, and which category tab it belongs to |
| `tyco:shop_category` | Shop | Defines a category tab's display (text or item icon) |

---

## `tyco:generating` (Miner / Lumberjack)

```js
ServerEvents.recipes(event => {
  event.custom({
    type: 'tyco:generating',
    machine: 'miner',                  // 'miner' or 'lumberjack' - which block this applies to
    blocks: ['minecraft:iron_ore', 'minecraft:deepslate_iron_ore'],
    coin_input: { item: 'tyco:coal_coin' },
    coin_count: 4,
    output: { id: 'minecraft:raw_iron' },
    min_count: 1,                      // optional, default 1
    max_count: 1,                      // optional, default 1
    bonus_chance: 0.05,                // optional, default 0 - chance to override with bonus_count instead
    bonus_count: 2,                    // optional, default 0
    interval: 20                       // ticks between production cycles (20 = 1 second)
  })
})
```

### Weighted output pool (multiple possible results, e.g. a "mystery ore" block)

Use `outputs` instead of `output`/`min_count`/`max_count`/`bonus_chance`/`bonus_count` — if `outputs` is present, it takes priority entirely:

```js
ServerEvents.recipes(event => {
  event.custom({
    type: 'tyco:generating',
    machine: 'miner',
    blocks: ['modid:mystery_ore'],
    coin_input: { item: 'tyco:coal_coin' },
    coin_count: 6,
    outputs: [
      { item: 'minecraft:raw_iron', weight: 50, min_count: 1, max_count: 1 },
      { item: 'minecraft:raw_copper', weight: 30, min_count: 1, max_count: 2 },
      { item: 'minecraft:raw_gold', weight: 15, min_count: 1, max_count: 1 },
      { item: 'minecraft:diamond', weight: 5, min_count: 1, max_count: 1, bonus_chance: 0.05, bonus_count: 2 }
    ],
    interval: 30
  })
})
```

Weights are relative — they don't need to sum to 100.

---

## `tyco:selling` (Seller)

Direction is always **item in → coins out**.

```js
ServerEvents.recipes(event => {
  event.custom({
    type: 'tyco:selling',
    input: { item: 'minecraft:iron_ingot' },
    input_count: 1,
    output: { id: 'tyco:coal_coin', count: 5 },
    interval: 20
  })
})
```

---

## `tyco:banking` (Banker — custom currencies only)

The built-in Coal↔Copper↔Iron↔Gold↔Diamond↔Netherite conversion is **not** driven by this recipe type — it's handled directly by the Banker using live config values (see Config section below), so it stays instantly editable without a recipe reload.

Use `tyco:banking` only for currencies **other than** Tyco's own six coins (e.g. a modpack's own custom currency item):

```js
ServerEvents.recipes(event => {
  event.custom({
    type: 'tyco:banking',
    direction: 'up',                  // 'up' or 'down' - which Banker mode this applies to
    input: { item: 'modid:custom_token' },
    input_count: 10,
    output: { id: 'modid:custom_token_gold' },
    interval: 20
  })
})
```

---

## `tyco:shop_entry` (Shop)

```js
ServerEvents.recipes(event => {
  event.custom({
    type: 'tyco:shop_entry',
    item: { id: 'minecraft:diamond', count: 1 },
    price: 50,                        // always denominated in Coal Coin value
    category: 'Ores'                  // optional, defaults to "Misc"
  })
})
```

Players can pay with any mix of coin tiers — the Shop automatically converts using the live Banker config ratios and gives change back in the largest denominations that fit.

---

## `tyco:shop_category` (Shop tab display)

Optional — any category referenced by a `shop_entry` automatically gets a plain text tab. Define this only if you want a category to show an item icon instead:

```js
ServerEvents.recipes(event => {
  event.custom({
    type: 'tyco:shop_category',
    category: 'Ores',
    icon: 'minecraft:diamond'         // optional - omit entirely for a plain text tab
  })
})
```

---

## Removing or Overriding Shipped Defaults

Every recipe Tyco ships has a predictable ID: `tyco:<recipe_type>/<file_name>`. To replace one, remove it first, then add your own version:

```js
ServerEvents.recipes(event => {
  event.remove({ id: 'tyco:generating/iron' })

  event.custom({
    type: 'tyco:generating',
    machine: 'miner',
    blocks: ['minecraft:iron_ore', 'minecraft:deepslate_iron_ore'],
    coin_input: { item: 'tyco:coal_coin' },
    coin_count: 8,
    output: { id: 'minecraft:raw_iron', count: 2 },
    interval: 100
  })
})
```

### Wiping an entire category of defaults

```js
ServerEvents.recipes(event => {
  event.remove({ type: 'tyco:generating' })   // removes ALL default Miner/Lumberjack recipes
  event.remove({ type: 'tyco:selling' })      // removes ALL default Seller recipes
  event.remove({ type: 'tyco:shop_entry' })   // removes ALL default Shop items
})
```

`tyco:banking` recipes are unaffected by anything above, since the built-in tier conversion doesn't use them at all.

---

## Config File (`config/tyco-common.toml`)

Generated automatically on first launch. Controls the built-in coin tier conversion (Banker) — these apply live, with no recipe reload needed:

```toml
[banker]
    # How many Coal Coins are needed to convert into 1 Copper Coin
    coalToCopperRatio = 10
    # How many Copper Coins are needed to convert into 1 Iron Coin
    copperToIronRatio = 10
    # How many Iron Coins are needed to convert into 1 Gold Coin
    ironToGoldRatio = 10
    # How many Gold Coins are needed to convert into 1 Diamond Coin
    goldToDiamondRatio = 10
    # How many Diamond Coins are needed to convert into 1 Netherite Coin
    diamondToNetheriteRatio = 10
    # How many ticks the Banker takes to perform one coin tier conversion (20 ticks = 1 second)
    conversionIntervalTicks = 20
```

These same ratios are what the Shop uses to calculate change when a player pays with a higher-tier coin than an item's price requires.

---

## Item Tags

Coins belong to the `tyco:coins` tag (`data/tyco/tags/item/coins.json`), which controls what the Miner/Lumberjack/Seller/Banker's input slots accept. Custom currencies added by a pack maker won't be accepted by Tyco's own machines unless added to this tag — but they can still be used freely through the `tyco:banking`/`tyco:shop_entry` recipe types above, which check the specific item ID directly rather than the tag.
