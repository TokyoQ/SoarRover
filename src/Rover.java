/* Written by Joshua Dowling and Alec Collier */

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;

import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortList;

import sml.Agent;
import sml.Identifier;
import sml.Kernel;
import sml.WMElement;
//import sml.smlUpdateEventId;
//import sml.Kernel.UpdateEventInterface;

public class Rover {
    
    private static Kernel kernel;
    private static Agent agent;
    private static String line;
    private String input;
    private static String port = "COM4";
    //private static String port = "/dev/ttyUSB0";
    private static int treeLevelCount = 0;
    private final static int interruptCount = 10;
    
    protected static int inputSel = 0; //0-Sensors, 1-File, 2-Command Line 
    protected static int logSel = 0; //0-None, 1-inputs only, 2-full trace
    protected static int outputSel = 0; //0-Motors, 1-Trace
    protected static String inputFile = "testData.txt";
    protected static String outputFile = null;
    protected static String agentName = "Crawler.soar";
    
    private static SerialPort serialPort; //Implement port
    private static SensorPanel sp;
    private static RoverPanel rp;
    
    private static BufferedReader br;
    protected static Thread t;
    
    public Rover()
    {
        sp = new SensorPanel();
        rp = new RoverPanel("Rover", sp);        
    }
    
    public static void runAgent()
    {
        //Create kernel and agent
        RoverPanel.write("Creating Kernel...");
        kernel = Kernel.CreateKernelInNewThread();
        RoverPanel.write("Creating Agent...");
        agent = kernel.CreateAgent("soar");
	
        //Load Productions
        if (!agent.LoadProductions(agentName)) {
            System.out.println("Can't load " + agentName);
            System.exit(1);
        }
        
        //If using sensors or motors, connect to robot
        if(inputSel == 0 | outputSel == 0) {
            try {
                ports();
                serialPort = new SerialPort(port);
                RoverPanel.write("Port opened: " + serialPort.openPort());
                RoverPanel.write("Params set: " + serialPort.setParams(115200, 8, 1, 0));
                String str = serialPort.readString(9);
                RoverPanel.write(str);
            } catch (SerialPortException ex){
                System.out.println(ex);
            }
        }
        //If using input file, try to find it
        if(inputSel == 1) {
            try {
                br = new BufferedReader(new FileReader(inputFile));
            } catch(FileNotFoundException e){
                RoverPanel.write("File not found");
                //System.out.println("File not found");
                System.exit(1);
            }
        }
        
        //Spawn Java Debugger - not working 
        RoverPanel.write("Debugger spawned: " + agent.SpawnDebugger());

        //Initialise input data
        int l_edRange=0, l_frRange=0, fdRange=0, r_frRange=0, r_edRange=0, r_sdRange=0, bkRange =0, l_sdRange =0;
        
        //While agent allowed to run
        boolean ok = true;
        while(ok) {
            //Get Input
            switch(inputSel){
                case(0): //Sensors
                    int[] data = scan(serialPort);
                    l_edRange = data[0];
                    l_frRange = data[1];
                    fdRange = data[2];
                    r_frRange = data[3];
                    r_edRange = data[4];
                    r_sdRange = data[5];
                    bkRange = data[6];
                    l_sdRange = data[7];
                    break;
                case(1): //Text file
                    try{
                        line = br.readLine();
                    } catch(IOException e) {
                        break;
                    }
                    
                    if(line== null || line=="")
                    {
                        ok = false;
                        RoverPanel.write("No more input");
                        break;
                    }
                    try{ 
                        String[] result = line.split("\\s");
                        l_edRange = Integer.parseInt(result[0]);
                        l_frRange = Integer.parseInt(result[1]);
                        fdRange = Integer.parseInt(result[2]);
                        r_frRange = Integer.parseInt(result[3]);
                        r_edRange = Integer.parseInt(result[4]);
                        r_sdRange = Integer.parseInt(result[5]);
                        bkRange = Integer.parseInt(result[6]);
                        l_sdRange = Integer.parseInt(result[7]);
                    }
                    catch(NumberFormatException ex)
                    {
                        RoverPanel.write("Not proper input");
                        ok = false;
                    }
                    break;
                case(2): // Command Line
                    
                    //Not implemented
                    break;
            }
            //Calc Speed Diff
            int spdDiff = (l_frRange - r_frRange)/5;
            if(spdDiff > 0)
                spdDiff = Math.min(spdDiff, 20);
            if(spdDiff < 0)
                spdDiff = Math.max(spdDiff, -20);
            //RoverPanel.write("spdDiff: " + spdDiff);
            
            //Place data on input link e.g. (I2 ^ultrasonic ^forward ^range fwdRange)
            Identifier in = agent.GetInputLink();
            if(in==null) {
                System.out.println("Input Link Error");
                System.exit(1);
            }
            Identifier ultras = in.CreateIdWME("ultrasonic");
            ultras.CreateIdWME("forward").CreateIntWME("range",fdRange);
            ultras.CreateIdWME("leftEdge").CreateIntWME("range",l_edRange);
            ultras.CreateIdWME("rightEdge").CreateIntWME("range",r_edRange);
            ultras.CreateIdWME("leftFront").CreateIntWME("range",l_frRange);
            ultras.CreateIdWME("rightFront").CreateIntWME("range",r_frRange);
            ultras.CreateIdWME("leftSide").CreateIntWME("range",l_sdRange);
            ultras.CreateIdWME("rightSide").CreateIntWME("range",r_sdRange);
            ultras.CreateIntWME("speedDiff",spdDiff);
            
            //Update graphic
            sp.updateSensors(l_edRange, l_frRange, fdRange, r_frRange, r_edRange, r_sdRange, bkRange, l_sdRange);
            
            //Run Agent until output or 15 cycles
            kernel.RunAllTilOutput();
            RoverPanel.write("Decision count: " + Integer.toString(agent.GetDecisionCycleCounter()));
            
            //Check output link
            Identifier out = agent.GetOutputLink();
            if(out==null) {
                System.out.println("Output Link Error");
                System.exit(1);
            }
            
            //Remove input from input-link
            ultras.DestroyWME();
            
            //Add ^status complete to each action so output can be removed within Soar
            Identifier motors = findChild(out, "motors").ConvertToIdentifier();
            motors.AddStatusComplete();
            
            final int leftSpd = Integer.parseInt(findChild(motors, "left").GetValueAsString());
            final int rightSpd = Integer.parseInt(findChild(motors, "right").GetValueAsString());
            final int runTime = Integer.parseInt(findChild(motors, "time").GetValueAsString());
            //String mode = findChild(out, "mode").GetValueAsString();
            
            //Display action decision
            //RoverPanel.write("Mode: " + mode);
            
            //Print input and output
            RoverPanel.write("Forward: " + fdRange + ", Left: " + l_edRange + ", Right: " + r_edRange + ", Back: " + bkRange);
            RoverPanel.write("Left: " + leftSpd + ", Right: " + rightSpd + ", Time: " + runTime);
            
            //Send move command to robot if using motors, otherwise delay
            if(outputSel == 0)
                move(serialPort, leftSpd, rightSpd, runTime);
            else {
                try {
                    Thread.sleep(500L);
                    //System.out.println("I should be sleeping");

                } catch(InterruptedException ex) {
                    ok = false;
                    //System.out.println("I am interrupted");
                    break;
                }
            }
        }
        //Close serialPort if using it
        if(inputSel == 0 | outputSel == 0) {
            try {
                System.out.println("Port closed: " + serialPort.closePort());
            } catch (SerialPortException ex){
                System.out.println(ex);
            }
        }
        //Destroy agent and shutdown kernel
        RoverPanel.write("Shutting down...");
        System.out.println("Shutting down...");
        kernel.DestroyAgent(agent);
        kernel.Shutdown();
    }
    
    public static void pause()
    {
        //Stop moving
    }
    
    public static void stop() 
    {
        t.interrupt();
    }
    
    //Returns the handle of the node which has the attribute attr, searches immediate children only
    private static WMElement findChild(Identifier node, String attr)
    {
        for(int i = 0; i<node.GetNumberChildren(); i++) {
            String testAttr = node.GetChild(i).GetAttribute();
            if(testAttr.equalsIgnoreCase(attr))
                return node.GetChild(i);
        }
	System.out.println("Child not found");
            return null;
    }
    
    //Prints the tree structure of the given node to the console - useful for debugging i/o links
    private void printTree(Identifier node)
    {
        if(treeLevelCount == 0) {
            System.out.println("\n----" + node.GetAttribute() + "----");
	}
	treeLevelCount += 1;
	for(int i = 0; i<node.GetNumberChildren(); i++) {
            WMElement child = node.GetChild(i);
            for(int j = 0; j<treeLevelCount-1; j++)
            {	System.out.print(">> "); }
            System.out.println(child.GetAttribute());
            if(!child.IsIdentifier()) {
                for(int j = 0; j<treeLevelCount; j++)
                { System.out.print(">> "); }
                System.out.println(child.GetValueAsString());
            } else {
                printTree(child.ConvertToIdentifier());
            }
	}
	treeLevelCount -= 1;
	if(treeLevelCount == 0) {
            System.out.println();
	}
    }
     
    private static int[] scan(SerialPort serialPort){  
        int[] result  = new int[8];
        int[] map = {0,4,1,5,2,6,3,7};
        try {
            System.out.println("Successfully writen "+ "AN" +", to port: " + serialPort.writeString("AN"));
            serialPort.writeString("AN");
            for(int i = 0; i< 8; i++){
                String str1 = serialPort.readHexString(1);
                String str2 = serialPort.readHexString(1);
                result[map[i]] = Integer.valueOf(str1, 16)*256 + Integer.valueOf(str2, 16);
                //System.out.println(result[map[i]]);
            }
            try {
                Thread.sleep(200L);	  // one scan
            } catch (Exception e) {}	
            if(serialPort.readString(1).equals("*")){System.out.println("sync");}else{System.out.println("Out of sync");}
            return result;
        } catch (SerialPortException ex){
            System.out.println(ex);
            return result;
        }
    }

    private static int motorsready(SerialPort serialPort){  
        int result  = 100;
        try {
            System.out.println("Successfully writen "+ "HA" +", to port: " + serialPort.writeString("HA"));
            String str1 = serialPort.readHexString(1);
            String str2 = serialPort.readHexString(1);
            result = Integer.valueOf(str1, 16)*256 + Integer.valueOf(str2, 16);
            System.out.println("motors ready result: "+result);
            if(serialPort.readString(1).equals("*")){System.out.println("sync");}else{System.out.println("Out of sync");}
            try{
                Thread.sleep(100);
            } catch(Exception e){}
            return result;
        } catch (SerialPortException ex){
            System.out.println(ex);
            return result;
        }
    }

    private static void move(SerialPort serialPort, int lm, int rm, int time){
        try {
            System.out.println("Successfully written "+ "HB" +", to port: " + serialPort.writeString("HB"));
            //serialPort.writeString("HB");
            int multlm = Math.abs(lm);
            int multrm = Math.abs(rm);
            double modelm = Math.signum(lm)+1;
            double moderm = Math.signum(rm)+1;
            //System.out.println(" multlm: "+multlm+" multrm: "+multrm+" modelm: "+modelm+" moderm: "+moderm);
            serialPort.writeByte((byte)modelm);
            serialPort.writeByte((byte)(multlm));
            serialPort.writeByte((byte)moderm);
            serialPort.writeByte((byte)(multrm));
            //System.out.println(" high: "+(time/256)+" low: "+(time%256)+" time: "+time);
            serialPort.writeByte((byte)(time/256));
            serialPort.writeByte((byte)(time%256));
            //serialPort.readString(1);
            if(serialPort.readString(1).equals("*")){System.out.println("sync");}else{System.out.println("Out of sync");}
            try{
                Thread.sleep(100);
            } catch(Exception e){System.out.println(e);}
        } catch (SerialPortException ex){
            System.out.println(ex);
        }
    }
    
    private static void ports(){
        String[] portNames = SerialPortList.getPortNames();
        if (portNames.length == 0){
            System.out.println("Robot not found!");
        } else if(portNames.length == 1){
            System.out.print("One Port Found: ");
            System.out.println(portNames[0] + ", Using that port.");
            port = portNames[0];
        } else if(portNames.length > 1){
            System.out.print("Multiple Ports found, ");
            for(int i = 0; i < portNames.length; i++){
                System.out.print(portNames[i] + ", ");
            }
        System.out.println("Defaulting to COM4, until better implementation");
        }
    }  

    // Prompts the user whether they want to quit and returns true/false
    // 'y'|'Y' - true, else false
    private boolean quit()
    {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        try {
            System.out.print("Quit (y/n)?  ");
            input = br.readLine();
            if(input.equalsIgnoreCase("y"))
                return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void main(String[] args)
    {
        new Rover();
       
    }
}

class RoverPanel extends JFrame {
    
    //declaring and instantiating components
    private JLabel chooseAgent = new JLabel("Agent Name:");
    private JTextField agentName = new JTextField("Crawler.soar", 20);
    private JLabel chooseInput = new JLabel("Sensor Input:");
    private String[] inputOpts = {"Sensors", "File", "Command Line"};
    private JComboBox inputSelect = new JComboBox(inputOpts);
    private JLabel inFileName = new JLabel("Input Filename:");
    private JTextField fileIn = new JTextField("testdata.txt", 20);
    private JLabel chooseOutput = new JLabel("Output Options:");
    private String[] outputOpts = {"Motors", "Trace"};
    private JComboBox outputSelect = new JComboBox(outputOpts);
    private JLabel chooseLog = new JLabel("Logging Options:");
    private String[] logOpts = {"None", "Input Only", "Trace"};
    private JComboBox logSelect = new JComboBox(logOpts);
    private JLabel outFileName = new JLabel("Output Filename:");
    private JTextField fileOut = new JTextField("logfile.txt", 20);
    private static JTextArea traceBox = new JTextArea("Rover started \n", 20, 50);
    JScrollPane tracePane = new JScrollPane(traceBox);
    private JButton runButton = new JButton("RUN");
    private JButton stopButton = new JButton("STOP");
    
    private final Dimension v_EDGE_GAP = new Dimension(0,10);
    private final Dimension v_SMALL_GAP = new Dimension(0,20);
    private final Dimension h_EDGE_GAP = new Dimension(10,0);
    private final Dimension h_SMALL_GAP = new Dimension(20,0);
    
    public RoverPanel(String name, SensorPanel sp) 
    {
        super(name);
	
        fileOut.setEnabled(false);
        fileIn.setEnabled(false);
        stopButton.setEnabled(false);
        
        //initialising JPanels and dimensions
        JPanel mainPanel = new JPanel();
        JPanel agentPanel = new JPanel();
        JPanel chooseInPanel = new JPanel();
        JPanel fileInPanel = new JPanel();
        JPanel chooseOutPanel = new JPanel();
        JPanel logPanel = new JPanel();
        JPanel fileOutPanel = new JPanel();
        JPanel buttonsPanel = new JPanel();
        JPanel optionsPanel = new JPanel();
        SensorPanel sensors = sp;
        
        optionsPanel.setMaximumSize(new Dimension(100,500));
        //fileIn.setMaximumSize(new Dimension(300,100));
        
        //setting format for JPanels
        agentPanel.setLayout(new BoxLayout(agentPanel, BoxLayout.X_AXIS));
        chooseInPanel.setLayout(new BoxLayout(chooseInPanel, BoxLayout.X_AXIS));
	fileInPanel.setLayout(new BoxLayout(fileInPanel, BoxLayout.X_AXIS));
        chooseOutPanel.setLayout(new BoxLayout(chooseOutPanel, BoxLayout.X_AXIS));
        logPanel.setLayout(new BoxLayout(logPanel, BoxLayout.X_AXIS));
	fileOutPanel.setLayout(new BoxLayout(fileOutPanel, BoxLayout.X_AXIS));
        buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.X_AXIS));
	optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS));
	 
        //declaring and initialising listeners
	RoverPanel.ButtonListener BL = new RoverPanel.ButtonListener();
	RoverPanel.ReturnListener RL = new RoverPanel.ReturnListener();
        
        fileIn.addKeyListener(RL);
        fileOut.addKeyListener(RL);
        inputSelect.addActionListener(BL);
        outputSelect.addActionListener(BL);
        logSelect.addActionListener(BL);
        runButton.addActionListener(BL);
        stopButton.addActionListener(BL);
        
        /* Adding components and spacing to containers */
        //Main Panel
        mainPanel.add(Box.createRigidArea(h_EDGE_GAP));
        mainPanel.add(optionsPanel);
        mainPanel.add(Box.createRigidArea(h_SMALL_GAP));
        mainPanel.add(sensors);
        //Options panel
        optionsPanel.add(Box.createRigidArea(v_EDGE_GAP));
        optionsPanel.add(agentPanel);
        optionsPanel.add(Box.createRigidArea(v_SMALL_GAP));
        optionsPanel.add(chooseInPanel);
        optionsPanel.add(fileInPanel);
        optionsPanel.add(Box.createRigidArea(v_SMALL_GAP));
        optionsPanel.add(chooseOutPanel);
        optionsPanel.add(Box.createRigidArea(v_SMALL_GAP));
        optionsPanel.add(logPanel);
        optionsPanel.add(fileOutPanel);
        optionsPanel.add(Box.createRigidArea(v_SMALL_GAP));
        optionsPanel.add(tracePane);
        //optionsPanel.add(Box.createRigidArea(v_SMALL_GAP));
        optionsPanel.add(buttonsPanel);
        //Agent Panel
        agentPanel.add(Box.createRigidArea(h_EDGE_GAP));
        agentPanel.add(chooseAgent);
        agentPanel.add(Box.createRigidArea(h_SMALL_GAP));
        agentPanel.add(agentName);
        //Input Panel
        chooseInPanel.add(Box.createRigidArea(h_EDGE_GAP));
        chooseInPanel.add(chooseInput);
        chooseInPanel.add(Box.createRigidArea(h_SMALL_GAP));
        chooseInPanel.add(inputSelect);
        //File Input
        fileInPanel.add(Box.createRigidArea(h_EDGE_GAP));
        fileInPanel.add(inFileName);
        fileInPanel.add(Box.createRigidArea(h_SMALL_GAP));
        fileInPanel.add(fileIn);
        //Output Panel
        chooseOutPanel.add(Box.createRigidArea(h_EDGE_GAP));
        chooseOutPanel.add(chooseOutput);
        chooseOutPanel.add(Box.createRigidArea(h_SMALL_GAP));
        chooseOutPanel.add(outputSelect);
        //Logger
        logPanel.add(Box.createRigidArea(h_EDGE_GAP));
        logPanel.add(chooseLog);
        logPanel.add(Box.createRigidArea(h_SMALL_GAP));
        logPanel.add(logSelect);
        //File Output
        fileOutPanel.add(Box.createRigidArea(h_EDGE_GAP));
        fileOutPanel.add(outFileName);
        fileOutPanel.add(Box.createRigidArea(h_SMALL_GAP));
        fileOutPanel.add(fileOut);
        //Buttons Panel
        buttonsPanel.add(runButton);
        buttonsPanel.add(Box.createRigidArea(h_SMALL_GAP));
        buttonsPanel.add(stopButton);
        
        setSize(1050, 550);
        setMinimumSize(new Dimension(1050,550));
        setMaximumSize(new Dimension(1150,550));
        setContentPane(mainPanel);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); //stop program on window close
	setVisible(true);
                
        }
    
    public static void write(String s)
    {
        traceBox.append(s + "\n");
    }
    

private class ReturnListener extends KeyAdapter {
    
    public void keyPressed(KeyEvent key) {
        int keyCode = key.getKeyCode();
		Object source = key.getSource();
        
        if(keyCode == KeyEvent.VK_ENTER) {
            if(source == fileIn) {
                traceBox.setText("Input from " + fileIn.getText() +".");
                Rover.inputFile = fileIn.getText();
            }
            if(source == fileOut){
                traceBox.setText("Writing to " + fileOut.getText() +".");
                Rover.outputFile = fileOut.getText();
            }
            if(source == agentName){
                traceBox.setText("Loading " + agentName.getText() +".");
                Rover.agentName = agentName.getText();
            }
        }
    }//keyPressed

}//ReturnListener


private class ButtonListener implements ActionListener {
    Thread t;
    public void actionPerformed(ActionEvent action) {
        
        Object source = action.getSource();
        
        if(source == runButton) {
            RoverPanel.write("Agent running");
            
            t = new Thread(new Runnable(){
                public void run() {
                    stopButton.setEnabled(true);
                    runButton.setEnabled(false);
                    Rover.runAgent();
                    stopButton.setEnabled(false);
                    runButton.setEnabled(true);    }
            });
            Rover.t = t;
            t.start();
            
        }
        
        if(source == stopButton) {
            RoverPanel.write("Agent stopped.");
            Rover.stop();
            stopButton.setEnabled(false);
            runButton.setEnabled(true);
        }
        if(source == inputSelect) {
            if(inputSelect.getSelectedItem().equals("Sensors")) {
                RoverPanel.write("Reading from sensors");
                fileIn.setEnabled(false);
                Rover.inputSel = 0;
            }
            if(inputSelect.getSelectedItem().equals("File")) {
                RoverPanel.write("Reading from file");
                fileIn.setEnabled(true);
                Rover.inputFile = fileIn.getText();
                Rover.inputSel = 1;
            }
            if(inputSelect.getSelectedItem().equals("Command Line")) {
                RoverPanel.write("Reading from command line");
                fileIn.setEnabled(false);
                Rover.inputSel = 2;
            }
        }
        if(source == outputSelect) {
            if(outputSelect.getSelectedItem().equals("Motors")) {
                RoverPanel.write("Running motors");
                Rover.outputSel = 0;
            }
            if(outputSelect.getSelectedItem().equals("Trace")) {
                RoverPanel.write("Not running motors");
                Rover.outputSel = 1;
            }
        }
        if(source == logSelect) {
            if(logSelect.getSelectedItem().equals("None")) {
                RoverPanel.write("Not logging");
                fileOut.setEnabled(false);
                Rover.logSel = 0;
            }
            if(logSelect.getSelectedItem().equals("Input Only")) {
                RoverPanel.write("Logging Input data");
                fileOut.setEnabled(true);
                Rover.outputFile = fileOut.getText();
                Rover.logSel = 1;
            }
            if(logSelect.getSelectedItem().equals("Trace")) {
                RoverPanel.write("Writing full log");
                fileOut.setEnabled(true);
                Rover.outputFile = fileOut.getText();
                Rover.logSel = 2;
            }
        }
    }
        
  }
}

class SensorFrame extends JFrame {
    SensorFrame(SensorPanel sp)
    {
        setSize(sp.getHeight(), sp.getWidth());
        setContentPane(sp);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); //stop program on window close
		setVisible(true);
    }
    
}
