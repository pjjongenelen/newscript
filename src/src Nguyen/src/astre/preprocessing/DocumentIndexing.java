package astre.preprocessing;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import astre.representation.Document;
import astre.representation.Trigger;
import astre.representation.Triple;
import astre.representation.Word;
/**
 * Indexing a collection of documents
 * @author nguyen
 *
 */
public class DocumentIndexing implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * document ids
	 */
	public List<String> docIds = new ArrayList<String>();
	/**
	 * <head,index>
	 */
	public Map<String,Integer> index_head = new HashMap<String,Integer>();
	/**
	 * <trigger,index>
	 */
	public Map<String,Integer> index_relation = new HashMap<String,Integer>();
	/**
	 * <predicate,index>
	 */
	public Map<String,Integer> index_predicate = new HashMap<String,Integer>();
	/**
	 * <attribute,index>
	 */
	public Map<String,Integer> index_attribute = new HashMap<String,Integer>();
	/**
	 * <index,head>
	 */
	public Map<Integer,String> dic_head = new HashMap<Integer,String>();
	/**
	 * <index,trigger>
	 */
	public Map<Integer,String> dic_relation = new HashMap<Integer,String>();
	/**
	 * <index,predicate>
	 */
	public Map<Integer,String> dic_predicate = new HashMap<Integer,String>();
	/**
	 * <index,attribute>
	 */
	public Map<Integer,String> dic_attribute = new HashMap<Integer,String>();
	/**
	 * 
	 */
	private int idx_head = 0;
	/**
	 * 
	 */
	private int idx_relation = 0;
	/**
	 * 
	 */
	private int idx_predicate = 0;
	/**
	 * 
	 */
	private int idx_attribute = 0;
	/**
	 * List of tuples
	 */
	public Triple[][] triples;
	/**
	 * 
	 * @param docIds documentId
	 */
	public void setDocIds(List<String> docIds) {
		this.docIds = docIds;
	}
	/**
	 * 
	 * @param triples Entity triples
	 */
	public void setTriples(Triple[][] triples) {
		this.triples = triples;
	}
	/**
	 * 
	 * @param word
	 * @param index
	 * @param dic
	 * @param idx
	 * @return (new) word index
	 */
	private int update(
			Word word, 
			Map<String,Integer> index, 
			Map<Integer,String> dic, 
			int idx) {
		String text = word.getText();
		if (!index.containsKey(text)) {
			word.setIndex(idx);
			index.put(text, idx);
			dic.put(idx, text);
			return 1;
		} else {
			word.setIndex(index.get(text));
			return 0;
		}
	}
	/**
	 * 
	 * @param documents documents
	 */
	public void updateIndex(Triple[][] documents) {
		for(Triple[] document:documents) {
			for(Triple tuple:document) {
				idx_head += update(tuple.getEntity(), index_head, dic_head, idx_head);
				for(Trigger trigger:tuple.getTriggers()) {
					idx_relation += update(trigger.getRelation(), index_relation, dic_relation,	idx_relation);
					idx_predicate += update(trigger.getPredicate(), index_predicate, dic_predicate, idx_predicate);
				}
				if (tuple.getAttributes() != null) {
					for(Word attribute:tuple.getAttributes()) {
						idx_attribute += update(attribute, index_attribute, dic_attribute,	idx_attribute);
					}
				}
			}
		}
	}
	/**
	 * 
	 * @param triples	Entity triples
	 */
	public void updateInverseDependency(Triple[][] triples) {
		for(Triple[] document:triples) {
			for(Triple tuple:document) {
				for(Trigger trigger:tuple.getTriggers()) {
					String dependency = trigger.getRelation().getText();
					String predicate = dependency.split(":")[0];
					String type = dependency.split(":")[1];
					if ("nsubj".equals(type)||"dobj".equals(type)) {
						String invType = "nsubj".equals(type)?"dobj":"nsubj";
						String invDependency = predicate + ":" + invType;
						if (index_relation.containsKey(invDependency)) {
							Word word = new Word(invDependency);
							word.setIndex(index_relation.get(invDependency));
							trigger.setInvRelation(word);
						}
					}
				}
			}
		}
	}
	
	public static void main(String args[]) throws IOException, ClassNotFoundException, ParserConfigurationException, SAXException {
//		System.err.println("DocumentIndexing documents index");
//		String in = args[0];
//		String out = args[1];
//		
//		FileInputStream fin = new FileInputStream(in);
//		ObjectInputStream ois = new ObjectInputStream(fin);
//		@SuppressWarnings("unchecked")
//		Map<String,List<Tuple>> documents = (Map<String,List<Tuple>>) ois.readObject();
//		ois.close();		
//		List<String> docIds = new ArrayList<String>();
//		List<Tuple[]> _tuples = new ArrayList<Tuple[]>();
//		for(Entry<String,List<Tuple>> entry:documents.entrySet()) {
//			System.out.println(entry.getKey());
//			Tuple[] a = new Tuple[entry.getValue().size()];
//			entry.getValue().toArray(a);
//			_tuples.add(a);
//			docIds.add(entry.getKey());
//		}
//		Tuple[][] tuples = new Tuple[_tuples.size()][];
//		_tuples.toArray(tuples);
//		DocumentIndexing indexer = new DocumentIndexing();
//		
//		indexer.updateIndex(tuples);
//		indexer.updateInverseDependency(tuples);
//		
//		indexer.setDocIds(docIds);
//		indexer.setTuples(tuples);
//		
//		FileOutputStream fout = new FileOutputStream(out);
//		ObjectOutputStream oos = new ObjectOutputStream(fout);
//		oos.flush();
//		oos.writeObject(indexer);
//		oos.close();		
		
		String path_input = args[0];
		String path_index = args[1];
		int cut_off = 5;
		
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		File[] files = new File(path_input).listFiles();
		Arrays.sort(files);
		StanfordAnalyzer analyzer = new StanfordAnalyzer();			
		
		RelationExtractor generator = new RelationExtractor(false, cut_off);
		
		// count term frequency
		
		Map<String,Document> docs = new HashMap<String,Document>();
		for(File file:files) {
			System.err.println(file);
			org.w3c.dom.Document xmlDoc = db.parse(file);
			Document doc = analyzer.traverseDocument(xmlDoc, true);
			generator.countFrequency(doc);
			docs.put(file.getName(), doc);
		}
		
		// create data
		Map<String,List<Triple>> documents = new TreeMap<String,List<Triple>>();
		for(Entry<String,Document> entry:docs.entrySet()) {
			String docId = entry.getKey();
			docId = docId.replaceAll("\\.xml", "");
			List<Triple> tuples = generator.analyze(docId, entry.getValue());
			documents.put(entry.getKey(),tuples);
		}
		
		// index documents
		
		List<String> docIds = new ArrayList<String>();
		List<Triple[]> _tuples = new ArrayList<Triple[]>();
		for(Entry<String,List<Triple>> entry:documents.entrySet()) {
			Triple[] a = new Triple[entry.getValue().size()];
			entry.getValue().toArray(a);
			_tuples.add(a);
			docIds.add(entry.getKey());
		}
		Triple[][] tuples = new Triple[_tuples.size()][];
		_tuples.toArray(tuples);
		DocumentIndexing indexer = new DocumentIndexing();
		
		indexer.updateIndex(tuples);
		indexer.updateInverseDependency(tuples);
		
		indexer.setDocIds(docIds);
		indexer.setTriples(tuples);
		
		// serialize index
		
		FileOutputStream findex = new FileOutputStream(path_index);
		ObjectOutputStream sindex = new ObjectOutputStream(findex);
		sindex.flush();
		sindex.writeObject(indexer);
		sindex.close();		
	}
}