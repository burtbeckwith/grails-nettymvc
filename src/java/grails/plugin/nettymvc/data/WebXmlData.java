package grails.plugin.nettymvc.data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
public class WebXmlData {

	protected LinkedHashMap<String, FilterData> filters;
	protected Map<String, ServletData> servlets;
	protected int sessionTimeout;
	protected CookieConfigData cookieConfigData;

	public WebXmlData(LinkedHashMap<String, FilterData> filters, Map<String, ServletData> servlets,
			int sessionTimeout, CookieConfigData cookieConfigData) {
		this.filters = filters;
		this.servlets = servlets;
		this.sessionTimeout = sessionTimeout;
		this.cookieConfigData = cookieConfigData;
	}

	public LinkedHashMap<String, FilterData> getFilters() {
		return filters;
	}

	public Map<String, ServletData> getServlets() {
		return servlets;
	}

	public int getSessionTimeout() {
		return sessionTimeout;
	}

	public CookieConfigData getCookieConfigData() {
		return cookieConfigData;
	}

	@Override
	public String toString() {
		return "WebXmlData filters: '" + filters + "' servlets: '" + servlets +
				", sessionTimeout: " + sessionTimeout + ", cookieConfigData: " + cookieConfigData;
	}
}
