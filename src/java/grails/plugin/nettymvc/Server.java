package grails.plugin.nettymvc;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.Assert;
import org.springframework.web.context.WebApplicationContext;

/**
 * Registered as the "nettyServer" Spring bean.
 *
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
public class Server implements ApplicationContextAware {

	protected int port;
	protected WebApplicationContext ctx;
	protected ServerBootstrap server = new ServerBootstrap();

	public Server(int port) {
		this.port = port;
	}

	public void start() throws Exception {
		try {
			server.group(new NioEventLoopGroup(), new NioEventLoopGroup())
				.channel(NioServerSocketChannel.class)
				.localAddress(port)
				.childHandler(new DispatcherServletChannelInitializer(ctx));

			server.bind().sync().channel().closeFuture().sync();
		}
		finally {
			server.shutdown();
		}
	}

	public void stop() {
		server.shutdown();
	}

	public int getPort() {
		return port;
	}

	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		Assert.isInstanceOf(WebApplicationContext.class, applicationContext);
		ctx = (WebApplicationContext) applicationContext;
	}
}
