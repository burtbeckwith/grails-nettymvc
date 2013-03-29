package grails.plugin.nettymvc.data;

/**
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
public class NameAndValue {

	protected String name;
	protected String value;

	public NameAndValue(String name, String value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public String getValue() {
		return value;
	}

	@Override
	public String toString() {
		return "name: '" + name + "' value: '" + value + "'";
	}
}
