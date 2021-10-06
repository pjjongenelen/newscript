package astre.evaluation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
/**
 * Slot mapping: map each reference slot to its best performed learned slot
 * using the development set as described in 
 * 'Generative event schema induction with entity disambiguation'
 * @author nguyen
 *
 */
public class SlotMapping {
	/**
	 * Slot mapping on development set
	 * @param metric
	 * @param answerSlots
	 * @param precisionReferences
	 * @param recallReferences
	 * @param nTypes
	 * @param nSlots
	 * @param template_mapping
	 * @return <Reference_slot,learned_slot>
	 * @throws IOException
	 * @throws JSONException
	 */
	private Map<String,String> slotMapping(
			EvaluationUtils metric,
			Map<String,Set<String>> answerSlots, 
			Map<String,Set<Set<String>>> precisionReferences, 
			Map<String,Set<Set<String>>> recallReferences, 
			int nTypes, int nSlots,
			boolean template_mapping,
			boolean relevant,
			boolean isSlot) throws IOException, JSONException {
		Map<String,String> mapping = new HashMap<String,String>();
		Set<String> slots = new TreeSet<String>();
		
		slots.add("KIDNAPPING-hum_tgt");
		slots.add("KIDNAPPING-perp_id");

/*
		references.add("KIDNAPPING-incident_instrument_id");
		references.add("KIDNAPPING-phys_tgt_id"); 
*/
		
		slots.add("BOMBING-hum_tgt");
		slots.add("BOMBING-incident_instrument_id");
		slots.add("BOMBING-phys_tgt_id");
		slots.add("BOMBING-perp_id");
		
		slots.add("ATTACK-hum_tgt");
		slots.add("ATTACK-phys_tgt_id");
		slots.add("ATTACK-perp_id");
		
		slots.add("ATTACK-incident_instrument_id");
		
		slots.add("ARSON-hum_tgt");

/*
		slots.add("ARSON-incident_instrument_id"); 
*/
		
		slots.add("ARSON-phys_tgt_id");
		slots.add("ARSON-perp_id");

		int optimize = 6;	// f-score
		
		System.err.println("Selecting best fit slot mapping...");
		
		for(String slot:slots) {
			System.err.println(slot + ":");
			double max = -1;
			String best = null;
			for(int topic = 0; topic < nSlots * nTypes; topic++) {
				
				Map<String,String> _mapping = new HashMap<String,String>();
				_mapping.put(slot, String.valueOf(topic));
				
				Set<String> _slots = new HashSet<String>();
				for(String s:slots) {
					if (s.split("-")[1].equals(slot.split("-")[1])) {
						_slots.add(s);
					}
				}
				
				Map<String,Set<String>> _answer = metric.extractAnswersFromMapping(answerSlots, _mapping, "DEV-MUC3");
				Map<String,Set<Set<String>>> _precisionReferences = metric.extractSubsetFromReferences(precisionReferences, _slots, "DEV-MUC3");
				Map<String,Set<Set<String>>> _recallReferences = metric.extractSubsetFromReferences(recallReferences, _mapping.keySet(), "DEV-MUC3");
				
				double score[] = metric.evaluate(_answer, _precisionReferences, _recallReferences, template_mapping, relevant, isSlot);
				
				
				if (score[optimize] > max) {
					System.err.format(" %d M/M+/E/E+/P/R/F: %d/%d/%d/%d/%.2f/%.2f/%.2f%n", topic, (int) score[1], (int) score[2], (int) score[0], (int) score[3], score[4], score[5], score[6]);
					max = score[optimize];
					best = String.valueOf(topic);
				}
			}
			if (best != null) {
				mapping.put(slot, best);
			}
		}
		
		return mapping;
	}
	
	/**
	 * Print out answer for test set
	 * @param mapping
	 * @param answer
	 * @param metric
	 * @throws IOException 
	 */
	private void printAnswer(String filler, Map<String,String> mapping, Map<String,Set<String>> answer, EvaluationUtils metric) throws IOException {
		Writer w = new FileWriter(new File(filler));
		w.flush();
		
		Map<String,Set<String>> mapped_answers = metric.extractAnswersFromMapping(answer, mapping, "TST[34]-MUC4");
		
		for(Entry<String,Set<String>> entry:mapped_answers.entrySet()) {
			w.write(entry.getKey() + " " + StringUtils.join(entry.getValue(), ",") + "\n");
		}
		w.close();
	}
	/**
	 * 
	 * @param args
	 * @throws FileNotFoundException
	 * @throws JSONException
	 * @throws IOException
	 * @throws ParseException
	 * 
	 * <br> Usage: SlotMapping <br>
	 *  -assignments <arg>   slot labeled documents <br>
	 *  -eventTypes <arg>    number of event types <br>
	 *  -filler <arg>        output filer file <br>
	 *  -slots <arg>         number of slots <br>
	 */
	public static void main(String args[]) throws FileNotFoundException, JSONException, IOException, ParseException {
		// create Options object
		Options options = new Options();
		// add options
		options.addOption("assignments", true, "slot labeled documents");
		options.addOption("eventTypes", true, "number of event types");
		options.addOption("slots", true, "number of slots");
		options.addOption("filler", true, "output filer file");
		// parse arguments
		CommandLineParser parser = new BasicParser();
		CommandLine cmd = parser.parse( options, args);
		if (cmd.hasOption("assignments") && cmd.hasOption("eventTypes") && cmd.hasOption("slots") && cmd.hasOption("filler")) {
			boolean template_mapping = false;
			boolean relevant = false;
			boolean isSlot = false;
			
			String path = cmd.getOptionValue("assignments");
			int nTypes = Integer.parseInt(cmd.getOptionValue("eventTypes"));
			int nSlots = Integer.parseInt(cmd.getOptionValue("slots"));
			String filler = cmd.getOptionValue("filler");
			
			SlotMapping evaluator = new SlotMapping();
			
			URL urlEventType = evaluator.getClass().getResource("../resource/muc-template-type");
			URL urlHead = evaluator.getClass().getResource("../resource/phrase-head");
			URL urlRef = evaluator.getClass().getResource("../resource/json");
			File fileEventType = new File(urlEventType.getFile());
			File fileHead = new File(urlHead.getFile());
			File fileRef = new File(urlRef.getFile());
			
			EvaluationUtils metric = new EvaluationUtils(fileEventType, fileHead, false);

			JSONObject objReferences = metric.getReferenceObject(fileRef);
			JSONObject objAnswers = metric.getSystem(path);
			
			Map<String,Set<Set<String>>> precisionReferences = metric.getReferences(objReferences, false);
			Map<String,Set<Set<String>>> recallReferences = metric.getReferences(objReferences, true);
			Map<String,Set<String>> answers = metric.getAnswerSlots(objAnswers);
			Map<String,String> slotMapping = evaluator.slotMapping(
					metric, answers, precisionReferences, recallReferences, nTypes, nSlots, template_mapping, relevant, isSlot);
			evaluator.printAnswer(filler, slotMapping, answers, metric);			
		} else {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("SlotMapping", options );
		}		
	}
}

 