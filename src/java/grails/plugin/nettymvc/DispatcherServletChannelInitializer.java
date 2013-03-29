package grails.plugin.nettymvc;

import grails.plugin.nettymvc.data.FilterData;
import grails.plugin.nettymvc.data.NameAndValue;
import grails.plugin.nettymvc.data.ServletData;
import grails.plugin.nettymvc.data.WebXmlData;
import grails.plugin.nettymvc.http.NettyFilterConfig;
import grails.plugin.nettymvc.http.NettyServletConfig;
import grails.plugin.nettymvc.http.SessionManager;
import grails.plugin.nettymvc.util.GroovyUtils;
import grails.util.BuildSettingsHolder;
import grails.util.Metadata;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.stream.ChunkedWriteHandler;

import java.io.File;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.codehaus.groovy.grails.web.servlet.GrailsDispatcherServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
public class DispatcherServletChannelInitializer extends ChannelInitializer<SocketChannel> {

	protected GrailsDispatcherServlet dispatcherServlet = new GrailsDispatcherServlet();
	protected ServletContext servletContext;
	protected SessionManager sessionManager;
	protected WebXmlData data;

	protected Logger log = LoggerFactory.getLogger(getClass());

	public DispatcherServletChannelInitializer(WebApplicationContext ctx) throws MalformedURLException, ServletException {

		Field field = ReflectionUtils.findField(DispatcherServlet.class, "webApplicationContext");
		field.setAccessible(true);
		ReflectionUtils.setField(field, dispatcherServlet, ctx);

		servletContext = ctx.getBean("servletContext", ServletContext.class);
		sessionManager = ctx.getBean("nettySessionManager", SessionManager.class);

		parseWebxml();

		sessionManager.setMaxInactiveInterval(data.getSessionTimeout() * 60);
	}

	@Override
	public void initChannel(SocketChannel channel) throws Exception {

		// Create a default pipeline implementation.
		ChannelPipeline pipeline = channel.pipeline();

		// Uncomment the following line if you want HTTPS
		//SSLEngine engine = SecureChatSslContextFactory.getServerContext().createSSLEngine();
		//engine.setUseClientMode(false);
		//pipeline.addLast("ssl", new SslHandler(engine));

		pipeline.addLast("decoder", new HttpRequestDecoder());
		pipeline.addLast("aggregator", new HttpObjectAggregator(65536));
		pipeline.addLast("encoder", new HttpResponseEncoder());
		pipeline.addLast("chunkedWriter", new ChunkedWriteHandler());
		pipeline.addLast("handler", new ServletNettyHandler(servletContext, sessionManager, data));
	}

	protected void parseWebxml() throws ServletException, MalformedURLException {

		if (Metadata.getCurrent().isWarDeployed()) {
			URL webxml = servletContext.getResource("/WEB-INF/web.xml");
			data = GroovyUtils.parseWebXml(webxml, dispatcherServlet);
		}
		else {
			File webxml = BuildSettingsHolder.getSettings().getWebXmlLocation();
			data = GroovyUtils.parseWebXml(webxml, dispatcherServlet);
		}

		log.debug("WebXmlData: {}", data);

		for (Map.Entry<String, FilterData> entry : data.getFilters().entrySet()) {
			String name = entry.getKey();
			FilterData filterData = entry.getValue();

			NettyFilterConfig config = new NettyFilterConfig(servletContext, name);
			for (NameAndValue nav : filterData.getParams()) {
				config.addInitParameter(nav.getName(), nav.getValue());
			}
			filterData.getFilter().init(config);
		}

		for (Map.Entry<String, ServletData> entry : data.getServlets().entrySet()) {
			String name = entry.getKey();
			ServletData servletData = entry.getValue();

			NettyServletConfig config = new NettyServletConfig(servletContext, name);
			for (NameAndValue nav : servletData.getParams()) {
				config.addInitParameter(nav.getName(), nav.getValue());
			}
			servletData.getServlet().init(config);
		}
	}
}
