package astre.entityDisambiguation.wikinews;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.cli.ParseException;
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
			Utils metric,
			Map<String,Set<String>> answerSlots, 
			Map<String,Set<Set<String>>> precisionReferences, 
			Map<String,Set<Set<String>>> recallReferences, 
			int nTypes, int nSlots) throws IOException, JSONException {
		
		Map<String,String> mapping = new HashMap<String,String>();
		Set<String> slots = new TreeSet<String>();
		
//		slots.add("ATTACK.Time");
//		slots.add("ATTACK.Place");
		slots.add("SUE.Defendant");
		slots.add("CONVICT.Defendant");
		slots.add("SUE.Plaintiff");
		slots.add("ARREST-JAIL.Person");
		slots.add("ATTACK.Attacker");
		slots.add("TRIAL-HEARING.Defendant");
		slots.add("CHARGE-INDICT.Crime");
		slots.add("ATTACK.Instrument");
		slots.add("CHARGE-INDICT.Defendant");
//		slots.add("ARREST-JAIL.Place");
		slots.add("SENTENCE.Sentence");
		slots.add("CONVICT.Crime");
		slots.add("ARREST-JAIL.Crime");
		slots.add("RELEASE-PAROLE.Person");
//		slots.add("ARREST-JAIL.Time");
		slots.add("ARREST-JAIL.Agent");
		slots.add("SENTENCE.Crime");
		slots.add("DIE.Victim");
		slots.add("SENTENCE.Defendant");
		slots.add("INJURE.Victim");
		slots.add("ATTACK.Target");

		
//		slots.add("DIE.Victim");
//		slots.add("DIE.Agent");
//		slots.add("DIE.Instrument");
//		
//		slots.add("INJURY.Victim");
//		slots.add("INJURY.Agent");
//		slots.add("INJURY.Instrument");
//		
//		slots.add("ATTACK.Attacker");
//		slots.add("ATTACK.Target");
//		slots.add("ATTACK.Instrument");
//		
//		slots.add("SUE.Defendant");
//		slots.add("SUE.Plaintiff");
//		slots.add("SUE.Adjudicator");
//		slots.add("SUE.Crime");
//		
//		slots.add("CHARGE-INDICT.Defendant");
//		slots.add("CHARGE-INDICT.Prosecutor");
//		slots.add("CHARGE-INDICT.Adjudicator");
//		slots.add("CHARGE-INDICT.Crime");
//		
//		slots.add("ARREST-JAIL.Person");
//		slots.add("ARREST-JAIL.Agent");
//		slots.add("ARREST-JAIL.Crime");
//		
//		slots.add("CONVICT.Defendant");
//		slots.add("CONVICT.Prosecutor");
//		slots.add("CONVICT.Adjudicator");
//		slots.add("CONVICT.Crime");
//		
//		slots.add("SENTENCE.Defendant");
//		slots.add("SENTENCE.Sentence");
//		slots.add("SENTENCE.Adjudicator");
//		slots.add("SENTENCE.Crime");
//		
//		slots.add("RELEASE-PAROLE.Person");
//		slots.add("RELEASE-PAROLE.Agent");
//		slots.add("RELEASE-PAROLE.Crime");		
//		
//		slots.add("EXTRADITE.Person");
//		slots.add("EXTRADITE.Agent");
//		slots.add("EXTRADITE.Origin");
//		slots.add("EXTRADITE.Destination");
//		
//		slots.add("FINE.Target");
//		slots.add("FINE.Adjudicator");
//		slots.add("FINE.Money");
//		slots.add("FINE.Crime");
//		
//		slots.add("EXECUTE.Person");
//		slots.add("EXECUTE.Agent");
//		slots.add("EXECUTE.Crime");
//		
//		slots.add("ACQUIT.Defendant");
//		slots.add("ACQUIT.Adjudicator");
//		slots.add("ACQUIT.Crime");
//		
//		slots.add("PARDON.Defendant");
//		slots.add("PARDON.Adjudicator");
//		slots.add("PARDON.Crime");
//		
//		slots.add("APPEAL.Defendant");
//		slots.add("APPEAL.Adjudicator");
//		slots.add("APPEAL.Crime");		
		
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
				_slots.add(slot);
				
				Map<String,Set<String>> _answer = metric.extractAnswersFromMapping(
						answerSlots, _mapping);
				Map<String,Set<Set<String>>> _precisionReferences = metric.extractSubsetFromReferences(
						precisionReferences, _slots);
				Map<String,Set<Set<String>>> _recallReferences = metric.extractSubsetFromReferences(
						recallReferences, _mapping.keySet());
				
				double score[] = metric.evaluate(_answer, _precisionReferences, _recallReferences);
				
				
				if (score[optimize] > max) {
					System.err.format(" %d M/M+ E/E+ P/R/F: %d/%d %d/%d %.2f/%.2f/%.2f%n", topic, (int) score[1], (int) score[2], (int) score[0], (int) score[3], score[4], score[5], score[6]);
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
//	
//	/**
//	 * Print out answer for test set
//	 * @param mapping
//	 * @param answer
//	 * @param metric
//	 * @throws IOException 
//	 */
//	private void printAnswer(String filler, Map<String,String> mapping, Map<String,Set<String>> answer, Utils metric) throws IOException {
//		Writer w = new FileWriter(new File(filler));
//		w.flush();
//		
//		Map<String,Set<String>> mapped_answers = metric.extractAnswersFromMapping(answer, mapping);
//		
//		for(Entry<String,Set<String>> entry:mapped_answers.entrySet()) {
//			w.write(entry.getKey() + " " + StringUtils.join(entry.getValue(), ",") + "\n");
//		}
//		w.close();
//	}
	/**
	 * 
	 * @param args
	 * @throws FileNotFoundException
	 * @throws JSONException
	 * @throws IOException
	 * @throws ParseException
	 * 
	 */
	public static void main(String args[]) throws FileNotFoundException, JSONException, IOException, ParseException {
		SlotMapping mapping = new SlotMapping();
		Utils metric = new Utils();
		
//		String path_assignment = "/vol/zola/users/nguyen/ASTRE/WikiNews/WebRetrieval/induction/assignment";
		String path_assignment = "/vol/zola/users/nguyen/ASTRE/WikiNews/WebRetrieval/induction/assignment_100";
		
		String path_reference = "/vol/zola/users/nguyen/ASTRE/WikiNews/WebRetrieval/induction/references";
		int nTypes = 1;
		
//		int nSlots = 35;
		int nSlots = 100;
		
		JSONObject objAnswers = metric.getSystem(path_assignment);
		
		Map<String,Set<Set<String>>> references = metric.getReferences(path_reference);
		
		Map<String,Set<String>> answers = metric.getAnswerSlots(objAnswers);
		Map<String,String> slotMapping = mapping.slotMapping(
				metric, answers, references, references, nTypes, nSlots);
		
		Map<String,Set<String>> _answer = metric.extractAnswersFromMapping(
				answers, slotMapping);
		
		double score[] = metric.evaluate(_answer, references, references);
		System.err.format("Overall: M/M+ E/E+ P/R/F: %d/%d %d/%d %.2f/%.2f/%.2f%n", (int) score[1], (int) score[2], (int) score[0], (int) score[3], score[4], score[5], score[6]);
		
//		mapping.printAnswer(filler, slotMapping, answers, metric);
	}
}

 