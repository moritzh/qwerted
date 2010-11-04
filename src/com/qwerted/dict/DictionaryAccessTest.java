/*
qwerted - virtual keyboard for android
Copyright (c) 2010 Moritz Haarmann. All Rights Reserved.

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version
3 of the License, or (at your option) any later version.
 */
package com.qwerted.dict;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import android.util.Log;

public class DictionaryAccessTest extends junit.framework.TestCase {
    final static String TAG = "DICTTEST";

    private void killDictionary(final String foo) {
        final File f = new File(foo);
        f.delete();
    }

    public void writeAndReadSingleItem() throws Exception {
        killDictionary("test");
        DictionaryItem f = new DictionaryItem();
        f.c = 'a';
        final Dictionary d = new Dictionary("test");
        DictionaryItem.dict = d;

        d.writeItem(f);
        f = null;
        f = d.readFirst();
        assertTrue("first char is a", f.c == 'a');
        assertTrue("first position is 0", f.position == 0);
    }

    public void writeChildrenWorks() throws Exception {
        killDictionary("test");
        final Dictionary d = new Dictionary("test");
        DictionaryItem.dict = d;
        DictionaryItem f = DictionaryItem.create("a");
        f.write();
        final DictionaryItem g = DictionaryItem.create("bcdasdfasdfasdf���");
        g.write();
        final DictionaryItem j = DictionaryItem.create("ba���aaas");
        j.write();
        final DictionaryItem y = DictionaryItem.lookup("ba�", false);
        f = d.readFirst();
        assertTrue("char is right.", f.c == 'a');
        assertTrue("new item can be looked up", y != null);
    }

    public void hasSiblingWorks() throws IOException {
        final DictionaryItem f = new DictionaryItem();
        assertTrue("new items shouldn't have a sibling.",
                f.hasSibling() == false);
    }

    public void conversionFromLongToShortWorks() {
        Log.d(TAG, "" + Dictionary.fromShort(Dictionary.fromInt(100)));
        assertTrue("It works",
                Dictionary.fromShort(Dictionary.fromInt(100)) == 100);
    }

    public void testFilesCompleteWithoutComplaint() throws Exception {
        killDictionary("test");
        final Dictionary d = new Dictionary("test");
        DictionaryItem.dict = d;
        processfile("test_dict_1");

    }

    public void bigDictionaryReadSuceeds() throws Exception {
        System.setProperty("file.encoding", "UTF-8");

        // killDictionary("test");
        final Dictionary d = new Dictionary("german.dict");
        DictionaryItem.dict = d;
        processfile("dict");
        DictionaryItem.dict = new Dictionary("test");
        final DictionaryItem foo = DictionaryItem.lookup("zo", false);
        assertTrue("Reading word works.", foo.fullWord.compareTo("zo") == 0);
        System.out.println("Amount;: " + foo.amount);
    }

    public void processfile(final String name) throws Exception {
        final File f = new File(name);
        String line;
        final FileInputStream is = new FileInputStream(f);
        final InputStreamReader reader = new InputStreamReader(is, "UTF-8");
        final BufferedReader r = new BufferedReader(reader);

        while (r.ready()) {
            line = r.readLine();
            line = line.trim();
            System.out.println(line);
            Log.d(TAG, line);
            DictionaryItem.create(line);

        }
        r.close();
    }

    public void stringOperationsComparison() {
        final String foo = "��aaoossdf";
        assertTrue("charTo and the first of toCharArray are identical",
                foo.charAt(0) == foo.toCharArray()[0]);
        assertTrue("First char is utf8 and working!", (foo.charAt(0) == '�')
                && (foo.toCharArray()[0] == '�')
                && (foo.substring(1).toCharArray()[0] == '�'));
        System.out.println(System.getProperty("file.encoding"));
        System.setProperty("file.encoding", "UTF-8");
        System.out.println(System.getProperty("file.encoding"));

    }

}
