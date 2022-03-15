package com.pirasalbe.models;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import com.pirasalbe.utils.DateUtils;

/**
 * Cache object
 *
 * @author pirasalbe
 *
 * @param <K> Key type
 * @param <V> Value type
 */
public class Cache<K, V> {

	private static class CacheElement<Z> {
		private LocalDateTime insert;
		private Z value;

		public CacheElement(Z value) {
			this.insert = DateUtils.getNow();
			this.value = value;
		}

		public LocalDateTime getInsert() {
			return insert;
		}

		public Z getValue() {
			return value;
		}
	}

	private long maxAgeSeconds;

	private Map<K, CacheElement<V>> values;

	public Cache(long maxAgeSeconds) {
		this.values = new HashMap<>();
		this.maxAgeSeconds = maxAgeSeconds;
	}

	private void checkAge(K key) {
		if (values.containsKey(key)) {
			CacheElement<V> element = values.get(key);

			if (DateUtils.getNow().isAfter(element.getInsert().plusSeconds(maxAgeSeconds))) {
				values.remove(key);
			}
		}
	}

	public V get(K key) {
		V result = null;

		if (containsKey(key)) {
			result = values.get(key).getValue();
		}

		return result;
	}

	public boolean containsKey(K key) {
		checkAge(key);

		return values.containsKey(key);
	}

	public void put(K key, V value) {
		values.put(key, new CacheElement<>(value));
	}

}
