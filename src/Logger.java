import java.io.*;
import java.util.*;
import java.awt.Color;
import java.text.DecimalFormat;

public class Logger {

    private BufferedReader buffRead;
    private BufferedWriter buffWrite;
    private int lineCount;

    public Logger() 
    {
        lineCount = 0; 
    }
	
    public Logger(File inputFile) throws FileNotFoundException
    {
        FileReader fileRead = new FileReader(inputFile);
        buffRead = new BufferedReader(fileRead);
        lineCount = 0;
    }
    
    public void processInput() throws NoSuchElementException
    {
	StringTokenizer tokenizer;
	String line = ""; 
	lineCount = 0;
	
	try {
	    line = buffRead.readLine();
	} catch (IOException e) {
	    System.err.println(e);
	}
        
        while(line != null) {  //while there are still lines to read
            //Do stuff
            lineCount++;  //increments line number
            tokenizer = new StringTokenizer(line, ":");
            
            String data1 = tokenizer.nextToken();
            
            try {
		line = buffRead.readLine();
	    } catch (IOException e) {
			System.err.println(e);
	    }
        }
    }
    
    public void writeOutput() 
    {
	        
    }
}
