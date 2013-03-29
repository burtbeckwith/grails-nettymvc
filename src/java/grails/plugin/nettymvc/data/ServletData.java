package grails.plugin.nettymvc.data;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.Servlet;

/**
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
public class ServletData {

	protected String name;
	protected Servlet servlet;
	protected List<NameAndValue> params = new ArrayList<NameAndValue>();
	protected List<String> mappingUrls = new ArrayList<String>();
	protected Integer loadOnStartup;

	public ServletData(String name, Servlet servlet, Integer loadOnStartup) {
		this.name = name;
		this.servlet = servlet;
		this.loadOnStartup = loadOnStartup;
	}

	public String getName() {
		return name;
	}

	public Servlet getServlet() {
		return servlet;
	}

	public Integer getLoadOnStartup() {
		return loadOnStartup;
	}

	public List<String> getMappingUrls() {
		return mappingUrls;
	}

	public List<NameAndValue> getParams() {
		return params;
	}

	@Override
	public String toString() {
		return "FilterData name: '" + name + "' class: '" + servlet.getClass().getName() +
				"' params: " + params + ", mappings: " + mappingUrls;
	}
}
