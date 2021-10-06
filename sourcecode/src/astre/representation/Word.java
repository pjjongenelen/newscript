package astre.representation;

import java.io.Serializable;
/**
 * Smallest unit for sampling
 * could be use for storing heads, triggers, predicates, and attributes...
 * @author nguyen
 *
 */
public class Word implements Comparable<Word>, Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * 
	 */
	private String text;
	/**
	 * 
	 */
	private int index;
	/**
	 * 
	 * @return text
	 */
	public String getText() {
		return text;
	}
	/**
	 * 
	 * @param text
	 */
	public void setText(String text) {
		this.text = text;
	}
	/**
	 * 
	 * @return index
	 */
	public int getIndex() {
		return index;
	}
	/**
	 * 
	 * @param index
	 */
	public void setIndex(int index) {
		this.index = index;
	}
	/**
	 * 
	 * @param text
	 */
	public Word(String text) {
		super();
		this.text = text;
		this.index = -1;
	}
	@Override
	public int compareTo(Word o) {
		return this.text.compareTo(o.getText());
	}
}