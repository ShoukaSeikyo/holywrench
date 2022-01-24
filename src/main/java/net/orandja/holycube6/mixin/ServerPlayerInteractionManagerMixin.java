package net.orandja.holycube6.mixin;

import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import net.orandja.holycube6.modules.DebugWrench;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;

import static net.orandja.holycube6.modules.DebugWrenchKt.isWrench;

@Mixin(ServerPlayerInteractionManager.class)
public abstract class ServerPlayerInteractionManagerMixin {

    @Shadow
    protected ServerPlayerEntity player;
    @Shadow
    protected ServerWorld world;

    private int toolCooldown = 0;

    @Inject(method = "update", at = @At("HEAD"))
    public void update(CallbackInfo ci) {
        if(toolCooldown > 0) {
            toolCooldown--;
        }
    }

    @Inject(method = "processBlockBreakingAction", at = @At(value = "INVOKE", shift = At.Shift.BEFORE, ordinal = 0, target = "Lnet/minecraft/server/world/ServerWorld;canPlayerModifyAt(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/math/BlockPos;)Z"), cancellable = true)
    public void processBlockBreakingAction(BlockPos pos, net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action action, Direction direction, int worldHeight, CallbackInfo info) {
        if(toolCooldown == 0) {
            DebugWrench.Companion.processBlockBreakingAction(player, pos, world, info);
            toolCooldown = 4;
        }
    }

    @Redirect(method = "processBlockBreakingAction", at = @At(value = "INVOKE", target = "Ljava/util/Objects;equals(Ljava/lang/Object;Ljava/lang/Object;)Z"))
    public boolean processBlockBreakingAction_mismatchfix(Object a, Object b) {
        return isWrench(player.getMainHandStack()) || Objects.equals(a, b);
    }
}
