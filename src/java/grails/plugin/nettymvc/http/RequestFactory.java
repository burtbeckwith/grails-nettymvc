package grails.plugin.nettymvc.http;

import grails.plugin.nettymvc.data.WebXmlData;

import java.lang.reflect.Proxy;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

/**
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
public class RequestFactory {

	public static HttpServletRequest createInstance(ServletContext servletContext,
			SessionManager sessionManager, WebXmlData webXmlData) {

		return (HttpServletRequest)Proxy.newProxyInstance(HttpServletRequest.class.getClassLoader(),
				new Class[] { HttpServletRequest.class },
				new RequestInvocationHandler(servletContext, sessionManager, webXmlData));
	}
}
