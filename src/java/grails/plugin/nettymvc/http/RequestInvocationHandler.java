package grails.plugin.nettymvc.http;

import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaders.Names.COOKIE;
import grails.plugin.nettymvc.data.WebXmlData;
import grails.plugin.nettymvc.util.Utils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.security.Principal;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.util.LinkedCaseInsensitiveMap;

/**
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
public class RequestInvocationHandler implements InvocationHandler {

	/**
	 * The default protocol: 'http'.
	 */
	public static final String DEFAULT_PROTOCOL = "http";
	/**
	 * The default server name: 'localhost'.
	 */
	public static final String DEFAULT_SERVER_NAME = "localhost";
	/**
	 * The default server port: '80'.
	 */
	public static final int DEFAULT_SERVER_PORT = 80;
	/**
	 * The default remote address: '127.0.0.1'.
	 */
	public static final String DEFAULT_REMOTE_ADDR = "127.0.0.1";
	/**
	 * The default remote host: 'localhost'.
	 */
	public static final String DEFAULT_REMOTE_HOST = "localhost";
	/**
	 * The default server address: '127.0.0.1'.
	 */
	public static final String DEFAULT_SERVER_ADDR = "127.0.0.1";

	protected static final String CHARSET_PREFIX = "charset=";

	public RequestInvocationHandler(ServletContext sc, SessionManager sm, WebXmlData webXmlData) {
		servletContext = sc;
		sessionManager = sm;
		this.webXmlData = webXmlData;
		locales.add(Locale.ENGLISH);
	}

	protected Map<String, Object> attributes = new LinkedHashMap<String, Object>();
	protected Map<String, HeaderValueHolder> headers = new LinkedCaseInsensitiveMap<HeaderValueHolder>();
	protected Map<String, String[]> parameters = new LinkedHashMap<String, String[]>(16);
	/** List of locales in descending order */
	protected List<Locale> locales = new LinkedList<Locale>();
	protected Set<String> userRoles = new HashSet<String>();
	protected boolean active = true;
	protected boolean secure = false;
	protected boolean requestedSessionIdFromCookie = false;
	protected boolean requestedSessionIdFromURL = false;
	protected boolean cookiesParsed = false;
	protected String requestURI = "";
	protected String method = "";
	protected String contextPath = "";
	protected String servletPath = "";
	protected String serverName = DEFAULT_SERVER_NAME;
	protected String protocol = DEFAULT_PROTOCOL;
	protected String scheme = DEFAULT_PROTOCOL;
	protected int serverPort = DEFAULT_SERVER_PORT;
	protected String remoteAddr = DEFAULT_REMOTE_ADDR;
	protected String remoteHost = DEFAULT_REMOTE_HOST;
	protected int remotePort = DEFAULT_SERVER_PORT;
	protected String localName = DEFAULT_SERVER_NAME;
	protected String localAddr = DEFAULT_SERVER_ADDR;
	protected int localPort = DEFAULT_SERVER_PORT;

	protected NettyFilterChain filterChain;
	protected ServletContext servletContext;
	protected SessionManager sessionManager;
	protected String characterEncoding;
	protected String contentType;
	protected byte[] content;
	protected String authType;
	protected Cookie[] cookies;
	protected String pathInfo;
	protected String queryString;
	protected String remoteUser;
	protected Principal userPrincipal;
	protected String requestedSessionId;
	protected NettyHttpSession session;
	protected HttpServletResponse response;
	protected WebXmlData webXmlData;

	/* (non-Javadoc)
	 * @see java.lang.reflect.InvocationHandler#invoke(java.lang.Object, java.lang.reflect.Method, java.lang.Object[])
	 */
	public Object invoke(Object proxy, Method m, Object[] args) throws Throwable {

		String methodName = m.getName();

		if ("getAttribute".equals(methodName)) {
			checkActive();
			return attributes.get(args[0]);
		}

		if ("getAttributeNames".equals(methodName)) {
			checkActive();
			return new Vector<String>(attributes.keySet()).elements();
		}

		if ("getCharacterEncoding".equals(methodName)) {
			return characterEncoding;
		}
		if ("setCharacterEncoding".equals(methodName)) {
			characterEncoding = (String) args[0];
			updateContentTypeHeader();
			return null;
		}

		if ("getContentLength".equals(methodName)) {
			return content == null ? -1 : content.length;
		}

		if ("getContentType".equals(methodName)) {
			return contentType;
		}

		if ("getInputStream".equals(methodName)) {
			if (content == null) {
				return null;
			}

			return new DelegatingServletInputStream(new ByteArrayInputStream(content));
		}

		if ("getParameter".equals(methodName)) {
			String[] arr = parameters.get(args[0]);
			return (arr != null && arr.length > 0 ? arr[0] : null);
		}

		if ("getParameterNames".equals(methodName)) {
			return Collections.enumeration(parameters.keySet());
		}

		if ("getParameterValues".equals(methodName)) {
			return parameters.get(args[0]);
		}

		if ("getParameterMap".equals(methodName)) {
			return Collections.unmodifiableMap(parameters);
		}

		if ("getProtocol".equals(methodName)) {
			return protocol;
		}

		if ("getScheme".equals(methodName)) {
			return scheme;
		}

		if ("getServerName".equals(methodName)) {
			return serverName;
		}

		if ("getServerPort".equals(methodName)) {
			return serverPort;
		}

		if ("getReader".equals(methodName)) {
			if (content == null) {
				return null;
			}

			InputStream sourceStream = new ByteArrayInputStream(content);
			Reader sourceReader = characterEncoding == null ? new InputStreamReader(sourceStream) :
				new InputStreamReader(sourceStream, characterEncoding);
			return new BufferedReader(sourceReader);
		}

		if ("getRemoteAddr".equals(methodName)) {
			return remoteAddr;
		}

		if ("getRemoteHost".equals(methodName)) {
			return remoteHost;
		}

		if ("setAttribute".equals(methodName)) {
			checkActive();

			String name = (String) args[0];
			Object value = args[1];
			if (value == null) {
				attributes.remove(name);
			}
			else {
				attributes.put(name, value);
			}

			return null;
		}

		if ("removeAttribute".equals(methodName)) {
			checkActive();
			attributes.remove(args[0]);
			return null;
		}

		if ("getLocale".equals(methodName)) {
			return locales.get(0);
		}

		if ("getLocales".equals(methodName)) {
			return Collections.enumeration(locales);
		}

		if ("isSecure".equals(methodName)) {
			return secure;
		}

		if ("getRequestDispatcher".equals(methodName)) {
			return new NettyRequestDispatcher((String) args[0], filterChain);
		}

		if ("getRealPath".equals(methodName)) {
			return getRealPath((String) args[0]);
		}

		if ("getRemotePort".equals(methodName)) {
			return remotePort;
		}

		if ("getLocalName".equals(methodName)) {
			return localName;
		}

		if ("getLocalAddr".equals(methodName)) {
			return localAddr;
		}

		if ("getLocalPort".equals(methodName)) {
			return localPort;
		}

		if ("getServletContext".equals(methodName)) {
			return servletContext;
		}

		if ("getAuthType".equals(methodName)) {
			return authType;
		}

		if ("getCookies".equals(methodName)) {
			parseCookies();
			return cookies;
		}

		if ("getDateHeader".equals(methodName)) {
			String name = (String) args[0];

			HeaderValueHolder header = HeaderValueHolder.getByName(headers, name);
			Object value = header == null ? null : header.getValue();

			if (value instanceof Date) {
				return ((Date) value).getTime();
			}

			if (value instanceof Number) {
				return ((Number) value).longValue();
			}

			if (value == null) {
				return -1L;
			}

			throw new IllegalArgumentException("Value for header '" + name + "' is neither a Date nor a Number: " + value);
		}

		if ("getHeader".equals(methodName)) {
			HeaderValueHolder header = HeaderValueHolder.getByName(headers, (String) args[0]);
			return header == null ? null : header.getStringValue();
		}

		if ("getHeaders".equals(methodName)) {
			HeaderValueHolder header = HeaderValueHolder.getByName(headers, (String) args[0]);
			return Collections.enumeration(header == null ? new LinkedList<String>() : header.getStringValues());
		}

		if ("getHeaderNames".equals(methodName)) {
			return Collections.enumeration(headers.keySet());
		}

		if ("getIntHeader".equals(methodName)) {
			String name = (String) args[0];

			HeaderValueHolder header = HeaderValueHolder.getByName(headers, name);
			Object value = header == null ? null : header.getValue();

			if (value instanceof Number) {
				return ((Number) value).intValue();
			}

			if (value instanceof String) {
				return Integer.valueOf((String) value);
			}

			if (value == null) {
				return -1;
			}

			throw new NumberFormatException("Value for header '" + name + "' is not a Number: " + value);
		}

		if ("getMethod".equals(methodName)) {
			return method;
		}

		if ("getPathInfo".equals(methodName)) {
			return pathInfo;
		}

		if ("getPathTranslated".equals(methodName)) {
			return pathInfo == null ? null : getRealPath(pathInfo);
		}

		if ("getContextPath".equals(methodName)) {
			return contextPath;
		}

		if ("getQueryString".equals(methodName)) {
			return queryString;
		}

		if ("getRemoteUser".equals(methodName)) {
			return remoteUser;
		}

		if ("isUserInRole".equals(methodName)) {
			return userRoles.contains(args[0]);
		}

		if ("getUserPrincipal".equals(methodName)) {
			return userPrincipal;
		}

		if ("getRequestedSessionId".equals(methodName)) {
			return requestedSessionId;
		}

		if ("getRequestURI".equals(methodName)) {
			return requestURI;
		}

		if ("getRequestURL".equals(methodName)) {
			StringBuffer url = new StringBuffer(scheme);
			url.append("://").append(serverName).append(':').append(serverPort);
			url.append(requestURI);
			return url;
		}

		if ("getServletPath".equals(methodName)) {
			return servletPath;
		}

		if ("isRequestedSessionIdValid".equals(methodName)) {
			return isRequestedSessionIdValid();
		}

		if ("isRequestedSessionIdFromCookie".equals(methodName)) {
			return requestedSessionId != null && requestedSessionIdFromCookie;
		}

		if ("isRequestedSessionIdFromURL".equals(methodName)) {
			return requestedSessionId != null && requestedSessionIdFromURL;
		}

		if ("isRequestedSessionIdFromUrl".equals(methodName)) {
			return requestedSessionId != null && requestedSessionIdFromURL;
		}

		if ("authenticate".equals(methodName)) {
			return userPrincipal != null && remoteUser != null && authType != null;
		}

		if ("login".equals(methodName)) {
			throw new ServletException("Username-password authentication not supported - override the login method");
		}

		if ("logout".equals(methodName)) {
			userPrincipal = null;
			remoteUser = null;
			authType = null;
			return null;
		}

		if ("getSession".equals(methodName)) {
			boolean create = (args == null || args.length == 0) ? true : (Boolean)args[0];
			return getSession(create);
		}

		if ("isAsyncSupported".equals(methodName)) {
			return false;
		}

		if ("getDispatcherType".equals(methodName) || "getParts".equals(methodName) ||
				"getParts".equals(methodName) || methodName.contains("Async")) {
			throw new ServletException("Servlet 3.0 methods are not supported");
		}

		throw new ServletException("Unknown method " + methodName);
	}

	protected HttpSession getSession(boolean create) {
		checkActive();

		// Reset session if invalidated.
		if (session != null && !session.isValid()) {
			session = null;
		}
		if (session != null) {
			return session;
		}

		lookupCookieSessionId();

		if (requestedSessionId != null) {
			session = sessionManager.findSession(requestedSessionId);
			if (session != null && !session.isValid()) {
				session = null;
			}
			if (session != null) {
				session.access();
				return session;
			}
		}

		if (!create) {
			return null;
		}

		session = sessionManager.createSession();

		if (session != null) {
			session.access();
			response.addCookie(Utils.createSessionCookie(
					session.getId(), secure, contextPath, webXmlData));
		}

		return session;
	}

	protected void parseCookies() {
		if (cookiesParsed) {
			return;
		}

		cookiesParsed = true;

		HeaderValueHolder header = HeaderValueHolder.getByName(headers, COOKIE);
		if (header != null) {
			String value = header.getStringValue();
			if (value != null) {
				cookies = Utils.extractCookies(value);
			}
		}
	}

	public void setFilterChain(NettyFilterChain fc) {
		filterChain = fc;
	}

	public void setResponse(HttpServletResponse servletResponse) {
		response = servletResponse;
	}

	protected void lookupCookieSessionId() {
		parseCookies();

		if (cookies == null || cookies.length == 0) {
			return;
		}

		String sessionCookieName = webXmlData.getCookieConfigData().getName(); // JSESSIONID
		for (Cookie cookie : cookies) {
			if (!cookie.getName().equals(sessionCookieName)) {
				continue;
			}

			// Override anything requested in the URL
			if (!requestedSessionIdFromCookie) {
				// Accept only the first session id cookie
				requestedSessionId = cookie.getValue();
				requestedSessionIdFromCookie = true;
				requestedSessionIdFromURL = false;
			}
			else {
				if (!isRequestedSessionIdValid()) {
					// Replace the session id until one is valid
					requestedSessionId = cookie.getValue();
				}
			}
		}
	}

	public boolean isRequestedSessionIdValid() {
		return requestedSessionId != null && sessionManager.isSessionIdValid(requestedSessionId);
	}

	/**
	 * Return whether this request is still active (that is, not completed yet).
	 */
	public boolean isActive() {
		return active;
	}

	/**
	 * Mark this request as completed, keeping its state.
	 */
	public void close() {
		active = false;
	}

	/**
	 * Invalidate this request, clearing its state.
	 */
	public void invalidate() {
		close();
		clearAttributes();
	}

	public void setContent(byte[] content) {
		this.content = content;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
		if (contentType != null) {
			int charsetIndex = contentType.toLowerCase().indexOf(CHARSET_PREFIX);
			if (charsetIndex != -1) {
				String encoding = contentType.substring(charsetIndex + CHARSET_PREFIX.length());
				characterEncoding = encoding;
			}
			updateContentTypeHeader();
		}
	}

	/**
	 * Set a single value for the specified HTTP parameter.
	 * <p>
	 * If there are already one or more values registered for the given
	 * parameter name, they will be replaced.
	 */
	public void setParameter(String name, String value) {
		setParameter(name, new String[] { value });
	}

	/**
	 * Set an array of values for the specified HTTP parameter.
	 * <p>
	 * If there are already one or more values registered for the given
	 * parameter name, they will be replaced.
	 */
	public void setParameter(String name, String[] values) {
		parameters.put(name, values);
	}

	/**
	 * Sets all provided parameters <emphasis>replacing</emphasis> any existing
	 * values for the provided parameter names. To add without replacing
	 * existing values, use {@link #addParameters(java.util.Map)}.
	 */
	@SuppressWarnings("rawtypes")
	public void setParameters(Map params) {
		for (Object key : params.keySet()) {
			Object value = params.get(key);
			if (value instanceof String) {
				setParameter((String) key, (String) value);
			}
			else if (value instanceof String[]) {
				setParameter((String) key, (String[]) value);
			}
			else {
				throw new IllegalArgumentException("Parameter map value must be single value " + " or array of type [" +
						String.class.getName() + "]");
			}
		}
	}

	/**
	 * Add a single value for the specified HTTP parameter.
	 * <p>
	 * If there are already one or more values registered for the given
	 * parameter name, the given value will be added to the end of the list.
	 */
	public void addParameter(String name, String value) {
		addParameter(name, new String[] { value });
	}

	/**
	 * Add an array of values for the specified HTTP parameter.
	 * <p>
	 * If there are already one or more values registered for the given
	 * parameter name, the given values will be added to the end of the list.
	 */
	public void addParameter(String name, String[] values) {
		String[] oldArr = parameters.get(name);
		if (oldArr == null) {
			parameters.put(name, values);
		}
		else {
			String[] newArr = new String[oldArr.length + values.length];
			System.arraycopy(oldArr, 0, newArr, 0, oldArr.length);
			System.arraycopy(values, 0, newArr, oldArr.length, values.length);
			parameters.put(name, newArr);
		}
	}

	/**
	 * Adds all provided parameters <emphasis>without</emphasis> replacing any
	 * existing values. To replace existing values, use
	 * {@link #setParameters(java.util.Map)}.
	 */
	@SuppressWarnings("rawtypes")
	public void addParameters(Map params) {
		for (Object key : params.keySet()) {
			Object value = params.get(key);
			if (value instanceof String) {
				addParameter((String) key, (String) value);
			}
			else if (value instanceof String[]) {
				addParameter((String) key, (String[]) value);
			}
			else {
				throw new IllegalArgumentException("Parameter map value must be single value " + " or array of type [" +
						String.class.getName() + "]");
			}
		}
	}

	/**
	 * Remove already registered values for the specified HTTP parameter, if
	 * any.
	 */
	public void removeParameter(String name) {
		parameters.remove(name);
	}

	/**
	 * Removes all existing parameters.
	 */
	public void removeAllParameters() {
		parameters.clear();
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public void setScheme(String scheme) {
		this.scheme = scheme;
	}

	public void setServerName(String serverName) {
		this.serverName = serverName;
	}

	public void setServerPort(int serverPort) {
		this.serverPort = serverPort;
	}

	public void setRemoteAddr(String remoteAddr) {
		this.remoteAddr = remoteAddr;
	}

	public void setRemoteHost(String remoteHost) {
		this.remoteHost = remoteHost;
	}

	/**
	 * Clear all of this request's attributes.
	 */
	public void clearAttributes() {
		attributes.clear();
	}

	/**
	 * Add a new preferred locale, before any existing locales.
	 */
	public void addPreferredLocale(Locale locale) {
		locales.add(0, locale);
	}

	public void setSecure(boolean secure) {
		this.secure = secure;
	}

	public void setRemotePort(int remotePort) {
		this.remotePort = remotePort;
	}

	public void setLocalName(String localName) {
		this.localName = localName;
	}

	public void setLocalAddr(String localAddr) {
		this.localAddr = localAddr;
	}

	public void setLocalPort(int localPort) {
		this.localPort = localPort;
	}

	public void setAuthType(String authType) {
		this.authType = authType;
	}

	/**
	 * Add a header entry for the given name.
	 * <p>If there was no entry for that header name before, the value will be used
	 * as-is. In case of an existing entry, a String array will be created,
	 * adding the given value (more specifically, its toString representation)
	 * as further element.
	 * <p>Multiple values can only be stored as list of Strings, following the
	 * Servlet spec (see <code>getHeaders</code> accessor). As alternative to
	 * repeated <code>addHeader</code> calls for individual elements, you can
	 * use a single call with an entire array or Collection of values as
	 * parameter.
	 * @see #getHeaderNames
	 * @see #getHeader
	 * @see #getHeaders
	 * @see #getDateHeader
	 * @see #getIntHeader
	 */
	public void addHeader(String name, Object value) {
		if (CONTENT_TYPE.equalsIgnoreCase(name)) {
			setContentType((String) value);
			return;
		}
		doAddHeaderValue(name, value, false);
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public void setPathInfo(String pathInfo) {
		this.pathInfo = pathInfo;
	}

	public void setContextPath(String contextPath) {
		this.contextPath = contextPath;
	}

	public void setQueryString(String queryString) {
		this.queryString = queryString;
	}

	public void setRemoteUser(String remoteUser) {
		this.remoteUser = remoteUser;
	}

	public void addUserRole(String role) {
		userRoles.add(role);
	}

	public void setUserPrincipal(Principal userPrincipal) {
		this.userPrincipal = userPrincipal;
	}

	public void setRequestURI(String requestURI) {
		this.requestURI = requestURI;
	}

	public void setServletPath(String servletPath) {
		this.servletPath = servletPath;
	}

	/**
	 * Check whether this request is still active (that is, not completed yet),
	 * throwing an IllegalStateException if not active anymore.
	 */
	protected void checkActive() throws IllegalStateException {
		if (!active) {
			throw new IllegalStateException("Request is not active anymore");
		}
	}

	protected String getRealPath(String path) {
		return servletContext.getRealPath(path);
	}

	protected void updateContentTypeHeader() {
		if (contentType != null) {
			StringBuilder sb = new StringBuilder(contentType);
			if (!contentType.toLowerCase().contains(CHARSET_PREFIX) && characterEncoding != null) {
				sb.append(";").append(CHARSET_PREFIX).append(characterEncoding);
			}
			doAddHeaderValue(CONTENT_TYPE, sb.toString(), true);
		}
	}

	protected void doAddHeaderValue(String name, Object value, boolean replace) {
		HeaderValueHolder header = HeaderValueHolder.getByName(headers, name);
		if (header == null || replace) {
			header = new HeaderValueHolder();
			headers.put(name, header);
		}
		if (value instanceof Collection) {
			header.addValues((Collection<?>) value);
		}
		else if (value.getClass().isArray()) {
			header.addValueArray(value);
		}
		else {
			header.addValue(value);
		}
	}
}
