package edu.grinnell.csc207.compression;

import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * A HuffmanTree derives a space-efficient coding of a collection of byte
 * values.
 *
 * The huffman tree encodes values in the range 0--255 which would normally
 * take 8 bits.  However, we also need to encode a special EOF character to
 * denote the end of a .grin file.  Thus, we need 9 bits to store each
 * byte value.  This is fine for file writing (modulo the need to write in
 * byte chunks to the file), but Java does not have a 9-bit data type.
 * Instead, we use the next larger primitive integral type, short, to store
 * our byte values.
 */
public class HuffmanTree {
    Node root;
    private Map<Short, String> charMap = new HashMap<>();

    private class Node{
        short value;
        int frequency;
        Node left;
        Node right;
        boolean isLeaf;

        //Constructor for when we create a leaf
        Node(short value, int freq){
            this.value = value;
            this.frequency = freq;
            this.isLeaf = true;
        }

        //Cunstructor for when we create a inside node
        Node(Node left, Node right){
            this.left = left;
            this.right = right;
            this.frequency = left.frequency +right.frequency;
            this.isLeaf = false;
        }
    }
    /**
     * Constructs a new HuffmanTree from a frequency map.
     * @param freqs a map from 9-bit values to frequencies.
     */
    public HuffmanTree (Map<Short, Integer> freqs) {
        //Create a priority Queue to hold the nodes
        PriorityQueue<Node> queue = new PriorityQueue<>();

        //Add all of the nodes to the queue
        for(Map.Entry<Short, Integer> character : freqs.entrySet()){
            short key = character.getKey();
            int freq = character.getValue();
            queue.add(new Node(key, freq));
        }
        //Add the EOF to the queue as well
        queue.add(new Node((short) 256, 1));

        //Build the tree by combining the first two elements in the queue until it contains only 1 element
        while(queue.size() > 1){
            Node left = queue.poll();
            Node right = queue.poll();
            Node newNode = new Node(left, right);
            queue.add(newNode);
        }

        //Get the final completed tree
        this.root = queue.poll();

        //Create the charMap
        charMap(root, "");
    }

    /**
     * Constructs a new HuffmanTree from the given file.
     * @param in the input file (as a BitInputStream)
     */
    public HuffmanTree (BitInputStream in) {
        this.root = readTree(in);
    }

    /**
     * Go through the entire binary coded Huffman tree and recursivly recover the original shape of the tree
     * @param in the input file (as a BitInputStream)
     * @return The root node of the entire tree
     */
    public Node readTree(BitInputStream in){
        //Take in a single bit from the file 
        int bit = in.readBit();

        //If bit == 1 return the node and the value associated with it else keep traversing down the tree
        if(bit == 0){
            short val = (short)in.readBits(9);
            return new Node(val, 0);
        } else {
            Node l = readTree(in);
            Node r = readTree(in);
            return new Node(l, r);
        }
    }

    /**
     * Writes this HuffmanTree to the given file as a stream of bits in a
     * serialized format.
     * @param out the output file as a BitOutputStream
     */
    public void serialize (BitOutputStream out) {
        serializeHelper(root, out); 
    }

    /**
     * Helper method for serialize
     * @param node A node object
     * @param out the bitoutput stream 
     */
    public void serializeHelper(Node node, BitOutputStream out){
        if(node.isLeaf){
            out.writeBit(0);
            out.writeBits(node.value, 9);
        } else{
            out.writeBit(1);
            serializeHelper(node.left, out);
            serializeHelper(node.right, out);
        }
    }
   

    public void charMap(Node root, String path){
        if(root.isLeaf){
            charMap.put(root.value, path);
        } else {
            charMap(root.left, path + "0");
            charMap(root.right, path + "1");
        }
    }
    /**
     * Encodes the file given as a stream of bits into a compressed format
     * using this Huffman tree. The encoded values are written, bit-by-bit
     * to the given BitOuputStream.
     * @param in the file to compress.
     * @param out the file to write the compressed output to.
     */
    public void encode (BitInputStream in, BitOutputStream out) {
        int input;

        //Keep reading the input from the BitInputStream until we hit EOF
        while((input = in.readBits(8)) != -1){
            String path = charMap.get((short)input);
            int length = path.length();
            out.writeBits(length, Integer.parseInt(path));
        }

        //Write the EOF character
        out.writeBits(9, 256);
    }

    /**
     * Decodes a stream of huffman codes from a file given as a stream of
     * bits into their uncompressed form, saving the results to the given
     * output stream. Note that the EOF character is not written to out
     * because it is not a valid 8-bit chunk (it is 9 bits).
     * @param in the file to decompress.
     * @param out the file to write the decompressed output to.
     */
    public void decode (BitInputStream in, BitOutputStream out) {
        Node temp = root;
        int bit = in.readBit();
        while(bit != -1){
            if(bit == 0){
                temp = temp.left;
            } else {
                temp = temp.right;
            }

            //Check if we are at a leaf
            if(temp.isLeaf){
                //Check if we are at the EOF
                if(temp.value == 256){
                    //If it is the end end the function
                    return;
                }
                //Write the character out into the outstream
                out.writeBits(temp.value, 8);
                temp = root;
            }

            bit = in.readBit();
        }
    }
}
