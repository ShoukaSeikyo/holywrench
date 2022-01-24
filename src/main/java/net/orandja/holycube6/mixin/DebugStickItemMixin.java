package net.orandja.holycube6.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.state.property.Property;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.orandja.holycube6.modules.DebugWrench;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;

@Mixin(net.minecraft.item.DebugStickItem.class)
public abstract class DebugStickItemMixin extends net.minecraft.item.Item {
    public DebugStickItemMixin(Settings settings) {
        super(settings);
    }

    @Inject(method = "useOnBlock", at = @At("HEAD"))
    public void useOnBlock(ItemUsageContext context, CallbackInfoReturnable<ActionResult> info) {
        DebugWrench.Companion.useOnSign(context, info);
    }

    @Redirect(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;isCreativeLevelTwoOp()Z"))
    public boolean bypass_creative(PlayerEntity player, PlayerEntity player1, BlockState state, WorldAccess world, BlockPos pos, boolean update, ItemStack stack) {
        return DebugWrench.Companion.allowWrench(player, stack);
    }

    @Redirect(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/state/StateManager;getProperties()Ljava/util/Collection;"))
    public Collection<Property<?>> getProperties(net.minecraft.state.StateManager<?, ?> stateManager, PlayerEntity player, BlockState state, WorldAccess world, BlockPos pos, boolean update, ItemStack stack) {
        return DebugWrench.Companion.getProperties(player, stateManager, state, stack);
    }

    @Redirect(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/DebugStickItem;cycle(Lnet/minecraft/block/BlockState;Lnet/minecraft/state/property/Property;Z)Lnet/minecraft/block/BlockState;"))
    public <T extends Comparable<T>> BlockState getValues(BlockState state, Property<T> property, boolean inverse, PlayerEntity player, BlockState state1, WorldAccess world, BlockPos pos, boolean update, ItemStack stack) {
        return state.with(property, stolen_cycle(player.isCreativeLevelTwoOp() ? property.getValues() : DebugWrench.Companion.getValues(state, property), state.get(property), inverse));
    }

    private <T> T stolen_cycle(Iterable<T> elements, T current, boolean inverse) {
        return inverse ? Util.previous(elements, current) : Util.next(elements, current);
    }
}
