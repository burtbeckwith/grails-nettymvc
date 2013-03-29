package grails.plugin.nettymvc.http;

import grails.plugin.nettymvc.util.Utils;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
public class NettyRequestDispatcher implements RequestDispatcher {

	protected NettyFilterChain filterChain;
	protected String url;

	public NettyRequestDispatcher(String url, NettyFilterChain filterChain) {
		this.url = url;
		this.filterChain = filterChain;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.RequestDispatcher#forward(javax.servlet.ServletRequest, javax.servlet.ServletResponse)
	 */
	public void forward(ServletRequest request, ServletResponse response) throws ServletException, IOException {
		if (response.isCommitted()) {
			throw new IllegalStateException("Cannot perform forward - response is already committed");
		}

		Utils.findResponse(response).setForwardedUrl(url);

		filterChain.handleForward();
	}

	/* (non-Javadoc)
	 * @see javax.servlet.RequestDispatcher#include(javax.servlet.ServletRequest, javax.servlet.ServletResponse)
	 */
	public void include(ServletRequest request, ServletResponse response) throws ServletException, IOException {
		Utils.findResponse(response).addIncludedUrl(url);
	}
}
