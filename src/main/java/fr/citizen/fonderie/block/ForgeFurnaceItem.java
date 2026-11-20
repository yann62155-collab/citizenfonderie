package fr.citizen.fonderie.block;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class ForgeFurnaceItem extends BlockItem {
    private final ForgeTier tier;

    public ForgeFurnaceItem(ForgeFurnaceBlock block, Properties properties) {
        super(block, properties);
        this.tier = block.tier();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.citizenfonderie.taille", tier.blocks(), tier.dimensions())
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.citizenfonderie.emplacements", tier.slots())
                .withStyle(ChatFormatting.GOLD));
    }
}
