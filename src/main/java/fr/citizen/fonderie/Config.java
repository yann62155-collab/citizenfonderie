package fr.citizen.fonderie;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Fichier config/citizenfonderie-common.toml (modifiable par FTP sur le serveur). */
public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue BLOQUER_FOURS_VANILLA = BUILDER
            .comment("Si true, les fours, hauts fourneaux et fumoirs vanilla ne peuvent plus fondre les minerais",
                    "(items du tag citizenfonderie:minerais). Il faut alors un four de forgeron.")
            .define("bloquerMineraisFoursVanilla", true);

    public static final ModConfigSpec.BooleanValue ACCEPTER_TOUTES_CUISSONS = BUILDER
            .comment("Si true, les fours de forgeron acceptent tout ce qui a une recette de four (sable, nourriture...).",
                    "Si false, uniquement les minerais (tag citizenfonderie:minerais).")
            .define("accepterToutesCuissons", false);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {
    }

    public static boolean bloquerFoursVanilla() {
        return SPEC.isLoaded() ? BLOQUER_FOURS_VANILLA.get() : true;
    }

    public static boolean accepterToutesCuissons() {
        return SPEC.isLoaded() ? ACCEPTER_TOUTES_CUISSONS.get() : false;
    }
}
