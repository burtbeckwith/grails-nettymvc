package grails.plugin.nettymvc.data;

/**
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
public class CookieConfigData {

	protected boolean httpOnly;
	protected boolean secure;
	protected String name;
	protected int maxAge;
	protected String domain;
	protected String path;
	protected String comment;

	public CookieConfigData(boolean httpOnly, boolean secure, String name, int maxAge,
			String domain, String path, String comment) {
		this.httpOnly = httpOnly;
		this.secure = secure;
		this.name = name;
		this.maxAge = maxAge;
		this.domain = domain;
		this.path= path;
		this.comment= comment;
	}

	public boolean isHttpOnly() {
		return httpOnly;
	}

	public boolean isSecure() {
		return secure;
	}

	public String getName() {
		return name;
	}

	public int getMaxAge() {
		return maxAge;
	}

	public String getDomain() {
		return domain;
	}

	public String getPath() {
		return path;
	}

	public String getComment() {
		return comment;
	}
			
	@Override
	public String toString() {
		return "CookieConfigData name: '" + name + "' httpOnly: " + httpOnly +
				"' secure: " + secure + ", maxAge: " + maxAge + ", domain: " + domain +
				", path: " + path + ", comment: " + comment;
	}
}
