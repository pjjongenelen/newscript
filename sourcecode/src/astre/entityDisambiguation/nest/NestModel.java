package astre.entityDisambiguation.nest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.xml.sax.SAXException;

import astre.preprocessing.DocumentIndexing;
import astre.preprocessing.RelationExtractor;
import astre.preprocessing.StanfordAnalyzer;
import astre.representation.Document;
import astre.representation.Trigger;
import astre.representation.Triple;
import astre.representation.Word;
/**
 * Implementation of the full template model in 
 * 'Event Schema Induction with a Probabilistic Entity-Driven Model'
 * adding an slot-attribute distribution as presented in 
 * 'Desambiguation d'entites pour l'induction non supervisee de schemas evenementiels' 
 * based on LDA implementation of Gregor Heinrich at
 * 	http://www.arbylon.net/projects/LdaGibbsSampler.java
 * @author nguyen
 *
 */
public class NestModel implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * 
	 */
	private static final double EPSILON = Math.pow(10, -100);
	/**
	 * 
	 */
	private static final double THRESHOLD = 0.01;
	/**
	 * 
	 */
	private static final double lambda = Math.pow(10, -100);
	/**
	 * 
	 */
	private static final double SMOOTH = 0.5;
    /**
     * 
     */
	private boolean constraint = false;
    /**
     * 
     */
	private boolean decomposition = false;
    /**
     * 
     */
	private boolean use_attribute = false;
    /**
     * 
     */
	private double[][] prior;
    /**
     * 
     */
	private Map<Integer,Set<Integer>> cooccurrence = new HashMap<Integer,Set<Integer>>();
    /**
     * 
     */
	private int iteration;
	/**
	 * 
	 */
	private Random random;
	/**
     * Document data (Entity lists)
     */
	private Triple[][] documents;
    /**
     * Head vocabulary size
     */
	private int nV_head;
    /**
     * Feature vocabulary size
     */
	private int nV_attribute;
    /**
     * Dependency vocabulary size
     */
	private int nV_relation;
    /**
     * Predicate vocabulary size
     */
	private int nV_predicate;
    /**
     * Number of schema types
     */
	private int nK_type;
    /**
     * Number of slots
     */
	private int nK_slot;
    /**
     * Dirichlet parameter (document--schema type associations)
     */
	private double alpha;

    /**
     * Dirichlet parameter (slot--head associations)
     */
	private double beta_head;
    /**
     * Dirichlet parameter (slot--feature association)
     */
	private double beta_attribute;
    /**
     * Dirichlet parameter (slot--dependency associations)
     */
	private double beta_relation;
    /**
     * Dirichlet parameter (schema type--predicate associations)
     */
	private double beta_predicate;
    /**
     * Schema type assignments for each entity.
     */
	private int t[][];
    /**
     * Slot assignment for each entity
     */
	private int s[][];
	/**
	 * 
	 */
	private int ss[][][];
    /**
     * nw_p[p][t] number of instances of predicate p 
     * assigned to schema type t.
     */
	private int[][] nw_predicate;
    /**
     * nw_h[h][s] number of instances of head h
     * assigned to slot s and type t
     */
	private int[][] nw_head;
    /**
     * nw_f[f][s] number of instances of feature f
     * assigned to slot s and type t
     */
	private int[][] nw_attribute;
    /**
     * nw_d[d][s] number of instances of dependency d
     * assigned to slot s and type t
     */
	private int[][] nw_relation;
    /**
     * nd[doc][t] number of entities in document doc 
     * assigned to schema type t.
     */
	private int[][] nd;
    /**
     * nwsum_h[s][t] total number of heads 
     * assigned to slot s and type t
     */
	private int[] nwsum_head;
    /**
     * nwsum_f[s][t] total number of features
     * assigned to slot s and type t
     */
	private int[] nwsum_attribute;
    /**
     * nwsum_d[s][t] total number of dependencies 
     * assigned to slot s and type t
     */    
	private int[] nwsum_relation;
    /**
     * nwsum_p[t] total number of predicates assigned to schema type t
     */
	private int[] nwsum_predicate;
    /**
     * ndsum_p[doc] total number of entities in document doc.
     */
	private int[] ndsum;
    /**
     * Accumulative statistics of theta
     * theta[doc][t] = probability(t|doc);
     */
	private double[][] thetasum;
    /**
     * phi_h[s][h] = probability(h|s,t);
     */
	private double[][] phisum_head;
    /**
     * phi_f[s][f] = probability(f|s,t)
     */
	private double[][] phisum_attribute;
    /**
     * phi_d[s][d] = probability(d|s,t);
     */    
	private double[][] phisum_relation;
    /**
     * phi_p[t][p] = probability(p|t);
     */    
	private double[][] phisum_predicate;
    /**
     * size of statistics
     */
	private int numstats;
    /**
     * sampling lag (?)
     */
    private static int THIN_INTERVAL = 100;
    /**
     * burn-in period
     */
    private static int BURN_IN = 2000;
    /**
     * max iterations
     */
    private static int ITERATIONS = 10000;
    /**
     * sample lag (if -1 only one sample taken)
     */
    private static int SAMPLE_LAG = 10;

    private static int dispcol = 0;
    
    
    /**
     * 
     * @return Using attributes
     */
    public boolean isUse_attribute() {
		return use_attribute;
	}
    /**
     * 
     * @return Number of event types
     */
	public int getnK_type() {
		return nK_type;
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
	 * @param seed				Java random seed
	 * @param constraint		Using subj|obj constraint
	 * @param decomposition		Decomposing slot-trigger distribution
	 * @param use_attribute		Using attributes 
	 * @param documents			Documents
	 * @param nV_head			Entity vocabulary size
	 * @param nV_relation		Trigger vocabulary size
	 * @param nV_predicate		Predicate vocabulary size
	 * @param nV_attribute		Attribute vocabulary size
	 * @param nK_type			#event_types
	 * @param nK_slot			#slots
	 * @param alpha				Doc-slot hyper-param
	 * @param beta_head			Slot-head hyper-param
	 * @param beta_relation		Slot-trigger hyper-param
	 * @param beta_predicate	Slot-predicate hyper-param
	 * @param beta_attribute	Slot-attribute hyper-param
	 */
    public NestModel(
    		int seed,
    		boolean constraint,
    		boolean decomposition,
    		boolean use_attribute,
    		Triple[][] documents, 
    		int nV_head, int nV_relation, int nV_predicate, int nV_attribute,
    		int nK_type, 
    		int nK_slot, 
    		double alpha, 
    		double beta_head, 
    		double beta_relation, 
    		double beta_predicate,
    		double beta_attribute) {
    	this.random = new Random(seed);
    	
    	this.constraint = constraint;
    	this.decomposition = decomposition;
    	this.use_attribute = use_attribute;
    	
    	this.documents = documents;
        this.nV_head = nV_head;
        this.nV_relation = nV_relation;
        this.nV_predicate = nV_predicate;
        this.nV_attribute = nV_attribute;
        this.nK_type = nK_type;
        this.nK_slot = nK_slot;
        this.alpha = alpha;
        this.beta_head = beta_head;
        this.beta_relation = beta_relation;
        this.beta_predicate = beta_predicate;
        this.beta_attribute = beta_attribute;
        
        nw_head = new int[nV_head][nK_slot * nK_type];
        nw_attribute = new int[nV_attribute][nK_slot * nK_type];
        nw_relation = new int[nV_relation][nK_slot * nK_type];
        
        nw_predicate = new int[nV_predicate][nK_type];
        
        nwsum_head = new int[nK_slot * nK_type];
        nwsum_attribute = new int[nK_slot * nK_type];
        nwsum_relation = new int[nK_slot * nK_type];
        
        nwsum_predicate = new int[nK_type];
    }
    /**
     * 
     */
    private void estimatePrior() {
    	prior = new double[nV_relation][nV_head];
    	double[] sum = new double[nV_head];
    	int M = documents.length;
    	for(int m = 0; m < M; m++) {
    		int N = documents[m].length;
    		for (int n = 0; n < N; n++) {
    			for(Trigger trigger:documents[m][n].getTriggers()) {
    				prior[trigger.getRelation().getIndex()][documents[m][n].getEntity().getIndex()]++;
					sum[documents[m][n].getEntity().getIndex()]++;
    			}
    		}
    	}
    	for(int i = 0; i < nV_relation; i++) {
    		Set<Integer> set = new HashSet<Integer>();
    		for(int j = 0; j < nV_head; j++) {
    			if (prior[i][j] > 0) {
    				set.add(j);
    				prior[i][j] /= sum[j];
    			} else {
    				prior[i][j] = 0;
    			}
    		}
    		if (!set.isEmpty()) {
    			cooccurrence.put(i, set);
    		}
    	}
    }         

    /**
     * 
     * @param nK_type	Number of event types
     * @param nK_slot	Number of slots
     */
    public void initialState(int nK_type, int nK_slot) {
        int M = documents.length;

        // initialise count variables.
        nw_head = new int[nV_head][nK_slot * nK_type];
        nw_attribute = new int[nV_attribute][nK_slot * nK_type];
        nw_relation = new int[nV_relation][nK_slot * nK_type];
        nw_predicate = new int[nV_predicate][nK_type];
        nd = new int[nK_type][M];
        nwsum_head = new int[nK_slot * nK_type];
        nwsum_attribute = new int[nK_slot* nK_type];
        nwsum_relation = new int[nK_slot * nK_type];
        nwsum_predicate = new int[nK_type];
        ndsum = new int[M];

        // The z_i are are initialised to values in [1,K] to determine the
        // initial state of the Markov chain.

        t = new int[M][];
        s = new int[M][];
        ss = new int[M][][];
        for (int m = 0; m < M; m++) {
            int N = documents[m].length;
            t[m] = new int[N];
            s[m] = new int[N];
            ss[m] = new int[N][nK_slot * nK_type];
            for (int n = 0; n < N; n++) {
                int slot = (int) (random.nextDouble() * nK_type * nK_slot);
                int type = slot / nK_slot;
                t[m][n] = type;
            	s[m][n] = slot;
            	ss[m][n][slot]++;
            	update(m, n, type, slot, +1);
            }
        }
    }

    /**
     * Main method: Select initial state ? Repeat a large number of times: 1.
     * Select an element 2. Update conditional on other elements. If
     * appropriate, output summary for each run.
     * 
     */
    public void gibbs() {
    	
        // init sampler statistics
        if (SAMPLE_LAG > 0) {
            thetasum = new double[documents.length][nK_type];
            phisum_head = new double[nK_type * nK_slot][nV_head];
            phisum_attribute = new double[nK_type * nK_slot][nV_attribute];
            phisum_relation = new double[nK_type * nK_slot][nV_relation];
            phisum_predicate = new double[nK_type][nV_predicate];
            numstats = 0;
        }

        // initial state of the Markov chain:
        initialState(nK_type, nK_slot);

        System.err.println("Sampling " + ITERATIONS
            + " iterations with burn-in of " + BURN_IN + " (B/S="
            + THIN_INTERVAL + ").");

        for (iteration = 0; iteration < ITERATIONS; iteration++) {
            for (int m = 0; m < documents.length; m++) {
                for (int n = 0; n < documents[m].length; n++) {
                	int type = t[m][n];
                	int slot = s[m][n];
                	
                	update(m, n, type, -1, -1);
                    type = sampleFullConditionalType(m, n);
                    update(m, n, type, -1, +1);
                    t[m][n] = type;
                    
                    update(m, n, -1, slot, -1);
                    slot = sampleFullConditionalSlot(m, n);
                    update(m, n, -1, slot, +1);
                    s[m][n] = slot;
                    ss[m][n][slot]++;
                }
            }

            if ((iteration < BURN_IN) && (iteration % THIN_INTERVAL == 0)) {
                System.err.print("B");
                dispcol++;
            }
            // display progress
            if ((iteration > BURN_IN) && (iteration % THIN_INTERVAL == 0)) {
                System.err.print("S");
                dispcol++;
            }
            // get statistics after burn-in
            if ((iteration > BURN_IN) && (SAMPLE_LAG > 0) && (iteration % SAMPLE_LAG == 0)) {
                updateParams();
                System.err.print("|");
                if (iteration % THIN_INTERVAL != 0)
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
     * @param type
     * @param slot
     * @param sign
     */
    private void update(int m, int n, int type, int slot, int sign) {
    	Triple tuple = documents[m][n];
    	if (type != -1) {
	    	// doc-type
	        nd[type][m] += sign;
	        ndsum[m] += sign;
	        // predicate-type
	        for(Trigger trigger:tuple.getTriggers()) {
	        	int idx_predicate = trigger.getPredicate().getIndex();
	        	nw_predicate[idx_predicate][type] += sign;
	        	nwsum_predicate[type] += sign;
	        }
    	}
    	
    	if (slot != -1) {
	        // Head
	        int idx_head = tuple.getEntity().getIndex();
	        nw_head[idx_head][slot] += sign;
	        nwsum_head[slot] += sign;
	        // Attribute
	        if (tuple.getAttributes() != null) {
	        	for(Word attribute:tuple.getAttributes()) {
		        	int idx_attribute = attribute.getIndex();
		        	nw_attribute[idx_attribute][slot] += sign;
		        	nwsum_attribute[slot] += sign;
	        	}
	        }
	        // Relation
	        for(Trigger trigger:tuple.getTriggers()) {
	        	int idx_relation = trigger.getRelation().getIndex();
	        	nw_relation[idx_relation][slot] += sign;
	        	nwsum_relation[slot] += sign;
	        }
    	}  	
    }
    /**
     * 
     * @param m
     * @param n
     * @return event type sampling value
     */
    private int sampleFullConditionalType(int m, int n) {
    	Triple entity = documents[m][n];
        double[] p = new double[nK_type];
        for (int k = 0; k < nK_type; k++) {
            p[k] = (nd[k][m] + alpha) / (ndsum[m] + nK_type * alpha);
            for(Trigger trigger:entity.getTriggers()) {
	        	int idx_predicate = trigger.getPredicate().getIndex();
	        	p[k] *= (nw_predicate[idx_predicate][k] + beta_predicate) / (nwsum_predicate[k] + nV_predicate * beta_predicate);
            }
            // smooth because of java float ~ 0
            p[k] += EPSILON;
        }
        for (int k = 1; k < p.length; k++) {
            p[k] += p[k - 1];
        }
        double u = random.nextDouble() * p[p.length - 1];
        int type = 0;
        for (; type < p.length; type++) {
            if (u < p[type]) {
                return type;
            }
        }
        return type;
    }
    /**
     * 
     * @param m
     * @param n
     * @return slot sampling value
     */
    private int sampleFullConditionalSlot(int m, int n) {
    	int type = t[m][n];
        double[] p = new double[nK_slot];
    	for (int k = 0; k < nK_slot; k++) {
    		int _id_slot = type * nK_slot + k;
    		// Validate subject-object constraint
    		boolean ok = true;
    		if (iteration > (NestModel.BURN_IN / 2) && iteration < NestModel.BURN_IN
    				&& constraint ) {
    			for(Trigger trigger:documents[m][n].getTriggers()) {
    				if (trigger.getInvRelation() != null) {
		        		int id_relation = trigger.getRelation().getIndex();
		        		int id_invRel = trigger.getInvRelation().getIndex();
		        		double prob_inv = (nw_relation[id_invRel][_id_slot] + beta_relation) / (nwsum_relation[_id_slot] + nV_relation * beta_relation);
		        		if (nw_relation[id_relation][_id_slot] < nw_relation[id_invRel][_id_slot] && prob_inv > THRESHOLD) {
		        			p[k] = lambda; ok = false; break;
		        		}
    				}
    			}
    		}if (!ok) continue;
    		
            // Calculate conditional slot probability
    		
    		// Entity-slot
            int id_head = documents[m][n].getEntity().getIndex();
            p[k] =	(nw_head[id_head][_id_slot] + beta_head) / (nwsum_head[_id_slot] + nV_head * beta_head);

        	// Relation-slot
            for(Trigger trigger:documents[m][n].getTriggers()) {
	        	int id_relation = trigger.getRelation().getIndex();
	            double _prob = (nw_relation[id_relation][_id_slot] + beta_relation) / (nwsum_relation[_id_slot] + nV_relation * beta_relation);
	            
	            if (iteration > NestModel.BURN_IN && decomposition) {
	            	// Calculating decomposition probability
	            	double prob = 0;
		            for(int _id_head:cooccurrence.get(id_relation)) {
	            		prob += prior[id_relation][_id_head] * (nw_head[_id_head][_id_slot] + beta_head);
		            }
		            prob /= nwsum_head[_id_slot] + nV_head * beta_head;
		            // Calculating smoothed probability
		            p[k] *= NestModel.SMOOTH * _prob + (1 - NestModel.SMOOTH) * prob;
	            } else {
	            	p[k] *= _prob;
	            }
            }
            
        	// Attribute-slot
            if ( use_attribute &&
            		documents[m][n].getAttributes() != null &&
            		iteration > NestModel.BURN_IN ) {
            	for(Word attribute:documents[m][n].getAttributes()) {
            		int id_attribute = attribute.getIndex();
        			p[k] *= (nw_attribute[id_attribute][_id_slot] + beta_attribute) / (nwsum_attribute[_id_slot] + nV_attribute * beta_attribute);
            	}
            }
            
            p[k] += EPSILON;
    	}
    	// Multinominal probability sampling
        for (int k = 1; k < p.length; k++) {
            p[k] += p[k - 1];
        }
        double u = random.nextDouble() * p[p.length - 1];
        int _slot = 0;
        for (;_slot < p.length; _slot++) {
            if (u < p[_slot])
                break;
        }
        
        int slot = type * nK_slot + _slot;
        return slot;
    }    
  
    /**
     * Add to the statistics the values of theta and phi for the current state.
     */
    private void updateParams() {
    	for (int m = 0; m < documents.length; m++) {
    		for (int k = 0; k < nK_type; k++) {
                thetasum[m][k] += (nd[k][m] + alpha) 
                		/ (ndsum[m] + nK_type * alpha);
            }
        }
        for (int k = 0; k < nK_type; k++) {
            for (int w = 0; w < nV_predicate; w++) {
                phisum_predicate[k][w] += (nw_predicate[w][k] + beta_predicate) 
                		/ (nwsum_predicate[k] + nV_predicate * beta_predicate);
            }
        }
    	for (int k = 0; k < nK_slot * nK_type; k++) {
            for (int w = 0; w < nV_head; w++) { 
        		phisum_head[k][w] += (nw_head[w][k] + beta_head) 
        				/ (nwsum_head[k] + nV_head * beta_head);
            }
            for (int w = 0; w < nV_attribute; w++) { 
        		phisum_attribute[k][w] += (nw_attribute[w][k] + beta_attribute) 
        				/ (nwsum_attribute[k] + nV_attribute * beta_attribute);
            }
            for (int w = 0; w < nV_relation; w++) { 
        		phisum_relation[k][w] += (nw_relation[w][k] + beta_relation) 
        				/ (nwsum_relation[k] + nV_relation * beta_relation);
            }
    	}
        numstats++;
    }
    
    /**
     * Retrieve estimated document--topic associations. If sample lag > 0 then
     * the mean value of all sampled statistics for theta[][] is taken.
     * 
     * @return theta multinomial mixture of document topics (M x K)
     */
    public double[][] getTheta() {
        double[][] theta = new double[documents.length][nK_type];
        if (SAMPLE_LAG > 0) {
        	for (int m = 0; m < documents.length; m++) {
        		for (int k = 0; k < nK_type; k++) {
                    theta[m][k] = thetasum[m][k] / numstats;
                }
            }
        } else {
        	for (int m = 0; m < documents.length; m++) {
        		for (int k = 0; k < nK_type; k++) {
                    theta[m][k] = (nd[m][k] + alpha) 
                    		/ (ndsum[m] + nK_type * alpha);
                }
            }
        }
        return theta;
    }

    /**
     * Retrieve estimated topic--word associations. If sample lag > 0 then the
     * mean value of all sampled statistics for phi[][] is taken.
     * 
     * @return phi multinomial mixture of topic words (K x V)
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
     * @param distribution
     * @return slot distribution
     */
    public double[][] getDistribution(String distribution) {
    	if ("head".equals(distribution)) {
    		return getPhi(nK_slot * nK_type, nV_head, phisum_head, nw_head, nwsum_head, beta_head);
    	} else if ("trigger".equals(distribution)) {
    		return getPhi(nK_slot * nK_type, nV_relation, phisum_relation, nw_relation, nwsum_relation, beta_relation);
    	} else if ("attribute".equals(distribution)) {
    		return getPhi(nK_slot * nK_type, nV_attribute, phisum_attribute, nw_attribute, nwsum_attribute, beta_attribute);
    	} else if ("predicate".equals(distribution)) {
    		return getPhi(nK_type, nV_predicate, phisum_predicate, nw_predicate, nwsum_predicate, beta_predicate);
    	}
    	return null;
    }    

    /**
     * Configure the gibbs sampler
     * 
     * @param iterations
     *            number of total iterations
     * @param burnIn
     *            number of burn-in iterations
     * @param thinInterval
     *            update statistics interval
     * @param sampleLag
     *            sample interval (-1 for just one sample at the end)
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
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws ParseException
     * 
     * <br> Usage: NestModel <br>
     *  -alpha <arg>    (optional) doc-type dirichlet (default 1) <br>
     *  -att            (optional) using attributes (default false) <br>
     *  -beta_a <arg>   (optional) att-slot dirichlet (default .1) <br>
     *  -beta_h <arg>   (optional) head-slot dirichlet (default 1) <br>
     *  -beta_p <arg>   (optional) predicate-type dirichlet (default 1) <br>
     *  -beta_t <arg>   (optional) trigger-slot dirichlet (default .1)  <br>
     *  -burn <arg>     (optional) burn-in iterations (default 2000) <br>
     *  -cut <arg>      (optional) cut-off term frequency (default 5) <br>
     *  -doc <arg>      Stanford parsed documents directory <br>
     *  -index <arg>    output index file <br>
     *  -iter <arg>     (optional) number of iterations (default 10000) <br>
     *  -lag <arg>      (optional) sample lag (default 10) <br>
     *  -model <arg>    output model file <br>
     *  -rand <arg>     (optional) random seed (default 1) <br>
     *  -slot <arg>     (optional) number of slots (default 10) <br>
     *  -step <arg>     (optional) interval step (default 100) <br>
     *  -type <arg>     (optional) number of event types (default 20) <br>
     */
    public static void main(String args[]) throws IOException, ClassNotFoundException, SAXException, ParserConfigurationException, ParseException {
		// create Options object
		Options options = new Options();
		// add options
		options.addOption("doc", true, "Stanford parsed documents directory");
		options.addOption("index", true, "output index file");
		options.addOption("model", true, "output model file");
		options.addOption("cut", true, "(optional) cut-off term frequency (default 5)");
		
		options.addOption("type", true, "(optional) number of event types (default 20)");
		options.addOption("slot", true, "(optional) number of slots (default 10)");
		options.addOption("att", false, "(optional) using attributes (default false)");
		options.addOption("alpha", true, "(optional) doc-type dirichlet (default 1)");
		options.addOption("beta_p", true, "(optional) predicate-type dirichlet (default 1)");
		options.addOption("beta_h", true, "(optional) head-slot dirichlet (default 1)");
		options.addOption("beta_a", true, "(optional) att-slot dirichlet (default .1)");
		options.addOption("beta_t", true, "(optional) trigger-slot dirichlet (default .1)");
		
		options.addOption("rand", true, "(optional) random seed (default 1)");
		options.addOption("iter", true, "(optional) number of iterations (default 10000)");
		options.addOption("burn", true, "(optional) burn-in iterations (default 2000)");
		options.addOption("step", true, "(optional) interval step (default 100)");
		options.addOption("lag", true, "(optional) sample lag (default 10)");
		
    	String path_input = null;
    	String path_index = null;
    	String path_model = null;
		int cut_off = 5;
		int type = 20;
		int slot = 10;
		boolean attribute = false;
		double alpha = 1;
		double beta_predicate = 1;
    	double beta_head = 1;
    	double beta_relation = .1;
    	double beta_attribute = .1;
    	boolean constraint = true;
    	boolean decomposition = false;    	
    	int random_seed = 1;
    	int iteration = 10000;
    	int burn_in = 2000;
    	int interval = 100;
    	int lag = 10;
		
		// parse arguments
		CommandLineParser parser = new BasicParser();
		CommandLine cmd = parser.parse(options, args);
		if (cmd.hasOption("doc") && cmd.hasOption("index") && cmd.hasOption("model")) {
			path_input = cmd.getOptionValue("doc");
			path_index = cmd.getOptionValue("index");
			path_model = cmd.getOptionValue("model");
			if (cmd.hasOption("cut")) {
				cut_off = Integer.parseInt(cmd.getOptionValue("cut"));
			}

			if (cmd.hasOption("type")) {
				slot = Integer.parseInt(cmd.getOptionValue("type"));
			}
			if (cmd.hasOption("slot")) {
				slot = Integer.parseInt(cmd.getOptionValue("slot"));
			}
			attribute = cmd.hasOption("att");
			if (cmd.hasOption("alpha")) {
				alpha = Double.parseDouble(cmd.getOptionValue("alpha"));
			}			
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
			
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			File[] files = new File(path_input).listFiles();
			Arrays.sort(files);
			StanfordAnalyzer analyzer = new StanfordAnalyzer();			
			
			RelationExtractor generator = new RelationExtractor(false, cut_off);
			
			// count term frequency
			
			Map<String,Document> docs = new HashMap<String,Document>();
			for(File file:files) {
				System.err.println(file);
				org.w3c.dom.Document xmlDoc = db.parse(file);
				Document doc = analyzer.traverseDocument(xmlDoc, true);
				generator.countFrequency(doc);
				docs.put(file.getName(), doc);
			}
			
			// create data
			Map<String,List<Triple>> documents = new TreeMap<String,List<Triple>>();
			for(Entry<String,Document> entry:docs.entrySet()) {
				String docId = entry.getKey();
				docId = docId.replaceAll("\\.xml", "");
				List<Triple> tuples = generator.analyze(docId, entry.getValue());
				documents.put(entry.getKey(),tuples);
			}
			
			// index documents
			
			List<String> docIds = new ArrayList<String>();
			List<Triple[]> _tuples = new ArrayList<Triple[]>();
			for(Entry<String,List<Triple>> entry:documents.entrySet()) {
				Triple[] a = new Triple[entry.getValue().size()];
				entry.getValue().toArray(a);
				_tuples.add(a);
				docIds.add(entry.getKey());
			}
			Triple[][] tuples = new Triple[_tuples.size()][];
			_tuples.toArray(tuples);
			DocumentIndexing indexer = new DocumentIndexing();
			
			indexer.updateIndex(tuples);
			indexer.updateInverseDependency(tuples);
			
			indexer.setDocIds(docIds);
			indexer.setTriples(tuples);
			
			// serialize index
			
			FileOutputStream findex = new FileOutputStream(path_index);
			ObjectOutputStream sindex = new ObjectOutputStream(findex);
			sindex.flush();
			sindex.writeObject(indexer);
			sindex.close();
			
			// learn model
			
	    	NestModel lda = new NestModel(
	    			random_seed,
	    			constraint,
	    			decomposition,
	    			attribute,
	    			indexer.triples, 
	    			indexer.dic_head.size(), 
	    			indexer.dic_relation.size(), 
	    			indexer.dic_predicate.size(),
	    			indexer.dic_attribute.size(),
	    			type, 
	        		slot, 
	        		alpha, 
	        		beta_head, 
	        		beta_relation, 
	        		beta_predicate,
	        		beta_attribute);
	    	
	    	lda.configure(iteration, burn_in, interval, lag);
	    	long start = System.currentTimeMillis();
	    	if (lda.decomposition) lda.estimatePrior();
	    	lda.gibbs();
	    	long end = System.currentTimeMillis();
	    	System.out.println("Running time: " + (end-start));
	    	
	    	// serialize model
	    	
	    	FileOutputStream fout = new FileOutputStream(path_model);
			ObjectOutputStream oos = new ObjectOutputStream(fout);
			oos.flush();
			oos.writeObject(lda);
			oos.close();    	
		} else {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("NestModel", options );
		}
    }        
}