import grails.plugin.nettymvc.Server
import grails.plugin.nettymvc.http.SessionManager

class NettymvcGrailsPlugin {
	String version = '0.1'
	String grailsVersion = '2.0 > *'
	String author = 'Burt Beckwith'
	String authorEmail = 'beckwithb@vmware.com'
	String title = 'NettyMVC Plugin'
	String description = 'NettyMVC Plugin'
	String documentation = 'http://grails.org/plugin/nettymvc'
	List pluginExcludes = [
		'docs/**',
		'src/docs/**'
	]

	String license = 'APACHE'
	def issueManagement = [system: 'GitHub', url: 'https://github.com/burtbeckwith/grails-nettymvc/issues']
	def scm = [url: 'https://github.com/burtbeckwith/grails-nettymvc']

	def doWithSpring = {

		def port = application.config.grails.plugin.nettymvc.port
		if (!(port instanceof Number)) {
			port = 8080
		}
		nettyServer(Server, port) { bean ->
			bean.destroyMethod = 'stop'
		}

		nettySessionManager(SessionManager) {
			servletContext = ref('servletContext')
		}
	}

	def doWithApplicationContext = { ctx ->
		Thread.start {
			ctx.nettyServer.start()
		}
	}
}
