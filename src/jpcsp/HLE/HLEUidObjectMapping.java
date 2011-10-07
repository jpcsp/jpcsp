/*
This file is part of jpcsp.

Jpcsp is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

Jpcsp is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with Jpcsp.  If not, see <http://www.gnu.org/licenses/>.
 */
package jpcsp.HLE;

import java.util.HashMap;

public class HLEUidObjectMapping {
	static public class DoubleHash<TKey, TValue> {
		private HashMap<TKey, TValue> map;
		private HashMap<TValue, TKey> reverseMap;
		
		public DoubleHash() {
			map = new HashMap<TKey, TValue>();
			reverseMap = new HashMap<TValue, TKey>();
		}
		
		public void put(TKey key, TValue value) {
			map.put(key, value);
			reverseMap.put(value, key);
		}
		
		public TValue getValueByKey(TKey key) {
			return map.get(key);
		}

		public TKey getKeyByValue(TValue value) {
			return reverseMap.get(value);
		}
		
		public boolean containsKey(TKey key) {
			return map.containsKey(key);
		}

		public boolean containsValue(TValue value) {
			return reverseMap.containsKey(value);
		}

		public void removeKey(TKey key) {
			TValue value = map.get(key);
			map.remove(key);
			reverseMap.remove(value);
		}
		
		public void removeValue(TValue value) {
			TKey key = reverseMap.get(value);
			map.remove(key);
			reverseMap.remove(value);
		}

		public int size() {
			return map.size();
		}
	}
	
	private static HashMap<Class<?>, DoubleHash<Integer, Object>> map = new HashMap<Class<?>, DoubleHash<Integer, Object>>();

	static public void reset() {
		map.clear();
	}
	
	static protected DoubleHash<Integer, Object> getMapForClass(Class<?> type) {
		if (!map.containsKey(type)) {
			map.put(type, new DoubleHash<Integer, Object>());
		}
		return map.get(type);
	}
	
	static public int addObjectMap(Class<?> type, int uid, Object object) {
		getMapForClass(type).put(uid, object);
		return uid;
	}

	static public int addObjectMap(int uid, Object object) {
		return addObjectMap(object.getClass(), uid, object);
	}

	static public int createUidForObject(Class<?> type, Object object) {
		return addObjectMap(type, getMapForClass(type).size(), object);
	}

	static public int createUidForObject(Object object) {
		return createUidForObject(object.getClass(), object);
	}

	static public Object getObject(Class<?> type, int uid) {
		return getMapForClass(type).getValueByKey(uid);
	}
	
	static public void removeObject(Class<?> type, Object object) {
		getMapForClass(type).removeValue(object);
	}
	
	static public void removeObject(Object object) {
		removeObject(object.getClass(), object);
	}
}
