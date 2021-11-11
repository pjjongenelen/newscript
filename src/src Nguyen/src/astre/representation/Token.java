package astre.representation;
/**
 * Storing token
 * @author nguyen
 *
 */
public class Token implements Comparable<Token>{
	/**
	 * 
	 */
	private int id;
	/**
	 * 
	 */
	private String word;
	/**
	 * 
	 */
	private String lemma;
	/**
	 * 
	 */
	private String pos;
	/**
	 * 
	 */
	private String ner;
	/**
	 * Constructor
	 * @param id		token id
	 * @param word		word
	 * @param lemma		lemma
	 * @param pos		part of speech
	 * @param ner		named entity type
	 */
	public Token(int id, String word, String lemma, String pos, String ner) {
		super();
		this.id = id;
		this.word = word;
		this.lemma = lemma;
		this.pos = pos;
		this.ner = ner;
	}
	/**
	 * 
	 * @return word
	 */
	public String getWord() {
		return word;
	}
	/**
	 * 
	 * @param word
	 */
	public void setWord(String word) {
		this.word = word;
	}
	/**
	 * 
	 * @return lemma
	 */
	public String getLemma() {
		return lemma;
	}
	/**
	 * 
	 * @param lemma
	 */
	public void setLemma(String lemma) {
		this.lemma = lemma;
	}
	/**
	 * 
	 * @return POS
	 */
	public String getPos() {
		return pos;
	}
	/**
	 * 
	 * @param pos
	 */
	public void setPos(String pos) {
		this.pos = pos;
	}
	/**
	 * 
	 * @return NE type
	 */
	public String getNer() {
		return ner;
	}
	/**
	 * 
	 * @param ner
	 */
	public void setNer(String ner) {
		this.ner = ner;
	}
	/**
	 * 
	 * @return token id
	 */
	public int getId() {
		return id;
	}
	/**
	 * 
	 * @param id
	 */
	public void setId(int id) {
		this.id = id;
	}
	/**
	 * Override Object.equals() function
	 * @param token
	 * @return Is equal to token?
	 */
	public boolean equals(Token token) {
		return lemma.equals(token.getLemma()) && pos.equals(token.getPos());
	}
	/**
	 * Override Object.toString() function
	 */
	public String toString() {
		return lemma + "/" + pos + "/" + ner;
	}
	@Override
	public int compareTo(Token token) {
		return toString().compareTo(token.toString());
	}
}