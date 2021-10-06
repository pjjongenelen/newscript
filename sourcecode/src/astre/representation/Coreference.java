package astre.representation;

import java.util.List;

/**
 * Storing entity co-reference chain 
 * @author nguyen
 *
 */
public class Coreference {
	/**
	 * 
	 */
	private List<CoreferenceMention> mentions;
	/**
	 * 
	 */
	private CoreferenceMention representative;
	/**
	 * 
	 * @return list of mentions
	 */
	public List<CoreferenceMention> getMentions() {
		return mentions;
	}
	/**
	 * 
	 * @param mentions
	 */
	public void setMentions(List<CoreferenceMention> mentions) {
		this.mentions = mentions;
	}
	/**
	 * 
	 * @return representative mention
	 */
	public CoreferenceMention getRepresentative() {
		return representative;
	}
	/**
	 * 
	 * @param representative
	 */
	public void setRepresentaive(CoreferenceMention representative) {
		this.representative = representative;
	}
	/**
	 * 
	 * @param mentions
	 * @param representative
	 */
	public Coreference(List<CoreferenceMention> mentions, CoreferenceMention representative) {
		super();
		this.mentions = mentions;
		this.representative = representative;
	}
	
}