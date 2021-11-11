package astre.representation;

import java.util.List;
import java.util.Map;

/**
 * Storing a document
 * @author nguyen
 *
 */
public class Document {
	/**
	 * 
	 */
	private Map<Integer,Sentence> sentences;
	/**
	 * 
	 */
	private List<Coreference> coreferences;
	/**
	 * 
	 * @return sentences
	 */
	public Map<Integer, Sentence> getSentences() {
		return sentences;
	}
	/**
	 * 
	 * @param sentences
	 */
	public void setSentences(Map<Integer, Sentence> sentences) {
		this.sentences = sentences;
	}
	/**
	 * 
	 * @return entity co-reference chains
	 */
	public List<Coreference> getCoreferences() {
		return coreferences;
	}
	/**
	 * 
	 * @param coreferences
	 */
	public void setCoreferences(List<Coreference> coreferences) {
		this.coreferences = coreferences;
	}
	/**
	 * 
	 * @param sentences
	 * @param coreferences
	 */
	public Document(Map<Integer, Sentence> sentences,
			List<Coreference> coreferences) {
		super();
		this.sentences = sentences;
		this.coreferences = coreferences;
	}
	/**
	 * 
	 * @param sentenceId
	 * @return sentence
	 */
	public Sentence getSentenceById(int sentenceId) {
		return sentences.get(sentenceId);
	}
}