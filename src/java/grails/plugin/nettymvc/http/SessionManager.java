package grails.plugin.nettymvc.http;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

/**
 * Based on org.apache.catalina.session.ManagerBase.
 *
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
public class SessionManager implements InitializingBean {

	protected static final int DEFAULT_MAX_INACTIVE_INTERVAL = 30 * 60;

	protected Logger log = LoggerFactory.getLogger(getClass());

	protected ServletContext servletContext;
	protected boolean distributable;

	protected Map<String, NettyHttpSession> sessions = new ConcurrentHashMap<String, NettyHttpSession>();
	protected SessionIdGenerator sessionIdGenerator = new SessionIdGenerator();
	protected int maxInactiveInterval = DEFAULT_MAX_INACTIVE_INTERVAL;

	protected int backgroundProcessorDelay = 60;

	/** The maximum number of active Sessions allowed, or -1 for no limit. */
	protected int maxActiveSessions = -1;

	/**
	 * Construct and return a new session object, based on the default
	 * settings specified by this Manager's properties.  The session
	 * id specified will be used as the session id.  
	 * If a new session cannot be created for any reason, return 
	 * <code>null</code>.
	 * 
	 * @exception IllegalStateException if a new session cannot be instantiated for any reason
	 */
	public NettyHttpSession createSession() {
		if (maxActiveSessions >= 0 && sessions.size() >= maxActiveSessions) {
			throw new IllegalStateException("Cannot create more than " + maxActiveSessions + " sessions");
		}
        
		String id = generateSessionId();

		NettyHttpSession session = new NettyHttpSession(servletContext, id, this);
		session.setMaxInactiveInterval(maxInactiveInterval);
		sessions.put(id, session);
		return session;
	}

	public NettyHttpSession findSession(String id) {
		return id == null ? null : sessions.get(id);
	}

	/**
	 * Set the maximum number of active Sessions allowed, or -1 for no limit.
	 *
	 * @param max The new maximum number of sessions
	 */
	public void setMaxActiveSessions(int max) {
		maxActiveSessions = max;
	}
	public int getMaxActiveSessions() {
		return maxActiveSessions;
	}

	public void setDistributable(boolean d) {
		distributable = d;
	}
	public boolean isDistributable() {
		return distributable;
	}

	/**
	 * Set the default maximum inactive interval (in seconds)
	 * for Sessions created by this Manager.
	 *
	 * @param interval The new default value
	 */
	public void setMaxInactiveInterval(int interval) {
		maxInactiveInterval = interval;
	}
	public int getMaxInactiveInterval() {
		return maxInactiveInterval;
	}

	public void remove(NettyHttpSession session) {
		if (session.getId() != null) {
			sessions.remove(session.getId());
		}
	}

	public boolean isSessionIdValid(String requestedSessionId) {
		NettyHttpSession session = sessions.get(requestedSessionId);
		return session != null && session.isValid();
	}

	/**
	 * Generate and return a new session identifier.
	 */
	protected String generateSessionId() {
		String result = null;
		do {
			result = sessionIdGenerator.generateSessionId();
		}
		while (sessions.containsKey(result));

		return result;
	}

	protected void startCleanupThread() {
		new Thread() {

			{ setDaemon(true); }

			@Override
			public void run() {
				while (true) {
					try {
						Thread.sleep(backgroundProcessorDelay * 1000L);
					}
					catch (InterruptedException e) {
						// Ignore
					}

					try {
						for (NettyHttpSession session : sessions.values()) {
							if (session != null && !session.isValid()) {
								session.expire(true);
							}
						}
					}
					catch (Throwable t) {
						log.error("Exception invoking session cleanup", t);
					}
				}
			}
		}.start();
	}

	/**
	 * Dependency injection.
	 * @param sc the ServletContext
	 */
	public void setServletContext(ServletContext sc) {
		servletContext = sc;
	}

	/* (non-Javadoc)
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws Exception {
		// Force initialization of the random number generator
		sessionIdGenerator.generateSessionId();
		startCleanupThread();
	}
}
