package astre.entityDisambiguation.nest;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
 * Assign topic to document elements
 * @author nguyen
 *
 */
public class TopicAssignment{
	/**
	 * 
	 */
	private static final double EPSILON = Math.pow(10, -100);
	/**
	 * 
	 */
	private NestModel lda;
	/**
	 * 
	 */
	private DocumentIndexing index;
	/**
	 * 
	 * @param lda	Nest model
	 * @param index	Document index
	 */
	public TopicAssignment(NestModel lda, DocumentIndexing index) {
		super();
		this.lda = lda;
		this.index = index;
	}
	/**
	 * 
	 * @param out
	 * @throws JSONException
	 * @throws IOException
	 */
	private void printAssignments(String out) throws JSONException, IOException {
		double[][] phi_attribute = lda.getDistribution("attribute");
		double[][] phi_relation = lda.getDistribution("trigger");
		double[][] phi_head= lda.getDistribution("head");	
		
		List<JSONObject> objs = new ArrayList<JSONObject>();
		for(int m = 0; m < index.triples.length; m++) {
			String message_id = index.docIds.get(m);
			message_id = message_id.replace(".xml", "");
			JSONObject obj = new JSONObject();
			obj.put("message_id", message_id);
			Map<String,List<String>> slots = new HashMap<String,List<String>>();
			for(int n = 0; n < index.triples[m].length; n++) {
				Triple entity = index.triples[m][n];
				
				String slot = null;
				
				double max = EPSILON;
				for(int k = 0; k < lda.getnK_slot(); k++) {
					double prob = 1;
					prob *= phi_head[k][entity.getEntity().getIndex()];
					for(Trigger trigger:entity.getTriggers()) {
						prob *= phi_relation[k][trigger.getRelation().getIndex()];
					}
					if (lda.isUse_attribute() && entity.getAttributes() != null) {
						for(Word attribute:entity.getAttributes()) {
							prob *= phi_attribute[k][attribute.getIndex()];
						}
					}
					prob += EPSILON;
					if (prob > max) {
						max = prob;
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
	 * <br> Usage: TopicAssignment <br>
	 *  -index <arg>   input index file <br>
	 *  -lbl <arg>     output slot labeled file <br>
	 *  -model <arg>   input model file <br>
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
			NestModel lda = (NestModel) stream.readObject();
			stream.close();
			
			FileInputStream fin = new FileInputStream(doc_index);
			ObjectInputStream ois = new ObjectInputStream(fin);
			DocumentIndexing index  = (DocumentIndexing) ois.readObject();
			ois.close();
			
			TopicAssignment filler = new TopicAssignment(lda, index);
			try {
				filler.printAssignments(answer);
			} catch (JSONException e) {
				e.printStackTrace();
			}			
		} else {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("TopicAssignment", options );			
		}
	}
}