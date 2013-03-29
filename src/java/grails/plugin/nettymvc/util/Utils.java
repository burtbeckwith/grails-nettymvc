package grails.plugin.nettymvc.util;

import grails.plugin.nettymvc.data.CookieConfigData;
import grails.plugin.nettymvc.data.WebXmlData;
import grails.plugin.nettymvc.http.NettyHttpServletResponse;
import grails.plugin.nettymvc.http.RequestInvocationHandler;
import io.netty.handler.codec.http.CookieDecoder;
import io.netty.handler.codec.http.DefaultCookie;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Set;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponseWrapper;

import org.springframework.util.ReflectionUtils;

/**
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
public class Utils {

   protected static Method setHttpOnlyMethod = ReflectionUtils.findMethod(Cookie.class, "setHttpOnly", boolean.class);
   protected static Method isHttpOnlyMethod = ReflectionUtils.findMethod(Cookie.class, "isHttpOnly");

	public static NettyHttpServletResponse findResponse(ServletResponse response) {
		if (response instanceof NettyHttpServletResponse) {
			return (NettyHttpServletResponse) response;
		}
		if (response instanceof HttpServletResponseWrapper) {
			return findResponse(((HttpServletResponseWrapper)response).getResponse());
		}
		throw new IllegalStateException("Expected a NettyHttpServletResponse");
	}

	public static RequestInvocationHandler findRequestHandler(ServletRequest request) {
		if (request instanceof Proxy) {
			InvocationHandler rh = Proxy.getInvocationHandler(request);
			if (rh instanceof RequestInvocationHandler) {
				return (RequestInvocationHandler)rh;
			}
		}
		if (request instanceof HttpServletRequestWrapper) {
			return findRequestHandler(((HttpServletRequestWrapper)request).getRequest());
		}
		throw new IllegalStateException("Expected a MockHttpServletRequest");
	}

	public static void setHttpOnly(Cookie cookie) {
		if (setHttpOnlyMethod != null) {
			ReflectionUtils.invokeMethod(setHttpOnlyMethod, cookie, true);
		}
	}

	public static void setHttpOnly(DefaultCookie nettyCookie, Cookie cookie) {
		if (isHttpOnlyMethod != null) {
			nettyCookie.setHttpOnly((Boolean) ReflectionUtils.invokeMethod(isHttpOnlyMethod, cookie));
		}
	}

	public static DefaultCookie createCookie(Cookie cookie) {
		DefaultCookie nettyCookie = new DefaultCookie(cookie.getName(), cookie.getValue());

		nettyCookie.setComment(cookie.getComment());
		nettyCookie.setDomain(cookie.getDomain());
		nettyCookie.setPath(cookie.getPath());
		nettyCookie.setSecure(cookie.getSecure());
		nettyCookie.setVersion(cookie.getVersion());
		nettyCookie.setMaxAge(cookie.getMaxAge());
		nettyCookie.setMaxAge(cookie.getMaxAge() > -1 ? cookie.getMaxAge() : Long.MIN_VALUE);

		setHttpOnly(nettyCookie, cookie);

		return nettyCookie;
	}

	public static Cookie[] extractCookies(String header) {
		Set<io.netty.handler.codec.http.Cookie> nettyCookies = CookieDecoder.decode(header);
		Cookie[] cookies = new Cookie[nettyCookies.size()];

		int index = 0;
		for (io.netty.handler.codec.http.Cookie c : nettyCookies) {
			Cookie cookie = new Cookie(c.getName(), c.getValue());
			cookie.setComment(c.getComment());
			if (c.getDomain() != null) {
				cookie.setDomain(c.getDomain());
			}
			cookie.setPath(c.getPath());
			cookie.setSecure(c.isSecure());
			cookie.setVersion(c.getVersion());
			if (c.getMaxAge() > Long.MIN_VALUE) {
				cookie.setMaxAge((int) c.getMaxAge());
			}
			if (c.isDiscard()) {
				cookie.setMaxAge(-1);
			}
			if (c.isHttpOnly()) {
				setHttpOnly(cookie);
			}
			cookies[index++] = cookie;
		}

		return cookies;
	}

	public static Cookie createSessionCookie(String id, boolean secure, String contextPath, WebXmlData webXmlData) {

		CookieConfigData cookieConfig = webXmlData.getCookieConfigData();

		Cookie cookie = new Cookie(cookieConfig.getName(), id);

		cookie.setMaxAge(cookieConfig.getMaxAge());
		cookie.setComment(cookieConfig.getComment());
		cookie.setSecure(secure || cookieConfig.isSecure());

		if (cookieConfig.getDomain() != null) {
			cookie.setDomain(cookieConfig.getDomain());
		}

		if (cookieConfig.isHttpOnly()) {
			setHttpOnly(cookie);
		}

		String path = cookieConfig.getPath();
		if (path == null || path.length() == 0) {
			path = contextPath;
		}
		if (!path.endsWith("/")) {
			path += '/';
		}
		cookie.setPath(path);

		return cookie;
	}
}
