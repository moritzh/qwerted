package org.momo.dict;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import android.util.Log;

public class DictionaryAccessTest extends junit.framework.TestCase{
	final static String TAG ="DICTTEST";
	
	private void killDictionary(String foo){
		File f = new File(foo);
		f.delete();
	}

	public void writeAndReadSingleItem() throws Exception{
		killDictionary("test");
		DictionaryItem f = new DictionaryItem();
		f.c = 'a';
		Dictionary d = new Dictionary("test");
		DictionaryItem.dict = d;

		d.writeItem(f);
		f = null;
		f = d.readFirst();
		assertTrue("first char is a", f.c=='a');
		assertTrue("first position is 0", f.position == 0);
	}
	
	public void writeChildrenWorks() throws Exception{
		killDictionary("test");
		Dictionary d = new Dictionary("test");
		DictionaryItem.dict = d;
		DictionaryItem f= DictionaryItem.create("a");
		f.write();
		DictionaryItem g = DictionaryItem.create("bcdasdfasdfasdfüüü");
		g.write();
		DictionaryItem j = DictionaryItem.create("baüüüaaas");
		j.write();
		DictionaryItem y = DictionaryItem.lookup("baü", false);
		f = d.readFirst();
		assertTrue("char is right.", f.c == 'a');
		assertTrue("new item can be looked up", y != null);
	}
	

	
	public void hasSiblingWorks() throws IOException{
		DictionaryItem f = new DictionaryItem();
		assertTrue("new items shouldn't have a sibling.", f.hasSibling() == false);
	}
	

	public void conversionFromLongToShortWorks(){
		Log.d(TAG,""+Dictionary.fromShort(Dictionary.fromInt(100)));
		assertTrue("It works", Dictionary.fromShort(Dictionary.fromInt(100))==100);
	}
		
	public void testFilesCompleteWithoutComplaint() throws Exception{
		killDictionary("test");
		Dictionary d = new Dictionary("test");
		DictionaryItem.dict = d;
		processfile("test_dict_1");

	}
	
	public void bigDictionaryReadSuceeds() throws Exception{
		System.setProperty("file.encoding", "UTF-8");

		//killDictionary("test");
		Dictionary d = new Dictionary("german.dict");
		DictionaryItem.dict = d;
		processfile("dict");
		DictionaryItem.dict = new Dictionary("test");
		DictionaryItem foo = DictionaryItem.lookup("zo",false);
		assertTrue("Reading word works.", foo.fullWord.compareTo("zo")==0);
		System.out.println("Amount;: " + foo.amount);
	}
	
	public void processfile(String name) throws Exception{
		File f = new File(name);
		String line;
		FileInputStream is = new FileInputStream(f);      
		InputStreamReader reader = new InputStreamReader(is, "UTF-8");
		BufferedReader r = new BufferedReader(reader);

		while(r.ready()){
			line = r.readLine();
			line = line.trim();
			System.out.println(line);
			Log.d(TAG,line);
				DictionaryItem.create(line);
			
			
		}
		r.close();
	}
	
	
	public void stringOperationsComparison(){
		String foo = "ŠŸaaoossdf";
		assertTrue("charTo and the first of toCharArray are identical", foo.charAt(0) == foo.toCharArray()[0]);
		assertTrue("First char is utf8 and working!", foo.charAt(0) == 'Š' && foo.toCharArray()[0] == 'Š' && foo.substring(1).toCharArray()[0] == 'Ÿ');
		System.out.println(System.getProperty("file.encoding"));
		System.setProperty("file.encoding", "UTF-8");
		System.out.println(System.getProperty("file.encoding"));

	}
	
}
