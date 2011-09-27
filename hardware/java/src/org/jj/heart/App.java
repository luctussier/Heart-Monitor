package org.jj.heart;

import java.io.IOException;

/**
 * 
 * @author jjones
 */
public class App {
	public static void main(String[] args) throws IOException{
		GraphorWindow window = new GraphorWindow();
		window.setVisible(true);
//		window.graphFile(new File("c:/temp/logs/20110507.LOG"));

//		File dir = new File("d:/jjo/logs");
//		for(File f:dir.listFiles()){
//			if(f.isFile() && f.getName().matches(".*LOG")){
//				processFile(f);
//			}
//		}
	}
}
