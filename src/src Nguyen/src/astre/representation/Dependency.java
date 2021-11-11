package astre.representation;
/**
 * Storing a typed depedency
 * @author nguyen
 *
 */
public class Dependency implements Comparable<Dependency> {
	/**
	 * 
	 */
	private Token governor;
	/**
	 * 
	 */
	private Token dependent;
	/**
	 * 
	 */
	private String type;
	/**
	 * 
	 * @param governor
	 * @param dependent
	 * @param type
	 */
	public Dependency(Token governor, Token dependent, String type) {
		super();
		this.governor = governor;
		this.dependent = dependent;
		this.type = type;
	}
	/**
	 * 
	 * @return governor
	 */
	public Token getGovernor() {
		return governor;
	}	
	/**
	 * 
	 * @param governor
	 */
	public void setGovernor(Token governor) {
		this.governor = governor;
	}
	/**
	 * 
	 * @return dependent
	 */
	public Token getDependent() {
		return dependent;
	}
	/**
	 * 
	 * @param dependent
	 */
	public void setDependent(Token dependent) {
		this.dependent = dependent;
	}
	/**
	 * 
	 * @return dependency type
	 */
	public String getType() {
		return type;
	}
	/**
	 * 
	 * @param type
	 */
	public void setType(String type) {
		this.type = type;
	}
	/**
	 * Override Object.equals() function
	 * @param obj
	 * @return Is dependency equal?
	 */
	public boolean equals(Object obj) {
		return this.toString().equals(((Dependency) obj).toString());
	}
	/**
	 * Override Object.toString() function
	 * String representation of dependency
	 */
	public String toString() {
		return governor.getLemma() + ":" + type;
	}
	/**
	 * Override Object.compareTo()
	 * @param dependency
	 * @return 0 if dependency is equal
	 */
	public int compareTo(Dependency dependency) {
		return this.toString().compareTo(dependency.toString());
	}
}