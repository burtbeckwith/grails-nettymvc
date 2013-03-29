package grails.plugin.nettymvc.http;

import java.io.Serializable;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

/**
 * Based on org.springframework.mock.web.MockHttpSession and org.apache.catalina.session.
 *
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
public class NettyHttpSession implements HttpSession, Serializable {

	private static final long serialVersionUID = 1;

	protected final Logger log = LoggerFactory.getLogger(getClass());

	protected static final String[] EMPTY_ARRAY = {};

	protected static boolean strictServletCompliance = false;
	protected static boolean lastAccessAtStart = strictServletCompliance;

	protected String id;
	/** The time this session was created, in milliseconds since midnight, January 1, 1970 GMT. */
	protected long creationTime = System.currentTimeMillis();	
	/**
	 * The maximum time interval, in seconds, between client requests before
	 * the servlet container may invalidate this session. A negative time
	 * indicates that the session should never time out.
	 */
	protected int maxInactiveInterval = -1;
	/** The last accessed time for this Session. */
	protected volatile long lastAccessedTime = creationTime;
	protected transient ServletContext servletContext;
	protected transient SessionManager sessionManager;
	protected Map<String, Object> attributes = new ConcurrentHashMap<String, Object>();
	/** Flag indicating whether this session is new. */
	protected boolean isNew = true;
	/** Flag indicating whether this session is valid. */
	protected volatile boolean isValid = true;
	/** The current accessed time for this session. */
	protected volatile long thisAccessedTime = creationTime;

	/**
	 * We are currently processing a session expiration, so bypass
	 * certain IllegalStateException tests. NOTE: This value is not
	 * included in the serialized version of this object.
	 */
	protected transient volatile boolean expiring = false;

	public NettyHttpSession(ServletContext sc, String sessionId, SessionManager sm) {
		servletContext = sc;
		sessionManager = sm;
		id = sessionId;
	}

	public long getCreationTime() {
		assertValid();
		return creationTime;
	}

	public String getId() {
		return id;
	}

	public long getLastAccessedTime() {
		assertValid();
		return lastAccessedTime;
	}

	public ServletContext getServletContext() {
		return servletContext;
	}

	public void setMaxInactiveInterval(int interval) {
		maxInactiveInterval = interval;
	}

	public int getMaxInactiveInterval() {
		return maxInactiveInterval;
	}

	@SuppressWarnings("deprecation")
	public javax.servlet.http.HttpSessionContext getSessionContext() {
		throw new UnsupportedOperationException("getSessionContext");
	}

	public Object getAttribute(String name) {
		assertValid();

		return name == null ? null : attributes.get(name);
	}

	public Object getValue(String name) {
		return getAttribute(name);
	}

	public Enumeration<String> getAttributeNames() {
		assertValid();
		return Collections.enumeration(new HashSet<String>(attributes.keySet()));
	}

	public String[] getValueNames() {
		assertValid();
		return keys();
	}

	public void setAttribute(String name, Object value) {
		setAttribute(name, value, true);
	}

	public void putValue(String name, Object value) {
		setAttribute(name, value);
	}

	public void removeAttribute(String name) {
		removeAttribute(name, true);
	}

	public void removeValue(String name) {
		removeAttribute(name);
	}

	public void invalidate() {
		assertValid();
		expire(true);
	}

	public boolean isNew() {
		assertValid();
		return isNew;
	}

	/////////////////////
	// non-API methods //
	/////////////////////

	public void access() {
		lastAccessedTime = System.currentTimeMillis();
		isNew = false;
	}

	/**
	 * Serialize the attributes of this session into an object that can be
	 * turned into a byte array with standard Java serialization.
	 *
	 * @return a representation of this session's serialized state
	 */
	public Serializable serializeState() {
		HashMap<String, Serializable> state = new HashMap<String, Serializable>();
		for (Iterator<Map.Entry<String, Object>> it = attributes.entrySet().iterator(); it.hasNext();) {
			Map.Entry<String, Object> entry = it.next();
			String name = entry.getKey();
			Object value = entry.getValue();
			it.remove();
			if (value instanceof Serializable) {
				state.put(name, (Serializable) value);
			}
			else {
				// Not serializable... Servlet containers usually automatically
				// unbind the attribute in this case.
				if (value instanceof HttpSessionBindingListener) {
					((HttpSessionBindingListener) value).valueUnbound(new HttpSessionBindingEvent(this, name, value));
				}
			}
		}
		return state;
	}

	/**
	 * Deserialize the attributes of this session from a state object created by
	 * {@link #serializeState()}.
	 *
	 * @param state a representation of this session's serialized state
	 */
	@SuppressWarnings("unchecked")
	public void deserializeState(Serializable state) {
		attributes.putAll((Map<String, Object>) state);
	}

	/**
	 * Remove the object bound with the specified name from this session. If
	 * the session does not have an object bound with this name, this method
	 * does nothing.
	 * <p>
	 * After this method executes, and if the object implements
	 * <code>HttpSessionBindingListener</code>, the container calls
	 * <code>valueUnbound()</code> on the object.
	 *
	 * @param name Name of the object to remove from this session.
	 * @param notify Should we notify interested listeners that this attribute is being removed?
	 *
	 * @exception IllegalStateException if this method is called on an invalidated session
	 */
	protected void removeAttribute(String name, boolean notify) {
		assertValid();
		removeAttributeInternal(name, notify);
	}

	/**
	 * Remove the object bound with the specified name from this session. If
	 * the session does not have an object bound with this name, this method
	 * does nothing.
	 * <p>
	 * After this method executes, and if the object implements
	 * <code>HttpSessionBindingListener</code>, the container calls
	 * <code>valueUnbound()</code> on the object.
	 *
	 * @param name Name of the object to remove from this session.
	 * @param notify Should we notify interested listeners that this attribute is being removed?
	 */
	protected void removeAttributeInternal(String name, boolean notify) {

		if (name == null) return;

		Object value = attributes.remove(name);

		if (!notify || value == null) {
			return;
		}

		HttpSessionBindingEvent event = null;
		if (value instanceof HttpSessionBindingListener) {
			event = new HttpSessionBindingEvent(this, name, value);
			((HttpSessionBindingListener) value).valueUnbound(event);
		}
	}

	/**
	 * Bind an object to this session, using the specified name. If an object
	 * of the same name is already bound to this session, the object is
	 * replaced.
	 * <p>
	 * After this method executes, and if the object implements
	 * <code>HttpSessionBindingListener</code>, the container calls
	 * <code>valueBound()</code> on the object.
	 *
	 * @param name Name to which the object is bound, cannot be null
	 * @param value Object to be bound, cannot be null
	 * @param notify whether to notify session listeners
	 * @exception IllegalArgumentException if an attempt is made to add a
	 * non-serializable object in an environment marked distributable.
	 * @exception IllegalStateException if this method is called on an invalidated session
	 */
	protected void setAttribute(String name, Object value, boolean notify) {
		Assert.notNull(name, "Attribute name must not be null");

		if (value == null) {
			removeAttribute(name);
			return;
		}

		assertValid();

		HttpSessionBindingEvent event = null;
		if (notify && value instanceof HttpSessionBindingListener) {
			Object oldValue = attributes.get(name);
			if (value != oldValue) {
				event = new HttpSessionBindingEvent(this, name, value);
				try {
					((HttpSessionBindingListener) value).valueBound(event);
				}
				catch (Throwable t) {
					log.error(t.getMessage(), t);
				}
			}
		}

		Object unbound = attributes.put(name, value);

		if (notify && (unbound != null) && (unbound != value) && (unbound instanceof HttpSessionBindingListener)) {
			try {
				((HttpSessionBindingListener) unbound).valueUnbound(new HttpSessionBindingEvent(this, name));
			}
			catch (Throwable t) {
				log.error(t.getMessage(), t);
			}
		}
	}

	public boolean isValid() {
		if (expiring) {
			return true;
		}

		if (!isValid) {
			return false;
		}

		if (maxInactiveInterval > 0) {
			long timeNow = System.currentTimeMillis();
			int timeIdle;
			if (lastAccessAtStart) {
				timeIdle = (int) ((timeNow - lastAccessedTime) / 1000L);
			}
			else {
				timeIdle = (int) ((timeNow - thisAccessedTime) / 1000L);
			}
			if (timeIdle >= maxInactiveInterval) {
				expire(true);
			}
		}

		return isValid;
	}

	/**
	 * Perform the internal processing required to invalidate this session,
	 * without triggering an exception if the session has already expired.
	 *
	 * @param notify Should we notify listeners about the demise of this session?
	 */
	public void expire(boolean notify) {

		// Check to see if expire is in progress or has previously been called
		if (expiring || !isValid) {
			return;
		}

		synchronized (this) {
			// Check again, now we are inside the sync so this code only runs once
			// Double check locking - expiring and isValid need to be volatile
			if (expiring || !isValid) {
				return;
			}

			expiring = true;
			isValid = false;
			sessionManager.remove(this);
			expiring = false;

			String keys[] = keys();
			for (int i = 0; i < keys.length; i++) {
				removeAttributeInternal(keys[i], notify);
			}
		}
	}

	protected void assertValid() {
		if (isValid || expiring) {
			return;
		}
		throw new IllegalStateException("Session is invalid or expired");
	}

	/**
	 * Return the names of all currently defined session attributes
	 * as an array of Strings. If there are no defined attributes, a
	 * zero-length array is returned.
	 */
	protected String[] keys() {
		return attributes.keySet().toArray(EMPTY_ARRAY);
	}
}
