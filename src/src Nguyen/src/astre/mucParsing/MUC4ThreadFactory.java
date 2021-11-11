package astre.mucParsing;

import java.util.concurrent.ThreadFactory;

public class MUC4ThreadFactory implements ThreadFactory{

	@Override
	public Thread newThread(Runnable r) {
		return new MUC4Worker(r);
	}

}
