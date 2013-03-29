package grails.plugin.nettymvc.http;

import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

/**
 * Based on org.springframework.mock.web.MockServletConfig.
 *
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
public class NettyServletConfig implements ServletConfig {

	protected ServletContext servletContext;
	protected String servletName;
	protected Map<String, String> initParameters = new LinkedHashMap<String, String>();

	public NettyServletConfig(ServletContext servletContext, String servletName) {
		this.servletContext = servletContext;
		this.servletName = servletName;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletConfig#getServletName()
	 */
	public String getServletName() {
		return servletName;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletConfig#getServletContext()
	 */
	public ServletContext getServletContext() {
		return servletContext;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletConfig#getInitParameter(java.lang.String)
	 */
	public String getInitParameter(String name) {
		return initParameters.get(name);
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletConfig#getInitParameterNames()
	 */
	public Enumeration<String> getInitParameterNames() {
		return Collections.enumeration(initParameters.keySet());
	}

	public void addInitParameter(String name, String value) {
		initParameters.put(name, value);
	}
}
