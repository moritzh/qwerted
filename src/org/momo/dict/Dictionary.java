package org.momo.dict;

import java.io.File;
import java.io.FileDescriptor;

import android.content.res.AssetFileDescriptor;
import android.util.*;
import java.io.*;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * only entrypoint to the dictionary. create yourself an instance, load a
 * datafile and read data. should be fast.
 * 
 * @author moritzhaarmann
 * 
 */
public class Dictionary {
	// keeps the file pointer, this is really vital. we'll keep it for the
	// random access
	transient final private File fileHandle; // NOPMD by moritzhaarmann on
												// 29.11.09 16:19
	transient  private RandomAccessFile file;
	private int writeCount = 0;
	public final static int ITEM_SIZE = 12;
	public final static String TAG ="Dictionary";


	/**
	 * just returns a well-formed handle to this monster.
	 * 
	 * @param fileName
	 *            the filename of the file to be used as dictionary. may be
	 *            non-existant
	 * @throws IOException
	 */
	public Dictionary(String fileDesc) throws IOException {
		DictionaryCache.reset();
		fileHandle = new File(fileDesc);

		file = new RandomAccessFile(fileHandle, "rw");
		
	

	}

	public DictionaryItem readFirst() {
		try {
			return readItem((short) 0); // NOPMD by moritzhaarmann on 29.11.09
										// 16:20
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Log.e(TAG,e.getMessage());
			return null;
		}
	}

	public DictionaryItem readItem(int at) throws IOException {
		if (DictionaryCache.exist(at)) {
			return DictionaryCache.get(at);
		}
		
		long pos = at * ITEM_SIZE;
		final DictionaryItem dictionaryItem = new DictionaryItem();
		file.seek(pos);
		int nChildPos;
			try {
				nChildPos = file.readInt();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				try {
					Thread.sleep(200);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				nChildPos = file.readInt();

				e.printStackTrace();
			}
		

		dictionaryItem.setNextSiblingPosition(nChildPos);

		int fChildPos = file.readInt();

		dictionaryItem.setFirstChildPosition(fChildPos);
		dictionaryItem.c = file.readChar();
		dictionaryItem.fullWord = Character.toString(dictionaryItem.c);
		dictionaryItem.amount = file.readShort();
		dictionaryItem.position = at;
		dictionaryItem.isSaved = true;
		return dictionaryItem;

	}

	public DictionaryItem writeItem(DictionaryItem itemOfChoice)
			throws Exception {
		if (itemOfChoice.position == -1 && itemOfChoice.isSaved == false) {
			itemOfChoice.position = (int) (file.length() / ITEM_SIZE);
		}
		if (itemOfChoice.position == 0) {
		}
		file.seek((long) itemOfChoice.position * ITEM_SIZE);

		/*
		 * if ( itemOfChoice.hasSibling() &&
		 * itemOfChoice.getNextSibling().isSaved == true){ int nSiblingPos =
		 * itemOfChoice.nextSiblingPos;
		 */

		file.writeInt(itemOfChoice.nextSiblingPos);

		if (!itemOfChoice.isLeaf()
				&& itemOfChoice.getFirstChild().isSaved == true) {
			int fChildPos = itemOfChoice.firstChildPos;

			file.writeInt(fChildPos);
		} else {
			file.writeInt(-1);
		}
		file.writeChar(itemOfChoice.c);
		file.writeShort(itemOfChoice.amount);


		itemOfChoice.isSaved = true;

		DictionaryCache.put(itemOfChoice);
		writeCount++;
		if (writeCount > 400000){
			// reopen file, resync.
			file.close();

			for ( int i = 0;i<7;i++){
				System.out.print(".");
				Thread.sleep(1000);
			}
			System.out.print("\n");
			file = new RandomAccessFile(fileHandle, "rw");

			writeCount =0;
		}
		return itemOfChoice;

	}

	@Override
	protected void finalize() throws Throwable {
		file.close();
		super.finalize(); // not necessary if extending Object.
	}

	public static int fromShort(short foo) {
		return (int) (foo);
	}

	/**
	 * strips the upper 2 byte from a long and flips the lsb
	 * 
	 * @param foo
	 * @return
	 */
	public static short fromInt(int foo) {
		return (short) (foo);
	}
}

class DictionaryCache {
	private static HashMap<Integer, DictionaryItem> items;
	private static LinkedBlockingQueue<Integer> keyQueue;

	static {
		items = new HashMap<Integer, DictionaryItem>(90000);
		keyQueue = new LinkedBlockingQueue<Integer>(90000);
	}

	public static void put(DictionaryItem item) {
		if (keyQueue.remainingCapacity()==0){
			items.remove(keyQueue.remove());
		}
		keyQueue.add(item.position);
		items.put(item.position, item);

	}

	public static boolean exist(int index) {
		return items.containsKey(index);

	}
	
	public static void reset(){
		items.clear();
		keyQueue.clear();
	}

	public static DictionaryItem get(int index) {
		return items.get(index);
	}
}