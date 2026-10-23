package com.sanhiruzu.magitemaocean;
import com.google.gson.Gson;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.storage.LevelResource;

/** Refuses a silent in-place generator change for saves created with this preset. */
public final class WorldFingerprint {
 private WorldFingerprint() {}
 public static void verify(MinecraftServer server) {
  var stems=server.registryAccess().registryOrThrow(Registries.LEVEL_STEM);
  var overworld=stems.get(LevelStem.OVERWORLD);
  if(overworld==null || !(overworld.generator() instanceof NoiseBasedChunkGenerator n) || !OceanWorld.isOcean(n)) return;
  try {
   var values=new TreeMap<String,Object>();
   values.put("implementation","magitema_ocean:terrain-v1");
   values.put("seed",server.getWorldData().worldGenOptions().seed());
   var floor=OceanWorld.unwrap(n.generatorSettings().value().noiseRouter().continents());
   values.put("profile",floor instanceof OceanFloor f && f.islands()?"exploration":"oceanblock");
   // Hash effective data-pack resources, so a higher-priority override also changes the lock.
   for(String prefix:List.of("worldgen","tags/worldgen","neoforge/biome_modifier","nautec/lithostitched/biome_injector","loot_table","recipe")) {
    server.getResourceManager().listResources(prefix,p->true).entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e->{
     try(var in=e.getValue().open()) { values.put(e.getKey().toString(),new String(in.readAllBytes(),StandardCharsets.UTF_8)); }
     catch(Exception ex) { throw new IllegalStateException(ex); }
    });
   }
   byte[] bytes=new Gson().toJson(values).getBytes(StandardCharsets.UTF_8);
   String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
   Path lock=server.getWorldPath(LevelResource.ROOT).resolve("magitema_ocean-settings.sha256");
   if(Files.exists(lock)) {
    String prior=Files.readString(lock).trim();
    if(!prior.equals(hash)) throw new IllegalStateException("Ocean generation resources changed for this save. Restore the recorded mod/datapack versions or start a new world. Expected "+prior+", found "+hash);
   } else Files.writeString(lock,hash+"\n",StandardOpenOption.CREATE_NEW);
  } catch(RuntimeException ex) { throw ex; }
    catch(Exception ex) { throw new IllegalStateException("Could not verify the Magitema Ocean save fingerprint",ex); }
 }
}
