package com.sanhiruzu.magitemaocean;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
public final class OceanState extends SavedData {
    public boolean platformBuilt;
    public boolean firstPlayerPlaced;
    public static OceanState get(ServerLevel level) { return level.getDataStorage().computeIfAbsent(new Factory<>(OceanState::new,OceanState::load),"magitema_ocean_start"); }
    private static OceanState load(CompoundTag tag, HolderLookup.Provider provider) { var s=new OceanState();s.platformBuilt=tag.getBoolean("platformBuilt");s.firstPlayerPlaced=tag.getBoolean("firstPlayerPlaced");return s; }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) { tag.putBoolean("platformBuilt",platformBuilt);tag.putBoolean("firstPlayerPlaced",firstPlayerPlaced);return tag; }
}
