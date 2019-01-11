/* ***** BEGIN LICENSE BLOCK *****
 * 
 * Copyright (C) 2014 Linagora
 *
 * This program is free software: you can redistribute it and/or 
 * modify it under the terms of the GNU Affero General Public License as 
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version, provided you comply 
 * with the Additional Terms applicable for OBM connector by Linagora 
 * pursuant to Section 7 of the GNU Affero General Public License, 
 * subsections (b), (c), and (e), pursuant to which you must notably (i) retain 
 * the “Message sent thanks to OBM, Free Communication by Linagora” 
 * signature notice appended to any and all outbound messages 
 * (notably e-mail and meeting requests), (ii) retain all hypertext links between 
 * OBM and obm.org, as well as between Linagora and linagora.com, and (iii) refrain 
 * from infringing Linagora intellectual property rights over its trademarks 
 * and commercial brands. Other Additional Terms apply, 
 * see <http://www.linagora.com/licenses/> for more details. 
 *
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY 
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License 
 * for more details. 
 *
 * You should have received a copy of the GNU Affero General Public License 
 * and its applicable Additional Terms for OBM along with this program. If not, 
 * see <http://www.gnu.org/licenses/> for the GNU Affero General Public License version 3 
 * and <http://www.linagora.com/licenses/> for the Additional Terms applicable to 
 * OBM connectors. 
 * 
 * ***** END LICENSE BLOCK ***** */
package com.linagora.scheduling;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;

public class ScheduledTask<T extends Task> implements Delayed {

	public static class Id {
		
		public static Id generate() {
			return new Id(UUID.randomUUID());
		}
		
		private UUID id;
		
		private Id(UUID uuid) {
			id = uuid;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Id) {
				Id other = (Id) obj;
				return Objects.equal(id, other.id);
			}
			return false;
		}
		
		@Override
		public int hashCode() {
			return Objects.hashCode(id);
		}
	}
	
	public enum State {
		CANCELED,
		FAILED,
		NEW,
		RUNNING,
		TERMINATED,
		WAITING
	}

	public static <T extends Task> Builder<T> builder() {
		return new Builder<T>();
	}
	
	public static class Builder<T extends Task> {
		
		private ZonedDateTime scheduledTime;
		private T task;
		private ImmutableList.Builder<Listener<T>> listeners;

		private Builder() {
			listeners = ImmutableList.<Listener<T>>builder();
		}
		
		public Builder<T> scheduledTime(ZonedDateTime scheduledTime) {
			this.scheduledTime = scheduledTime;
			return this;
		}
		
		public Builder<T> task(T task) {
			this.task = task;
			return this;
		}
		
		public Builder<T> addListener(Listener<T> listener) {
			listeners.add(listener);
			return this;
		}
		
		public Builder<T> addListeners(List<Listener<T>> listeners) {
			this.listeners.addAll(listeners);
			return this;
		}
		
		public ScheduledTask<T> schedule(Scheduler<T> scheduler) {
			return new ScheduledTask<>(
					Id.generate(), scheduledTime, task, scheduler, 
					new ListenersNotifier<>(ScheduledTask.class, listeners.build()))
				.schedule();
		}
	}
	
	private final Id id;
	private final ZonedDateTime scheduledTime;
	private final T task;
	private final Scheduler<T> scheduler;
	private final ListenersNotifier<T> listenersNotifier;
	private State state;
	
	protected ScheduledTask(Id id, ZonedDateTime scheduledTime, T task, Scheduler<T> scheduler, ListenersNotifier<T> listenersNotifier) {
		this.id = id;
		this.scheduledTime = scheduledTime;
		this.task = task;
		this.scheduler = scheduler;
		this.listenersNotifier = listenersNotifier;
		this.state = State.NEW;
	}
	
	private ScheduledTask<T> schedule() {
		ScheduledTask<T> task = scheduler.schedule(this);
		notifyScheduled();
		return task;
	}

	public boolean cancel() {
		if (scheduler.cancel(this)) {
			notifyCanceled();
			return true;
		}
		return false;
	}

	public State state() {
		return state;
	}

	public Id id() {
		return id;
	}
	
	public ZonedDateTime scheduledTime() {
		return scheduledTime;
	}
	
	public T task() {
		return task;
	}

	/* package */ Runnable runnable() {
		return new Runnable() {
			@Override
			public void run() {
				notifyStart();
				try {
					task.run();
					notifyTerminated();
				} catch (Exception e) {
					notifyFailed(e);
				}
			}
		};
	}
	
	private void notifyScheduled() {
		state = State.WAITING;
		listenersNotifier.notifyScheduled(this);
	}
	
	private void notifyCanceled() {
		state = State.CANCELED;
		listenersNotifier.notifyCanceled(this);
	}
	
	private void notifyStart() {
		state = State.RUNNING;
		listenersNotifier.notifyRunning(this);
	}
	
	private void notifyTerminated() {
		state = State.TERMINATED;
		listenersNotifier.notifyTerminated(this);
	}

	private void notifyFailed(Exception e) {
		state = State.FAILED;
		listenersNotifier.notifyFailed(this, e);
	}
	
	@Override
	public int compareTo(Delayed o) {
		return Long.compare(getDelay(TimeUnit.SECONDS), o.getDelay(TimeUnit.SECONDS));
	}
	
	@Override
	public long getDelay(TimeUnit unit) {
		return 
			unit.convert(
				Duration.between(scheduler.now(), scheduledTime).getSeconds(), 
				TimeUnit.SECONDS);
	}

}
