package astre.mucParsing;

import java.util.Properties;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;

public class MUC4Worker extends Thread{
	protected static ThreadLocal<StanfordCoreNLP> pipeline = new ThreadLocal<StanfordCoreNLP>();
	protected static ThreadLocal<StanfordCoreNLP> truecasePipeline = new ThreadLocal<StanfordCoreNLP>();
	
	protected MUC4Worker(Runnable r) {
		super(r);
	}

	@Override
	public void run() {
//	    Properties truecaseProps = new Properties();
//	    truecaseProps.put("annotators", "tokenize,ssplit,pos,lemma");
//		truecaseProps.put("pos.model",
//	    		"C:/Users/timjo/stanford-corenlp-full-2014-01-04/stanford-corenlp-3.3.1-models/edu/stanford/nlp/" +
//						"models/pos-tagger/english-left3words/english-caseless-left3words-distsim.tagger");
//	    truecasePipeline.set(new StanfordCoreNLP(truecaseProps));
	    
	    Properties props = new Properties();
	    props.put("annotators", "tokenize,ssplit,pos,lemma,ner,parse,dcoref");
	    props.put("parse.maxlen", "100");
	    pipeline.set(new StanfordCoreNLP(props));		
		super.run();
	}
}