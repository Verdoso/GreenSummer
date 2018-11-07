package org.greeneyed.summer.util;

/*-
 * #%L
 * GreenSummer
 * %%
 * Copyright (C) 2018 GreenEyed (Daniel Lopez)
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

import static com.googlecode.cqengine.query.QueryFactory.attribute;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.googlecode.cqengine.ConcurrentIndexedCollection;
import com.googlecode.cqengine.attribute.SimpleAttribute;
import com.googlecode.cqengine.attribute.support.SimpleFunction;
import com.googlecode.cqengine.persistence.onheap.OnHeapPersistence;
import com.googlecode.cqengine.query.option.QueryOptions;
import com.hazelcast.core.AbstractIMapEvent;
import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.MapEvent;
import com.hazelcast.core.Member;
import com.hazelcast.map.listener.EntryAddedListener;
import com.hazelcast.map.listener.EntryEvictedListener;
import com.hazelcast.map.listener.EntryRemovedListener;
import com.hazelcast.map.listener.EntryUpdatedListener;
import com.hazelcast.map.listener.MapClearedListener;
import com.hazelcast.map.listener.MapEvictedListener;
import com.hazelcast.map.listener.MapListener;
import java.util.concurrent.locks.Lock;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class ClusteredConcurrentIndexedCollection<K extends Comparable<K>, O extends Serializable>
		extends ConcurrentIndexedCollection<O> implements MapListener {
	private final IMap<K, O> map;
	private final HazelcastInstance hazelcastInstance;
	private final Lock lock;
	private final Member member;
	private final SimpleAttribute<O, K> primaryKeyAttribute;
	private final ClusteredCollectionEntryListener<O> clusteredCollectionEntryListener;
	private final boolean hasRemoteStorage;
	private final boolean requiresNotification;
	private final boolean isLocallyHandled;

	public ClusteredConcurrentIndexedCollection(String name, HazelcastInstance hazelcastInstance,
			SimpleAttribute<O, K> primaryKeyAttribute, ClusteredCollectionEntryListener<O> entryListener) {
		super(OnHeapPersistence.<O, K>onPrimaryKey(primaryKeyAttribute));
		this.primaryKeyAttribute = primaryKeyAttribute;
		this.hazelcastInstance = hazelcastInstance;
		this.clusteredCollectionEntryListener = entryListener;
		if (hazelcastInstance != null) {
			this.map = this.hazelcastInstance.getMap(name);
			this.map.addEntryListener(new HazelcastEntryListener(), true);
			this.member = hazelcastInstance.getCluster().getLocalMember();
			this.lock = this.hazelcastInstance.getLock(name + "_LOCK");
		} else {
			log.debug("No hazelcast instance defined, acting just locally");
			this.map = null;
			this.member = null;
			this.lock = null;
		}
		this.hasRemoteStorage = this.map != null;
		this.requiresNotification = this.clusteredCollectionEntryListener != null;
		this.isLocallyHandled = hasRemoteStorage || requiresNotification;
	}

	public ClusteredConcurrentIndexedCollection(String name, HazelcastInstance hazelcastInstance,
			SimpleAttribute<O, K> primaryKeyAttribute) {
		this(name, hazelcastInstance, primaryKeyAttribute, null);
	}

	public ClusteredConcurrentIndexedCollection(String name, HazelcastInstance hazelcastInstance,
			SimpleFunction<O, K> primaryKeyFunction, ClusteredCollectionEntryListener<O> entryListener) {
		this(name, hazelcastInstance, attribute(name + "_PK", primaryKeyFunction), entryListener);
	}

	public ClusteredConcurrentIndexedCollection(String name, HazelcastInstance hazelcastInstance,
			SimpleFunction<O, K> attribute) {
		this(name, hazelcastInstance, attribute, null);
	}

	public void resync() {
		if (this.hasRemoteStorage) {
			this.lock.lock();
		}
		try {
			this.stream().forEach(this::notifyRemoval);
			super.clear();
			if (this.hasRemoteStorage) {
				for (O object : this.map.values()) {
					notifyAddition(object);
					super.add(object);
				}
			}
		} finally {
			if (this.hasRemoteStorage) {
				this.lock.unlock();
			}
		}
	}

	private K getKey(O object) {
		return getKey(object, null);
	}

	private K getKey(O object, QueryOptions queryOptions) {
		return this.primaryKeyAttribute.getValue(object, queryOptions);
	}

	private void clearHazelcastMap() {
		if (this.hasRemoteStorage) {
			this.lock.lock();
			try {
				this.map.clear();
			} finally {
				this.lock.unlock();
			}
		}
	}
	
	private void addToHazelcastMap(QueryOptions queryOptions, O objectToAdd) {
	    if (this.hasRemoteStorage) {
	        this.lock.lock();
	        try {
	            this.map.put(getKey(objectToAdd, queryOptions), objectToAdd);
	        } finally {
	            this.lock.unlock();
	        }
	    }
	}

	private void removeFromHazelcast(QueryOptions queryOptions, O objectToRemove) {
		if (this.hasRemoteStorage) {
			this.lock.lock();
			try {
				this.map.remove(getKey(objectToRemove, queryOptions));
			} finally {
				this.lock.unlock();
			}
		}
	}

	private void notifyAddition(O objectToAdd) {
		if (this.requiresNotification) {
			this.clusteredCollectionEntryListener.objectAdded(objectToAdd);
		}
	}

	private void notifyRemoval(O objectToRemove) {
		if (this.requiresNotification) {
			this.clusteredCollectionEntryListener.objectRemoved(objectToRemove);
		}
	}
	
	private void notifyMapCleared() {
	    if (this.requiresNotification) {
	        this.clusteredCollectionEntryListener.cleared();
	    }
	}

	private void handleAddition(O objectToAdd) {
		handleAddition(null, objectToAdd);
	}

	private void handleAddition(QueryOptions queryOptions, O objectToAdd) {
		notifyAddition(objectToAdd);
		addToHazelcastMap(queryOptions, objectToAdd);
	}

	private void handleRemoval(O objectToRemove) {
		handleRemoval(null, objectToRemove);
	}

	private void handleRemoval(QueryOptions queryOptions, O objectToRemove) {
		notifyRemoval(objectToRemove);
		removeFromHazelcast(queryOptions, objectToRemove);
	}

	@Override
	public boolean update(Iterable<O> objectsToRemove, Iterable<O> objectsToAdd) {
		if (this.isLocallyHandled) {
			for (O objectToRemove : objectsToRemove) {
				handleRemoval(objectToRemove);
			}
			for (O objectToAdd : objectsToAdd) {
				handleAddition(objectToAdd);
			}
		}
		return super.update(objectsToRemove, objectsToAdd);
	}

	@Override
	public boolean update(Iterable<O> objectsToRemove, Iterable<O> objectsToAdd, QueryOptions queryOptions) {
		if (this.isLocallyHandled) {
			for (O objectToRemove : objectsToRemove) {
				handleRemoval(queryOptions, objectToRemove);
			}
			for (O objectToAdd : objectsToAdd) {
				handleAddition(queryOptions, objectToAdd);
			}
		}
		return super.update(objectsToRemove, objectsToAdd, queryOptions);
	}

	@Override
	public boolean add(O objectToAdd) {
		if (this.isLocallyHandled) {
			handleAddition(objectToAdd);
		}
		return super.add(objectToAdd);
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean remove(Object objectToRemove) {
		if (this.isLocallyHandled) {
			handleRemoval((O) objectToRemove);
		}
		return super.remove(objectToRemove);
	}
	
	@Override
	public void clear() {
	    if (this.isLocallyHandled) {
	        notifyMapCleared();
	        clearHazelcastMap();
	    }
	    super.clear();
	}

	@Override
	public boolean addAll(Collection<? extends O> objectsToAdd) {
		if (this.isLocallyHandled) {
			for (O objectToAdd : objectsToAdd) {
				handleAddition(objectToAdd);
			}
		}
		return super.addAll(objectsToAdd);
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean removeAll(Collection<?> objectsToRemove) {
		if (this.isLocallyHandled) {
			for (O objectToRemove : (Collection<O>) objectsToRemove) {
				handleRemoval(objectToRemove);
			}
		}
		return super.removeAll(objectsToRemove);
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean retainAll(Collection<?> objectsToRetain) {
		if (this.isLocallyHandled) {
			Set<K> mapToRetain = ((Collection<O>) objectsToRetain).stream().map(this::getKey)
					.collect(Collectors.toSet());
			final Predicate<O> isToBeRetained = object -> mapToRetain.contains(getKey(object));
			List<O> notRetained = this.stream().filter(isToBeRetained.negate()).collect(Collectors.toList());
			for (O objectToRemove : notRetained) {
				handleRemoval(objectToRemove);
			}
		}
		return super.retainAll(objectsToRetain);
	}

	public static interface ClusteredCollectionEntryListener<O extends Serializable> {
	    
	    public void objectAdded(O object);
	    
	    public void objectRemoved(O object);
	    
	    public void cleared();
	    
	}
	
	
	private class HazelcastEntryListener implements EntryAddedListener<K, O>, EntryRemovedListener<K, O>, EntryUpdatedListener<K, O>,
			EntryEvictedListener<K, O>, MapEvictedListener, MapClearedListener {

		private boolean isRemoteEvent(AbstractIMapEvent event) {
			return ClusteredConcurrentIndexedCollection.this.member != event.getMember();
		}

		@Override
		public void entryAdded(EntryEvent<K, O> event) {
			if (isRemoteEvent(event)) {
				log.trace("Entry Added: {}", event);
				ClusteredConcurrentIndexedCollection.this.notifyAddition(event.getValue());
				ClusteredConcurrentIndexedCollection.super.add(event.getValue());
			}
		}

		@Override
		public void entryRemoved(EntryEvent<K, O> event) {
			if (isRemoteEvent(event)) {
				log.trace("Entry Removed: {}", event);
				ClusteredConcurrentIndexedCollection.this.notifyRemoval(event.getOldValue());
				ClusteredConcurrentIndexedCollection.super.remove(event.getOldValue());
			}
		}

		@Override
		public void entryUpdated(EntryEvent<K, O> event) {
			if (isRemoteEvent(event)) {
				log.trace("Entry Updated: {}", event);
				ClusteredConcurrentIndexedCollection.this.notifyRemoval(event.getOldValue());
				ClusteredConcurrentIndexedCollection.super.remove(event.getOldValue());
				ClusteredConcurrentIndexedCollection.this.notifyAddition(event.getValue());
				ClusteredConcurrentIndexedCollection.super.add(event.getValue());
			}
		}

		@Override
		public void entryEvicted(EntryEvent<K, O> event) {
			if (isRemoteEvent(event)) {
				log.trace("Entry Evicted: {}", event);
				ClusteredConcurrentIndexedCollection.this.notifyRemoval(event.getValue());
				ClusteredConcurrentIndexedCollection.super.remove(event.getValue());
			}
		}

		@Override
		public void mapEvicted(MapEvent event) {
			if (isRemoteEvent(event)) {
				log.trace("Map Evicted: {}", event);
				ClusteredConcurrentIndexedCollection.super.clear();
			}
		}

		@Override
		public void mapCleared(MapEvent event) {
			if (isRemoteEvent(event)) {
				log.trace("Map Cleared: {}", event);
				ClusteredConcurrentIndexedCollection.super.clear();
			}
		}
	}
}
