package astre.representation;

import java.io.Serializable;
import java.util.List;
/**
 * Storing event triple
 * @author nguyen
 *
 */
public class Triple implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * 
	 */
	private Word entity;
	/**
	 * 
	 */
	private List<Word> attributes;
	/**
	 * 
	 */
	private List<Trigger> triggers;
	/**
	 * 
	 * @return entity
	 */
	public Word getEntity() {
		return entity;
	}
	/**
	 * 
	 * @return attributes
	 */
	public List<Word> getAttributes() {
		return attributes;
	}
	/**
	 * 
	 * @return triggers
	 */
	public List<Trigger> getTriggers() {
		return triggers;
	}
	/**
	 * 
	 * @param triggers		event triggers
	 * @param entity		entity head word
	 * @param attributes	entity attributes
	 */
	public Triple(List<Trigger> triggers, Word entity, List<Word> attributes) {
		super();
		this.triggers = triggers;
		this.entity = entity;
		this.attributes = attributes;
	}
	/**
	 * 
	 */
	public String toString() {
		String relations = "";
		for(Trigger trigger:triggers) {
			relations += trigger.getRelation().getText() + " ";
		}
		relations = relations.trim();
		String sAttributes = "";
		if (attributes != null) {
			for(Word attribute:attributes) {
				sAttributes += attribute.getText() + " ";
			}
		}
		sAttributes = sAttributes.trim();
		String tuple = sAttributes + " " + entity.getText().toUpperCase() + " " + relations;
		
		return tuple;
	}
}
