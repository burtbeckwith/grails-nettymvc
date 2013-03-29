package grails.plugin.nettymvc.data;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
public class FilterMappingData {

	protected String urlPattern;
	protected List<String> dispatchers = new ArrayList<String>();

	public FilterMappingData(String urlPattern) {
		this.urlPattern = urlPattern;
	}

	public List<String> getDispatchers() {
		return dispatchers;
	}

	public String getUrlPattern() {
		return urlPattern;
	}

	@Override
	public String toString() {
		return "FilterMappingData urlPattern: '" + urlPattern + ", dispatchers: " + dispatchers;
	}
}
