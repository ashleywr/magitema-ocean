package com.sanhiruzu.magitemaocean;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.item.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.*;

public final class OceanLifecycle {
    public static final BlockPos SPAWN = new BlockPos(5,64,5);
    @SubscribeEvent public void selectSpawn(LevelEvent.CreateSpawnPosition event) {
        if (event.getLevel() instanceof ServerLevel level && OceanWorld.isOcean(level.getChunkSource().getGenerator())) {
            event.getSettings().setSpawn(SPAWN,0);
            event.setCanceled(true);
        }
    }
    @SubscribeEvent public void starting(ServerAboutToStartEvent event) { WorldFingerprint.verify(event.getServer()); }
    @SubscribeEvent public void firstLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var level = player.serverLevel();
        if (!OceanWorld.isOcean(level.getChunkSource().getGenerator())) return;
        var state = OceanState.get(level);
        if (state.firstPlayerPlaced) return;
        initialize(level);
        player.teleportTo(level, SPAWN.getX()+0.5, SPAWN.getY(), SPAWN.getZ()+0.5, player.getYRot(), player.getXRot());
        state.firstPlayerPlaced = true;
        state.setDirty();
    }
    @SubscribeEvent public void started(ServerStartedEvent event) {
        var level = event.getServer().overworld();
        if (OceanWorld.isOcean(level.getChunkSource().getGenerator())) initialize(level);
    }
    public static void initialize(ServerLevel level) {
        var state = OceanState.get(level);
        if (state.platformBuilt) return;
        // One bounded chunk. No terrain search, per-player grants or statistics checks.
        level.getChunk(0,0);
        var marker = new BlockPos(5,62,5);
        // Crash recovery: a final marker prevents supply duplication if SavedData lags blocks.
        if (!level.getBlockState(marker).is(Blocks.LODESTONE)) {
            for (int x=0;x<=10;x++) for(int z=0;z<=10;z++) {
                level.setBlock(new BlockPos(x,63,z),Blocks.OAK_PLANKS.defaultBlockState(),3);
                for(int y=64;y<=68;y++) level.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),3);
            }
            // Renewable garden, small tree patch, workbench and original supply chest.
            for(int x=1;x<=3;x++) for(int z=1;z<=3;z++) level.setBlock(new BlockPos(x,63,z),Blocks.DIRT.defaultBlockState(),3);
            level.setBlock(new BlockPos(2,64,2),Blocks.OAK_SAPLING.defaultBlockState(),3);
            level.setBlock(new BlockPos(8,64,8),Blocks.CRAFTING_TABLE.defaultBlockState(),3);
            level.setBlock(new BlockPos(1,64,9),Blocks.TORCH.defaultBlockState(),3);
            level.setBlock(new BlockPos(9,64,1),Blocks.TORCH.defaultBlockState(),3);
            var chestPos=new BlockPos(8,64,7);
            level.setBlock(chestPos,Blocks.CHEST.defaultBlockState(),3);
            if (level.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
                ItemStack[] supplies={new ItemStack(Items.OAK_SAPLING,4),new ItemStack(Items.DIRT,24),new ItemStack(Items.BREAD,16),new ItemStack(Items.WHEAT_SEEDS,8),new ItemStack(Items.CARROT,2),new ItemStack(Items.POTATO,2),new ItemStack(Items.KELP,8),new ItemStack(Items.SUGAR_CANE,2),new ItemStack(Items.COBBLESTONE,24),new ItemStack(Items.STRING,6),new ItemStack(Items.BONE_MEAL,12),new ItemStack(Items.LAVA_BUCKET),new ItemStack(Items.WATER_BUCKET),new ItemStack(Items.SAND,8),new ItemStack(Items.PODZOL,2),new ItemStack(Items.STONE,4)};
                for(int i=0;i<supplies.length;i++) chest.setItem(i,supplies[i]);
            }
            level.setBlock(marker,Blocks.LODESTONE.defaultBlockState(),3);
        }
        level.setDefaultSpawnPos(SPAWN,0);
        level.getGameRules().getRule(GameRules.RULE_SPAWN_RADIUS).set(0,level.getServer());
        state.platformBuilt=true;
        state.setDirty();
    }
}
