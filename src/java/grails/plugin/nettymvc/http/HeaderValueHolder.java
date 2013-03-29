package grails.plugin.nettymvc.http;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * Based on package-scope org.springframework.mock.web.HeaderValueHolder
 *
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
public class HeaderValueHolder {

	protected List<Object> values = new LinkedList<Object>();

	/**
	 * Find a HeaderValueHolder by name, ignoring casing.
	 * @param headers the Map of header names to HeaderValueHolders
	 * @param name the name of the desired header
	 * @return the corresponding HeaderValueHolder,
	 * or <code>null</code> if none found
	 */
	public static HeaderValueHolder getByName(Map<String, HeaderValueHolder> headers, String name) {
		Assert.notNull(name, "Header name must not be null");
		for (String headerName : headers.keySet()) {
			if (headerName.equalsIgnoreCase(name)) {
				return headers.get(headerName);
			}
		}
		return null;
	}

	public void setValue(Object value) {
		values.clear();
		values.add(value);
	}

	public void addValue(Object value) {
		values.add(value);
	}

	public List<Object> getValues() {
		return Collections.unmodifiableList(values);
	}

	public List<String> getStringValues() {
		List<String> stringList = new ArrayList<String>(values.size());
		for (Object value : values) {
			stringList.add(value.toString());
		}
		return Collections.unmodifiableList(stringList);
	}

	public Object getValue() {
		return values.isEmpty() ? null : values.get(0);
	}

	public String getStringValue() {
		return values.isEmpty() ? null : values.get(0).toString();
	}

	public void addValues(Collection<?> c) {
		values.addAll(c);
	}

	public void addValueArray(Object array) {
		CollectionUtils.mergeArrayIntoCollection(array, values);
	}
}
