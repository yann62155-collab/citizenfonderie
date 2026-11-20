package fr.citizen.fonderie.mixin;

import fr.citizen.fonderie.Config;
import fr.citizen.fonderie.ModRegistry;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Optional;

/**
 * Four, haut fourneau et fumoir vanilla : ils ne trouvent plus de recette pour les minerais
 * (tag citizenfonderie:minerais). Il faut passer par un four de forgeron.
 * Désactivable avec bloquerMineraisFoursVanilla=false.
 */
@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceBlockEntityMixin {

    @Redirect(
            method = "serverTick",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/item/crafting/RecipeManager$CachedCheck;getRecipeFor(Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/world/level/Level;)Ljava/util/Optional;"),
            require = 0)
    private static Optional<?> citizenfonderie$bloquerMinerais(RecipeManager.CachedCheck<RecipeInput, ?> check,
                                                               RecipeInput input, Level level) {
        if (Config.bloquerFoursVanilla() && input.size() > 0 && input.getItem(0).is(ModRegistry.MINERAIS)) {
            return Optional.empty();
        }
        return check.getRecipeFor(input, level);
    }
}
