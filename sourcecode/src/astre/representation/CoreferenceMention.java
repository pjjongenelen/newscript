package astre.representation;
/**
 * Storing an entity mention (in co-reference chain)
 * @author nguyen
 *
 */
public class CoreferenceMention {
	/**
	 * 
	 */
	private int sentenceId;
	/**
	 * 
	 */
	private int headId;
	/**
	 * 
	 */
	private String text;
	/**
	 * 
	 * @return sentenceId
	 */
	public int getSentenceId() {
		return sentenceId;
	}
	/**
	 * 
	 * @param sentenceId
	 */
	public void setSentenceId(int sentenceId) {
		this.sentenceId = sentenceId;
	}
	/**
	 * 
	 * @return entity mention head word id
	 */
	public int getHeadId() {
		return headId;
	}
	/**
	 * 
	 * @param headId
	 */
	public void setHeadId(int headId) {
		this.headId = headId;
	}
	/**
	 * 
	 * @return mention text
	 */
	public String getText() {
		return text;
	}
	/**
	 * 
	 * @param text mention text
	 */
	public void setText(String text) {
		this.text = text;
	}
	/**
	 * 
	 * @param sentenceId
	 * @param headId
	 * @param text
	 */
	public CoreferenceMention(int sentenceId, int headId, String text) {
		super();
		this.sentenceId = sentenceId;
		this.headId = headId;
		this.text = text;
	}
	
}
