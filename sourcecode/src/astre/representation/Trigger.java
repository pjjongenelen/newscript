package astre.representation;

import java.io.Serializable;
/**
 * Storing event trigger
 * @author nguyen
 *
 */
public class Trigger implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * 
	 */
	private Word relation;
	/**
	 * 
	 */
	private Word predicate;
	/**
	 * 
	 */
	private Word invRelation;
	/**
	 * 
	 * @param relation		Event trigger
	 * @param predicate		Event predicate
	 */
	public Trigger(Word relation, Word predicate) {
		this.relation = relation;
		this.predicate = predicate;
	}
	/**
	 * 
	 * @return inversed relation (kill:dobj vs kill:nsubj)
	 */
	public Word getInvRelation() {
		return invRelation;
	}
	/**
	 * 
	 * @return predicate
	 */
	public Word getPredicate() {
		return predicate;
	}
	/**
	 * 
	 * @return relation
	 */
	public Word getRelation() {
		return relation;
	}
	/**
	 * 
	 * @param invRelation
	 */
	public void setInvRelation(Word invRelation) {
		this.invRelation = invRelation;
	}	
}
