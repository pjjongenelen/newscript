package astre.representation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
/**
 * Storing sentence
 * @author nguyen
 *
 */
public class Sentence {
	/**
	 * 
	 */
	private Token root;
	/**
	 * 
	 * @param root
	 */
	public void setRoot(Token root) {
		this.root = root;
	}
	/**
	 * 
	 * @return get dependency root
	 */
	public Token getRoot() {
		return root;
	}
	/**
	 * 
	 */
	private int id;
	/**
	 * 
	 */
	private Map<Integer,Token> tokens;
	/**
	 * 
	 */
	private List<Dependency> dependencies;
	/**
	 * 
	 * @return get sentence id
	 */
	public int getId() {
		return id;
	}
	/**
	 * 
	 * @param id set sentence id
	 */
	public void setId(int id) {
		this.id = id;
	}
	/**
	 * Get tokens
	 * @return tokens
	 */
	public Map<Integer,Token> getTokens() {
		return tokens;
	}
	/**
	 * Set tokens
	 * @param tokens
	 */
	public void setTokens(Map<Integer,Token> tokens) {
		this.tokens = tokens;
	}
	/**
	 * Get depedencies
	 * @return dependencies
	 */
	public List<Dependency> getDependencies() {
		return dependencies;
	}
	/**
	 * Set dependencies
	 * @param dependencies
	 */
	public void setDependencies(List<Dependency> dependencies) {
		this.dependencies = dependencies;
	}
	/**
	 * Constructor
	 * @param id
	 * @param tokens
	 * @param dependencies
	 */
	public Sentence(int id, Map<Integer,Token> tokens, List<Dependency> dependencies) {
		super();
		this.id = id;
		this.tokens = tokens;
		this.dependencies = dependencies;
	}
	/**
	 * Get token by id
	 * @param tokenId
	 * @return token
	 */
	public Token getTokenById(int tokenId) {
		return tokens.get(tokenId);
	}
	/**
	 * Get dependencies by dependent
	 * @param tokenId
	 * @return dependencies
	 */
	public List<Dependency> getDependenciesByDependent(int tokenId) {
		List<Dependency> list = new ArrayList<Dependency>();
		for(Dependency dependency:dependencies) {
			if (!"root".equals(dependency.getType()) && 
					dependency.getDependent().getId() == tokenId) {
				list.add(dependency);
			}
		}
		return list;
	}
	/**
	 * Get dependencies by govenor
	 * @param tokenId
	 * @return dependencies
	 */
	public List<Dependency> getDependenciesByGovernor(int tokenId) {
		List<Dependency> list = new ArrayList<Dependency>();
		for(Dependency dependency:dependencies) {
			if (!"root".equals(dependency.getType()) && 
					dependency.getGovernor().getId() == tokenId) {
				list.add(dependency);
			}
		}
		return list;
	}	
}