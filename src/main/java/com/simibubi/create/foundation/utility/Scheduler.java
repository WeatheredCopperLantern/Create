package com.simibubi.create.foundation.utility;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.ParametersAreNonnullByDefault;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * A Scheduler. <br>
 * <span style="color: red">Scheduled Tasks do not get saved!!!</span> <br>
 * <p><u><b>Differences to {@link net.minecraft.server.MinecraftServer#execute(Runnable) MinecraftServer.execute():}</b></u> <br>
 * Allows specifying a tick delay. <br>
 * Doesn't run when ticks are frozen using {@link net.minecraft.server.commands.TickCommand TickCommand}. <br>
 * Runs <b>before</b> vanilla tick code. </p>
 *
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class Scheduler extends SavedData {

	//region Fields
	/**
	 * This needs to be a power of 2 so we can use a bitmask instead of modulo for index finding. <br>
	 * Increasing the size will reduce the potential of encountering Runnables that need to run on a future cycle, but will increase memory usage.
	 */
	private static final int QUEUE_SIZE = 64;
	private static final int QUEUE_MASK = QUEUE_SIZE - 1;
	/**
	 * Using a List with relative indexes serves two purposes. <br>
	 * 1. Remove the need to iterate and count down every pending Runnable every Tick <br>
	 * 2. Reuse a fixed set of Lists to keep GC pressure and Memory allocation low <br>
	 * <br>
	 * While still allowing to schedule at any arbitrary delay.
	 */
	private final List<ScheduledTask>[] scheduledRunnables;
	//Temporarily holds Tasks that need to go into the currently in use List.
	private final List<ScheduledTask> pending;
	//Yes this will overflow after ~3.4 Years. No I don't care.
	private int currentTick = 0;
	private int lastMaxTick = 0;
	private boolean ticking = false;
	//endregion

	public void tick(ServerTickEvent.Pre event) {
		if (event.getServer().tickRateManager().isFrozen() && !event.getServer().tickRateManager().isSteppingForward()) return;

		this.ticking = true;
		final int index = this.currentTick & QUEUE_MASK;
		final Iterator<ScheduledTask> it = this.scheduledRunnables[index].iterator();
		while (it.hasNext()) {
			final ScheduledTask task = it.next();

			if (task.targetTick() <= this.currentTick) {
				it.remove();
				task.runnable().run();
			}
		}
		for (int i = 0, size = pending.size(); i < size; i++) {
			scheduledRunnables[index].add(pending.get(i));
		}
		this.pending.clear();
		this.currentTick++;
		this.ticking = false;
		this.setDirty();
	}

	public void levelLoaded(final LevelAccessor world) {
		if (world instanceof final ServerLevel serverLevel) {
			serverLevel.getServer().overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(() -> {
				this.lastMaxTick = 0;
				return this;
			}, (compoundTag, provider) -> {
				this.lastMaxTick = compoundTag.getInt("Tick");
				return this;
			}), "create_scheduler");
		}
	}

	/**
	 * Reschedules a task that was scheduled in the last session while keeping the intended delay. <br>
	 * <span style="color: red">Scheduled Tasks do not get saved!!!</span> <br>
	 *
	 * @param targetTick value returned from {@link #schedule(int, Runnable) schedule} in the previous instance
	 * @param runnable   Code to run
	 * @return {@code targetTick} - pass to {@link #reSchedule(int, Runnable) reSchedule} after a restart to preserve the intended delay
	 */
	public int reSchedule(int targetTick, Runnable runnable) {
		final int delay = Math.max(0, targetTick - this.lastMaxTick - this.currentTick);
		return this.schedule(delay, runnable);
	}

	@Override
	public CompoundTag save(final CompoundTag compoundTag, final HolderLookup.Provider provider) {
		this.setDirty(false);
		compoundTag.putInt("Tick", this.currentTick);
		return compoundTag;
	}

	/**
	 * Schedules a task to run after waiting the specified number of ticks. <br>
	 * <span style="color: red">Scheduled Tasks do not get saved!!!</span> <br>
	 *
	 * @param delay    number of ticks to wait(0 means run next tick)
	 * @param runnable Code to run
	 * @return {@code targetTick} - pass to {@link #reSchedule(int, Runnable) reSchedule} after a restart to preserve the intended delay
	 * @throws IllegalArgumentException if {@code delay < 0}
	 */
	public int schedule(int delay, Runnable runnable) {
		if (delay < 0) {
			throw new IllegalArgumentException("delay must be positive");
		}
		final int targetTick = this.currentTick + delay + (this.ticking ? 1 : 0);
		final ScheduledTask task = new ScheduledTask(targetTick, runnable);
		if (this.ticking && (delay & QUEUE_MASK) == 0) {
			pending.add(task);
		} else {
			scheduledRunnables[targetTick & QUEUE_MASK].add(task);
		}
		return targetTick;
	}

	public void shutdown() {
		for (int i = 0; i < QUEUE_SIZE; i++) {
			this.scheduledRunnables[i].clear();
		}
		this.pending.clear();
	}

	@SuppressWarnings("unchecked")
	public Scheduler() {
		this.scheduledRunnables = new List[QUEUE_SIZE];
		for (int i = 0; i < QUEUE_SIZE; i++) {
			this.scheduledRunnables[i] = new ArrayList<>(4);
		}
		this.pending = new ArrayList<>(4);
	}
}
