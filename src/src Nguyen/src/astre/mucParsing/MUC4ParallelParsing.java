package astre.mucParsing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Parallel parsing of MUC-4 documents with Stanford NLP
 * required true case annotation as preprocessing
 * @author nguyen
 *
 */
public class MUC4ParallelParsing {
//	/**
//	 * Input file containing documents, each in a line
//	 */
//	private static final String in_path = 
//			"/vol/zola/users/nguyen/ASTRE/Dataset/muc-dataset(cleaned)";
//	/**
//	 * Output directory
//	 */
//	private static final String out_path = 
//			"/vol/zola/users/nguyen/ASTRE/Dataset/muc-truecase-parsed(cleaned)";
	
	/**
	 * Parse input file, each document in a line
	 * @param file
	 * @param executor
	 * @throws InterruptedException
	 * @throws IOException
	 */
	private void parse(File file, ExecutorService executor, String out_path) throws InterruptedException, IOException {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String line = null;
		out_path = "C:/Users/timjo/PycharmProjects/sourcecode/parsed";
		while ((line = reader.readLine()) != null) {
			String pair[] = line.split("\t");
			String docId = pair[0];
			String text = pair[1];
			text = text.toLowerCase(); // TODO important for true case annotation
			executor.execute(new MUC4Task(text, out_path + "/" + docId + ".xml"));
		}
	}
	/**
	 * Main entry
	 * @param args args[0] text file containing MUC texts in format:
	 * 	each line is a document: [doc_id]\t[text]
	 * 	args[1] output directory where Stanford parsed documents should be placed
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public static void main(String args[]) throws InterruptedException, IOException {
		String in_path = args[0];
		String out_path = args[1];
		
		int cores = Runtime.getRuntime().availableProcessors();
		MUC4ThreadFactory threadFactory = new MUC4ThreadFactory();
		ExecutorService executor = Executors.newFixedThreadPool(cores, threadFactory);
		MUC4ParallelParsing parser = new MUC4ParallelParsing();
		parser.parse(new File(in_path), executor, out_path);
		executor.shutdown();
	}
}
