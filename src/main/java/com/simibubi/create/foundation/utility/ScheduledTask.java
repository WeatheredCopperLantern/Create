package com.simibubi.create.foundation.utility;

/**
 * This is used instead of a {@link net.createmod.catnip.data.Pair {@code Pair<Integer, Runnable>}} to prevent Integer (un)boxing
 */
public record ScheduledTask(int targetTick, Runnable runnable) {

}
