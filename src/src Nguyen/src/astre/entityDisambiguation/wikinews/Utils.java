package astre.entityDisambiguation.wikinews;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.stanford.nlp.process.Morphology;
/**
 * Evaluation utils
 * @author nguyen
 *
 */
class Utils {
	/**
	 * Token normalization
	 * @param term
	 * @return
	 */
	private String normalizeTerm(String term) {
		term = term.toLowerCase();
		term = Morphology.stemStatic(term, "noun").word();
		return term;
	}
	/**
	 * Get answer JSON object
	 * @param in
	 * @return
	 * @throws FileNotFoundException
	 * @throws JSONException
	 * @throws IOException
	 */
	protected JSONObject getSystem(String in) throws FileNotFoundException, JSONException, IOException {
		return new JSONObject(IOUtils.toString(new FileInputStream(in)));
	}
	/**
	 * Parse answer from JSON object
	 * @param answer
	 * @return
	 * @throws JSONException
	 */
	@SuppressWarnings("unchecked")
	protected Map<String,Set<String>> getAnswerSlots(JSONObject answer) throws JSONException {
		Map<String,Set<String>> map = new TreeMap<String,Set<String>>();
		JSONArray data = (JSONArray) answer.get("data");
		for(int i = 0; i < data.length(); i++) {
			JSONObject document = (JSONObject) data.get(i);
			String message_id = document.getString("message_id");
			for(Iterator<String> iter = document.keys(); iter.hasNext();) {
			    String slotId = iter.next();
			    if (!"message_id".equals(slotId)) {
			    	JSONArray a = document.getJSONArray(slotId);
			    	Set<String> heads = new HashSet<String>();
			    	for(int j = 0; j < a.length(); j++) {
			    		String head = a.getString(j).split("-")[0];
			    		head = normalizeTerm(head);
			    		heads.add(head);
			    	}
			    	String slot = message_id + ":" + slotId;
			    	if (!map.containsKey(slot)) {
			    		map.put(slot, heads);
			    	} else {
			    		map.get(slot).addAll(heads);
			    	}
			    }
			}
		}
		return map;
	}	
	/**
	 * 
	 * @param phrase
	 * @return
	 */
	private String phraseHead(String phrase) {
		phrase = phrase.replaceAll("\"", "");
		phrase = phrase.replaceAll("\\([^\\)]+\\)", "");
		phrase = phrase.replaceAll("\\[[^\\]]+\\]", "");
		phrase = phrase.trim();
		phrase = phrase.replaceAll("\\s+", " ");
		String[] tokens = phrase.split(",? ");
		int pos = 0;
		for(; pos < tokens.length; pos++) {
			if ("OF".equals(tokens[pos])) break;
		}
		pos--;
		if (pos >= 0) return tokens[pos];
		else return null; 
	}
	
	/**
	 * Parse references from JSON object
	 * @param objReferences
	 * @param recall
	 * @return
	 * @throws JSONException
	 * @throws IOException 
	 */
	protected Map<String,Set<Set<String>>> getReferences(String file) throws JSONException, IOException {
		Map<String,Set<Set<String>>> map = new HashMap<String,Set<Set<String>>>();
		BufferedReader r = new BufferedReader(new FileReader(new File(file)));
		String entry = null;
		while ((entry = r.readLine()) != null) {
			String tokens[] = entry.split("\t");
			String key = tokens[0];
			Set<String> mention_heads = new HashSet<String>();
			for(int i = 1; i < tokens.length; i++) {
				String mention = tokens[i];
				mention = mention.replaceAll("\"", "");
				String head = phraseHead(mention);
				mention_heads.add(head);
			}
			Set<Set<String>> entities = new HashSet<Set<String>>();
			entities.add(mention_heads);
			map.put(key, entities);
		}
		r.close();
		return map;
	}				
	/**
	 * Create slot fillers using model topic and slot mapping
	 * @param answer
	 * @param mapping
	 * @param prefix
	 * @return
	 */
	protected Map<String,Set<String>> extractAnswersFromMapping(Map<String,Set<String>> answer, Map<String,String> mapping) {
		Map<String, Set<String>> mapped_answer = new TreeMap<String, Set<String>>();
		for(Entry<String,String> mapping_entry:mapping.entrySet()) {
			String slot = mapping_entry.getKey();
			String topic = mapping_entry.getValue();
			for(Entry<String,Set<String>> answer_entry:answer.entrySet()) {
				String messageId = answer_entry.getKey().split(":")[0];
				String _topic = answer_entry.getKey().split(":")[1];
				if (topic.equals(_topic)) {
					mapped_answer.put(messageId + ":" + slot, answer_entry.getValue());
				}
			}			
		}
		
		return mapped_answer;
	}
	/**
	 * Extract subset of reference containing slots and subdataset
	 * @param references
	 * @param slots
	 * @param prefix
	 * @return
	 */
	protected Map<String,Set<Set<String>>> extractSubsetFromReferences(Map<String,Set<Set<String>>> references, Set<String> slots) {
		Map<String, Set<Set<String>>> subReferences = new HashMap<String, Set<Set<String>>>();
		for(Entry<String,Set<Set<String>>> entry:references.entrySet()) {
			String slot = entry.getKey().split(":")[1];
			if (slots.contains(slot)) {
				subReferences.put(entry.getKey(), entry.getValue());
			}
		}
		return subReferences;
	}	
	/**
	 * Evaluate answer using reference
	 * @param answer
	 * @param precisionReferences
	 * @param recallReferences
	 * @param template_mapping
	 * @return
	 */
	protected double[] evaluate(
			Map<String,Set<String>> answer, 
			Map<String,Set<Set<String>>> precisionReferences, 
			Map<String,Set<Set<String>>> recallReferences){
		
		double total_entity = 0;		// entity recall
		double correct_entity = 0;		
		
		double predict_mention = 0;		// mention precision
		double correct_mention = 0;
		
		
		// mention precision
		for(String answer_entry:answer.keySet()) {
			predict_mention += answer.get(answer_entry).size();
			for(String head:answer.get(answer_entry)) {
				if (precisionReferences.containsKey(answer_entry)) {
					for(Set<String> mentions:precisionReferences.get(answer_entry)) {
						if (mentions.contains(head)) {
							correct_mention++; break;
						}
					}
				}
			}
		}
		
		// entity recall
		
		for(String reference_entry:recallReferences.keySet()) {
			total_entity += recallReferences.get(reference_entry).size();
			for(Set<String> mentions:recallReferences.get(reference_entry)) {
				for(String mention:mentions) {
					if (answer.containsKey(reference_entry)
							&& answer.get(reference_entry).contains(mention)) {
						correct_entity++; break;
					}
				}
			}
		}
		
		double performance[] = new double[7];
		performance[0] = total_entity;
		performance[1] = predict_mention;
		performance[2] = correct_mention;
		performance[3] = correct_entity;
		performance[4] = 100 * correct_mention / predict_mention;
		performance[5] = 100 * correct_entity / total_entity;
		if (performance[4] > 0 && performance[5] > 0) {
			performance[6] = 2 * performance[4] * performance[5] / (performance[4] + performance[5]);
		}
		
		return performance;
	}
//	public static void main(String args[]) throws IOException, JSONException {
//		Metric metric = new Metric(
//				"/vol/zola/users/nguyen/ASTRE/Dataset/muc-template-type", 
//				"/vol/zola/users/nguyen/ASTRE/Dataset/phrase-head",
//				false);		// no debug output
//		
//		JSONObject objReferences = metric.getReferenceObject(
//				"/vol/zola/users/nguyen/ASTRE/Dataset/json");
//		
//		metric.getReferences(objReferences, true);		
//	}
}