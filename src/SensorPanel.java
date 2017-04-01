import java.awt.*; // For Graphics, Color etc.
import java.awt.geom.*; // For Rectangle2D, etc.
import javax.swing.*; // For JPanel, etc.

class SensorPanel extends JPanel {
    final int FR_WIDTH = 500; 
    final int FR_HEIGHT = 500; 
    final int ROBOT_W = 25; 
    final int ROBOT_H = 35; 
    final int RADIUS = 20; 
    //Define sides of robot
    final int ROBOT_Fy = (FR_WIDTH-ROBOT_W)/2;
    final int ROBOT_By = (FR_WIDTH+ROBOT_W)/2;
    final int ROBOT_Lx = (FR_HEIGHT-ROBOT_W)/2;
    final int ROBOT_Rx = (FR_HEIGHT+ROBOT_W)/2;
    //Define robot
    final Rectangle2D robot = new Rectangle2D.Double(ROBOT_Lx, ROBOT_Fy, ROBOT_W, ROBOT_H);
        
    static int sLE = 50;
    static int sLF = 50;
    static int sLS = 50;
    static int sF = 50;
    static int sRE = 50;
    static int sRF = 50;
    static int sRS = 50;
    static int sB = 50;
    
    public SensorPanel()
    {
        setBackground(Color.WHITE);
    }
    
    public void updateSensors(int LE, int LF, int F, int RF, int RE, int RS, int B, int LS)
    {
        sLE = LE;
        sLF = LF;
        sLS = LS;
        sRE = RE;
        sRF = RF;
        sRS = RS;
        sF = F;
        sB = B;
        this.repaint();
    }
    
    public int getWidth()
    {
        return FR_WIDTH;
    }
    
    public int getHeight()
    {
        return FR_HEIGHT;
    }
    
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g); //clears panel
        Graphics2D g2d = (Graphics2D) g;
        g2d.setBackground(Color.WHITE);
        
        //Calculate sensor components
        //Left Edge: 60 deg -> cos 60 = 1/2, sin 60 = sqrt(3)/2
        int xLE = (int)Math.round(sLE*Math.sqrt(3)/2);
        int yLE = sLE*1/2; 
        xLE = ROBOT_Lx - xLE; 
        yLE = ROBOT_Fy - yLE;
        //Left Front: 30 deg -> cos 30 = sqrt(3)/2, sin 30 = 1/2
        int xLF = sLF*1/2; 
        int yLF = (int)Math.round(sLF*Math.sqrt(3)/2);
        xLF = ROBOT_Lx + ROBOT_W/4 - xLF; 
        yLF = ROBOT_Fy - yLF;
        //Left Side
        int xLS = sLS; 
        int yLS = 0;
        xLS = ROBOT_Lx - xLS; 
        yLS = FR_HEIGHT/2 + yLS;
        //Forward
        int xF = 0;
        int yF = sF;
        xF = FR_WIDTH/2 + xF; 
        yF = ROBOT_Fy - yF;
        //Right Edge: 60 deg -> cos 60 = 1/2, sin 60 = sqrt(3)/2
        int xRE = (int)Math.round(sRE*Math.sqrt(3)/2);
        int yRE = sRE*1/2;
        xRE = ROBOT_Rx + xRE; 
        yRE = ROBOT_Fy - yRE;
        //Right Front: 30 deg -> cos 30 = sqrt(3)/2, sin 30 = 1/2
        int xRF = sRF*1/2; 
        int yRF = (int)Math.round(sRF*Math.sqrt(3)/2);
        xRF = ROBOT_Rx - ROBOT_W/4 + xRF; 
        yRF = ROBOT_Fy - yRF;
        //Right Side
        int xRS = sRS; 
        int yRS = 0;
        xRS = ROBOT_Rx + xRS; 
        yRS = FR_HEIGHT/2 + yRS;
        //Back
        int xB = 0;
        int yB = sB;
        xB = FR_WIDTH/2 + xB; 
        yB = ROBOT_By + yB;
        
        //Draw robot
        g2d.setColor(Color.RED);
        g2d.draw(robot); 
        g2d.fill(robot);
        
        //Define radii
        int le_rad = sLE/10 + RADIUS;
        int lf_rad = sLF/10 + RADIUS;
        int ls_rad = sLS/10 + RADIUS;
        int re_rad = sRE/10 + RADIUS;
        int rf_rad = sRF/10 + RADIUS;
        int rs_rad = sRS/10 + RADIUS;
        int f_rad = sF/10 + RADIUS;
        int b_rad = sB/10 + RADIUS;
        
        //Draw Sensor info
        g2d.setColor(Color.BLUE);
        g2d.fillOval(xLE-le_rad/2, yLE-le_rad/2, le_rad, le_rad);
        g2d.drawLine(xLE, yLE, ROBOT_Lx, ROBOT_Fy);
        g2d.fillOval(xLF-lf_rad/2, yLF-lf_rad/2, lf_rad, lf_rad);
        g2d.drawLine(xLF, yLF, ROBOT_Lx + ROBOT_W/4, ROBOT_Fy);
        g2d.fillOval(xLS-ls_rad/2, yLS-ls_rad/2, ls_rad, ls_rad);
        g2d.drawLine(xLS, yLS, ROBOT_Lx, FR_HEIGHT/2);
        g2d.fillOval(xRE-re_rad/2, yRE-re_rad/2, re_rad, re_rad);
        g2d.drawLine(xRE, yRE, ROBOT_Rx, ROBOT_Fy);
        g2d.fillOval(xRF-rf_rad/2, yRF-rf_rad/2, rf_rad, rf_rad);
        g2d.drawLine(xRF, yRF, ROBOT_Rx - ROBOT_W/4, ROBOT_Fy);
        g2d.fillOval(xRS-rs_rad/2, yRS-rs_rad/2, rs_rad, rs_rad);
        g2d.drawLine(xRS, yRS, ROBOT_Rx, FR_HEIGHT/2);
        g2d.fillOval(xF-f_rad/2, yF-f_rad/2, f_rad, f_rad);
        g2d.drawLine(xF, yF, FR_WIDTH/2, ROBOT_Fy);
        g2d.fillOval(xB-b_rad/2, yB-b_rad/2, b_rad, b_rad);
        g2d.drawLine(xB, yB, FR_WIDTH/2, ROBOT_By+12);
     }
}