/*
qwerted - virtual keyboard for android
Copyright (c) 2010 Moritz Haarmann. All Rights Reserved.

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version
3 of the License, or (at your option) any later version.
 */
package com.qwerted.dict;

import java.io.IOException;
import java.util.HashMap;
import java.util.Vector;

/**
 * holds a single item. is exactly 8 byte wide..
 * 
 * @author moritzhaarmann
 * 
 */
public class DictionaryItem {
    public static Dictionary dict;
    // internally it's long, but it's saved short.
    public int position;
    public DictionaryItem parent;
    public DictionaryItem prevSibling;
    // information about first child and sibling is stored here. for lazy
    // loading.
    public int nextSiblingPos;
    public int firstChildPos;

    public DictionaryItem firstChild;

    public DictionaryItem nextSibling;
    public boolean isSaved;
    public char c;
    public String fullWord; // really not there. more a virtual thing.s

    public short amount;

    /**
     * this one is the circle. let's fix it.
     * 
     * @param what
     * @return
     * @throws Exception
     */
    private static DictionaryItem addRoot(final String what) throws Exception {
        if (DictionaryItem.lookup(what.substring(0, 1), true) != null) {
            return DictionaryItem.lookup(what.substring(0, 1), true);
        }
        if ((what == null) || (what.trim().length() == 0)) {
            throw new RuntimeException("Yellow Submarine");
        }
        final DictionaryItem d = new DictionaryItem();
        d.setChar(what.charAt(0));
        d.write();

        DictionaryItem item = dict.readFirst();
        while ((item != null) && item.hasSibling()) {
            item = item.getNextSibling();
        }
        if (item != null) {
            item.setNextSibling(d);
            item.write();
        }
        return d;
    }

    public static DictionaryItem[] getRootItems() throws IOException {
        final Vector<DictionaryItem> result = new Vector<DictionaryItem>();
        DictionaryItem root = dict.readFirst();
        result.add(root);
        while (root.hasSibling()) {
            result.add(root.getNextSibling());
            root = root.getNextSibling();
        }
        final DictionaryItem[] resA = new DictionaryItem[result.size()];
        return result.toArray(resA);
    }

    /**
     * this is a lookup-or-create method. on successful lookup, the looked up
     * item gets increased, otherwise this item is created. always returns a
     * dictionary item.
     * 
     * @param string
     *            the new item
     * @return
     * @throws Exception
     */
    public static DictionaryItem create(final String string) throws Exception {
        if ((string.length() == 0) || (string == null)) {
            throw new RuntimeException("Yellow Submarine");
        }
        DictionaryItem entry = lookup(string, true);
        // not even a matching root entry, create one.
        if (entry == null) {
            entry = DictionaryItem.addRoot(string);
        }
        if (entry.getFullWord().compareTo(string) == 0) {
            entry.increase();
            return entry;
        } else {
            if (entry.getFullWord().length() > string.length()) {
            }
            entry.addSibling(string.substring(entry.getFullWord().length()));
            return entry;
        }
    }

    private static DictionaryItem lookup(final DictionaryItem item,
            String query, final boolean partial) throws Exception {
        // exit cond. 1 : empty query means that item is what we are looking
        // for.

        if (query == null) {
            throw new RuntimeException("null for lookup seems wrong.");
        }
        query = query.trim();
        if (query.length() == 0) {
            return item;
        }
        final DictionaryItem child = item.childContainingChar(query
                .toCharArray()[0]);
        if (child != null) {
            return lookup(child, query.substring(1), partial);
        } else {
            if (partial) {
                return item;
            } else {
                return null;
            }
        }

    }

    /**
     * perfoms a lookup. if partial is set to true returns even an incomplete
     * match in favor of null.
     * 
     * @param partial
     * @return
     * @throws Exception
     */
    public static DictionaryItem lookup(String query, final boolean partial)
            throws Exception {
        query = query.trim();
        if (query.length() == 0) {
            return null;
        }
        DictionaryItem item = dict.readFirst();
        if (item == null) {
            return null;
        } else {
            while ((item.c != query.toCharArray()[0]) && item.hasSibling()) {
                item = item.getNextSibling();
            }

            if ((item != null) && (item.c == query.toCharArray()[0])) {
                return lookup(item, query.substring(1), partial);
            } else {
                return null;
            }
        }
    }

    public DictionaryItem() {
        this.nextSiblingPos = -1;
        this.firstChildPos = -1;
        this.position = -1;
        isSaved = false;
    }

    public DictionaryItem(final DictionaryItem parent, final int position) {
        this();
    }

    public DictionaryItem(final String string) {
        this();
        // could get tricky.
    }

    /**
     * adds a sibling in the form of a string to this node. of course, it first
     * performs a check whether that sibling already exists. if so, it delegates
     * the same action to that sibling.
     * 
     * @param what
     *            the string to add. please note that you are not supposed to
     *            deliver full strings, but only the range to add.
     * @throws Exception
     * @throws Exception
     */
    public DictionaryItem addSibling(final String what) throws Exception {
        if (what == null) {
            throw new RuntimeException("No String supplied!");
        }
        // assuming it's reached the end of the cycle, so this is meant.
        // increase and return.
        if (what.length() == 0) {
            this.increase();
            return this;
        }
        final DictionaryItem childWithChar = this.childContainingChar(what
                .toCharArray()[0]);
        if (childWithChar != null) {
            // call recursive.
            return childWithChar.addSibling(what.substring(1));
        } else {
            final DictionaryItem newChild = this
                    .createChild(what.toCharArray()[0]);
            newChild.write();
            return newChild.addSibling(what.substring(1));

        }
    }

    public DictionaryItem childContainingChar(final char charAt) {
        // TODO Auto-generated method stub

        DictionaryItem[] siblings;
        try {
            siblings = this.children();
        } catch (final Exception e) {
            // TODO Auto-generated catch block
            return null;
        }
        for (int i = 0; i < siblings.length; i++) {
            if (siblings[i].c == charAt) {
                return siblings[i];

            }
        }
        return null;
    }

    public DictionaryItem[] children() throws Exception {
        final Vector<DictionaryItem> children = new Vector<DictionaryItem>();
        if (this.isLeaf()) {
            return new DictionaryItem[0];
        } else {
            // start with first item, then get all subsequent siblings.
            DictionaryItem currentItem = this.getFirstChild();
            children.add(currentItem);
            while (currentItem.hasSibling()) {
                currentItem = currentItem.getNextSibling();
                children.add(currentItem);

            }

            return children.toArray(new DictionaryItem[children.size()]);
        }
    }

    public static HashMap<Character, Float> childrenWithWeights(
            final DictionaryItem foo) {
        DictionaryItem[] mChildren;
        try {
            if (foo == null) {
                mChildren = DictionaryItem.getRootItems();
            } else {
                mChildren = foo.children();
            }
            if (mChildren.length == 0) {
                return new HashMap<Character, Float>();
            } else {

                final HashMap<Character, Float> resultMap = new HashMap<Character, Float>();
                // find the largest amount.
                int upperLimit = 0;
                for (int i = 0; i < mChildren.length; i++) {
                    if (mChildren[i].amount > upperLimit) {
                        upperLimit = mChildren[i].amount;
                    }
                }
                DictionaryItem curr;
                for (int i = 0; i < mChildren.length; i++) {
                    curr = mChildren[i];
                    final int amount = curr.amount;
                    // rather logarithmic..
                    resultMap.put(curr.c,
                            (float) (Math.log(amount + 1.0f) / Math
                                    .log(upperLimit + 1.0f)));
                }
                return resultMap;
            }
        } catch (final Exception e) {
            return new HashMap<Character, Float>();
        }

    }

    /**
     * creates a child, that is a node with the char c as char with the current
     * node as parent. In case there is already a node with that contents,
     * returns null, so better check thisbefore.
     * 
     * @param c
     *            the Char to be set as the new char
     * @return a new, saved, dictionaryitem
     * @throws Exception
     * @throws RuntimeException
     *             if there is already a child that represents the char.
     */
    private DictionaryItem createChild(final char c) throws Exception {
        // perform some preconditions checks
        if (this.childContainingChar(c) != null) {
            throw new RuntimeException("Can't create child: already exists!");
        } else {
            final DictionaryItem item = new DictionaryItem();
            item.setChar(c);
            item.amount = 1;
            item.setParent(this);
            item.write();
            if (this.isLeaf()) {
                this.setFirstChild(item);
                this.write();
            } else {
                // oh, so this was "major bug no. 1"

                final DictionaryItem[] siblings = this.children();
                DictionaryItem previousSibling;
                for (int i = 0; i < siblings.length; i++) {
                    if (!siblings[i].hasSibling()) {
                        previousSibling = siblings[i];
                        previousSibling.setNextSibling(item);
                        previousSibling.write();
                        break;
                    }
                }

            }
            return item;
        }
    }

    /**
     * decreases the "amount" of a node by -1.
     * 
     * @throws Exception
     */
    public void decrease() throws Exception {
        this.modifyTreeAmountWith(-1);
    }

    /**
     * @throws Exception
     *             if there is no child. returns the first child, if any.
     * 
     * @return a child
     */
    public DictionaryItem getFirstChild() throws Exception {
        if (isLeaf()) {

            throw new Exception("No child here, check this first.");
        } else {
            if (firstChild == null) {
                firstChild = dict.readItem(firstChildPos);
                firstChild.setParent(this);
            }
            return firstChild;
        }

    }

    /**
     * getter for the internal full word.
     * 
     * @return the full word represented by a node.
     */
    private String getFullWord() {
        return this.fullWord;
    }

    /**
     * getter for the next sibling
     * 
     * @return
     * @throws IOException
     */
    public DictionaryItem getNextSibling() throws IOException {
        if (!this.hasSibling()) {
            throw new RuntimeException(
                    "No next sibling here. Check before calling.");
        } else {
            if (nextSibling == null) {
                nextSibling = dict.readItem(nextSiblingPos);
                nextSibling.setPrevSibling(this);
            }
            return nextSibling;
        }
    }

    /**
     * returns a node's parent, as DictionaryItem. The item is not guaranteed to
     * be not null, e.g. when calling a root node.
     * 
     * @return a DictionaryItem representing the parent.
     */
    public DictionaryItem getParent() {
        return parent;
    }

    /**
     * indicates whether a node has more siblings.
     * 
     * @return true, if there are siblings, false, if not.
     */
    public boolean hasSibling() {
        return (this.nextSiblingPos != -1);
    }

    /**
     * increases this nodes amount by +1.
     * 
     * @throws Exception
     */
    public void increase() throws Exception {
        this.modifyTreeAmountWith(+1);
    }

    /**
     * checks if this node is a leaf, i.e. has no children.
     * 
     * @return true if there are no children, false if there are.
     */
    public boolean isLeaf() {
        return (this.firstChildPos == -1);
    }

    /**
     * returns whether this item is in the root tree, i.e. has no parent node.
     * 
     * @return true if this is a root node, false if it is not.
     */
    public boolean isRoot() {
        return (this.getParent() == null);
    }

    /**
     * helper to do the work for de- and increase. this method is called for
     * every node in the path to the root once. Each call is quite expensive
     * because a lot of saving occurs here.
     * 
     * @param change
     *            the amount of change. +1 and -1 will be mostly used.
     * @throws Exception
     */
    public void modifyTreeAmountWith(final int change) throws Exception {
        this.amount = (short) (this.amount + change);
        this.write();
        if (!this.isRoot()) {
            this.getParent().modifyTreeAmountWith(change);
        }
    }

    /**
     * sets the char of a node. should only be called before the first save.
     * 
     * @param c
     */
    public void setChar(final char c) {
        this.c = c;
        if (this.getParent() != null) {
            this.fullWord = this.getParent().getFullWord() + c;
        } else {
            this.fullWord = Character.toString(c);
        }
    }

    /**
     * sets the first child of a node, but only if this child is already saved,
     * because otherwise strange memory effects occur. Drops all preceding first
     * childs.
     * 
     * @param firstChild
     *            the new first child.
     */
    public void setFirstChild(final DictionaryItem firstChild) {
        if ((firstChild != null) && (firstChild.isSaved == true)) {
            this.setFirstChildPosition(firstChild.position);
            this.firstChild = firstChild;
        } else {
            throw new RuntimeException("Can't set unsaved item as first child!");
        }
    }

    /**
     * simple setter for the first child position.
     * 
     * @param fromShort
     *            the position of the first child.
     */
    public void setFirstChildPosition(final int fromShort) {
        if (fromShort == -1) {
            this.firstChildPos = -1;
        } else {
            this.firstChildPos = fromShort;
        }

    }

    /**
     * setter for the next sibling. takes a dictionaryitem.
     * 
     * @param nextSibling
     *            the next sibling, as dictionaryitem.
     */
    public void setNextSibling(final DictionaryItem newNextSibling) {
        this.nextSibling = newNextSibling;

        if ((newNextSibling != null) && (newNextSibling.isSaved == true)) {
            this.setNextSiblingPosition(newNextSibling.position);
        } else {
            throw new RuntimeException("Can't set unsaved nodes as refernce.");
        }

    }

    /**
     * sets the raw position of the next sibling. checks for some boundaries.
     * 
     * @param fromShort
     *            the raw position of the next sibling.
     */
    public void setNextSiblingPosition(final int newPos) {
        if ((newPos == -1) || (newPos == 0)) {
            this.nextSiblingPos = -1;
        } else {
            this.nextSiblingPos = newPos;
        }

    }

    /**
     * setter for the parent node. important, because it also set's the full
     * word.
     * 
     * @param parent
     *            the parent node, as DictionaryItem
     */
    public void setParent(final DictionaryItem parent) {
        this.parent = parent;
        this.fullWord = parent.getFullWord() + this.c;
    }

    /**
     * sets the previous sibling of an item. includes a call to setParent, which
     * basically pulls up all stuff like assigning a full word.
     * 
     * @param prevSibling
     *            the previous sibling
     */
    public void setPrevSibling(final DictionaryItem prevSibling) {
        this.prevSibling = prevSibling;
        if (!this.prevSibling.isRoot()) {
            this.setParent(prevSibling.getParent());
        } else {
            this.fullWord = Character.toString(this.c);
        }
    }

    /**
     * wrapper for the Dictionary's writeItem method.
     * 
     * @throws Exception
     */
    public void write() throws Exception {
        dict.writeItem(this);
    }

}
