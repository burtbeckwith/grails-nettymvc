package grails.plugin.nettymvc.data;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.Filter;

/**
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
public class FilterData {

	protected String name;
	protected Filter filter;
	protected List<NameAndValue> params = new ArrayList<NameAndValue>();
	protected List<FilterMappingData> mappings = new ArrayList<FilterMappingData>();

	public FilterData(String name, Filter filter) {
		this.name = name;
		this.filter = filter;
	}

	public String getName() {
		return name;
	}

	public Filter getFilter() {
		return filter;
	}

	public List<FilterMappingData> getMappings() {
		return mappings;
	}

	public List<NameAndValue> getParams() {
		return params;
	}

	@Override
	public String toString() {
		return "FilterData name: '" + name + "' class: '" + filter.getClass().getName() +
				"' params: " + params + ", mappings: " + mappings;
	}
}
