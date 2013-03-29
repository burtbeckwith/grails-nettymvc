package grails.plugin.nettymvc.http;

import grails.plugin.nettymvc.data.FilterData;
import grails.plugin.nettymvc.data.FilterMappingData;
import grails.plugin.nettymvc.data.ServletData;
import grails.plugin.nettymvc.util.GroovyUtils;
import grails.plugin.nettymvc.util.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
public class NettyFilterChain implements FilterChain {

	protected static final String REQUEST = "REQUEST";

	protected int index;
	protected int filterCount;
	protected LinkedHashMap<String, FilterData> allFilters;
	protected Map<String, ServletData> servlets;
	protected ArrayList<Filter> filters = new ArrayList<Filter>();
	protected ArrayList<String> names = new ArrayList<String>();
	protected boolean handled;

	protected Logger log = LoggerFactory.getLogger(getClass());

	protected HttpServletRequest chainRequest;
	protected HttpServletResponse chainResponse;

	public NettyFilterChain(LinkedHashMap<String, FilterData> allFilters, Map<String, ServletData> servlets, HttpServletRequest request) {
		this.allFilters = allFilters;
		this.servlets = servlets;
		initChain(request);
	}

	public boolean wasHandled() {
		return handled;
	}

	public HttpServletRequest getChainRequest() {
		return chainRequest;
	}

	public HttpServletResponse getChainResponse() {
		return chainResponse;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.FilterChain#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse)
	 */
	public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {

		chainRequest = (HttpServletRequest) request;
		chainResponse = (HttpServletResponse) response;

		if (index > 0 && log.isDebugEnabled()) {
			GroovyUtils.debugResponse(chainResponse, false, index - 1, names);
		}

		// Call the next filter if there is one
		if (index < filterCount) {
			filters.get(index++).doFilter(chainRequest, chainResponse, this);
			return;
		}

		if (log.isDebugEnabled()) {
			GroovyUtils.debugResponse(chainResponse);
		}

		handleWithServlet();
	}

	protected void handleForward() throws ServletException, IOException {
		NettyHttpServletResponse mockResponse = Utils.findResponse(chainResponse);
		if (mockResponse.getForwardedUrl() == null) {
			return;
		}

		RequestInvocationHandler mockRequest = Utils.findRequestHandler(chainRequest);
		mockRequest.setRequestURI(chainRequest.getContextPath() + mockResponse.getForwardedUrl());
		handleWithServlet();
	}

	protected void handleWithServlet() throws ServletException, IOException {
		Servlet servlet = findServlet();
		if (servlet == null) {
			return;
		}
		servlet.service(chainRequest, chainResponse);
		handled = true;
	}

	protected void initChain(HttpServletRequest request) {

		String uri = uriWithoutContext(request);

		for (FilterData data : allFilters.values()) {
			for (FilterMappingData mappingData : data.getMappings()) {
				if (!mappingData.getDispatchers().isEmpty() && !mappingData.getDispatchers().contains(REQUEST)) {
					continue;
				}
				if (patternApplies(mappingData.getUrlPattern(), uri)) {
					filters.add(data.getFilter());
					names.add(data.getName());
					break;
				}
			}
		}

		filterCount = filters.size();
	}

	// from org.apache.catalina.core.ApplicationFilterFactory.matchFiltersURL
	protected boolean patternApplies(String urlPattern, String uri) {

		if (urlPattern.equals(uri)) {
			return true;
		}

		if (urlPattern.equals("/*")) {
			return true;
		}

		if (urlPattern.endsWith("/*")) {
			if (urlPattern.regionMatches(0, uri, 0, urlPattern.length() - 2)) {
				if (uri.length() == urlPattern.length() - 2) {
					return true;
				}
				if ('/' == uri.charAt(urlPattern.length() - 2)) {
					return true;
				}
			}
			return false;
		}

		if (urlPattern.startsWith("*.")) {
			int slash = uri.lastIndexOf('/');
			int period = uri.lastIndexOf('.');
			if (slash >= 0 && period > slash && period != uri.length() - 1 && uri.length() - period == urlPattern.length() - 1) {
				return urlPattern.regionMatches(2, uri, period + 1, urlPattern.length() - 2);
			}
		}

		return false;
	}

	protected String uriWithoutContext(HttpServletRequest request) {
		return uriWithoutContext(request.getRequestURI(), request.getContextPath());
	}

	protected String uriWithoutContext(String uri, String contextPath) {
		return contextPath.length() == 0 || !uri.startsWith(contextPath) ? uri : uri.substring(contextPath.length());
	}

	protected Servlet findServlet() {

		String uri = chainRequest.getRequestURI();
		String contextPath = chainRequest.getContextPath();

		uri = uriWithoutContext(uri, contextPath);
		for (ServletData data : servlets.values()) {
			for (String urlPattern : data.getMappingUrls()) {
				if (patternApplies(urlPattern, uri)) {
					return data.getServlet();
				}
			}
		}
		return null;
	}
}
