package astre.evaluation;

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
import java.util.TreeSet;

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
class EvaluationUtils {
	/**
	 * Mapping between template Id and type 
	 */
	private final Map<String,String> templateTypes = new HashMap<String,String>();
	/**
	 * Head words of phrase using right most heuristic
	 */
	private final Map<String,String> phraseHeads = new HashMap<String,String>();
	/**
	 * Set of event considered bombing, attack, kidnapping, arson
	 */
	private final Set<String> EVENT = new HashSet<String>();
	
	private boolean debug = false;
	/**
	 * Constructor
	 * @param templateType
	 * @param phraseHead
	 * @throws IOException
	 */
	public EvaluationUtils(File templateType, File phraseHead, boolean debug) throws IOException {
		getTemplateType(templateType);
		getPhraseHeads(phraseHead);
		EVENT.add("BOMBING");
		EVENT.add("ATTACK");
		EVENT.add("KIDNAPPING");
		EVENT.add("ARSON");
		this.debug = debug;
	}
	/**
	 * Get template id type mapping
	 * @param in
	 * @throws IOException
	 */
	private void getTemplateType(File in) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(in));
		String line = null;
		while ((line = reader.readLine()) != null) {
			String messageId = line.split(" ")[0];
			String templateId = line.split(" ")[1];
			String templateType = line.split(" ")[2];
			templateTypes.put(messageId + ":" + templateId, templateType);
		}
		reader.close();
	}
	/**
	 * Get phrase head words
	 * @param in
	 * @throws IOException
	 */
	private void getPhraseHeads(File in) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(in));
		String pair = null;
		while ((pair = reader.readLine()) != null) {
			String tokens[] = pair.split("\t");
			String phrase = tokens[0];
			String head = tokens[1];
			phraseHeads.put(phrase, head);
		}
		reader.close();
	}
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
	 * Get JSON reference object with text clean up first
	 * @param in
	 * @return
	 * @throws IOException
	 * @throws JSONException
	 */
	protected JSONObject getReferenceObject(File in) throws IOException, JSONException {
		BufferedReader reader = new BufferedReader(new FileReader(in));
		StringBuilder str =  new StringBuilder();
		str.append("{\"data\":\n[\n");
		String line = null;
		while ((line = reader.readLine()) != null) {
			line = line.replaceAll("%%%", "");
			line = line.replaceAll("^(\\s+\\[\"\\w+\"),", "$1:");
			line = line.replaceAll("^\\[", "{");
			line = line.replaceAll("^\\]", "}");
			line = line.replaceAll("^(\\s+)\\[", "$1{");
			line = line.replaceAll("\\],$", "},");
			line = line.replaceAll("\\]$", "}");			
			line = line.replaceAll("^\\{", "[");
			line = line.replaceAll("^\\}", "],");
			
			line = line.replace("\"perp_individual_id\"", "\"perp_id\"");
			line = line.replace("\"perp_organization_id\"", "\"perp_id\"");
			
			line = line.replace("\"hum_tgt_name\"", "\"hum_tgt\"");
			
			line = line.replace("\"hum_tgt_description\"", "\"hum_tgt\"");

			if (!line.isEmpty()) {
				str.append(line + "\n");
			}
		}
		reader.close();
		str.append("]\n}\n");
		return new JSONObject(str.toString());
	}
	/**
	 * Extract heads of slot from JSON object
	 * @param obj
	 * @param tag
	 * @param heads
	 * @throws JSONException
	 */
	private void addHeads(JSONObject obj, String tag, Set<String> heads) throws JSONException {
		JSONArray phrases = obj.getJSONArray(tag);
		for(int k = 0; k < phrases.length(); k++) {
			String phrase = phrases.get(k).toString();		
			if (phrase != null && !"null".equals(phrase)) {
				String head = phraseHead(phrase);
				head = normalizeTerm(head);
				heads.add(head);
			}
		}
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
	 */
	@SuppressWarnings("unchecked")
	protected Map<String,Set<Set<String>>> getReferences(JSONObject objReferences, boolean recall) throws JSONException {
		Map<String,Set<Set<String>>> map = new TreeMap<String,Set<Set<String>>>();
		JSONArray data = (JSONArray) objReferences.get("data");
		for(int i = 0; i < data.length(); i++) {
			JSONArray template = data.getJSONArray(i);
			String message_id = null;
			String template_id = null;
			String template_type = null;			
			boolean optional = false;
		    for(int j = 0; j < template.length(); j++) {
		    	JSONObject slot = template.getJSONObject(j);
		    	if (slot.has("message_template_optional")) {
		    		optional = true;
		    		break;
		    	}
		    }
		    
		    // Ignore optional template when calculating recall
		    if (optional && recall) continue;
		    
		    for(int j = 0; j < template.length(); j++) {
		    	JSONObject slot = template.getJSONObject(j);
		    	for(Iterator<String> iter = slot.keys(); iter.hasNext();) {
		    		String tag = (String) iter.next();
		    		if ("message_id".equals(tag)) {
		    			message_id = slot.getString(tag);
		    		} else if ("message_template_optional".equals(tag)) {
		    			// ignore because of null tag
		    		} else if ("message_template".equals(tag)) {
		    			template_id = slot.get(tag).toString();
		    			template_type = templateTypes.get(message_id + ":" + template_id);
		    		} else if (!slot.isNull(tag)){
		    			JSONObject obj = slot.getJSONObject(tag);
		    			String slot_id = message_id + ":" + template_type + "-" + tag;
		    			
		    			// Ignore optional slots when calculating recall
		    			if (obj.has("optional") && recall) continue;
		    			
	    				Set<String> heads = new TreeSet<String>();
		    			String type = obj.getString("type");
		    			if ("simple_strings".equals(type)) {
		    				addHeads(obj, "strings", heads);
		    			} else if ("colon_clause".equals(type)) {
		    				addHeads(obj, "strings_lhs", heads);
		    				addHeads(obj, "strings_rhs", heads);
		    			}
	    				if (map.containsKey(slot_id)) {
	    					map.get(slot_id).add(heads);
	    				} else {
	    					Set<Set<String>> set = new HashSet<Set<String>>();
	    					set.add(heads);
	    					map.put(slot_id, set);
	    				}
		    		}
		    	}
		    }
		}
		return map;
	}				
	/**
	 * Create slot fillers using model topic and slot mapping
	 * @param answer
	 * @param mapping
	 * @param prefix
	 * @return
	 */
	protected Map<String,Set<String>> extractAnswersFromMapping(Map<String,Set<String>> answer, Map<String,String> mapping, String prefix) {
		Map<String, Set<String>> mapped_answer = new TreeMap<String, Set<String>>();
		for(Entry<String,String> mapping_entry:mapping.entrySet()) {
			String slot = mapping_entry.getKey();
			String topic = mapping_entry.getValue();
			for(Entry<String,Set<String>> answer_entry:answer.entrySet()) {
				String messageId = answer_entry.getKey().split(":")[0];
				String _topic = answer_entry.getKey().split(":")[1];
				if (topic.equals(_topic) && messageId.matches(prefix + "-0*\\d+")) {
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
	protected Map<String,Set<Set<String>>> extractSubsetFromReferences(Map<String,Set<Set<String>>> references, Set<String> slots, String prefix) {
		Map<String, Set<Set<String>>> subReferences = new HashMap<String, Set<Set<String>>>();
		for(Entry<String,Set<Set<String>>> entry:references.entrySet()) {
			String messageId = entry.getKey().split(":")[0];
			String slot = entry.getKey().split(":")[1];
			if (slots.contains(slot) && messageId.matches(prefix + "-0*\\d+")) {
				subReferences.put(entry.getKey(), entry.getValue());
			}
		}
		return subReferences;
	}
	/**
	 * Extract answer by role
	 * @param answer
	 * @param role
	 * @return
	 */
	protected Map<String,Set<String>> extractAnswersByRoles(Map<String,Set<String>> answer, String role) {
		Map<String,Set<String>> sub_answer = new HashMap<String,Set<String>>();
		for(String entry:answer.keySet()) {
			String _role = entry.split(":")[1].split("-")[1];
			if (role.equals(_role)) {
				sub_answer.put(entry, answer.get(entry));
			}
		}
		return sub_answer;
	}
	/**
	 * Extract reference by role
	 * @param reference
	 * @param role
	 * @return
	 */
	protected Map<String,Set<Set<String>>> extractReferencesByRoles(Map<String,Set<Set<String>>> reference, String role) {
		Map<String,Set<Set<String>>> sub_reference = new HashMap<String,Set<Set<String>>>();
		for(String entry:reference.keySet()) {
			String _role = entry.split(":")[1].split("-")[1];
			if (role.equals(_role)) {
				sub_reference.put(entry, reference.get(entry));
			}
		}
		return sub_reference;
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
			Map<String,Set<Set<String>>> recallReferences,
			boolean template_mapping,
			boolean relevant,
			boolean isSlot){
		
		Map<String,Set<String>> relevant_documents = new HashMap<String,Set<String>>();
		if (relevant) {
			for(String entry:precisionReferences.keySet()) {
				String messageId = entry.split(":")[0];
				String template = entry.split(":")[1].split("-")[0];
				if (relevant_documents.containsKey(template)) {
					relevant_documents.get(template).add(messageId);
				} else {
					Set<String> set = new HashSet<String>();
					set.add(messageId);
					relevant_documents.put(template, set);
				}
			}
		}
		
		double total_slot = 0;			// recall
		double correct_slot = 0;		// recall
		
		double predict_mention = 0;		// precision
		double correct_mention = 0;		// precision
		
		
		// precision
		for(String answer_entry:answer.keySet()) {
			String messageId = answer_entry.split(":")[0];
			String template = answer_entry.split(":")[1].split("-")[0];
			
			if (relevant && !relevant_documents.get(template).contains(messageId)) continue;

			predict_mention += answer.get(answer_entry).size();
			
			String slot = answer_entry.split(":")[1].split("-")[1];
			
			Set<String> events = template_mapping?new HashSet<String>():EVENT;
			events.add(template);
			
			for(String head:answer.get(answer_entry)) {
				boolean ok = false;
				for(String event: events) {
					ok = false;
					String reference_entry = messageId + ":" + event + "-" + slot;
					if (precisionReferences.containsKey(reference_entry)) {
						for(Set<String> mentions:precisionReferences.get(reference_entry)) {
							if (mentions.contains(head)) {
								correct_mention++; ok = true; break;
							}
						}
					}
					if (ok) break;
				}
//				if (debug) {
//					String sign = ok?"(+)":"(-)";
//					System.out.println(sign + " " + head + " " + answer_entry);
//				}
			}
		}
		
		
		if (!isSlot) {
		
			// recall by entities
			
//			int aaa = 0;
			
			for(String reference_entry:recallReferences.keySet()) {
				total_slot += recallReferences.get(reference_entry).size();
				String messageId = reference_entry.split(":")[0];
				String slot = reference_entry.split(":")[1].split("-")[1];
				String template = reference_entry.split(":")[1].split("-")[0];
				for(Set<String> mentions:recallReferences.get(reference_entry)) {
//					aaa += mentions.size();
					boolean ok = false;
					for(String mention:mentions) {
						Set<String> events = template_mapping?new HashSet<String>():EVENT;
						events.add(template);
						for(String event: events) {
							ok = false;
							String answer_entry = messageId + ":" + event + "-" + slot;
							if (answer.containsKey(answer_entry)) {
								if (answer.get(answer_entry).contains(mention)) {
									ok = true; correct_slot++; break;
								}
							}
						}
						if (ok) break;
					}
					if (debug) {
						String sign = ok?"(+)":"(-)";
						System.out.println(sign + " " + mentions + " " + reference_entry);						
					}
				}
			}
			
//			System.out.println(aaa);
			
		} else {
		
			// recall by slots
			
			for(String reference_entry:recallReferences.keySet()) {
				total_slot += 1;
				String messageId = reference_entry.split(":")[0];
				String slot = reference_entry.split(":")[1].split("-")[1];
				String template = reference_entry.split(":")[1].split("-")[0];
				int cnt = 0;
				for(Set<String> mentions:recallReferences.get(reference_entry)) {
					for(String mention:mentions) {
						boolean ok = false;
						Set<String> events = template_mapping?new HashSet<String>():EVENT;
						events.add(template);
						for(String event: events) {
							ok = false;
							String answer_entry = messageId + ":" + event + "-" + slot;
							if (answer.containsKey(answer_entry)) {
								if (answer.get(answer_entry).contains(mention)) {
									ok = true; cnt++; break;
								}
							}
						}
						if (ok) break;
					}
				}
				if (cnt == recallReferences.get(reference_entry).size()) correct_slot++;
			}
			
		}
		
		double performance[] = new double[7];
		performance[0] = total_slot;
		performance[1] = predict_mention;
		performance[2] = correct_mention;
		performance[3] = correct_slot;
		performance[4] = 100 * correct_mention / predict_mention;
		performance[5] = 100 * correct_slot / total_slot;
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