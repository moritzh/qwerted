/*
qwerted - virtual keyboard for android
Copyright (c) 2010 Moritz Haarmann. All Rights Reserved.

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version
3 of the License, or (at your option) any later version.
 */
package com.qwerted.dict;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;

import android.util.Log;

/**
 * Is the entrypoint to any dictionary. Equipped with some basic operations (
 * like retrieving words, retrieving their children, creating items, etc. ).
 * This monster also handles all file write actions.
 * 
 * @author moritzhaarmann
 * 
 */
public class Dictionary {
    // keeps the file pointer, this is really vital. we'll keep it for the
    // random access
    transient final private File fileHandle;
    transient private RandomAccessFile file;
    public final static int ITEM_SIZE = 12;
    public final static String TAG = "Dictionary";

    /**
     * just returns a well-formed handle to this monster.
     * 
     * @param fileName
     *            the filename of the file to be used as dictionary. may be
     *            non-existant
     * @throws IOException
     */
    public Dictionary(final String fileDesc) throws IOException {
        DictionaryCache.reset();
        fileHandle = new File(fileDesc);

        file = new RandomAccessFile(fileHandle, "rw");

    }

    public DictionaryItem readFirst() {
        try {
            return readItem((short) 0); // NOPMD by moritzhaarmann on 29.11.09
                                        // 16:20
        } catch (final Exception e) {
            // TODO Auto-generated catch block
            Log.e(TAG, e.getMessage());
            return null;
        }
    }

    public DictionaryItem readItem(final int at) throws IOException {
        if (DictionaryCache.exist(at)) {
            return DictionaryCache.get(at);
        }

        final long pos = at * ITEM_SIZE;
        final DictionaryItem dictionaryItem = new DictionaryItem();
        file.seek(pos);
        int nChildPos;
        try {
            nChildPos = file.readInt();
        } catch (final Exception e) {
            // TODO Auto-generated catch block
            try {
                Thread.sleep(200);
            } catch (final InterruptedException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            nChildPos = file.readInt();

            e.printStackTrace();
        }

        dictionaryItem.setNextSiblingPosition(nChildPos);

        final int fChildPos = file.readInt();

        dictionaryItem.setFirstChildPosition(fChildPos);
        dictionaryItem.c = file.readChar();
        dictionaryItem.fullWord = Character.toString(dictionaryItem.c);
        dictionaryItem.amount = file.readShort();
        dictionaryItem.position = at;
        dictionaryItem.isSaved = true;
        DictionaryCache.put(dictionaryItem);
        return dictionaryItem;

    }

    public DictionaryItem writeItem(final DictionaryItem itemOfChoice)
            throws Exception {
        if ((itemOfChoice.position == -1) && (itemOfChoice.isSaved == false)) {
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
                && (itemOfChoice.getFirstChild().isSaved == true)) {
            final int fChildPos = itemOfChoice.firstChildPos;

            file.writeInt(fChildPos);
        } else {
            file.writeInt(-1);
        }
        file.writeChar(itemOfChoice.c);
        file.writeShort(itemOfChoice.amount);

        itemOfChoice.isSaved = true;

        // DictionaryCache.put(itemOfChoice);

        return itemOfChoice;

    }

    @Override
    protected void finalize() throws Throwable {
        file.close();
        super.finalize(); // not necessary if extending Object.
    }

    public static int fromShort(final short foo) {
        return (foo);
    }

    /**
     * strips the upper 2 byte from a long and flips the lsb
     * 
     * @param foo
     * @return
     */
    public static short fromInt(final int foo) {
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

    public static void put(final DictionaryItem item) {
        if (keyQueue.remainingCapacity() == 0) {
            items.remove(keyQueue.remove());
        }
        keyQueue.add(item.position);
        items.put(item.position, item);

    }

    public static boolean exist(final int index) {
        return items.containsKey(index);

    }

    public static void reset() {
        items.clear();
        keyQueue.clear();
    }

    public static DictionaryItem get(final int index) {
        return items.get(index);
    }
}