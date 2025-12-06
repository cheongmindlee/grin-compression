package edu.grinnell.csc207.compression;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The driver for the Grin compression program.
 */
public class Grin {
    /**
     * Decodes the .grin file denoted by infile and writes the output to the
     * .grin file denoted by outfile.
     * 
     * @param infile  the file to decode
     * @param outfile the file to ouptut to
     */
    public static void decode(String infile, String outfile) {
        try {
            BitInputStream in = new BitInputStream(infile);
            BitOutputStream out = new BitOutputStream(outfile);

            // Check the magic number is a grinfile
            int magic = in.readBits(32);
            if (magic != 1846) {
                throw new IllegalArgumentException("Incorrect magic number");
            }

            // Construct a Huffmantree from the infile
            HuffmanTree tree = new HuffmanTree(in);

            // Decode the text
            tree.decode(in, out);
        } catch (IOException e) {
            throw new IllegalArgumentException("There was an error reading the bitstream", e);
        }

    }

    /**
     * Creates a mapping from 8-bit sequences to number-of-occurrences of
     * those sequences in the given file. To do this, read the file using a
     * BitInputStream, consuming 8 bits at a time.
     * 
     * @param file the file to read
     * @return a freqency map for the given file
     */
    public static Map<Short, Integer> createFrequencyMap(String file) {
        // Create a frequency map
        Map<Short, Integer> frequencyMap = new HashMap<>();
        try {
            BitInputStream inStream = new BitInputStream(file);

            // Hold the 8 bits read from the file
            int bits;
            // Read the file and create the frequency map
            while ((bits = inStream.readBits(8)) != -1) {
                short key = (short) bits;
                if (frequencyMap.containsKey(key)) {
                    frequencyMap.put(key, frequencyMap.get(key) + 1);
                } else {
                    frequencyMap.put(key, 1);
                }
            }
            // Close the stream once it is used
            inStream.close();
        } catch (IOException e) {
            throw new IllegalArgumentException("There was an error with the BitInputStream", e);
        }

        return frequencyMap;
    }

    /**
     * Encodes the given file denoted by infile and writes the output to the
     * .grin file denoted by outfile.
     * 
     * @param infile  the file to encode.
     * @param outfile the file to write the output to.
     */
    public static void encode(String infile, String outfile) throws IOException {
        // Create a frequency map from the input file
        Map<Short, Integer> frequencyMap = createFrequencyMap(infile);

        // Create a HuffmanTree from the frequency map
        HuffmanTree tree = new HuffmanTree(frequencyMap);

        // Write the magic number to the outfile
        BitOutputStream out = new BitOutputStream(outfile);
        BitInputStream in = new BitInputStream(infile);
        out.writeBits(32, 1846);

        // Serialize the HuffmanTree to the outfile
        tree.serialize(out);
        tree.encode(in, out);

        // Close the streams
        out.close();
        in.close();
    }

    /**
     * The entry point to the program.
     * 
     * @param args the command-line arguments.
     */
    public static void main(String[] args) {
        System.out.println("Usage: java Grin <encode|decode> <infile> <outfile>");
        // Make sure there are correct user inputs
        if (args.length != 3) {
            System.out.println("Please make sure your input is of form <encode|decode> <infile> <outfile>");
            return;
        }
        // Make sure there were
        String input = args[0];
        String inFile = args[1];
        String outFile = args[2];

        if (input.equals("decode")) {
            decode(inFile, outFile);
        } else if (input.equals("encode")) {
            try {
                encode(inFile, outFile);
            } catch (IOException e) {
                throw new IllegalArgumentException("Error in encoding", e);
            }
        } else {
            System.out.println("First argument must be either 'encode' or 'decode'");
        }
    }
}
