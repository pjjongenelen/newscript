package astre.entityDisambiguation.wikinews;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.cli.ParseException;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import astre.preprocessing.DocumentIndexing;
import astre.preprocessing.RelationExtractor;
import astre.preprocessing.StanfordAnalyzer;
import astre.representation.Trigger;
import astre.representation.Triple;
import astre.representation.Word;

/**
 * Assign topic value for documents
 * @author nguyen
 *
 */
public class Inference{
	/**
	 * 
	 */
	private astre.entityDisambiguation.wikinews.SlotModel lda;
	/**
	 * 
	 */
	private DocumentIndexing index;
	/**
	 * 
	 * @param lda slot model
	 * @param index document index
	 */
	public Inference(astre.entityDisambiguation.wikinews.SlotModel lda, DocumentIndexing index) {
		super();
		this.lda = lda;
		this.index = index;
	}
	/**
	 * 
	 * @param triples
	 * @param out
	 * @throws JSONException
	 * @throws IOException
	 */
	private void printAssignments(String message_id, List<Triple> triples, List<JSONObject> objs) throws JSONException, IOException {
		double[][] phi_attribute = lda.getDistribution("attribute");
		double[][] phi_relation = lda.getDistribution("trigger");
		double[][] phi_head= lda.getDistribution("head");
		
		JSONObject obj = new JSONObject();
		obj.put("message_id", message_id);
		
		// mle slot assignment
		
		Map<String,List<String>> slots = new HashMap<String,List<String>>();
		for(int n = 0; n < triples.size(); n++) {
			Triple entity = triples.get(n);
			
			String slot = null;
			
			double max = 0;
			for(int k = 0; k < lda.getnK_slot(); k++) {
				double x = 1;
				if (index.index_head.containsKey(entity.getEntity().getText())) {
					x *= phi_head[k][index.index_head.get(entity.getEntity().getText())];
				}
				
				for(Trigger trigger:entity.getTriggers()) {
					if (index.index_relation.containsKey(trigger.getRelation().getText())) {
						x *= phi_relation[k][index.index_relation.get(trigger.getRelation().getText())];
					}
				}

				if (lda.isUse_attribute() && entity.getAttributes() != null) {
					for(Word attribute:entity.getAttributes()) {
						if (index.index_attribute.containsKey(attribute.getText())) {
							x *= phi_attribute[k][index.index_attribute.get(attribute.getText())];
						}
					}
				}
				x += Math.pow(10, -100);	// TODO in case float precision ~ 0
				if (x > max) {
					max = x;
					slot = String.valueOf(k);
				}
			}
			
			if (slot != null) {
				String relations = "";
				for(Trigger trigger:entity.getTriggers()) {
					relations += trigger.getRelation().getText() + " ";
				}
				relations = relations.trim();
				String attributes = "";
				if (entity.getAttributes() != null) {
					for(Word attribute:entity.getAttributes()) {
						attributes += attribute.getText() + " ";
					}
				}
				attributes = attributes.trim();
				String filler = entity.getEntity().getText() + "-" + relations + "/" + attributes;
				if (!slots.containsKey(slot)) {
					List<String> fillers = new ArrayList<String>();
					fillers.add(filler);
					slots.put(slot, fillers);
				} else {
					slots.get(slot).add(filler);
				}
			}
		}
		
		for(Entry<String,List<String>> entry:slots.entrySet()) {
			obj.put(entry.getKey().toString(), entry.getValue());
		}
		
		objs.add(obj);
		
//		Writer writer = new FileWriter(out);
//		writer.flush();
//		JSONObject all = new JSONObject();
//		all.put("data", objs);
//		writer.write(all.toString());
//		writer.close();
	}
	/**
	 * 
	 * @param args
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws ParseException
	 * 
	 * <br> Usage: TopicLabeler <br>
	 *	-index <arg>   input index file <br>
	 *	-lbl <arg>     output slot labeled file <br>
	 *	-model <arg>   input model file <br>
	 * @throws SAXException 
	 * @throws ParserConfigurationException 
	 */
	public static void main(String args[]) throws FileNotFoundException, IOException, ClassNotFoundException, ParseException, SAXException, ParserConfigurationException {
		String path = "/vol/zola/users/nguyen/ASTRE/WikiNews/Annotation/Manual-selection/parse";
		
		String doc_index = "/vol/zola/users/nguyen/ASTRE/WikiNews/WebRetrieval/induction/index";
		
//		String model = "/vol/zola/users/nguyen/ASTRE/WikiNews/WebRetrieval/induction/model_default";
//		String answer = "/vol/zola/users/nguyen/ASTRE/WikiNews/WebRetrieval/induction/assignment";
		
		String model = "/vol/zola/users/nguyen/ASTRE/WikiNews/WebRetrieval/induction/model_100";
		String answer = "/vol/zola/users/nguyen/ASTRE/WikiNews/WebRetrieval/induction/assignment_100";		
		
		ObjectInputStream stream = new ObjectInputStream(new FileInputStream(model));
		astre.entityDisambiguation.wikinews.SlotModel lda = (astre.entityDisambiguation.wikinews.SlotModel) stream.readObject();
		stream.close();
		
		FileInputStream fin = new FileInputStream(doc_index);
		ObjectInputStream ois = new ObjectInputStream(fin);
		DocumentIndexing index  = (DocumentIndexing) ois.readObject();
		ois.close();
		
		Inference filler = new Inference(lda, index);
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			StanfordAnalyzer analyzer = new StanfordAnalyzer();
			RelationExtractor extractor = new RelationExtractor(false, 0);
			File files[] = new File(path).listFiles();
			List<JSONObject> objs = new ArrayList<JSONObject>();
			Arrays.sort(files);
			for(int id = 0; id < files.length; id++) {
				Document xmlDoc = db.parse(files[id]);
				astre.representation.Document doc = analyzer.traverseDocument(xmlDoc, true);
				List<Triple> triples = extractor.analyze(String.valueOf(id), doc);
				filler.printAssignments(String.valueOf(id), triples, objs);
			}
			
			Writer writer = new FileWriter(answer);
			writer.flush();
			JSONObject all = new JSONObject();
			all.put("data", objs);
			writer.write(all.toString());
			writer.close();			
		} catch (JSONException e) {
			e.printStackTrace();
		}			
	}
}
