package com.villagershop.mixin;

import com.villagershop.block.ShopBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * Upgrade obsidienne (Fabric) : équivalent du hook getExplosionResistance de
 * NeoForge. Si le comptoir a l'upgrade anti-explosion, on impose une résistance
 * façon obsidienne (1200).
 */
@Mixin(ExplosionDamageCalculator.class)
public class ExplosionDamageCalculatorMixin {

    @Inject(method = "getBlockExplosionResistance", at = @At("HEAD"), cancellable = true)
    private void villagershop$blastProtectedCounter(Explosion explosion, BlockGetter reader, BlockPos pos,
                                                    BlockState state, FluidState fluid,
                                                    CallbackInfoReturnable<Optional<Float>> cir) {
        if (reader.getBlockEntity(pos) instanceof ShopBlockEntity be && be.hasBlastProtection()) {
            cir.setReturnValue(Optional.of(1200.0F));
        }
    }
}
