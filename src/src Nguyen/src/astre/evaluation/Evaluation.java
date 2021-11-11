package astre.evaluation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.json.JSONException;
import org.json.JSONObject;
/**
 * MUC34 slot filling evaluation
 * Test dataset: tst3, tst4
 * @author nguyen
 *
 */
public class Evaluation {
	/**
	 * Get slot filler answers
	 * @param path Slot fillers file path
	 * @return	<slot,<entity_mentions>>
	 * @throws IOException
	 */
	private Map<String,Set<String>> getSlotFillers(String path) throws IOException {
		Map<String,Set<String>> answer = new TreeMap<String,Set<String>>();
		BufferedReader r = new BufferedReader(new FileReader(path));
		String instance = null;
		while ((instance = r.readLine()) != null) {
			String slot = instance.split(" ")[0];
			Set<String> mentions = new TreeSet<String>();
			for(String mention:instance.split(" ")[1].split(",")) {
				mentions.add(mention);
			}
			answer.put(slot, mentions);
		}
		return answer;
	}
	/**
	 * Document classification evaluation
	 * @param answer
	 * @param reference
	 */
//	private void document_classification(Map<String,Set<String>> answer, Map<String,Set<Set<String>>> reference) {
//		double total = 0;	// total number of templates
//		double correct= 0;	// correct number of templates
//		double predict= 0;	// predict number of templates
//		
//		Map<String,Set<String>> system = new HashMap<String,Set<String>>();
//		Map<String,Set<String>> relevance = new HashMap<String,Set<String>>();
//		
//		for(String entry:reference.keySet()) {
//			String messageId = entry.split(":")[0];
//			String template = entry.split(":")[1].split("-")[0];
//			if (relevance.containsKey(messageId)) {
//				relevance.get(messageId).add(template);
//			} else {
//				Set<String> set = new HashSet<String>();
//				set.add(template);
//				relevance.put(messageId, set);
//			}
//		}
//		
//		for(String entry:answer.keySet()) {
//			String messageId = entry.split(":")[0];
//			String template = entry.split(":")[1].split("-")[0];
//			if (system.containsKey(messageId)) {
//				system.get(messageId).add(template);
//			} else {
//				Set<String> set = new HashSet<String>();
//				set.add(template);
//				system.put(messageId, set);
//			}
//		}
//		
//		// correct
//		
//		for(String messageId:system.keySet()) {
//			predict += system.get(messageId).size();
//			if (relevance.containsKey(messageId)) {
//				for(String template:system.get(messageId)) {
//					if (relevance.get(messageId).contains(template)) correct++;
//				}
//			}
//		}
//		
//		// total
//		
//		for(String messageId:relevance.keySet()) {
//			total += relevance.get(messageId).size();
//		}
//		
//		double precision = 100 * correct / predict;
//		double recall = 100 * correct / total;
//		double fscore = 2 * precision * recall / (precision + recall);
//		System.out.println("Document classification: ");
//		System.out.println("\tTotal\t: " + total);
//		System.out.println("\tPredict\t: " + predict);
//		System.out.println("\tCorrect\t: " + correct);
//		System.out.format("\tP\t: %.2f%n", precision);
//		System.out.format("\tR\t: %.2f%n", recall);
//		System.out.format("\tF\t: %.2f%n", fscore);
//	}
	/**
	 * Main entry
	 * @param args
	 * @throws FileNotFoundException
	 * @throws JSONException
	 * @throws IOException
	 * @throws ParseException 
	 * <br> Usage: Evaluation <br>
	 *  -filler <arg>   system slot fillers <br>
	 *  -verbose        more information <br>
	 */
	public static void main(String args[]) throws FileNotFoundException, JSONException, IOException, ParseException {
		// create Options object
		Options options = new Options();
		// add options
		options.addOption("filler", true, "system slot fillers");
		options.addOption("verbose", false, "more information");
		// parse arguments
		CommandLineParser parser = new BasicParser();
		CommandLine cmd = parser.parse( options, args);
		if (cmd.hasOption("filler")) {
			boolean template_mapping = false;
			boolean relevant = false;
			boolean isSlot = false;
			boolean debug = cmd.hasOption("verbose");
			String path = cmd.getOptionValue("filler");
			
			Evaluation evaluator = new Evaluation();
			
			URL urlEventType = evaluator.getClass().getResource("../resource/muc34-tst3-tst4-template-type");
			URL urlHead = evaluator.getClass().getResource("../resource/muc34-tst3-tst4-phrase-head");
			URL urlRef = evaluator.getClass().getResource("../resource/key_tst3_tst4");
			File fileEventType = new File(urlEventType.getFile());
			File fileHead = new File(urlHead.getFile());
			File fileRef = new File(urlRef.getFile());
			
			EvaluationUtils metric = new EvaluationUtils(
					fileEventType, 
					fileHead,
					debug);		// no debug output
			
			JSONObject goldenKey = metric.getReferenceObject(fileRef);

			Map<String,Set<Set<String>>> precisionReferences = metric.getReferences(goldenKey, false);

			Map<String,Set<Set<String>>> recallReferences = metric.getReferences(goldenKey, true);
			
			Map<String,Set<String>> answer = evaluator.getSlotFillers(path);
			
//			evaluator.document_classification(answer, precisionReferences);
			
			System.out.println("Template slot filling:");
			double performance[] = metric.evaluate(answer, precisionReferences, recallReferences, template_mapping, relevant, isSlot);
//			System.out.format("%s %.2f %.2f %.2f%n", new File(path).getName(), performance[4], performance[5], performance[6]);
			System.out.format("\tPredict mentions: %.0f%n", performance[1]);
			System.out.format("\tCorrect mentions: %.0f%n", performance[2]);
			System.out.format("\tTotal entities: %.0f%n", performance[0]);
			System.out.format("\tCorrect entities: %.0f%n", performance[3]);			
			System.out.format("\tPrecision: %.2f%n", performance[4]);
			System.out.format("\tRecall: %.2f%n", performance[5]);
			System.out.format("\tF-score: %.2f%n", performance[6]);
		} else {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("Evaluation", options );
		}		
	}
}

 
