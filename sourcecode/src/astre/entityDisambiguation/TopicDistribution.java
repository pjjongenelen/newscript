package astre.entityDisambiguation;


import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Arrays;
import java.util.Map;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import astre.preprocessing.DocumentIndexing;

/**
 * Display distributions of learned model
 * @author nguyen
 *
 */
public class TopicDistribution {
	/**
	 * 
	 * @author nguyen
	 *
	 */
	private class MyObject implements Comparable<MyObject> {
		/**
		 * 
		 */
		private int id;
		/**
		 * 
		 */
		private double prob;
		/**
		 * 
		 * @return
		 */
		public int getId() {
			return id;
		}
		/**
		 * 
		 * @return
		 */
		public double getProb() {
			return prob;
		}
		/**
		 * 
		 * @param id
		 * @param prob
		 */
		public MyObject(int id, double prob) {
			this.id = id;
			this.prob = prob;
		}
		@Override
		public int compareTo(MyObject o) {
			return -Double.compare(prob, o.getProb()) ;
		}
	}
	/**
	 * 
	 * @param phi_k
	 * @param dic
	 * @param threshold
	 * @return Is topic probability above threshold?
	 * @throws IOException
	 */
	private boolean printTopWords(double[] phi_k, Map<Integer,String> dic, double threshold) throws IOException {
		int nWords = Math.min(20, phi_k.length);
		MyObject[] words = new MyObject[phi_k.length];
		for(int i = 0; i < phi_k.length; i++) {
			MyObject word = new MyObject(i, phi_k[i]);
			words[i] = word;
		}
		Arrays.sort(words);
		if (words[0].getProb() > threshold) {
			for(int i = 0; i < nWords; i++) {
				System.out.println(
						"\t\t\t" 
						+ dic.get(words[i].getId()) 
						+ " " + words[i].getProb());
			}
			return true;
		}
		return false;
	}
	/**
	 * 
	 * @param lda
	 * @param index
	 * @param threshold
	 * @throws IOException
	 */
	private void getTopWords(SlotModel lda, DocumentIndexing index, double threshold) throws IOException {
		double[][] phi_attribute = lda.getDistribution("attribute");
		double[][] phi_relation = lda.getDistribution("trigger");
		double[][] phi_head= lda.getDistribution("head");		
		for(int k = 0; k < lda.getnK_slot(); k++) {
			System.out.println("Topic " + k);
			System.out.println("\tEntities:");
			printTopWords(phi_head[k], index.dic_head, threshold);			
			System.out.println("\tRelations:");
			printTopWords(phi_relation[k], index.dic_relation, threshold);
			System.out.println("\tAttributes:");
			printTopWords(phi_attribute[k], index.dic_attribute, threshold);			
		}
	}
	/**
	 * 
	 * @param args
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws ParseException
	 * 
	 * <br> Usage: TopicDistribution <br>
	 *	-index <arg>   input index file <br>
	 *	-model <arg>   input model file <br>
	 */ 
	public static void main(String[] args) throws IOException, ClassNotFoundException, ParseException {
		// create Options object
		Options options = new Options();
		// add options
		options.addOption("index", true, "input index file");
		options.addOption("model", true, "input model file");
		// parse arguments
		CommandLineParser parser = new BasicParser();
		CommandLine cmd = parser.parse( options, args);
		if (cmd.hasOption("index") && cmd.hasOption("model")) {
			String doc_index = cmd.getOptionValue("index");
			String model = cmd.getOptionValue("model");
			
			FileInputStream fin = new FileInputStream(model);
			ObjectInputStream ois = new ObjectInputStream(fin);
			SlotModel lda = (SlotModel) ois.readObject();
			ois.close();
			
			fin = new FileInputStream(doc_index);
			ois = new ObjectInputStream(fin);
			DocumentIndexing index  = (DocumentIndexing) ois.readObject();
			ois.close();
			
			TopicDistribution printer = new TopicDistribution();
			printer.getTopWords(lda, index, 0.001);
		} else {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("TopicDistribution", options );			
		}		
	}
}
