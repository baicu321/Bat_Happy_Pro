package com.cu6.bathappypro;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.forgespi.language.IModInfo;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.*;
@Mod.EventBusSubscriber()
public class ModListStateChecker {
    private static final List<SimpleModInfo> serverModList;
    private static Map<ServerPlayer, Long> unsafe = Collections.synchronizedMap(new HashMap<>());
    private static Pair<List<? extends String>, List<SimpleModInfo>> whitelistCache;
    private static Pair<List<? extends String>, List<SimpleModInfo>> blacklistCache;

    static {
        ImmutableList.Builder<SimpleModInfo> builder = ImmutableList.builder();
        ModList.get().getMods().forEach(iModInfo -> builder.add(SimpleModInfo.fromIModInfo(iModInfo)));
        serverModList = builder.build();
    }

    @SubscribeEvent
    public static void onServerSetup(ServerStartedEvent event) {
        List<ServerPlayer> shouldRemovedPlayer = new ArrayList<>();
        Thread checker = new Thread(() -> {
            while (true) {
                unsafe.forEach(((player, time) -> {//超时设定
                    if (System.currentTimeMillis() - time >= 1000) {
                        player.connection.disconnect(Component.translatable("bathappypro.timeout"));
                        shouldRemovedPlayer.add(player);
                    }
                }));
                shouldRemovedPlayer.forEach(key -> unsafe.remove(key));
                shouldRemovedPlayer.clear();
                try {
                    //noinspection BusyWait
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }, "ModListState-Checker-Thread");
        checker.setDaemon(true);
        checker.start();
        BatHappyPro.LOGGER.info("ModListState-Checker-Thread start working");
    }

    @SubscribeEvent
    public static void onPlayerJoiningWorld(PlayerEvent.PlayerLoggedInEvent event) {
        if (Config.enableBathappyPro.get()) {
            if (event.getEntity() instanceof ServerPlayer player) {
                Network.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new Network.ModListNeededPack());
                unsafe.put(player, System.currentTimeMillis());
                BatHappyPro.LOGGER.info("Successfully Send ModListNeededPack to Player:{}", player.getName().getString());
            }
        }
    }

    public static void checkModList(ServerPlayer player, List<SimpleModInfo> clientModList) {
        BatHappyPro.LOGGER.info("Received ModListPack from Player:{},Details:\n\t{}", player.getName().getString(), clientModList.toString());
        //暂时移除不安全列表
        unsafe.remove(player);

        boolean shouldKick = false;
        StringJoiner reasonMod = new StringJoiner(", ", "[", "]");

        if (Config.useWhiteList.get()) {
            List<? extends String> whitelistString = Config.whiteList.get();
            List<SimpleModInfo> whitelist;


            //检查白名单数据与缓存数据是否一致
            if (whitelistCache != null && whitelistString.equals(whitelistCache.getFirst())) {
                whitelist = whitelistCache.getSecond();
            } else {
                if (blacklistCache != null) BatHappyPro.LOGGER.info("Config changed!Refreshing Whitelist Cache...");
                whitelist = new ArrayList<>();
                for (String s : whitelistString) {
                    String[] split = s.split("[$]", 2);
                    whitelist.add(new SimpleModInfo(split[0], split.length < 2 ? null : split[1]));
                }
                whitelistCache = new Pair<>(whitelistString, whitelist);
            }
            //检查客户端mod
            loop:
            for (SimpleModInfo modInfo : clientModList) {
                for (SimpleModInfo whiteModInfo : whitelist) {
                    if (modInfo.isMatched(whiteModInfo)) {
                        continue loop;
                    }
                }
                for (SimpleModInfo serModInfo : serverModList) {
                    if (modInfo.isMatched(serModInfo)) {
                        continue loop;
                    }
                }
                shouldKick = true;
                reasonMod.add(modInfo.toString());
            }
        } else {
            List<? extends String> blacklistString = Config.blackList.get();
            List<SimpleModInfo> blacklist;
            //检查黑名单数据与缓存数据是否一致
            if (blacklistCache != null && blacklistString.equals(blacklistCache.getFirst())) {
                blacklist = blacklistCache.getSecond();
            } else {
                if (blacklistCache != null) BatHappyPro.LOGGER.info("Config changed!Refreshing Blacklist Cache...");
                blacklist = new ArrayList<>();
                for (String s : blacklistString) {
                    String[] split = s.split("[$]", 2);
                    blacklist.add(new SimpleModInfo(split[0], split.length < 2 ? null : split[1]));
                }
                blacklistCache = new Pair<>(blacklistString, blacklist);
            }
            //检查客户端mod
            loop:
            for (SimpleModInfo modInfo : clientModList) {
                for (SimpleModInfo blackModInfo : blacklist) {
                    if (modInfo.isMatched(blackModInfo)) {
                        shouldKick = true;
                        reasonMod.add(modInfo.toString());
                        continue loop;
                    }
                }
            }
        }
        if (shouldKick) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            BatHappyPro.LOGGER.warn("Warning!Player:{} installed Unexpected Mod!Cause by: {}", player.getName().getString(), reasonMod.toString());
            player.connection.disconnect(Component.translatable("bathappypro.lost_connection", reasonMod.toString()));
        }
    }

    public record SimpleModInfo(@NotNull String modid, String version) {
        public static SimpleModInfo fromIModInfo(IModInfo modInfo) {
            return new SimpleModInfo(modInfo.getModId(), modInfo.getVersion().toString());
        }

        /**
         * @param std 标准
         * @return 此modInfo是否满足标准
         * modid必须匹配，当提供标准版本时，版本必须匹配
         */
        public boolean isMatched(@NotNull SimpleModInfo std) {
            if (!std.modid.equals(this.modid)) return false;
//			if (std.version != null) if (!std.version.equals(this.version)) return false;
            if (std.version != null) return std.version.equals(this.version);
            return true;
        }

        @Override
        public String toString() {
            return version == null ? modid : modid + version;
        }
    }
}
