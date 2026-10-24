"""Focused data-policy check for the two shipped presets (Python standard library only)."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / "src/main/resources/data"

ISLAND_BIOMES = ("sandy_island", "jungle_island", "exposed_cliff", "volcanic_island")

MARINE_BIOMES = (
    "hadal_trench", "abyssal_basin", "basalt_ridge", "volcanic_slope",
    "seamount_slope", "rocky_seabed", "sand_shelf", "kelp_grove",
    "coral_garden", "sheltered_lagoon",
)

def read(path):
    return json.loads((DATA / path).read_text(encoding="utf-8"))

profiles = {}
for name, island_policy in (("oceanblock", False), ("exploration", True)):
    ns = f"magitema_ocean/worldgen/"
    preset = read(ns + f"world_preset/{name}.json")
    overworld = preset["dimensions"]["minecraft:overworld"]
    assert overworld["type"] == "magitema_ocean:deep_overworld"
    assert overworld["generator"]["type"] == "minecraft:noise"
    assert overworld["generator"]["settings"] == f"magitema_ocean:{name}"
    assert overworld["generator"]["biome_source"]["type"] == "magitema_ocean:marine"
    palette = overworld["generator"]["biome_source"]["biomes"]
    expected_palette = [f"magitema_ocean:{name}" for name in MARINE_BIOMES]
    expected_palette += [f"magitema_ocean:{name}" for name in ISLAND_BIOMES]
    expected_palette += ["magitema_ocean:stone_caves", "magitema_ocean:living_caves"]
    assert palette == expected_palette
    settings = read(ns + f"noise_settings/{name}.json")
    assert settings["noise"] == {"min_y": -704, "height": 1024, "size_horizontal": 1, "size_vertical": 2}
    assert settings["sea_level"] == 63
    assert settings["aquifers_enabled"] is False and settings["ore_veins_enabled"] is False
    floor = settings["noise_router"]["continents"]
    assert floor["type"] == "magitema_ocean:floor" and floor["islands"] is island_policy
    final = settings["noise_router"]["final_density"]
    assert final["type"] == "minecraft:interpolated"
    assert final["argument"]["type"] == "magitema_ocean:terrain"
    floor_biomes = {
        entry["if_true"].get("biome_is", [None])[0]: entry["then_run"].get("then_run", {}).get("result_state", {}).get("Name")
        for entry in settings["surface_rule"]["sequence"]
        if entry.get("if_true", {}).get("type") == "minecraft:biome"
    }
    assert floor_biomes["magitema_ocean:basalt_ridge"] == "minecraft:basalt"
    assert floor_biomes["magitema_ocean:sand_shelf"] == "minecraft:sand"
    tag = read(f"magitema_ocean/tags/worldgen/structure_set/{name}.json")
    assert tag["replace"] is True
    assert set(tag["values"]) == {
        f"magitema_ocean:deep_salvage_{name}",
        "nautec:crystal_geodes", "nautec:research_outposts", "nautec:ruins_1",
    }
    structure_set = read(ns + f"structure_set/deep_salvage_{name}.json")
    assert structure_set["structures"] == [{"structure": "magitema_ocean:deep_salvage", "weight": 1}]
    assert structure_set["placement"]["type"] == "minecraft:random_spread"
    profiles[name] = settings

assert profiles["oceanblock"]["noise_router"]["continents"]["islands"] is False
assert profiles["exploration"]["noise_router"]["continents"]["islands"] is True
marine_tag = read("magitema_ocean/tags/worldgen/biome/marine.json")
assert set(marine_tag["values"]) == {f"magitema_ocean:{name}" for name in MARINE_BIOMES}
marine_ids = set(marine_tag["values"])
assert "#magitema_ocean:marine" in read("minecraft/tags/worldgen/biome/is_ocean.json")["values"]
assert "#magitema_ocean:marine" in read("c/tags/worldgen/biome/is_ocean.json")["values"]
assert "#magitema_ocean:marine" in read("minecraft/tags/worldgen/biome/is_overworld.json")["values"]
assert set(read("magitema_ocean/tags/worldgen/biome/surface_biomes.json")["values"]) == {"#magitema_ocean:marine"} | {f"magitema_ocean:{name}" for name in ISLAND_BIOMES}
for biome in ISLAND_BIOMES:
    assert f"magitema_ocean:{biome}" in read("minecraft/tags/worldgen/biome/is_overworld.json")["values"]
    definition = read(f"magitema_ocean/worldgen/biome/{biome}.json")
    assert "spawners" in definition and definition["features"] == []
deep_ids = {
    "magitema_ocean:hadal_trench", "magitema_ocean:abyssal_basin",
    "magitema_ocean:basalt_ridge", "magitema_ocean:volcanic_slope",
    "magitema_ocean:seamount_slope", "magitema_ocean:rocky_seabed",
}
assert set(read("minecraft/tags/worldgen/biome/is_deep_ocean.json")["values"]) == deep_ids
assert set(read("c/tags/worldgen/biome/is_deep_ocean.json")["values"]) == deep_ids
assert set(read("nautec/tags/worldgen/biome/abyssal.json")["values"]) == {
    "magitema_ocean:hadal_trench", "magitema_ocean:abyssal_basin"
}
assert set(read("nautec/tags/worldgen/biome/reef.json")["values"]) == {
    "magitema_ocean:coral_garden", "magitema_ocean:sand_shelf", "magitema_ocean:sheltered_lagoon"
}
assert set(read("nautec/tags/worldgen/biome/vents.json")["values"]) == {
    "magitema_ocean:hadal_trench", "magitema_ocean:basalt_ridge", "magitema_ocean:volcanic_slope"
}
for biome in MARINE_BIOMES:
    definition = read(f"magitema_ocean/worldgen/biome/{biome}.json")
    assert definition["effects"]["water_color"] != 0
    assert "spawners" in definition and "features" in definition
channels = read("magitema_ocean/worldgen/noise/channels.json")
assert channels["firstOctave"] == -11
ore_outputs = read("magitema_ocean/tags/block/exploration_ores.json")
ore_hosts = read("magitema_ocean/tags/block/exploration_ore_hosts.json")
ore_exclusions = read("magitema_ocean/tags/block/exploration_ore_exclusions.json")
assert ore_outputs["replace"] is False and "minecraft:diamond_ore" in ore_outputs["values"]
assert ore_outputs["replace"] is False and "minecraft:deepslate_emerald_ore" in ore_outputs["values"]
assert {"create:zinc_ore", "magitech:fluorite_ore", "magitech:tourmaline_ore", "zps:lithium_ore"}.issubset(ore_outputs["values"])
assert {"id": "#c:ores", "required": False} in ore_outputs["values"]
assert ore_exclusions["replace"] is False
assert {"minecraft:ancient_debris", "minecraft:nether_gold_ore", "minecraft:nether_quartz_ore"}.issubset(ore_exclusions["values"])
assert {"createreautomated:copper_node", "createreautomated:nether_quartz_node"}.issubset(ore_exclusions["values"])
assert ore_hosts["replace"] is False and set(ore_hosts["values"]) == {"minecraft:stone", "minecraft:deepslate"}
ore_source = (ROOT / "src/main/java/com/sanhiruzu/magitemaocean/OceanOreVein.java").read_text(encoding="utf-8")
assert "EXCLUDED_ORES" in ore_source and "holder.is(EXCLUDED_ORES)" in ore_source
for profile in ("oceanblock", "exploration"):
    allowed = read(f"magitema_ocean/tags/worldgen/placed_feature/{profile}.json")
    assert allowed["replace"] is True
    assert {"nautec:deep_kelp", "nautec:abyssal_coral", "nautec:prismarine_frond"}.issubset(allowed["values"])
    assert {"minecraft:sea_pickle", "minecraft:warm_ocean_vegetation", "magitema_ocean:vent_cluster", "nautec:vent_tubeworm"}.issubset(allowed["values"])
    for biome in MARINE_BIOMES + (ISLAND_BIOMES if profile == "exploration" else ()):
        scoped = read(f"magitema_ocean/tags/worldgen/placed_feature/{profile}_{biome}.json")
        assert scoped["replace"] is False and isinstance(scoped["values"], list)
assert {"minecraft:trees_sparse_jungle", "minecraft:patch_grass_jungle", "minecraft:patch_dead_bush_badlands"}.issubset(
    set(read("magitema_ocean/tags/worldgen/placed_feature/exploration.json")["values"])
)
assert set(read("magitema_ocean/tags/worldgen/placed_feature/exploration_jungle_island.json")["values"]) == {"minecraft:trees_sparse_jungle", "minecraft:patch_grass_jungle"}
assert set(read("magitema_ocean/tags/worldgen/placed_feature/exploration_sandy_island.json")["values"]) == {"minecraft:patch_dead_bush_badlands"}
assert {"minecraft:sea_pickle", "minecraft:warm_ocean_vegetation"}.issubset(
    set(read("magitema_ocean/tags/worldgen/placed_feature/oceanblock_coral_garden.json")["values"])
)
edge_sensitive = set(read("magitema_ocean/tags/worldgen/placed_feature/chunk_edge_sensitive.json")["values"])
edge_sensitive_large = set(read("magitema_ocean/tags/worldgen/placed_feature/chunk_edge_sensitive_large.json")["values"])
assert {"minecraft:trees_sparse_jungle", "minecraft:warm_ocean_vegetation", "minecraft:sea_pickle"}.issubset(edge_sensitive)
assert {"minecraft:patch_grass_jungle", "minecraft:sea_pickle", "nautec:prismarine_sand_ocean"}.issubset(edge_sensitive_large)
assert {"magitema_ocean:vent_cluster", "nautec:vent_tubeworm"}.issubset(
    set(read("magitema_ocean/tags/worldgen/placed_feature/oceanblock_hadal_trench.json")["values"])
)
assert read("magitema_ocean/worldgen/placed_feature/vent_cluster.json")["feature"] == "magitema_ocean:vent_cluster"
assert read("magitema_ocean/worldgen/configured_feature/vent_cluster.json")["type"] == "magitema_ocean:vent_cluster"
vent_feature = (ROOT / "src/main/java/com/sanhiruzu/magitemaocean/OceanVentClusterFeature.java").read_text(encoding="utf-8")
assert "NAUTEC_VENT" in vent_feature and "distanceSquared > 4" in vent_feature
decoration = (ROOT / "src/main/java/com/sanhiruzu/magitemaocean/OceanDecoration.java").read_text(encoding="utf-8")
marine_source = (ROOT / "src/main/java/com/sanhiruzu/magitemaocean/MarineBiomes.java").read_text(encoding="utf-8")
assert "h.value().placeWithBiomeCheck(level,generator,random" in decoration and 'biome.equals("kelp_grove")' in decoration
assert "features.sort(Comparator.comparing" in decoration and "forChunk(cp,candidateMargin)" in decoration
biome_filter = (ROOT / "src/main/java/com/sanhiruzu/magitemaocean/mixin/OceanPlacedFeatureBiomeFilterMixin.java").read_text(encoding="utf-8")
assert "OceanFeatureScopes.allowsInBiome" in biome_filter
feature_scopes = (ROOT / "src/main/java/com/sanhiruzu/magitemaocean/OceanFeatureScopes.java").read_text(encoding="utf-8")
assert '"surface_biomes"' in feature_scopes and 'profile + "_" + path' in feature_scopes
assert "return !scoped || matches" in feature_scopes
assert "OceanFeaturePlacementBoundsMixin" in (ROOT / "src/main/resources/magitema_ocean.mixins.json").read_text(encoding="utf-8")
assert "floor > 63" in marine_source and "floor >= 78" in marine_source
assert "ConcurrentHashMap" in marine_source
assert "putBounded(surfaceChoices, key, computed, 8192)" in marine_source
assert "putBounded(floorSamples, key, computed, 32768)" in marine_source
assert "OceanPlacedFeatureBiomeFilterMixin" in (ROOT / "src/main/resources/magitema_ocean.mixins.json").read_text(encoding="utf-8")
mapping = read("nautec/data_maps/block/bacteria_obtaining.json")
assert mapping["replace"] is False
assert mapping["values"]["minecraft:sand"]["biome"] == "magitema_ocean:marine"
assert mapping["values"]["minecraft:podzol"]["biome"] == "magitema_ocean:marine"
iron = read("magitema_ocean/recipe/iron_nugget_from_seafloor.json")
assert {item["item"] for item in iron["ingredients"]} == {"minecraft:gravel", "minecraft:charcoal"}
assert iron["result"]["id"] == "minecraft:iron_nugget"
for p in (DATA / "magitema_ocean").rglob("*.json"):
    json.loads(p.read_text(encoding="utf-8"))
print("PASS: both actual Overworld presets, ten marine and four island biomes, ocean/deep-ocean and NauTec tags, biome-scoped surface/feature policies, expanded bounds, profile rules, and all JSON parse.")
