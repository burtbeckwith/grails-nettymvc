package grails.plugin.nettymvc.util

import grails.plugin.nettymvc.data.CookieConfigData
import grails.plugin.nettymvc.data.FilterData
import grails.plugin.nettymvc.data.FilterMappingData
import grails.plugin.nettymvc.data.NameAndValue
import grails.plugin.nettymvc.data.ServletData
import grails.plugin.nettymvc.data.WebXmlData
import grails.plugin.nettymvc.http.NettyHttpServletResponse

import javax.servlet.ServletResponse

import org.codehaus.groovy.grails.web.servlet.GrailsDispatcherServlet
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
class GroovyUtils {

	protected static Logger log = LoggerFactory.getLogger(this)

	static WebXmlData parseWebXml(URL url, GrailsDispatcherServlet dispatcherServlet) {
		parseWebXml url.text, dispatcherServlet
	}

	static WebXmlData parseWebXml(File webxml, GrailsDispatcherServlet dispatcherServlet) {
		parseWebXml webxml.text, dispatcherServlet
	}

	static WebXmlData parseWebXml(String xml, GrailsDispatcherServlet dispatcherServlet) {

		def root = new XmlSlurper().parseText(xml)

		Map<String, ServletData> servlets = findServlets(root, dispatcherServlet)
		LinkedHashMap<String, FilterData> filters = findFilters(root)
		int sessionTimeout = findSessionTimeout(root)
		CookieConfigData cookieConfigData = findcookieConfig(root)

		new WebXmlData(filters, servlets, sessionTimeout, cookieConfigData)
	}

	static LinkedHashMap<String, FilterData> findFilters(root) {

		LinkedHashSet<String> names = root.'filter-mapping'.collect { text(it.'filter-name') }

		def filters = [:]

		for (String name in names) {
			def filter = root.filter.find { text(it.'filter-name') == name }
			if (!filter) continue

			String className = text(filter.'filter-class')
			def instance = Thread.currentThread().contextClassLoader.loadClass(className, true).newInstance()
			def data = new FilterData(name, instance)
			filters[name] = data
			filter.'init-param'.each { initParam ->
				data.params << new NameAndValue(text(initParam.'param-name'),
				                                text(initParam.'param-value'))
			}
		}

		root.'filter-mapping'.each { mapping ->
			FilterData filter = filters[text(mapping.'filter-name')]
			def data = new FilterMappingData(text(mapping.'url-pattern'))
			mapping.dispatcher.each { data.dispatchers << text(it) }
			filter.mappings << data
		}

		filters
	}

	static Map<String, ServletData> findServlets(root, GrailsDispatcherServlet dispatcherServlet) {

		def servlets = [:]

		root.servlet.each { servlet ->
			String name = text(servlet.'servlet-name')
			String className = text(servlet.'servlet-class')
			def loadOnStartup = text(servlet.'load-on-startup')
			loadOnStartup = loadOnStartup?.isInteger() ? loadOnStartup.toInteger() : null
			def instance
			if (className == GrailsDispatcherServlet.name) {
				instance = dispatcherServlet
			}
			else {
				instance = Thread.currentThread().contextClassLoader.loadClass(className, true).newInstance()
			}
			def data = new ServletData(name, instance, loadOnStartup)
			servlets[name] = data
			servlet.'init-param'.each { initParam ->
				data.params << new NameAndValue(text(initParam.'param-name'),
				                                text(initParam.'param-value'))
			}
		}

		root.'servlet-mapping'.each { mapping ->
			servlets[text(mapping.'servlet-name')].mappingUrls << text(mapping.'url-pattern')
		}

		servlets
	}

	static int findSessionTimeout(root) {
		int sessionTimeout = 30

		def sessionConfig = root.'session-config'
		if (sessionConfig) {
			def timeout = text(sessionConfig.'session-timeout')
			if (timeout) {
				sessionTimeout = timeout.toInteger()
			}
		}
		
		sessionTimeout
	}

	static CookieConfigData findcookieConfig(root) {

		boolean httpOnly = false
		boolean secure = false
		String name = 'JSESSIONID'
		int maxAge = -1
		String domain
		String path
		String comment
		
		def sessionConfig = root.'session-config'
		if (sessionConfig) {
			def cookieConfig = sessionConfig.'cookie-config'
			if (cookieConfig) {
				httpOnly = 'true'.equalsIgnoreCase(text(cookieConfig.'http-only'))
				secure = 'true'.equalsIgnoreCase(text(cookieConfig.'secure'))
				name = text(cookieConfig.'name') ?: 'JSESSIONID'
				domain = text(cookieConfig.'domain') ?: null
				path = text(cookieConfig.'path') ?: null
				comment = text(cookieConfig.'comment') ?: null
				def max = text(sessionConfig.'max-age')
				if (max) {
					maxAge = max.toInteger()
				}
			}
		}

		new CookieConfigData(httpOnly, secure, name, maxAge, domain, path, comment)
	}

	static void debugResponse(ServletResponse res) throws UnsupportedEncodingException {
		debugResponse res, true, null, null
	}

	static void debugResponse(ServletResponse res, boolean finished, Integer index, List<String> filterNames) throws UnsupportedEncodingException {

		NettyHttpServletResponse response = Utils.findResponse(res)
		List<String> cookies = response.cookies.collect { it.dump() }

		boolean committed = response.isCommitted()
		String content = response.contentAsString
		response.setCommitted(committed)

		def headers = [:]
		response.@headers.each { String name, values -> headers[name] = values.dump() }

		String message = (finished ? "\n\nBefore servlet.service" : "\n\nAfter " + filterNames[index]) +
				", cookies: $cookies, headers: $headers, Status: $response.status" +
				", ErrorMessage: $response.errorMessage, ContentLength: $response.contentLength" +
				", ContentType: $response.contentType, Committed: $response.committed" +
				", RedirectedUrl: $response.redirectedUrl, ForwardedUrl: $response.forwardedUrl" +
				", IncludedUrls: $response.includedUrls, CharacterEncoding: $response.characterEncoding" +
				", ContentAsString: $content\n\n"
		log.debug message
	}

	static String text(node) {
		node.text().trim()
	}
}
