package com.cu6.bathappypro;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.slf4j.Logger;

import java.lang.reflect.Modifier;


@Mod(BatHappyPro.MOD_ID)
public class BatHappyPro{
    public static final String MOD_ID = "bathappypro";

    public static final Logger LOGGER = LogUtils.getLogger();
    public static final String VERSION = ModList.get().getModFileById(BatHappyPro.MOD_ID).versionString();
    public static final Gson STD_GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .excludeFieldsWithModifiers(Modifier.STATIC, Modifier.TRANSIENT)
            .create();

    private static final ResourceLocation TIP = new ResourceLocation("bathappypro", "texture/gui/tip.png");
    public BatHappyPro()
    {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.configSpec);
        Network.init();
    }
}
