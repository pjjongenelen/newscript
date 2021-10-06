package astre.preprocessing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import astre.representation.Coreference;
import astre.representation.CoreferenceMention;
import astre.representation.Dependency;
import astre.representation.Document;
import astre.representation.Sentence;
import astre.representation.Token;

/**
 * Reading Stanford parsed document
 * @author nguyen
 *
 */
public class StanfordAnalyzer {
	/**
	 * Get tokens from parsed sentence
	 * @param sentence
	 * @return tokens
	 */
	private Map<Integer,Token> getTokens(NodeList lTokens) {
		Map<Integer,Token> tokens = new TreeMap<Integer,Token>();
		for(int i = 0; i < lTokens.getLength(); i++) {
			Element eToken = (Element) lTokens.item(i);
			int id = Integer.parseInt(eToken.getAttribute("id"));
			String word = eToken.getElementsByTagName("word").item(0).getTextContent().trim();
			String lemma = eToken.getElementsByTagName("lemma").item(0).getTextContent().trim();
			String pos = null;
			String ner = null;
			if (eToken.getElementsByTagName("POS").getLength() > 0) {
				pos = eToken.getElementsByTagName("POS").item(0).getTextContent().trim();
			} else {
				pos = eToken.getElementsByTagName("pos").item(0).getTextContent().trim();	
			}
			if (eToken.getElementsByTagName("NER").getLength() > 0) {
				ner = eToken.getElementsByTagName("NER").item(0).getTextContent().trim();
			} else {
				ner = eToken.getElementsByTagName("ner").item(0).getTextContent().trim();	
			}

			Token token = new Token(id, word, lemma, pos, ner);
			tokens.put(id, token);
		}
		return tokens;
	}
	/**
	 * Get dependencies from parse sentence
	 * @param lDependencies
	 * @param tokens
	 * @return dependencies
	 */
	private List<Dependency> getDependencies(
			NodeList lDependencies, 
			Map<Integer,Token> tokens,
			boolean normalization) {
		List<Dependency> dependencies = new ArrayList<Dependency>();
		for(int i = 0; i < lDependencies.getLength(); i++) {
			Element eDependency = (Element) lDependencies.item(i);
			String type = eDependency.getAttribute("type");
			if (!"root".equals(type)) {
				if (normalization) {
					if ("nsubjpass".equals(type)) {
						type = "dobj"; 
					} else if ("agent".equals(type)) {
						type = "nsubj";
					} else if ("poss".equals(type)) {
						type = "prep_of";
					} else if ("prep_by".equals(type)) {
						type = "nsubj";
					}
				}
				Element eGovernor = (Element) eDependency.getElementsByTagName("governor").item(0);
				Element eDependent = (Element) eDependency.getElementsByTagName("dependent").item(0);
				int governorId = Integer.parseInt(eGovernor.getAttribute("idx"));
				int dependentId = Integer.parseInt(eDependent.getAttribute("idx"));
				Token governor = tokens.get(governorId);
				Token dependent = tokens.get(dependentId);
				Dependency dependency = new Dependency(governor, dependent, type);
				dependencies.add(dependency);
			}
		}
		return dependencies;
	}
	/**
	 * 
	 * @param lDependencies
	 * @param tokens
	 * @return dependency tree root
	 */
	private Token getRoot(NodeList lDependencies, 
			Map<Integer,Token> tokens) {
		for(int i = 0; i < lDependencies.getLength(); i++) {
			Element eDependency = (Element) lDependencies.item(i);
			String type = eDependency.getAttribute("type");
			if ("root".equals(type)) {
				Element eDependent = (Element) eDependency.getElementsByTagName("dependent").item(0);
				int dependentId = Integer.parseInt(eDependent.getAttribute("idx"));
				Token dependent = tokens.get(dependentId);
				return dependent;
			}
		}
		return null;
	}
	/**
	 * Get mentions of a coreference
	 * @param lMentions
	 * @return coreference mention
	 */
	private CoreferenceMention getCoreferenceMentions(NodeList lMentions, List<CoreferenceMention> mentions) {
		CoreferenceMention representative = null;
		for(int i = 0; i < lMentions.getLength(); i++) {
			Element eMention = (Element) lMentions.item(i);
			int sentenceId = Integer.parseInt(eMention.getElementsByTagName("sentence").item(0).getTextContent());
			int headId = Integer.parseInt(eMention.getElementsByTagName("head").item(0).getTextContent());
			NodeList nodeList = eMention.getElementsByTagName("text");
			String text = nodeList.getLength() == 0?null:nodeList.item(0).getTextContent();
			CoreferenceMention mention = new CoreferenceMention(sentenceId, headId, text);
			mentions.add(mention);
			if ("true".equals(eMention.getAttribute("representative"))) {
				representative = mention;
			}			
		}
		return representative;
	}
	/**
	 * Get coreferences from parsed document
	 * @param lCoreferences
	 * @return coreferences
	 */
	private List<Coreference> getCoreferences(NodeList lCoreferences) {
		List<Coreference> coreferences = new ArrayList<Coreference>();
		for(int i = 0; i < lCoreferences.getLength(); i++) {
			Element eCoreference = (Element) lCoreferences.item(i);
			NodeList lMentions = eCoreference.getElementsByTagName("mention");
			List<CoreferenceMention> mentions = new ArrayList<CoreferenceMention>();
			CoreferenceMention representative = getCoreferenceMentions(lMentions, mentions);
			Coreference coreference = new Coreference(mentions, representative);
			coreferences.add(coreference);
		}
		return coreferences;
		
	}
	/**
	 * Create a document from Stanford parsed xml text
	 * @param xmlDoc
	 * @param normalization typed dependencies normalizing?
	 * @return document
	 */
	public Document traverseDocument(
			org.w3c.dom.Document xmlDoc,
			boolean normalization) {
		Map<Integer,Sentence> sentences = new TreeMap<Integer,Sentence>();
		NodeList lSentences = xmlDoc.getElementsByTagName("sentence");
		for(int i = 0; i < lSentences.getLength(); i++) {
			Element eSentence = (Element) lSentences.item(i);
			NodeList lTokens = eSentence.getElementsByTagName("token");
			Map<Integer,Token> tokens = getTokens(lTokens);
			NodeList _dependencies = eSentence.getElementsByTagName("dependencies");
			Element eDependencies = null;
			for(int j = 0; j < _dependencies.getLength(); j++) {
				eDependencies = (Element) _dependencies.item(j);
				if ("collapsed-ccprocessed-dependencies".equals(eDependencies.getAttribute("type"))) {
					break;
				}
			}
			if (eDependencies != null) {
				NodeList lDependencies = eDependencies.getElementsByTagName("dep");
				List<Dependency> dependencies = getDependencies(lDependencies, tokens, normalization);
				int id = Integer.parseInt(eSentence.getAttribute("id"));
				Sentence sentence = new Sentence(id, tokens, dependencies);
				sentence.setRoot(getRoot(lDependencies, tokens));
				sentences.put(id, sentence);
			}
		}
		List<Coreference> coreferences = new ArrayList<Coreference>();
		if (xmlDoc.getElementsByTagName("coreference").getLength() > 0) {
			NodeList lCoreferences = ((Element) xmlDoc.getElementsByTagName("coreference").item(0)).getElementsByTagName("coreference");
			coreferences = getCoreferences(lCoreferences);
		}
		return new Document(sentences, coreferences);
	}
//	public static void main(String args[]) throws ParserConfigurationException, SAXException, IOException {
//		System.err.println("StanfordAnalyzer parsed_corpus");
//		String parsed_data = args[0];
//		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
//		DocumentBuilder db = dbf.newDocumentBuilder();
//		File subcorpus = new File(parsed_data);
//		StanfordAnalyzer analyzer = new StanfordAnalyzer();
//		int count = 0;
//		for(File file:subcorpus.listFiles()) {
//			System.err.println(file);
//			org.w3c.dom.Document xmlDoc = db.parse(file);
//			analyzer.traverseDocument(xmlDoc, true);
//			if (++count == 10) break;
//		}
//	}
}