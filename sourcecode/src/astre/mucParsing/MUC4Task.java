package astre.mucParsing;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TrueCaseTextAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

public class MUC4Task implements Runnable{
	
	private String text;
	
	private String output;
	
	public MUC4Task(String text, String output) {
		this.text = text;
		this.output = output;
	}

	@Override
	public void run() {
		System.out.println(text);
		StanfordCoreNLP pipeline = MUC4Worker.pipeline.get();
		StanfordCoreNLP truecasePipeline = MUC4Worker.truecasePipeline.get();
		
		List<String> truecaseSentences = new ArrayList<String>();
		
//		Annotation document = new Annotation(text);
//		truecasePipeline.annotate(document);
//	    List<CoreMap> sentences = document.get(SentencesAnnotation.class);
//
//		// this for loop returns an ArrayList of null-values
//	    for(CoreMap sentence: sentences) {
//	    	for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
//				String truecaseToken = token.get(TrueCaseTextAnnotation.class);
//				System.out.println(truecaseToken);
//	    		truecaseSentences.add(truecaseToken);
//	    	}
//	    }
	    
//	    String truecaseText = StringUtils.join(truecaseSentences, " ");
	    Annotation truecaseDocument = new Annotation(text);
	    pipeline.annotate(truecaseDocument);
		try {
			pipeline.xmlPrint(truecaseDocument, new FileWriter(output));
			System.out.println(truecaseDocument);
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Written to " + output);
	}
}
