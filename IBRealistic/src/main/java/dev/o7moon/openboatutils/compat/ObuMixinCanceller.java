package dev.o7moon.openboatutils.compat;

import com.bawnorton.mixinsquared.api.MixinCanceller;

import java.util.List;

/**
 * Cancels OBU (OpenBoatUtils) mixin classes that conflict with IBRealistic.
 * OBU's AbstractBoatMixin uses the same @ModifyConstant injection points as
 * IBRealistic's BoatMixin — having both causes "failed injection check" crash
 * because the second @ModifyConstant can't find the constant already modified
 * by the first.
 *
 * Requires MixinSquared to be present at runtime.
 */
public class ObuMixinCanceller implements MixinCanceller {

    // OBU's AbstractBoatMixin (used in 1.21.3+) conflicts with IBRealistic's BoatMixin
    // on @ModifyConstant targets in updatePaddles (forwardsAccel, turnAccel, backwardsAccel)
    private static final String OBU_ABSTRACT_BOAT_MIXIN = "dev.o7moon.openboatutils.mixin.AbstractBoatMixin";

    @Override
    public boolean shouldCancel(List<String> targetClassNames, String mixinClassName) {
        return OBU_ABSTRACT_BOAT_MIXIN.equals(mixinClassName);
    }
}
