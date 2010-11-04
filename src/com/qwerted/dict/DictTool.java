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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * command line interface.
 * 
 * @author moritzhaarmann
 * 
 */
public class DictTool {
    private Dictionary d;
    private BufferedReader in;

    /**
     * @param args
     */
    public static void main(final String[] args) {
        @SuppressWarnings("unused")
        final DictTool d = new DictTool();
    }

    public DictTool() {
        try {
            shell();
        } catch (final IOException e) {
            // TODO Auto-generated catch block
            System.out.println("end.");
        }
    }

    private void shell() throws IOException {
        String line;
        in = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print(">");
            line = in.readLine();
            final String input[] = line.split(" ");
            if (input.length == 2) {
                command(input[0], input[1]);
            } else if (input.length == 1) {
                command(input[0], null);
            }
        }
    }

    private void command(final String name, final String args)
            throws IOException {
        if ((name.compareTo("load") == 0) && (args != null)) {
            System.out.println("loading dictionary from file " + args);
            d = null;
            try {
                d = new Dictionary(args);
                System.out.println("worked.");
                DictionaryItem.dict = d;
            } catch (final FileNotFoundException e) {
                System.out.println("could not load dictionary.");
            }
        } else if ((name.compareTo("process") == 0) && (args != null)
                && (d != null)) {
            System.out.println("Processing file " + args + " to dictionary.");
            try {
                processFile(args);
            } catch (final Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            System.out.println("Done.");
        } else if (name.compareTo("exit") == 0) {
            System.out.println("Good Bye.");
            System.exit(0);
        }
    }

    private void processFile(final String filename) throws Exception {
        final File f = new File(filename);
        String line;
        final BufferedReader r = new BufferedReader(new FileReader(f));
        while (r.ready()) {
            line = r.readLine();
            final String[] wElements = line.split("[\\W]+");
            for (int i = 0; i < wElements.length; i++) {
                if (wElements[i].trim().length() > 0) {
                    System.out.println(wElements[i].trim());
                    DictionaryItem.create(wElements[i].trim());
                }
            }
        }
        r.close();

    }

}
