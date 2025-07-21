package com.cu6.bathappypro;

import com.electronwill.nightconfig.core.ConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public class Config {
    public static final ForgeConfigSpec configSpec;
    public static final ForgeConfigSpec.IntValue timeoutTime;
    public static final ForgeConfigSpec.BooleanValue useWhiteList, enableBathappyPro;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> whiteList, blackList;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        useWhiteList = builder.comment("Using whitelist instead of blacklist.", "使用白名单模式代替黑名单模式。")
                .define("useWhitelist", false);
        enableBathappyPro = builder
                .comment("Should BatHappyPro Work.", "是否启用BatHappyPro。")
                .define("enableBatHappyPro", true);
        blackList = builder
                .comment("If there's a mod installed on client ,the player will be refused to join the server.",
                        "如果一个客户端安装的模组出现在黑名单中，那么这个玩家将无法进入服务器。",
                        "You can use '$'and it's exact version to limit the version.",
                        "使用 '$'+准确的版本号 来限制版本号。",
                        "e.g., [\"torcherino\",\"projecte$1.0.0\"].",
                        "例如： [\"torcherino\",\"projecte$1.0.0\"]。")
                .defineList("blacklist", List.of("torcherino"), (o) -> true);
        whiteList = builder
                .comment("If there's a mod that does not exist in either the mods installed on server or the whitelist ,the player will be refused to join the server.",
                        "如果一个客户端的模组既没有出现在服务器所安装的模组中，也没有出现在白名单，那么这个玩家将无法加入服务器。",
                        "NOTE:Server mods that do not appear in the whitelist will be assigned a version number by default.",
                        "注：未出现在白名单中的服务器模组将被默认指定版本号。",
                        "You can use '$'and it's exact version to specify the version.",
                        "使用 '$'+准确的版本号 来限制版本号。")
                .defineList("whitelist", List.of(), (o) -> true);
        timeoutTime = builder
                .comment("Timeout(ms) for client to sync mod-list to server.", "客户端向服务器同步模组列表的超时时间(毫秒)。")
                .defineInRange("timeoutTime", 1000, 1, Integer.MAX_VALUE);

        configSpec = builder.build();
    }
}
