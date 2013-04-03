package grails.plugin.nettymvc;

import static io.netty.handler.codec.http.HttpHeaders.setContentLength;
import static io.netty.handler.codec.http.HttpHeaders.Names.CACHE_CONTROL;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaders.Names.DATE;
import static io.netty.handler.codec.http.HttpHeaders.Names.EXPIRES;
import static io.netty.handler.codec.http.HttpHeaders.Names.IF_MODIFIED_SINCE;
import static io.netty.handler.codec.http.HttpHeaders.Names.LAST_MODIFIED;
import static io.netty.handler.codec.http.HttpHeaders.Names.LOCATION;
import static io.netty.handler.codec.http.HttpHeaders.Names.SET_COOKIE;
import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpMethod.POST;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.METHOD_NOT_ALLOWED;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_MODIFIED;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import grails.plugin.nettymvc.data.WebXmlData;
import grails.plugin.nettymvc.http.NettyFilterChain;
import grails.plugin.nettymvc.http.NettyHttpServletResponse;
import grails.plugin.nettymvc.http.RequestFactory;
import grails.plugin.nettymvc.http.RequestInvocationHandler;
import grails.plugin.nettymvc.http.SessionManager;
import grails.plugin.nettymvc.util.GroovyUtils;
import grails.plugin.nettymvc.util.Utils;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundMessageHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.ServerCookieEncoder;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder.ErrorDataDecoderException;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder.IncompatibleDataDecoderException;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder.NotEnoughDataDecoderException;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.codec.http.multipart.InterfaceHttpData.HttpDataType;
import io.netty.handler.stream.ChunkedFile;
import io.netty.handler.stream.ChunkedStream;
import io.netty.util.CharsetUtil;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

/**
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
public class ServletNettyHandler extends ChannelInboundMessageHandlerAdapter<FullHttpRequest> {

	protected static final String UTF8 = "UTF-8";
	protected static final String HTTP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";
	protected static final String HTTP_DATE_GMT_TIMEZONE = "GMT";
	protected static final int HTTP_CACHE_SECONDS = 60;

	protected static Pattern insecureUri = Pattern.compile(".*[<>&\"].*");

	protected ServletContext servletContext;
	protected SessionManager sessionManager;
	protected WebXmlData webXmlData;

	protected HttpServletRequest servletRequest;
	protected HttpServletResponse servletResponse = new NettyHttpServletResponse();

	protected Logger log = LoggerFactory.getLogger(getClass());

	public ServletNettyHandler(ServletContext sc, SessionManager sm, WebXmlData webXmlData) {
		servletContext = sc;
		sessionManager = sm;
		this.webXmlData = webXmlData;
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, FullHttpRequest request) throws ErrorDataDecoderException,
				IncompatibleDataDecoderException, NotEnoughDataDecoderException, IOException, ServletException, ParseException {

		createServletRequest(request);

		if (!request.getDecoderResult().isSuccess()) {
			sendError(ctx, BAD_REQUEST);
			return;
		}

		UriComponents uriComponents = UriComponentsBuilder.fromUriString(request.getUri()).build();
		if (isDisallowed(uriComponents)) {
			sendError(ctx, NOT_FOUND);
			return;
		}

		updateServletRequest(request, uriComponents);

		NettyFilterChain chain = new NettyFilterChain(webXmlData.getFilters(), webXmlData.getServlets(), servletRequest);
		RequestInvocationHandler handler = Utils.findRequestHandler(servletRequest);
		handler.setFilterChain(chain);
		handler.setResponse(servletResponse);

		chain.doFilter(servletRequest, servletResponse);

		servletRequest = chain.getChainRequest();
		servletResponse = chain.getChainResponse();

		if (log.isDebugEnabled()) {
			GroovyUtils.debugResponse(servletResponse);
		}

		NettyHttpServletResponse mockResponse = Utils.findResponse(servletResponse);

		if (!servletResponse.isCommitted()) {
			if (mockResponse.getStatus() == OK.code() && mockResponse.getByteArrayOutputStream().size() > 0) {
				// assume that since there wasn't an error but there is content, the request was processed by a filter (e.g. resources plugin)
				mockResponse.flushBuffer();
			}
		}

		if (servletResponse.isCommitted()) {
			String redirectedUrl = mockResponse.getRedirectedUrl();
			if (redirectedUrl != null) {
				sendRedirect(ctx, redirectedUrl);
				return;
			}
			List<String> includedUrls = mockResponse.getIncludedUrls();
			if (!includedUrls.isEmpty()) {
				// TODO
				log.warn("Unprocessed include URLs: {}", includedUrls);
			}
		}

		boolean close = true; // !isKeepAlive(request); TODO

		if (chain.wasHandled() || servletResponse.isCommitted()) {
			sendChainResponse(ctx, close);
		}
		else {
			writeStaticResponse(ctx, request, close);
		}
	}

	protected void sendChainResponse(ChannelHandlerContext ctx, boolean close) throws IOException {
		HttpResponseStatus status = HttpResponseStatus.valueOf(servletResponse.getStatus());
		HttpResponse response = new DefaultHttpResponse(HTTP_1_1, status);

		NettyHttpServletResponse mockResponse = Utils.findResponse(servletResponse);

		for (String name : mockResponse.getHeaderNames()) {
			for (Object value : mockResponse.getHeaderValues(name)) {
				response.headers().set(name, value);
			}
		}

		Object chunks = new ChunkedStream(new ByteArrayInputStream(mockResponse.getContentAsByteArray()));

		writeResponse(ctx, response, close, chunks);
	}

	protected void writeStaticResponse(ChannelHandlerContext ctx, FullHttpRequest request, boolean close) throws ParseException, IOException {

		if (request.getMethod() != GET) {
			sendError(ctx, METHOD_NOT_ALLOWED);
			return;
		}

		String uri = withoutContext(request.getUri(), servletRequest.getContextPath());
		String path = sanitizeUri(uri);
		if (path == null) {
			sendError(ctx, FORBIDDEN);
			return;
		}

		String realPath = servletContext.getRealPath(path);
		if (realPath != null) {
			File file = new File(realPath);
			if (file.isFile() && !file.isHidden() && file.exists()) {
				writeFileResponse(ctx, request, file, close);
				return;
			}
		}

		URL url = servletContext.getResource(path);
		if (url != null) {
			writeUrlResponse(ctx, request, url, path, close);
			return;
		}

		sendError(ctx, NOT_FOUND);
	}

	protected void writeFileResponse(ChannelHandlerContext ctx, FullHttpRequest request, File file, boolean close) throws ParseException, IOException {

		long lastModified = file.lastModified();

		if (notModified(request, lastModified, ctx)) {
			return;
		}

		RandomAccessFile raf;
		try {
			raf = new RandomAccessFile(file, "r");
		}
		catch (FileNotFoundException e) {
			sendError(ctx, NOT_FOUND);
			return;
		}

		long fileLength = raf.length();

		HttpResponse response = new DefaultHttpResponse(HTTP_1_1, OK);

		setFileHeaders(response, fileLength, lastModified, file.getPath(), close);

		Object chunks = new ChunkedFile(raf, 0, fileLength, 8192);

		writeResponse(ctx, response, close, chunks);
	}

	protected void writeUrlResponse(ChannelHandlerContext ctx, FullHttpRequest request, URL url, String path, boolean close) throws ParseException, IOException {

		URLConnection connection = url.openConnection();
		long lastModified = connection.getLastModified();

		HttpResponse response = new DefaultHttpResponse(HTTP_1_1, OK);

		if (notModified(request, lastModified, ctx)) {
			return;
		}

		setFileHeaders(response, connection.getContentLength(), lastModified, path, close);

		Object chunks = new ChunkedStream(connection.getInputStream(), 8192);

		writeResponse(ctx, response, close, chunks);
	}

	protected boolean notModified(FullHttpRequest request, long lastModified, ChannelHandlerContext ctx) throws ParseException {
		// Cache Validation
		String ifModifiedSince = request.headers().get(IF_MODIFIED_SINCE);
		if (ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
			SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
			Date ifModifiedSinceDate = dateFormatter.parse(ifModifiedSince);

			// Only compare up to the second because the datetime format we send to the client does not have milliseconds
			long ifModifiedSinceDateSeconds = ifModifiedSinceDate.getTime() / 1000;
			long fileLastModifiedSeconds = lastModified / 1000;
			if (ifModifiedSinceDateSeconds == fileLastModifiedSeconds) {
				sendNotModified(ctx);
				return true;
			}
		}

		return false;
	}

	protected void setFileHeaders(HttpResponse response, long fileLength, long lastModified, String path, boolean close) {
		setContentLength(response, fileLength);
		setContentTypeHeader(response, path);
		setDateAndCacheHeaders(response, lastModified);
		if (!close) {
			response.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
		}
	}

	protected String withoutContext(String uri, String contextPath) {
		return contextPath.length() == 0 || !uri.startsWith(contextPath) ? uri : uri.substring(contextPath.length());
	}

	protected void writeResponse(ChannelHandlerContext ctx, HttpResponse response, boolean close, Object chunks) {

		setCookieHeader(response);

		// Write the initial line and the header.
		ctx.write(response);

		// Write the content.
		ChannelFuture writeFuture = ctx.write(chunks);

		if (close) {
			// Close the connection when the whole content is written out.
			writeFuture.addListener(ChannelFutureListener.CLOSE);
		}
	}

	protected boolean isDisallowed(UriComponents uriComponents) {
      // Disallow any direct access to resources under WEB-INF or META-INF
		String contextPath = servletContext.getContextPath();
		String uriLower = uriComponents.getPath().toLowerCase();
		uriLower = contextPath.length() == 0 ? uriLower : uriLower.substring(contextPath.length());

      if (uriLower.startsWith("/meta-inf/") || uriLower.equals("/meta-inf") ||
      		uriLower.startsWith("/web-inf/") || uriLower.equals("/web-inf")) {
      	return true;
      }

      return false;
	}

	/**
	 * Create the initial request so we have cookies in case there's a problem.
	 *
	 * @param nettyRequest the Netty request
	 */
	protected void createServletRequest(FullHttpRequest nettyRequest) {

		HttpServletRequest mockRequest = RequestFactory.createInstance(servletContext, sessionManager, webXmlData);
		servletRequest = mockRequest;

		RequestInvocationHandler handler = Utils.findRequestHandler(mockRequest);
		handler.setContextPath(servletContext.getContextPath());
//		handler.setSecure(nettyRequest); TODO

		for (String name : nettyRequest.headers().names()) {
			for (String value : nettyRequest.headers().getAll(name)) {
				handler.addHeader(name, value);
			}
		}
	}

	protected void updateServletRequest(FullHttpRequest nettyRequest, UriComponents uriComponents)
			throws ErrorDataDecoderException, IncompatibleDataDecoderException, NotEnoughDataDecoderException, IOException {

		RequestInvocationHandler handler = Utils.findRequestHandler(servletRequest);

		handler.setContextPath(servletContext.getContextPath());
		handler.setRequestURI(uriComponents.getPath());
		handler.setPathInfo(uriComponents.getPath());
		handler.setMethod(nettyRequest.getMethod().name());

		if (uriComponents.getScheme() != null) {
			handler.setScheme(uriComponents.getScheme());
		}

		String hostHeader = HttpHeaders.getHost(nettyRequest);
		String[] parts = hostHeader.split(":");
		String serverName = parts[0];
		Integer port = parts.length > 1 ? Integer.valueOf(parts[1]) : null;

		if (uriComponents.getHost() == null) {
			handler.setServerName(serverName);
		}
		else {
			handler.setServerName(uriComponents.getHost());
		}

		if (uriComponents.getPort() != -1) {
			handler.setServerPort(uriComponents.getPort());
		}
		else if (port != null) {
			handler.setServerPort(port);
		}

		if (!(nettyRequest.data() instanceof CompositeByteBuf) || ((CompositeByteBuf)nettyRequest.data()).numComponents() > 0) {
			handler.setContent(nettyRequest.data().array());
		}

		try {
			if (uriComponents.getQuery() != null) {
				String query = UriUtils.decode(uriComponents.getQuery(), UTF8);
				handler.setQueryString(query);
			}

			for (Map.Entry<String, List<String>> entry : uriComponents.getQueryParams().entrySet()) {
				for (String value : entry.getValue()) {
					handler.addParameter(
							UriUtils.decode(entry.getKey(), UTF8),
							UriUtils.decode(value, UTF8));
				}
			}

			if (nettyRequest.getMethod() == POST) {
				for (InterfaceHttpData data : new HttpPostRequestDecoder(nettyRequest).getBodyHttpDatas()) {
					if (data.getHttpDataType() == HttpDataType.Attribute) {
						handler.addParameter(data.getName(), ((Attribute)data).getValue());
					}
				}
			}
		}
		catch (UnsupportedEncodingException ex) {
			// shouldn't happen
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		log.error(cause.getMessage(), cause);
		if (ctx.channel().isActive()) {
			sendError(ctx, INTERNAL_SERVER_ERROR);
		}
	}

	protected void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {

		FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, status,
				Unpooled.copiedBuffer("Failure: " + status + "\r\n", CharsetUtil.UTF_8));
		response.headers().set(CONTENT_TYPE, "text/plain; charset=UTF-8");

		writeWithClose(ctx, response);
	}

	protected void sendRedirect(ChannelHandlerContext ctx, String newUri) {
		FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, FOUND);
		response.headers().set(LOCATION, newUri);

		writeWithClose(ctx, response);
	}

	protected void sendNotModified(ChannelHandlerContext ctx) {
		FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, NOT_MODIFIED);
		setDateHeader(response);

		writeWithClose(ctx, response);
	}

	protected void writeWithClose(ChannelHandlerContext ctx, FullHttpResponse response) {
		setCookieHeader(response);

		// Close the connection as soon as the error message is sent.
		ctx.write(response).addListener(ChannelFutureListener.CLOSE);
	}

	protected void setCookieHeader(HttpResponse response) {
		Cookie[] cookies = Utils.findResponse(servletResponse).getCookies();
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				response.headers().add(SET_COOKIE, ServerCookieEncoder.encode(Utils.createCookie(cookie)));
			}
		}
	}

	protected void setDateHeader(FullHttpResponse response) {
		SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
		dateFormatter.setTimeZone(TimeZone.getTimeZone(HTTP_DATE_GMT_TIMEZONE));

		response.headers().set(DATE, dateFormatter.format(new GregorianCalendar().getTime()));
	}

	protected void setDateAndCacheHeaders(HttpResponse response, long lastModified) {
		SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
		dateFormatter.setTimeZone(TimeZone.getTimeZone(HTTP_DATE_GMT_TIMEZONE));

		// Date header
		Calendar time = new GregorianCalendar();
		response.headers().set(DATE, dateFormatter.format(time.getTime()));

		// Add cache headers
		time.add(Calendar.SECOND, HTTP_CACHE_SECONDS);
		response.headers().set(EXPIRES, dateFormatter.format(time.getTime()));
		response.headers().set(CACHE_CONTROL, "private, max-age=" + HTTP_CACHE_SECONDS);
		response.headers().set(LAST_MODIFIED, dateFormatter.format(new Date(lastModified)));
	}

	protected void setContentTypeHeader(HttpResponse response, String filename) {
		String mimeType = servletContext.getMimeType(filename);
		if (mimeType == null) {
			// TODO
		}
		else {
			response.headers().set(CONTENT_TYPE, mimeType);
		}
	}

	protected String sanitizeUri(String uri) {
		// Decode the path.
		try {
			uri = URLDecoder.decode(uri, "UTF-8");
      }
		catch (UnsupportedEncodingException e) {
			try {
				uri = URLDecoder.decode(uri, "ISO-8859-1");
			}
			catch (UnsupportedEncodingException e1) {
				throw new Error(); // TODO
			}
		}

		if (!uri.startsWith("/")) {
			return null;
		}

		// Convert file separators.
		uri = uri.replace('/', File.separatorChar);

		// Simplistic dumb security check.
		// You will have to do something serious in the production environment.
		if (uri.contains(File.separator + '.') || uri.contains('.' + File.separator) || uri.startsWith(".") ||
				uri.endsWith(".") || insecureUri.matcher(uri).matches()) {
			return null;
		}

		return uri;
	}
}
