/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package jpcsp.filesystems;

import java.io.*;

/**
 *
 * @author gigaherz
 */
public class SeekableRandomFile extends RandomAccessFile implements SeekableDataInput {

    public SeekableRandomFile(String fileName, String mode) throws FileNotFoundException
    {
        super(fileName, mode);
    }
    
    public SeekableRandomFile(File name, String mode) throws FileNotFoundException
    {
        super(name, mode);
    }
    
}
