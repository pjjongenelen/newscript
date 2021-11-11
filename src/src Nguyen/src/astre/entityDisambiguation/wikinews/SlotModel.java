package astre.entityDisambiguation.wikinews;


import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Random;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.xml.sax.SAXException;

import astre.preprocessing.DocumentIndexing;
import astre.representation.Trigger;
import astre.representation.Triple;
import astre.representation.Word;
/**
 * Slot model in <br>
 *	'Generative event schema induction with entity disambiguation' <br>
 * Code is based on <br>
 * 	http://www.arbylon.net/projects/LdaGibbsSampler.java <br>
 * @author nguyen
 *
 */
public class SlotModel implements Serializable{
	private static final double EPSILON = Math.pow(10, -100);
	private Random random;
	private static final long serialVersionUID = 1L;
	private boolean use_attribute = true;
	private Triple[][] documents;
	private int nV_head;
	private int nV_relation;
	private int nV_attribute;
	private int nK_slot;
	private double beta_head;
	private double beta_relation;
	private double beta_attribute;
	private int s[][];
	private int ss[][][];
	private int[][] nw_head;
	private int[][] nw_relation;
	private int[][] nw_attribute;
	private int[] nwsum_head;
	private int[] nwsum_relation;
	private int[] nwsum_attribute;
	private double[][] phisum_head;
	private double[][] phisum_relation;
	private double[][] phisum_attribute;
	
	private int numstats;
    private static int THIN_INTERVAL = 100;
    private static int BURN_IN = 2000;
    private static int ITERATIONS = 10000;
    private static int SAMPLE_LAG = 10;
    private static int dispcol = 0;
    private int iterate;
    
    /**
     * 
     * @return used attributes for induction
     */
    public boolean isUse_attribute() {
		return use_attribute;
	}
    /**
     * 
     * @return Number of slots
     */
	public int getnK_slot() {
		return nK_slot;
	}
	/**
     * Constructor
     * @param documents 	list of documents, each document contains a list of tuple
     * @param nV_head Head 	vocabulary size
     * @param nV_relation 	trigger vocabulary size
     * @param nV_attribute 	attribute vocabulary size
     * @param seed			random seed
     * @param use_attribute	using attribute for sampling
     */
    public SlotModel(
    		Triple[][] documents, 
    		int nV_head, int nV_relation, int nV_attribute,
    		int seed,
    		boolean use_attribute) {
    	this.documents = documents;
        this.nV_head = nV_head;
        this.nV_relation = nV_relation;
        this.nV_attribute = nV_attribute;
        random = new Random(seed);
        this.use_attribute = use_attribute;
    }
    /**
     *	Initialization 
     * @param nK_slot	number of slots
     */
    public void initialState(int nK_slot) {
        int M = documents.length;

        nw_head = new int[nV_head][nK_slot];
        nw_relation = new int[nV_relation][nK_slot];
        nw_attribute = new int[nV_attribute][nK_slot];
        nwsum_head = new int[nK_slot];
        nwsum_relation = new int[nK_slot];
        nwsum_attribute = new int[nK_slot];
        
        s = new int[M][];
        ss = new int[M][][];
        for (int m = 0; m < M; m++) {
            int N = documents[m].length;
            s[m] = new int[N];
            ss[m] = new int[N][nK_slot];
            for (int n = 0; n < N; n++) {
                int slot = (int) (random.nextDouble() * nK_slot);
                updateSlot(m, n, slot, +1);
            	s[m][n] = slot;
            }
        }
    }
    /**
     * 
     * @param m
     * @param n
     * @param slot
     * @param sign
     */
    private void updateSlot(int m, int n, int slot, int sign) {
        // slot-relation
    	for(Trigger trigger:documents[m][n].getTriggers()) {
	        int idx_relation = trigger.getRelation().getIndex();
	        nw_relation[idx_relation][slot] += sign;
	        nwsum_relation[slot] += sign;    		
    	}
        // slot-head
        int idx_head = documents[m][n].getEntity().getIndex();
        nw_head[idx_head][slot] += sign;
        nwsum_head[slot] += sign;
        // slot-attribute
        List<Word> attributes = documents[m][n].getAttributes();
        if (attributes != null) {
        	for(Word attribute:attributes) {
	        	int idx_attribute = attribute.getIndex();
	        	nw_attribute[idx_attribute][slot] += sign;
	        	nwsum_attribute[slot] += sign;
        	}
        }
	}
    /**
     * Gibbs sampling
     * @param nK_slot			number of slots
     * @param beta_head			head-slot Dirichlet
     * @param beta_relation		head-trigger Dirichlet
     * @param beta_attribute	head-attribute Dirichlet
     */
    public void gibbs(
    		int nK_slot, 
    		double beta_head, 
    		double beta_relation,
    		double beta_attribute) {
        this.nK_slot = nK_slot;
        this.beta_head = beta_head;
        this.beta_relation = beta_relation;
        this.beta_attribute = beta_attribute;

        // init sampler statistics
        if (SAMPLE_LAG > 0) {
            phisum_head = new double[nK_slot][nV_head];
            phisum_relation = new double[nK_slot][nV_relation];
            phisum_attribute = new double[nK_slot][nV_attribute];
            numstats = 0;
        }

        // initial state of the Markov chain:
        
        initialState(nK_slot);

        System.err.println("Sampling " + ITERATIONS
            + " iterations with burn-in of " + BURN_IN + " (B/S="
            + THIN_INTERVAL + ").");
        
        for (iterate = 0; iterate < ITERATIONS; iterate++) {
        	
            for (int m = 0; m < documents.length; m++) {
                for (int n = 0; n < documents[m].length; n++) {
                    int slot = sampleFullConditional(m, n);
                    s[m][n] = slot;
                    if ((iterate > BURN_IN) && (SAMPLE_LAG > 0) && (iterate % SAMPLE_LAG == 0)) ss[m][n][slot]++;
                }
            }
            
            if ((iterate < BURN_IN) && (iterate % THIN_INTERVAL == 0)) {
                System.err.print("B");
                dispcol++;
            }
            // display progress
            if ((iterate > BURN_IN) && (iterate % THIN_INTERVAL == 0)) {
                System.err.print("S");
                dispcol++;
            }
            // get statistics after burn-in
            if ((iterate > BURN_IN) && (SAMPLE_LAG > 0) && (iterate % SAMPLE_LAG == 0)) {
                updateParams();
                System.err.print("|");
                if (iterate % THIN_INTERVAL != 0)
                    dispcol++;
            }
            if (dispcol >= 100) {
                System.err.println();
                dispcol = 0;
            }
        }
    }
    /**
     * 
     * @param m
     * @param n
     * @return Slot sampling value
     */
    private int sampleFullConditional(int m, int n) {
    	int idx_head = documents[m][n].getEntity().getIndex();
    	
        int slot = s[m][n];
        updateSlot(m, n, slot, -1);
        
        double[] p = new double[nK_slot];
        for (int k = 0; k < nK_slot; k++) {
        	// head-slot
        	p[k] = (nw_head[idx_head][k] + beta_head) / (nwsum_head[k] + nV_head * beta_head);
        	
            // relation-slot
        	for(Trigger trigger:documents[m][n].getTriggers()) {
        		int idx_relation = trigger.getRelation().getIndex();
        		p[k] *= (nw_relation[idx_relation][k] + beta_relation) / (nwsum_relation[k] + nV_relation * beta_relation);
        	}
           	// attribute-slot
           	List<Word> attributes = documents[m][n].getAttributes();
           	if (iterate > SlotModel.BURN_IN && use_attribute && attributes != null) {
           		for(Word attribute:attributes) {
           			int idx_attribute = attribute.getIndex();
           			p[k] *= (nw_attribute[idx_attribute][k] + beta_attribute) / (nwsum_attribute[k] + nV_attribute * beta_attribute);
           		}
           	}
           	// TODO avoid java floating value ~ 0
           	p[k] += EPSILON;
        }
        for (int k = 1; k < p.length; k++) {
            p[k] += p[k - 1];
        }
        double u = random.nextDouble() * p[p.length - 1];
        for (slot = 0; slot < p.length; slot++) {
            if (u < p[slot])
                break;
        }
        updateSlot(m, n, slot, +1);
        
        return slot;
	}
    /**
     * 
     */
    private void updateParams() {
        for (int k = 0; k < nK_slot; k++) {
            for (int w = 0; w < nV_head; w++) { 
        		phisum_head[k][w] += (nw_head[w][k] + beta_head)
        				/ (nwsum_head[k] + nV_head * beta_head);
            }
        }
    	for (int k = 0; k < nK_slot; k++) {
            for (int w = 0; w < nV_relation; w++) { 
        		phisum_relation[k][w] += (nw_relation[w][k] + beta_relation) 
        				/ (nwsum_relation[k] + nV_relation * beta_relation);
            }
    	}
    	for (int k = 0; k < nK_slot; k++) {
            for (int w = 0; w < nV_attribute; w++) { 
        		phisum_attribute[k][w] += (nw_attribute[w][k] + beta_attribute) 
        				/ (nwsum_attribute[k] + nV_attribute * beta_attribute);
            }
    	}    	
        numstats++;
    }
    /**
     * 
     * @param K Number of slots
     * @param V Vocabulary size
     * @param phisum
     * @param nw
     * @param nwsum
     * @param beta
     * @return Slot distribution
     */
    private double[][] getPhi(
    		int K,
    		int V, 
    		double[][] phisum, 
    		int[][] nw, 
    		int[] nwsum, 
    		double beta) {
        double[][] phi = new double[K][V];
        if (SAMPLE_LAG > 0) {
            for (int k = 0; k < K; k++) {
                for (int w = 0; w < V; w++) {
                    phi[k][w] = phisum[k][w] / numstats;
                }
            }
        } else {
            for (int k = 0; k < K; k++) {
                for (int w = 0; w < V; w++) {
                	phi[k][w] = (nw[w][k] + beta) / (nwsum[k] + V * beta);
                }
            }
        }
        return phi;
    }
    /**
     * 
     * @param distribution [head, trigger, attribute]
     * @return Slot distribution
     */
    public double[][] getDistribution(String distribution) {
    	if ("head".equals(distribution)) {
    		return getPhi(nK_slot, nV_head, phisum_head, nw_head, nwsum_head, beta_head);
    	} else if ("trigger".equals(distribution)) {
    		return getPhi(nK_slot, nV_relation, phisum_relation, nw_relation, nwsum_relation, beta_relation);
    	} else if ("attribute".equals(distribution)) {
    		return getPhi(nK_slot, nV_attribute, phisum_attribute, nw_attribute, nwsum_attribute, beta_attribute);
    	}
    	return null;
    }
    /**
     * Parameter configuration
     * @param iterations	Number of iterations
     * @param burnIn		Number of burn-in iterations
     * @param thinInterval	Burn-in interval
     * @param sampleLag		Sample lag
     */
    public void configure(int iterations, int burnIn, int thinInterval,
        int sampleLag) {
        ITERATIONS = iterations;
        BURN_IN = burnIn;
        THIN_INTERVAL = thinInterval;
        SAMPLE_LAG = sampleLag;
    }
    /**
     * 
     * @param args
     * @throws ClassNotFoundException
     * @throws IOException
     * @throws ParseException 
     * @throws ParserConfigurationException 
     * @throws SAXException
     * 
     * <br> Usage: SlotModel <br>
 	 *	-att            (optional) using attributes (default false) <br>
 	 *	-beta_a <arg>   (optional) att-slot dirichlet (default .1) <br>
 	 *	-beta_h <arg>   (optional) head-slot dirichlet (default 1) <br>
 	 *	-beta_t <arg>   (optional) trigger-slot dirichlet (default .1) <br>
 	 *	-burn <arg>     (optional) burn-in iterations (default 2000) <br>
 	 *	-cut <arg>      (optional) cut-off term frequency (default 5) <br>
 	 *	-doc <arg>      Stanford parsed documents directory <br>
 	 *	-index <arg>    output index file <br>
 	 *	-iter <arg>     (optional) number of iterations (default 10000) <br>
 	 *	-lag <arg>      (optional) sample lag (default 10) <br>
 	 *	-model <arg>    output model file <br>
 	 *	-rand <arg>     (optional) random seed (default 1) <br>
 	 *	-slot <arg>     (optional) number of slots (default 35) <br>
 	 *	-step <arg>     (optional) interval step (default 100) <br>
     */
    public static void main(String args[]) throws ClassNotFoundException, IOException, ParseException, ParserConfigurationException, SAXException {
		// create Options object
		Options options = new Options();
		// add options
//		options.addOption("doc", true, "Stanford parsed documents directory");
		options.addOption("index", true, "output index file");
		options.addOption("model", true, "output model file");
		options.addOption("cut", true, "(optional) cut-off term frequency (default 5)");
		
		options.addOption("slot", true, "(optional) number of slots (default 35)");
		options.addOption("att", false, "(optional) using attributes (default false)");
		options.addOption("beta_h", true, "(optional) head-slot dirichlet (default 1)");
		options.addOption("beta_a", true, "(optional) att-slot dirichlet (default .1)");
		options.addOption("beta_t", true, "(optional) trigger-slot dirichlet (default .1)");
		
		options.addOption("rand", true, "(optional) random seed (default 1)");
		options.addOption("iter", true, "(optional) number of iterations (default 10000)");
		options.addOption("burn", true, "(optional) burn-in iterations (default 2000)");
		options.addOption("step", true, "(optional) interval step (default 100)");
		options.addOption("lag", true, "(optional) sample lag (default 10)");
		
//    	String path_input = null;
    	String path_index = null;
    	String path_model = null;
		int slot = 35;
		boolean attribute = false;
    	double beta_head = 1;
    	double beta_relation = .1;
    	double beta_attribute = .1;
    	int random_seed = 1;
    	int iteration = 10000;
    	int burn_in = 2000;
    	int interval = 100;
    	int lag = 10;
		
		// parse arguments
		CommandLineParser parser = new BasicParser();
		CommandLine cmd = parser.parse( options, args);
		if (cmd.hasOption("index") && cmd.hasOption("model")) {
			path_index = cmd.getOptionValue("index");
			path_model = cmd.getOptionValue("model");
			
			if (cmd.hasOption("slot")) {
				slot = Integer.parseInt(cmd.getOptionValue("slot"));
			}
			attribute = cmd.hasOption("att");
			if (cmd.hasOption("beta_h")) {
				beta_head = Double.parseDouble(cmd.getOptionValue("beta_h"));
			}
			if (cmd.hasOption("beta_a")) {
				beta_head = Double.parseDouble(cmd.getOptionValue("beta_a"));
			}
			if (cmd.hasOption("beta_t")) {
				beta_head = Double.parseDouble(cmd.getOptionValue("beta_t"));
			}						
			
			if (cmd.hasOption("rand")) {
				random_seed = Integer.parseInt(cmd.getOptionValue("rand"));
			}
			if (cmd.hasOption("iter")) {
				iteration = Integer.parseInt(cmd.getOptionValue("iter"));
			}			
			if (cmd.hasOption("burn")) {
				burn_in = Integer.parseInt(cmd.getOptionValue("burn"));
			}
			if (cmd.hasOption("step")) {
				interval = Integer.parseInt(cmd.getOptionValue("step"));
			}			
			if (cmd.hasOption("lag")) {
				lag = Integer.parseInt(cmd.getOptionValue("lag"));
			}			
			
			// read index
			
			FileInputStream fIndex = new FileInputStream(path_index);
			ObjectInputStream sIndex = new ObjectInputStream(fIndex);
			DocumentIndexing indexer = (DocumentIndexing) sIndex.readObject(); 
			sIndex.close();			
			
			// learn model
			
	    	SlotModel lda = new SlotModel(indexer.triples, indexer.dic_head.size(), indexer.dic_relation.size(), indexer.dic_attribute.size(), random_seed, attribute);
	    	lda.configure(iteration, burn_in, interval, lag);
	    	
	    	lda.gibbs(slot, beta_head, beta_relation, beta_attribute);
	    	
	    	// serialize model
	    	
	    	FileOutputStream fmodel = new FileOutputStream(path_model);
			ObjectOutputStream smodel = new ObjectOutputStream(fmodel);
			smodel.flush();
			smodel.writeObject(lda);
			smodel.close();	    	
		} else {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("SlotModel", options );
		}
    }
}