package astre.entityDisambiguation;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.json.JSONException;
import org.json.JSONObject;

import astre.preprocessing.DocumentIndexing;
import astre.representation.Trigger;
import astre.representation.Triple;
import astre.representation.Word;

/**
 * Assign topic value for documents
 * @author nguyen
 *
 */
public class TopicAssignment{
	/**
	 * Relevance threshold
	 */
	private static final double RELEVANCE_THRESHOLD = 0.001;
	/**
	 * 
	 */
	private SlotModel lda;
	/**
	 * 
	 */
	private DocumentIndexing index;
	/**
	 * 
	 * @param lda slot model
	 * @param index document index
	 */
	public TopicAssignment(SlotModel lda, DocumentIndexing index) {
		super();
		this.lda = lda;
		this.index = index;
	}
	/**
	 * 
	 * @param documents
	 * @param out
	 * @throws JSONException
	 * @throws IOException
	 */
	private void printAssignments(Triple[][] documents, String out) throws JSONException, IOException {
		double[][] phi_attribute = lda.getDistribution("attribute");
		double[][] phi_relation = lda.getDistribution("trigger");
		double[][] phi_head= lda.getDistribution("head");
		
		List<JSONObject> objs = new ArrayList<JSONObject>();
		for(int m = 0; m < documents.length; m++) {
			String message_id = index.docIds.get(m);
			message_id = message_id.replace(".xml", "");
			JSONObject obj = new JSONObject();
			obj.put("message_id", message_id);
			
			// relevant document filtering
			Map<Integer,Double> relevance = new HashMap<Integer,Double>();
			Set<Integer> ok = new HashSet<Integer>();
			int count = 0;
			for(int n = 0; n < documents[m].length; n++) {
				count += documents[m][n].getTriggers().size();
			}
			for(int k = 0; k < lda.getnK_slot(); k++) {
				for(int n = 0; n < documents[m].length; n++) {
					Triple tuple = documents[m][n];
					for(Trigger trigger:tuple.getTriggers()) {
						double prob = phi_relation[k][trigger.getRelation().getIndex()];
						if (prob >= 0.01) {
							ok.add(k);
						}
						if (relevance.containsKey(k)) {
							relevance.put(k, relevance.get(k) + prob);
						} else {
							relevance.put(k, prob);
						}
					}
				}
			}
			
			// mle slot assignment
			
			Map<String,List<String>> slots = new HashMap<String,List<String>>();
			for(int n = 0; n < documents[m].length; n++) {
				Triple entity = documents[m][n];
				
				String slot = null;
				
				double max = 0;
				for(int k = 0; k < lda.getnK_slot(); k++) {
					double x = phi_head[k][entity.getEntity().getIndex()];
					
					for(Trigger trigger:entity.getTriggers()) {
						x *= phi_relation[k][trigger.getRelation().getIndex()];
					}

					if (lda.isUse_attribute() && entity.getAttributes() != null) {
						for(Word attribute:entity.getAttributes()) {
							x *= phi_attribute[k][attribute.getIndex()];
						}
					}
					x += Math.pow(10, -100);	// TODO in case float precision ~ 0
					if (x > max) {
						max = x;
						slot = String.valueOf(k);
					}
				}
				
				if (slot != null
						&& ok.contains(Integer.parseInt(slot)) 
						&& relevance.get(Integer.parseInt(slot)) / count > RELEVANCE_THRESHOLD						
						) {
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
		}
		
		Writer writer = new FileWriter(out);
		writer.flush();
		JSONObject all = new JSONObject();
		all.put("data", objs);
		writer.write(all.toString());
		writer.close();
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
	 */
	public static void main(String args[]) throws FileNotFoundException, IOException, ClassNotFoundException, ParseException {
		// create Options object
		Options options = new Options();
		// add options
		options.addOption("index", true, "input index file");
		options.addOption("model", true, "input model file");
		options.addOption("lbl", true, "output slot labeled file");
		// parse arguments
		CommandLineParser parser = new BasicParser();
		CommandLine cmd = parser.parse( options, args);
		if (cmd.hasOption("lbl") && cmd.hasOption("index") && cmd.hasOption("model")) {
			String doc_index = cmd.getOptionValue("index");
			String model = cmd.getOptionValue("model");
			String answer = cmd.getOptionValue("lbl");
			
			ObjectInputStream stream = new ObjectInputStream(new FileInputStream(model));
			SlotModel lda = (SlotModel) stream.readObject();
			stream.close();
			
			FileInputStream fin = new FileInputStream(doc_index);
			ObjectInputStream ois = new ObjectInputStream(fin);
			DocumentIndexing index  = (DocumentIndexing) ois.readObject();
			ois.close();
			
			TopicAssignment filler = new TopicAssignment(lda, index);
			try {
				filler.printAssignments(index.triples, answer);
			} catch (JSONException e) {
				e.printStackTrace();
			}			
		} else {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("TopicAssignment", options );			
		}
	}
}
