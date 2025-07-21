package com.cu6.bathappypro;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class Network {
    static SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(BatHappyPro.MOD_ID, "network"),
            () -> BatHappyPro.VERSION,
            BatHappyPro.VERSION::equals,
            BatHappyPro.VERSION::equals
    );

    static {
        INSTANCE.messageBuilder(ModListNeededPack.class, 0)
                .encoder(ModListNeededPack::serialize)
                .decoder(ModListNeededPack::new)
                .consumerNetworkThread(ModListNeededPack::handler)
                .add();
        INSTANCE.messageBuilder(ModListPack.class, 1)
                .encoder(ModListPack::serialize)
                .decoder(ModListPack::new)
                .consumerNetworkThread(ModListPack::handler)
                .add();
    }

    public static void init() {
    }


    public interface INetworkPack {
        void serialize(FriendlyByteBuf friendlyByteBuf);

        boolean handler(Supplier<NetworkEvent.Context> contextSupplier);
    }
    
    public static class ModListPack implements INetworkPack {
        private final ImmutableList<ModListStateChecker.SimpleModInfo> MOD_LIST;

        public ModListPack(FriendlyByteBuf friendlyByteBuf) {
            try (ByteBufInputStream inputStream = new ByteBufInputStream(friendlyByteBuf);
                 GZIPInputStream gzipInputStream = new GZIPInputStream(inputStream);
                 InputStreamReader reader = new InputStreamReader(gzipInputStream)
            ) {
                ImmutableList.Builder<ModListStateChecker.SimpleModInfo> builder = ImmutableList.builder();
                JsonArray list = BatHappyPro.STD_GSON.fromJson(reader, JsonArray.class);
                list.forEach(jsonElement -> builder.add(new ModListStateChecker.SimpleModInfo(
                        jsonElement.getAsJsonObject().get("modid").getAsString(),
                        jsonElement.getAsJsonObject().get("version").getAsString())));
                MOD_LIST = builder.build();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public ModListPack() {
            ImmutableList.Builder<ModListStateChecker.SimpleModInfo> builder = ImmutableList.builder();
            ModList.get().getMods().forEach((modInfo -> builder.add(new ModListStateChecker.SimpleModInfo(modInfo.getModId(), modInfo.getVersion().toString()))));
            MOD_LIST = builder.build();
        }

        public boolean handler(Supplier<NetworkEvent.Context> contextSupplier) {
            contextSupplier.get().enqueueWork(() -> ModListStateChecker.checkModList(contextSupplier.get().getSender(), MOD_LIST));
            return true;
        }

        public void serialize(FriendlyByteBuf friendlyByteBuf) {
            try (ByteBufOutputStream byteBuf = new ByteBufOutputStream(friendlyByteBuf);
                 GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteBuf);
                 OutputStreamWriter writer = new OutputStreamWriter(gzipOutputStream, StandardCharsets.UTF_8)) {
                BatHappyPro.STD_GSON.toJson(MOD_LIST, writer);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }


    
    public static class ModListNeededPack implements INetworkPack {
        public ModListNeededPack() {
        }

        public ModListNeededPack(FriendlyByteBuf friendlyByteBuf) {
        }

        @Override
        public void serialize(FriendlyByteBuf friendlyByteBuf) {
        }

        @Override
        public boolean handler(Supplier<NetworkEvent.Context> contextSupplier) {
            contextSupplier.get().enqueueWork(() -> INSTANCE.sendToServer(new ModListPack()));
            return true;
        }

    }


}
