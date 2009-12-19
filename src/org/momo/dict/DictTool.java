package org.momo.dict;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

/** 
 * command line interface.
 * @author moritzhaarmann
 *
 */
public class DictTool {
	private Dictionary d;
	private BufferedReader in;
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		DictTool d = new DictTool();
	}
	
	public DictTool(){
		try {
			shell();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("end.");
		}
	}
	
	private void shell() throws IOException{
		String line;
		in = new BufferedReader(new InputStreamReader(System.in));
		while(true){
			System.out.print(">");
			line = in.readLine();
			String input[] = line.split(" ");
			if ( input.length == 2){
				command(input[0],input[1]);
			} else if (input.length == 1){
				command(input[0],null);
			}
		}
	}
	
	private void command(String name, String args) throws IOException{
		if (name.compareTo("load")==0 && args!=null){
			System.out.println("loading dictionary from file " + args);
			d = null;
			try {
				d = new Dictionary(args);
				System.out.println("worked.");
				DictionaryItem.dict = d;
			} catch (FileNotFoundException e) {
				System.out.println("could not load dictionary.");
			}
		} else if(name.compareTo("process")==0 && args!=null && d!=null){
			System.out.println("Processing file " + args + " to dictionary.");
			try {
				processFile(args);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println("Done.");
		} else if(name.compareTo("exit")==0){
			System.out.println("Good Bye.");
			System.exit(0);
		}
	}
	
	private void processFile(String filename) throws Exception{
		File f = new File(filename);
		String line;
		BufferedReader r = new BufferedReader(new FileReader(f));
		while(r.ready()){
			line = r.readLine();
			String[] wElements = line.split("[\\W]+");
			for(int i=0;i<wElements.length;i++){
				if ( wElements[i].trim().length()>0){
				System.out.println(wElements[i].trim());
				DictionaryItem.create(wElements[i].trim());
				}
			}
		}
		r.close();
		
	}

}
