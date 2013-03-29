package grails.plugin.nettymvc.http;

import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.springframework.util.LinkedCaseInsensitiveMap;
import org.springframework.web.util.WebUtils;

/**
 * Based on org.springframework.mock.web.MockHttpServletResponse.
 *
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
public class NettyHttpServletResponse implements HttpServletResponse {

	protected static final String CHARSET_PREFIX = "charset=";

	protected boolean outputStreamAccessAllowed = true;
	protected boolean writerAccessAllowed = true;
	protected String characterEncoding = WebUtils.DEFAULT_CHARACTER_ENCODING;
	protected boolean charset = false;
	protected ByteArrayOutputStream content = new ByteArrayOutputStream();
	protected ServletOutputStream outputStream = new ResponseServletOutputStream(content);
	protected PrintWriter writer;
	protected int contentLength = 0;
	protected String contentType;
	protected int bufferSize = 4096;
	protected boolean committed;
	protected Locale locale = Locale.getDefault();
	protected List<Cookie> cookies = new ArrayList<Cookie>();
	protected Map<String, HeaderValueHolder> headers = new LinkedCaseInsensitiveMap<HeaderValueHolder>();
	protected int status = HttpServletResponse.SC_OK;
	protected String errorMessage;
	protected String redirectedUrl;
	protected String forwardedUrl;
	protected List<String> includedUrls = new ArrayList<String>();

	public String getCharacterEncoding() {
		return characterEncoding;
	}

	public String getContentType() {
		return contentType;
	}

	public ServletOutputStream getOutputStream() throws IOException {
		if (!outputStreamAccessAllowed) {
			throw new IllegalStateException("OutputStream access not allowed");
		}
		return outputStream;
	}

	public PrintWriter getWriter() throws IOException {
		if (!writerAccessAllowed) {
			throw new IllegalStateException("Writer access not allowed");
		}
		if (writer == null) {
			Writer targetWriter = (characterEncoding != null ?
					new OutputStreamWriter(content, characterEncoding) : new OutputStreamWriter(content));
			writer = new ResponsePrintWriter(targetWriter);
		}
		return writer;
	}

	public void setCharacterEncoding(String encoding) {
		characterEncoding = encoding;
		charset = true;
		updateContentTypeHeader();
	}

	public void setContentLength(int length) {
		contentLength = length;
		doAddHeaderValue(CONTENT_LENGTH, contentLength, true);
	}

	public void setContentType(String type) {
		contentType = type;
		if (contentType != null) {
			int charsetIndex = contentType.toLowerCase().indexOf(CHARSET_PREFIX);
			if (charsetIndex != -1) {
				String encoding = contentType.substring(charsetIndex + CHARSET_PREFIX.length());
				characterEncoding = encoding;
				charset = true;
			}
			updateContentTypeHeader();
		}
	}

	public void setBufferSize(int size) {
		bufferSize = size;
	}

	public int getBufferSize() {
		return bufferSize;
	}

	public void flushBuffer() throws IOException {
		setCommitted(true);
	}

	public void resetBuffer() {
		if (isCommitted()) {
			throw new IllegalStateException("Cannot reset buffer - response is already committed");
		}
		content.reset();
	}

	public boolean isCommitted() {
		return committed;
	}

	public void reset() {
		resetBuffer();
		characterEncoding = null;
		contentLength = 0;
		contentType = null;
		locale = null;
		cookies.clear();
		headers.clear();
		status = HttpServletResponse.SC_OK;
		errorMessage = null;
	}

	public void setLocale(Locale l) {
		locale = l;
	}

	public Locale getLocale() {
		return locale;
	}

	public void addCookie(Cookie cookie) {
		cookies.add(cookie);
	}

	public boolean containsHeader(String name) {
		return HeaderValueHolder.getByName(headers, name) != null;
	}

	/**
	 * The default implementation returns the given URL String as-is.
	 * <p>Can be overridden in subclasses, appending a session id or the like.
	 */
	public String encodeURL(String url) {
		return url;
	}

	/**
	 * The default implementation delegates to {@link #encodeURL},
	 * returning the given URL String as-is.
	 * <p>Can be overridden in subclasses, appending a session id or the like
	 * in a redirect-specific fashion. For general URL encoding rules,
	 * override the common {@link #encodeURL} method instead, appyling
	 * to redirect URLs as well as to general URLs.
	 */
	public String encodeRedirectURL(String url) {
		return encodeURL(url);
	}

	public String encodeUrl(String url) {
		return encodeURL(url);
	}

	public String encodeRedirectUrl(String url) {
		return encodeRedirectURL(url);
	}

	public void sendError(int code, String message) throws IOException {
		if (isCommitted()) {
			throw new IllegalStateException("Cannot set error status - response is already committed");
		}
		status = code;
		errorMessage = message;
		setCommitted(true);
	}

	public void sendError(int code) throws IOException {
		if (isCommitted()) {
			throw new IllegalStateException("Cannot set error status - response is already committed");
		}
		status = code;
		setCommitted(true);
	}

	public void sendRedirect(String url) throws IOException {
		if (isCommitted()) {
			throw new IllegalStateException("Cannot send redirect - response is already committed");
		}
		redirectedUrl = url;
		setCommitted(true);
	}

	public void setDateHeader(String name, long date) {
		setHeaderValue(name, date);
	}

	public void addDateHeader(String name, long date) {
		addHeaderValue(name, date);
	}

	public void setHeader(String name, String value) {
		setHeaderValue(name, value);
	}

	public void addHeader(String name, String value) {
		addHeaderValue(name, value);
	}

	public void setIntHeader(String name, int value) {
		setHeaderValue(name, value);
	}

	public void addIntHeader(String name, int value) {
		addHeaderValue(name, value);
	}

	public void setStatus(int code) {
		status = code;
	}

	public void setStatus(int code, String message) {
		status = code;
		errorMessage = message;
	}

	public int getStatus() {
		return status;
	}

	/**
	 * Return the primary value for the given header as a String, if any.
	 * Will return the first value in case of multiple values.
	 * <p>As of Servlet 3.0, this method is also defined HttpServletResponse.
	 * As of Spring 3.1, it returns a stringified value for Servlet 3.0 compatibility.
	 * Consider using {@link #getHeaderValue(String)} for raw Object access.
	 * @param name the name of the header
	 * @return the associated header value, or <code>null<code> if none
	 */
	public String getHeader(String name) {
		HeaderValueHolder header = HeaderValueHolder.getByName(headers, name);
		return (header != null ? header.getStringValue() : null);
	}

	/**
	 * Return all values for the given header as a List of Strings.
	 * <p>As of Servlet 3.0, this method is also defined HttpServletResponse.
	 * As of Spring 3.1, it returns a List of stringified values for Servlet 3.0 compatibility.
	 * Consider using {@link #getHeaderValues(String)} for raw Object access.
	 * @param name the name of the header
	 * @return the associated header values, or an empty List if none
	 */
	public Collection<String> getHeaders(String name) {
		HeaderValueHolder header = HeaderValueHolder.getByName(headers, name);
		if (header == null) {
			return Collections.emptyList();
		}
		return header.getStringValues();
	}

	/**
	 * Return the names of all specified headers as a Set of Strings.
	 * <p>As of Servlet 3.0, this method is also defined HttpServletResponse.
	 * @return the <code>Set</code> of header name <code>Strings</code>, or an empty <code>Set</code> if none
	 */
	public Collection<String> getHeaderNames() {
		return headers.keySet();
	}

	//---------------------------------------------------------------------
	// Non-API methods
	//---------------------------------------------------------------------

	/**
	 * Set whether {@link #getOutputStream()} access is allowed.
	 * <p>Default is <code>true</code>.
	 */
	public void setOutputStreamAccessAllowed(boolean allowed) {
		outputStreamAccessAllowed = allowed;
	}

	/**
	 * Return whether {@link #getOutputStream()} access is allowed.
	 */
	public boolean isOutputStreamAccessAllowed() {
		return outputStreamAccessAllowed;
	}

	/**
	 * Set whether {@link #getWriter()} access is allowed.
	 * <p>Default is <code>true</code>.
	 */
	public void setWriterAccessAllowed(boolean allowed) {
		writerAccessAllowed = allowed;
	}

	/**
	 * Return whether {@link #getOutputStream()} access is allowed.
	 */
	public boolean isWriterAccessAllowed() {
		return writerAccessAllowed;
	}

	protected void updateContentTypeHeader() {
		if (contentType != null) {
			StringBuilder sb = new StringBuilder(contentType);
			if (contentType.toLowerCase().indexOf(CHARSET_PREFIX) == -1 && charset) {
				sb.append(";").append(CHARSET_PREFIX).append(characterEncoding);
			}
			doAddHeaderValue(CONTENT_TYPE, sb.toString(), true);
		}
	}

	public byte[] getContentAsByteArray() throws IOException {
		flushBuffer();
		return content.toByteArray();
	}

	public String getContentAsString() throws IOException {
		flushBuffer();
		return characterEncoding == null ? content.toString() : content.toString(characterEncoding);
	}

	public int getContentLength() {
		return contentLength;
	}

	protected void setCommittedIfBufferSizeExceeded() {
		int bufSize = getBufferSize();
		if (bufSize > 0 && content.size() > bufSize) {
			setCommitted(true);
		}
	}

	public void setCommitted(boolean c) {
		committed = c;
	}

	public Cookie[] getCookies() {
		return cookies.toArray(new Cookie[cookies.size()]);
	}

	public Cookie getCookie(String name) {
		for (Cookie cookie : cookies) {
			if (name.equals(cookie.getName())) {
				return cookie;
			}
		}
		return null;
	}

	/**
	 * Return the primary value for the given header, if any.
	 * <p>Will return the first value in case of multiple values.
	 * @param name the name of the header
	 * @return the associated header value, or <code>null<code> if none
	 */
	public Object getHeaderValue(String name) {
		HeaderValueHolder header = HeaderValueHolder.getByName(headers, name);
		return (header != null ? header.getValue() : null);
	}

	/**
	 * Return all values for the given header as a List of value objects.
	 * @param name the name of the header
	 * @return the associated header values, or an empty List if none
	 */
	public List<Object> getHeaderValues(String name) {
		HeaderValueHolder header = HeaderValueHolder.getByName(headers, name);
		if (header == null) {
			return Collections.emptyList();
		}
		return header.getValues();
	}

	public String getRedirectedUrl() {
		return redirectedUrl;
	}

	protected void setHeaderValue(String name, Object value) {
		if (setSpecialHeader(name, value)) {
			return;
		}
		doAddHeaderValue(name, value, true);
	}

	protected void addHeaderValue(String name, Object value) {
		if (setSpecialHeader(name, value)) {
			return;
		}
		doAddHeaderValue(name, value, false);
	}

	protected boolean setSpecialHeader(String name, Object value) {
		if (CONTENT_TYPE.equalsIgnoreCase(name)) {
			setContentType((String) value);
			return true;
		}

		if (CONTENT_LENGTH.equalsIgnoreCase(name)) {
			setContentLength(Integer.parseInt((String) value));
			return true;
		}

		return false;
	}

	protected void doAddHeaderValue(String name, Object value, boolean replace) {
		HeaderValueHolder header = HeaderValueHolder.getByName(headers, name);
		if (header == null) {
			header = new HeaderValueHolder();
			headers.put(name, header);
		}
		if (replace) {
			header.setValue(value);
		}
		else {
			header.addValue(value);
		}
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	//---------------------------------------------------------------------
	// Methods for MockRequestDispatcher
	//---------------------------------------------------------------------

	public void setForwardedUrl(String url) {
		forwardedUrl = url;
	}

	public String getForwardedUrl() {
		return forwardedUrl;
	}

	public void setIncludedUrl(String includedUrl) {
		includedUrls.clear();
		if (includedUrl != null) {
			includedUrls.add(includedUrl);
		}
	}

	public String getIncludedUrl() {
		int count = includedUrls.size();
		if (count > 1) {
			throw new IllegalStateException(
					"More than 1 URL included - check getIncludedUrls instead: " + includedUrls);
		}
		return (count == 1 ? includedUrls.get(0) : null);
	}

	public void addIncludedUrl(String includedUrl) {
		includedUrls.add(includedUrl);
	}

	public List<String> getIncludedUrls() {
		return includedUrls;
	}

	/**
	 * Inner class that adapts the ServletOutputStream to mark the
	 * response as committed once the buffer size is exceeded.
	 */
	protected class ResponseServletOutputStream extends ServletOutputStream {

		protected OutputStream targetStream;

		public ResponseServletOutputStream(OutputStream out) {
			targetStream = out;
		}

		@Override
		public void write(int b) throws IOException {
			targetStream.write(b);
			super.flush();
			targetStream.flush();
			setCommittedIfBufferSizeExceeded();
		}

		@Override
		public void flush() throws IOException {
			super.flush();
			targetStream.flush();
			setCommitted(true);
		}

		@Override
		public void close() throws IOException {
			super.close();
			targetStream.close();
		}
	}

	/**
	 * Inner class that adapts the PrintWriter to mark the
	 * response as committed once the buffer size is exceeded.
	 */
	protected class ResponsePrintWriter extends PrintWriter {

		public ResponsePrintWriter(Writer out) {
			super(out, true);
		}

		@Override
		public void write(char buf[], int off, int len) {
			super.write(buf, off, len);
			super.flush();
			setCommittedIfBufferSizeExceeded();
		}

		@Override
		public void write(String s, int off, int len) {
			super.write(s, off, len);
			super.flush();
			setCommittedIfBufferSizeExceeded();
		}

		@Override
		public void write(int c) {
			super.write(c);
			super.flush();
			setCommittedIfBufferSizeExceeded();
		}

		@Override
		public void flush() {
			super.flush();
			setCommitted(true);
		}
	}
}
