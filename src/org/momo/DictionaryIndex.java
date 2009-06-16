package org.momo;


/**
 * a damn simple layout of a in-file tree representation. there are always 3 kind of nodes,
 * that is enough to describe and extend endlessly. a node is always preceded by it's type as 
 * a 4-bit value.
 * Char-Node-------
 * holds a UTF-8 representation of a char, that is, this field is permitted to be of 
 * arbitrary length ( up to 4 bytes ), all in network-byte-order. the amount of bytes to follow is
 * given by 2 bits at the start of the offset. for reasons of simplicity, always 4 bytes of storage are used.
 *  after the utf-8 char, a pointer to the directory containing child entries is given, 4 bytes. total: 70 bit., 72 bit used.
 *  
 * Directory-Node-------
 * 4-bit indicating the amount of nodes to follow. if a pointer to a directory follows, this is treated as a
 * direct sibling. 
 * 
 * Pointer-Node--------
 * 
 * 
 * @author moritzhaarmann
 *
 */
class DictionaryIndex {
	public final static int NODE_CHAR = 1; 
	public final static int NODE_DIR = 2;
	public final static int NODE_POINTER = 3;
	public final static int NODE_SIZE = 4;
	public final static int NODE_UNUSED = 5;
	
}