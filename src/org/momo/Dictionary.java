package org.momo;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Logger;

/*
 * keeps all known words and returns them, probability sorted.
 * the clue is that one or more index files are used, and by design,
 * the built-in device persistent storage is flash, so block reading
 * and seeking isn't an expensive task. that is why all the data is stored in a
 * pointer level format. additions to this dictionary are considered expensive,
 * whereas reading is cheap. Additions are possible without refactoring of the
 * dictionary, although 
 */
public class Dictionary {
	private static HashMap<String,Integer> words;
	
	public Dictionary(){
		words = new HashMap<String,Integer>();
	}
	
	public void readTrainingFile(InputStream f, boolean lineSeperated) throws IOException{
		BufferedReader b = new BufferedReader(new InputStreamReader(f));
		String line;
		while (b.ready()){
			line = b.readLine();
			if ( lineSeperated ){
				putWord(line);
			} else {
				String[] lineComponents = line.split(" ");
				for(int i=0;i<lineComponents.length;i++)
					putWord(lineComponents[i]);
			}
		}
		f.close();
		Logger.getAnonymousLogger().info("Read dictionary training file");
		//words = mergeSort(words);
	}
		
		private void putWord(String line){
			line = line.trim();
			line = line.toLowerCase();
			if (!words.keySet().contains(line)){
				words.put(line,1);
			} else {
				words.put(line,words.get(line)+1);
			}
		}
	
	public HashMap<String, Float> returnByProbability(String snippet){
		
		String[] preProcessing = snippet.split(" ");
		snippet = preProcessing[preProcessing.length-1].toLowerCase();
		
		String candidate;
		Iterator<String> i= words.keySet().iterator();
		HashMap<String,Integer> returnStrings = new HashMap<String,Integer> ();
		
		// first step is to extract all candidate words
		
		while (i.hasNext()){
			candidate = i.next();
			if (candidate.startsWith(snippet)&&candidate.length()>snippet.length())
				returnStrings.put(candidate, words.get(candidate));
		}
		
		// intOfIntereset holds the position of the next letter that is written
		int intOfInterest = snippet.length();
		i = returnStrings.keySet().iterator();
		String t,stringOfInterest;
		HashMap<String,Integer> weights = new HashMap<String,Integer>(); 
		while (i.hasNext()){
			
			t = i.next();
			stringOfInterest = t.substring(intOfInterest,intOfInterest+1);
			if ( weights.containsKey(stringOfInterest)){
				
				weights.put(stringOfInterest, 1+weights.get(stringOfInterest)+returnStrings.get(t));
			} else
				weights.put(stringOfInterest, 1+returnStrings.get(t));
	
		}

		// weights now holds a list of single-char, yet multi-byte strings and a corresponding weight-indicating int
		int highest = 0;
		// find out the highest weight. important for subsequent calculations
		Iterator<Integer> it = weights.values().iterator();
		while (it.hasNext()){
			int can = it.next();
			if (highest < can)
				highest = can;
		}
		highest += 1;
		
		Iterator<String> ic = weights.keySet().iterator();
		HashMap<String,Float> realResult = new HashMap<String,Float>(); 
		String c;
		int weight;
		while (ic.hasNext()){
			c = ic.next();
			weight = weights.get(c) +1;
			// rather logarithmic..
			Double calc = Math.log(weight)/Math.log(highest);
			
			realResult.put(c,calc.floatValue() );
		}
		
		
		return realResult;
	}
	
	public void write(File f){
		
	}
	
	
	
}
