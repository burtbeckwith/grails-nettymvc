package grails.plugin.nettymvc.http;

import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;

/**
 * Based on org.springframework.mock.web.MockFilterConfig.
 *
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
public class NettyFilterConfig implements FilterConfig {

	protected ServletContext servletContext;
	protected String filterName;
	protected Map<String, String> initParameters = new LinkedHashMap<String, String>();

	public NettyFilterConfig(ServletContext servletContext, String filterName) {
		this.servletContext = servletContext;
		this.filterName = filterName;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.FilterConfig#getFilterName()
	 */
	public String getFilterName() {
		return filterName;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.FilterConfig#getServletContext()
	 */
	public ServletContext getServletContext() {
		return servletContext;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.FilterConfig#getInitParameter(java.lang.String)
	 */
	public String getInitParameter(String name) {
		return initParameters.get(name);
	}

	/* (non-Javadoc)
	 * @see javax.servlet.FilterConfig#getInitParameterNames()
	 */
	public Enumeration<String> getInitParameterNames() {
		return Collections.enumeration(initParameters.keySet());
	}

	public void addInitParameter(String name, String value) {
		initParameters.put(name, value);
	}
}
