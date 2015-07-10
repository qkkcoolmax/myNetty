package webSocketChat;

import io.netty.channel.Channel;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * 存放用户名及其对应的Channel。
 * 
 * */
public class UserMap implements Map {

	private Map<String, Channel> map;

	public Map<String, Channel> getMap() {
		return map;
	}

	public UserMap(Map map) {
		this.map = map;
	}

	@Override
	public int size() {

		return map.size();
	}

	@Override
	public boolean isEmpty() {
		// TODO Auto-generated method stub
		return map.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		// TODO Auto-generated method stub
		return map.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return map.containsValue(value);
	}

	@Override
	public Object get(Object key) {
		return map.get(key);
	}

	public Object getByValue(Object value) {
		if (containsValue(value)) {
			Iterator it = map.keySet().iterator();
			while (it.hasNext()) {
				Object Key = it.next();
				if (get(Key) == value) {
					return get(Key);
				}
			}
			return null;
		} else {
			return null;

		}
	}

	@Override
	public Object put(Object key, Object value) {
		if (containsValue(value)) {
			return null;
		}
		return map.put((String) key, (Channel) value);
	}

	@Override
	public Object remove(Object key) {
		// TODO Auto-generated method stub
		return map.remove(key);
	}

	public Object removeByValue(Object value) {
		if (map.containsValue(value)) {
			Iterator it = map.keySet().iterator();
			while (it.hasNext()) {
				Object k = it.next();
				if (map.get(k) == value) {
					it.remove();
					return k;
				}

			}
			return null;
		} else {

			return null;
		}
	}

	@Override
	public void putAll(Map m) {
		// TODO Auto-generated method stub

	}

	@Override
	public void clear() {
		// TODO Auto-generated method stub

	}

	@Override
	public Set keySet() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection values() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set entrySet() {
		// TODO Auto-generated method stub
		return null;
	}

}
