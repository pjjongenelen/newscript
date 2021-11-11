package astre.preprocessing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import astre.representation.Coreference;
import astre.representation.CoreferenceMention;
import astre.representation.Dependency;
import astre.representation.Document;
import astre.representation.Sentence;
import astre.representation.Token;
import astre.representation.Trigger;
import astre.representation.Triple;
import astre.representation.Word;

/**
 * Building tuples from parsed documents
 * @author nguyen
 *
 */
public class RelationExtractor {
	/**
	 * 
	 */
	private Set<String> NOUN_EVENT = null;
	/**
	 * Term occurrence threshold
	 */
	private int threshold = 5;
	/**
	 * 
	 */
	private boolean multi_attribute = false;
	/**
	 * Term frequency counting
	 */
	private Map<String,Integer> term_frequency = new HashMap<String,Integer>();
	
//	private Set<String> nounPhrases = new HashSet<String>();
	
//	private void getNounPhrases(String path) throws IOException {
//		BufferedReader r = new BufferedReader(new FileReader(path));
//		String l = null;
//		while ((l = r.readLine()) != null) {
//			String[] tokens = l.split("\t");
//			String docId = tokens[0];
//			String headId = tokens[1];
//			nounPhrases.add(docId + " " + headId);
//		}
//	}
	/**
	 * 
	 * @param multi_attribute 	Single or multi-attribute
	 * @param threshold			Term frequency cut-off
	 * @throws IOException
	 */
	public RelationExtractor(boolean multi_attribute, int threshold) throws IOException {
		this.multi_attribute = multi_attribute;
		this.threshold = threshold;
		this.NOUN_EVENT = getWordNetEvents();
//		getNounPhrases(nounPhrases);
	}
	/**
	 * Get WordNet feature set
	 * @return wordnet event
	 * @throws IOException
	 */
	private Set<String> getWordNetEvents() throws IOException {
		Set<String> events = new HashSet<String>();
		Set<String> synsets = new HashSet<String>(Arrays.asList(new String[] {"act", "event"}));
		URL url = RelationExtractor.class.getResource("../resource/wn_noun_top_synsets");
		File dir = new File(url.getFile());
		for(String synset:synsets) {
			String word = null;
			BufferedReader reader = new BufferedReader(
					new FileReader("C:/Users/timjo/PycharmProjects/sourcecode/out/production/sourcecode/astre/resource//wn_noun_top_synsets" + "/" + synset));
					//new FileReader(dir.getAbsolutePath() + "/" + synset));
			while ((word = reader.readLine()) != null) {
				events.add(word);
			}
			reader.close();
		}
		return events;
	}	
	/**
	 * Count a term
	 * @param term
	 */
	private void count(String term) {
		if (term_frequency.containsKey(term)) {
			term_frequency.put(term, term_frequency.get(term) + 1);
		} else {
			term_frequency.put(term, 1);
		}
	}
	/**
	 * Count a document
	 * @param doc document
	 */
	public void countFrequency(Document doc) {
		for(Sentence sentence: doc.getSentences().values()) {
			for(Dependency dependency:sentence.getDependencies()) {
				count(dependency.getGovernor().getLemma());
				count(dependency.getDependent().getLemma());
				count(dependency.getGovernor().getLemma() + ":" + dependency.getType());
				count(dependency.getDependent().getLemma() + ":" + dependency.getType());
			}
		}
	}
	/**
	 * 
	 * @param tokenId
	 * @param sentence
	 * @param sRelations
	 */
	private void addRelations(int tokenId, Sentence sentence, List<String> sRelations) {
		for(Dependency dependency:sentence.getDependencies()) {
			Token dependent = dependency.getDependent();
			if (dependent.getId() == tokenId) {
				String type = dependency.getType();
				Token governor = dependency.getGovernor();
				if ( ( "nsubj".equals(type) || "dobj".equals(type) || type.matches("prep_.+") 
							|| "tmod".equals(type) || type.matches("conj_.+") || "agent".equals(type) )
						&& ( governor.getPos().matches("VB.*")
								|| ( governor.getPos().matches("NNS?") && NOUN_EVENT.contains(governor.getLemma()) ) )
						&& !"be".equals(governor.getLemma())
						&& term_frequency.get(dependent.getLemma()) >= threshold
						&& term_frequency.get(governor.getLemma() + ":" + type) >= threshold
						) {
					sRelations.add(governor.getLemma() + ":" + type);
				}
			}
		}							
	}
	/**
	 * 
	 * @param token
	 * @param sentence
	 * @param sRelations
	 * @return entity triple
	 */
	private Triple generateTriple(Token token, Sentence sentence, List<String> sRelations) {
		Map<Integer,String> sAttributes = new TreeMap<Integer,String>();
		for(Dependency dependency:sentence.getDependencies()) {
			Token governor = dependency.getGovernor();
			if (governor.getId() == token.getId()) {
				String type = dependency.getType();
				Token dependent = dependency.getDependent();
				if ( ( "amod".equals(type) || "nn".equals(type) || "vmod".equals(type))
						&& ( dependent.getPos().matches("JJ") || dependent.getPos().matches("NNS?") || dependent.getPos().matches("VB.*"))
						&& term_frequency.get(governor.getLemma()) >= threshold
						&& term_frequency.get(dependent.getLemma() + ":" + type) >= threshold
						) {
					sAttributes.put(token.getId() - dependent.getId(),dependent.getLemma() + ":" + type);
				}
			}
		}
		
		Word entity = new Word(token.getLemma());
		
		List<Trigger> triggers = new ArrayList<Trigger>();
		for(String sRelation:sRelations) {
			Word relation = new Word(sRelation);
			Word predicate = new Word(sRelation.split(":")[0]);
			Trigger trigger = new Trigger(relation,predicate);
			triggers.add(trigger);
		}
		
		List<Word> attributes = null;
		Set<String> types = new HashSet<String>();
		if (!sAttributes.isEmpty()) {
			attributes = new ArrayList<Word>();
			for(Integer offset:sAttributes.keySet()) {
				String type = sAttributes.get(offset).split(":")[1];
				if (!types.contains(type)) {
					Word attribute = new Word(sAttributes.get(offset));
					types.add(type);
					attributes.add(attribute);
					if (!multi_attribute) break;
				}
			}
		}	
		
		Triple tuple = new Triple(triggers, entity, attributes);
		return tuple;
	}
	
	/**
	 * Extract tuples from document
	 * @param docId	document id
	 * @param doc document
	 * @return entity triples
	 */
	public List<Triple> analyze(String docId, Document doc) {
		List<Triple> tuples = new ArrayList<Triple>();
		Set<String> visit = new HashSet<String>();
		for(Coreference coref: doc.getCoreferences()) {
			int sentenceId = coref.getRepresentative().getSentenceId();
			int headId = coref.getRepresentative().getHeadId();
			Token representative = doc.getSentenceById(sentenceId).getTokenById(headId);
			if (representative.getPos().matches("NN.*")) {
				List<String> sRelations = new ArrayList<String>();
				for(CoreferenceMention mention:coref.getMentions()) {
					visit.add(mention.getSentenceId() + "-" + mention.getHeadId());
					addRelations(mention.getHeadId(), doc.getSentenceById(mention.getSentenceId()), sRelations);			
				}
				if (!sRelations.isEmpty()) {
					Triple tuple = generateTriple(representative, doc.getSentenceById(coref.getRepresentative().getSentenceId()), sRelations);
//					System.err.println(tuple);
					tuples.add(tuple);
				}
			}
		}
		for(Sentence sentence: doc.getSentences().values()) {
			for(Token token:sentence.getTokens().values()) {
				if (!visit.contains(sentence.getId() + "-" + token.getId()) && token.getPos().matches("NN.*")) {
					List<String> sRelations = new ArrayList<String>();
					addRelations(token.getId(), sentence, sRelations);
					if (!sRelations.isEmpty()) {
						Triple tuple = generateTriple(token, sentence, sRelations);
//						System.err.println(tuple);
						tuples.add(tuple);
					}
				}
		}
		}
		return tuples;
	}
	
//	public static void main(String args[]) throws ParserConfigurationException, SAXException, IOException, ClassNotFoundException {
////		String path = "/vol/zola/users/nguyen/ASTRE/timespace/annotation/Crime-and-law-2007/";
////		
////		String nounPhrases = "/vol/zola/users/nguyen/ASTRE/Dataset/np-head-unit";
//		
//		String path = args[0]; // collection of parsed documents
//		String out = args[1];
//		
//		boolean multi_attribute = true;
//		int threshold = 5;
//		
//		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
//		DocumentBuilder db = dbf.newDocumentBuilder();
//		File[] files = new File(path).listFiles();
//		Arrays.sort(files);
//		StanfordAnalyzer analyzer = new StanfordAnalyzer();
//		
////		RelationExtractor generator = new RelationExtractor(multi_attribute, threshold, nounPhrases);
//		
//		RelationExtractor generator = new RelationExtractor(multi_attribute, threshold);
//		
//		Map<String,Document> docs = new HashMap<String,Document>();
//		for(File file:files) {
//			System.out.println(file);
//			org.w3c.dom.Document xmlDoc = db.parse(file);
//			Document doc = analyzer.traverseDocument(xmlDoc, true);
//			generator.countFrequency(doc);
//			docs.put(file.getName(), doc);
//		}
//		
//		// create data
//		Map<String,List<Triple>> documents = new TreeMap<String,List<Triple>>();
//		for(Entry<String,Document> entry:docs.entrySet()) {
//			String docId = entry.getKey();
//			docId = docId.replaceAll("\\.xml", "");
//			List<Triple> tuples = generator.analyze(docId, entry.getValue());
//			documents.put(entry.getKey(),tuples);
//		}
//		
//		FileOutputStream fout = new FileOutputStream("/vol/zola/users/nguyen/ASTRE/timespace/annotation/crime-and-law.doc");
//		ObjectOutputStream oos = new ObjectOutputStream(fout);
//		oos.flush();
//		oos.writeObject(documents);
//		oos.close();
//	}
}