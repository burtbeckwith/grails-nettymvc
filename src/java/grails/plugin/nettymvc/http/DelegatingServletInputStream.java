package grails.plugin.nettymvc.http;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletInputStream;

/**
 * Based on org.springframework.mock.web.DelegatingServletInputStream.
 *
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
public class DelegatingServletInputStream extends ServletInputStream {

	protected InputStream sourceStream;

	/**
	 * Create a DelegatingServletInputStream for the given source stream.
	 * @param source the source stream (never <code>null</code>)
	 */
	public DelegatingServletInputStream(InputStream source) {
		sourceStream = source;
	}

	@Override
	public int read() throws IOException {
		return sourceStream.read();
	}

	@Override
	public void close() throws IOException {
		super.close();
		sourceStream.close();
	}
}
