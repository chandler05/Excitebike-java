import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.Random;
import javax.swing.*;

public class Main extends JPanel implements Runnable {

    Thread gameThread;
    Font smallFont;
    volatile boolean gameOver = true;

    int pY = 100;
    float pX = 100;
    float angle = 0;

    float yVel = 0;
    int speed = 5;
    int score = 0;

    class Ramp{
        int x;
        int y;
        int width;
        int height;
        int angle;
        boolean active;

        Ramp(int x, int y, int width, int height, int angle, boolean active){
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.angle = angle;
            this.active = active;
        }
    }
    Ramp[] ramps;

    boolean leftPress = false;
    boolean rightPress = false;
    boolean upPress = false;
    boolean spacePress = false;

    float gravity = 1;
    static final Random rand = new Random();

    Point2D RotPoint(Point2D point, Point2D pivot, float pointAngle){
        Point2D result = new Point2D.Double();
        AffineTransform rotation = new AffineTransform();
        double angleInRadians = (pointAngle * Math.PI / 180);
        rotation.rotate(angleInRadians, pivot.getX(), pivot.getY());
        rotation.transform(point, result);
        return result;
    }

    boolean SAT(float x, float y, float w, float h, float angleOne, float x2, float y2, float w2, float h2, float angleTwo){
        boolean result = false;

        Line2D[] objOne = new Line2D[4];
        Line2D[] objTwo = new Line2D[4];
        Point2D[] p1 = new Point2D[4];
        Point2D[] p2 = new Point2D[4];

        p1[0] = new Point2D.Float(x,y);
        p1[1] = new Point2D.Float(x+w,y);
        p1[2] = new Point2D.Float(x+w,y+h);
        p1[3] = new Point2D.Float(x,y+h);
        for(int i = 0; i < 4; i++){
            p1[i] = RotPoint(p1[i], new Point2D.Float(x+(w/2), y+(h/2)), angleOne);
        }

        p2[0] = new Point2D.Float(x2,y2);
        p2[1] = new Point2D.Float(x2+w2,y2);
        p2[2] = new Point2D.Float(x2+w2,y2+h2);
        p2[3] = new Point2D.Float(x2,y2+h2);
        for(int i = 0; i < 4; i++){
            p2[i] = RotPoint(p2[i], new Point2D.Float(x2+(w2/2), y2+(h2/2)), angleTwo);
        }

        objOne[0] = new Line2D.Float(p1[0],p1[1]);
        objOne[1] = new Line2D.Float(p1[1],p1[2]);
        objOne[2] = new Line2D.Float(p1[2],p1[3]);
        objOne[3] = new Line2D.Float(p1[3],p1[0]);

        objTwo[0] = new Line2D.Float(p2[0],p2[1]);
        objTwo[1] = new Line2D.Float(p2[1],p2[2]);
        objTwo[2] = new Line2D.Float(p2[2],p2[3]);
        objTwo[3] = new Line2D.Float(p2[3],p2[0]);

        for(int o = 0; o < 4; o++){
            for(int t=0; t < 4; t++){
                if(objOne[o].intersectsLine(objTwo[t])){
                    result = true;
                }
            }
        }

        return result;
    }

    boolean RampTop(float x, float y, float w, float h, float angleOne, float x2, float y2, float w2, float h2, float angleTwo){
        boolean result = false;

        Line2D[] objOne = new Line2D[4];
        Point2D[] p1 = new Point2D[4];

        p1[0] = new Point2D.Float(x,y);
        p1[1] = new Point2D.Float(x+w,y);
        p1[2] = new Point2D.Float(x+w,y+h);
        p1[3] = new Point2D.Float(x,y+h);
        for(int i = 0; i < 4; i++){
            p1[i] = RotPoint(p1[i], new Point2D.Float(x+(w/2), y+(h/2)), angleOne);
        }
        Point2D p2 = new Point.Float(x2+w2,y2);
        p2 = RotPoint(p2,new Point2D.Float(x2+(w2/2),y2+(h2/2)),angleTwo);

        objOne[0] = new Line2D.Float(p1[0],p1[1]);
        objOne[1] = new Line2D.Float(p1[1],p1[2]);
        objOne[2] = new Line2D.Float(p1[2],p1[3]);
        objOne[3] = new Line2D.Float(p1[3],p1[0]);

        for(int o = 0; o < 4; o++){
            if(objOne[o].ptLineDist(p2) <= 1){
                result = true;
                break;
            }
        }

        return result;
    }

    int nodeDist = 5;

    class Node{
        int x;
        int y;
        boolean open;

        Node(int x, int y, boolean open){
            this.x = x;
            this.y = y;
            this.open = open;
        }
    }

    Node[][] nodes;

    public Main() {
        setPreferredSize(new Dimension(640, 440));
        setBackground(Color.WHITE);
        setFont(new Font("TimesNewRoman", Font.BOLD, 48));
        setFocusable(true);

        smallFont = getFont().deriveFont(Font.BOLD, 18);

        addKeyListener(
                new KeyAdapter() {
                    @Override
                    public void keyPressed(KeyEvent e) {
                        if(e.getKeyCode() == KeyEvent.VK_SPACE){
                            if(gameOver){
                                StartNewGame();
                                repaint();
                            }
                        }

                        switch(e.getKeyCode()){
                            case KeyEvent.VK_LEFT:
                                leftPress = true;
                                break;
                            case KeyEvent.VK_RIGHT:
                                rightPress = true;
                                break;
                            case KeyEvent.VK_UP:
                                upPress = true;
                                break;
                            case KeyEvent.VK_SPACE:
                                spacePress = true;
                                break;
                        }
                    }
                    @Override
                    public void keyReleased(KeyEvent e) {
                        switch(e.getKeyCode()){
                            case KeyEvent.VK_LEFT:
                                leftPress = false;
                                break;
                            case KeyEvent.VK_RIGHT:
                                rightPress = false;
                                break;
                            case KeyEvent.VK_UP:
                                upPress = false;
                                break;
                            case KeyEvent.VK_SPACE:
                                spacePress = false;
                                break;
                        }
                    }
                }
        );
    }

    void InitRamps(){
        ramps = new Ramp[3];
        for(int i = 0; i < 3;  i++){
            ramps[i] = new Ramp(640, 310, 100, 100, -30, false);
        }
    }

    public int GetRandom(Random rnd, int start, int end, int... exclude){
        int random = start + rnd.nextInt(end - start + 1 - exclude.length);
        for(int ex: exclude){
            if(random < ex){
                break;
            }
            random++;
        }
        return random;
    }
    void SpawnRamp(){
        for(int i = 0; i < 3; i++){
            Ramp r = ramps[i];
            if(!r.active){
                r.x = 640;
                r.y = 310;
                r.width = GetRandom(rand, 80, 140);
                r.height = r.width;
                r.angle = GetRandom(rand, -45, -20);
                r.active = true;
                break;
            }
        }
    }

    void StartNewGame(){
        gameOver = false;
        
        Stop();

        pX=100;
        pY=100;
        InitRamps();
        SpawnRamp();

        (gameThread = new Thread(this)).start();
    }

    void Stop(){
        if(gameThread != null){
            Thread tmp = gameThread;
            gameThread = null;
            tmp.interrupt();
        }
    }

    @Override
    public void run() {
        while (Thread.currentThread() == gameThread) {
            try {
                Thread.sleep(25);
            } catch (InterruptedException e) {
                return;
            }


            RotatePlayer();
            Physics();
            repaint();
        }
    }

    void GameOver(){
        gameOver = false;
        Stop();
    }
    void RotatePlayer(){
        if(rightPress)
            angle += 10;
        /*else if(!leftPress && angle > 0 && !RampDetect())
            angle -= 10;
        else if(RampDetect() && angle > 65)
            angle -= 10;
         */
        if(leftPress)
            angle -= 10;
        /*
        else if(!rightPress && angle < 0 && !RampDetect())
            angle += 10;
        else if(RampDetect() && angle < 65)
            angle += 10;
         */
    }

    void Physics(){
        pY += yVel;
        if(GroundDetect() || RampDetect()){
            for(var i = 0; i < Math.abs(yVel); i++) {
                if (GroundDetect() || RampDetect()) {
                    pY += (-1 * (Math.abs(yVel) / yVel));
                }
            }
            if (upPress && yVel > 0) {
                yVel = -12;
            } else {
                yVel = 0;
                if(CanLaunch())
                    for (var i = 0; i < 6; i++)
                        yVel -= 1;
            }
        } else {
            yVel += gravity;
        }
        if(spacePress){
            if(speed < 10)
                speed++;
        }
        else{
            if(speed > 0)
                speed--;
        }
        if(speed > 0)
            score += speed;
        MoveRamp();
        if(GroundDetect() || RampDetect()){
            for(var i= 0; i < 6; i++){
                if(RampDetect() || GroundDetect()) {
                    pY--;
                }
            }
        }
    }

    boolean CanLaunch(){
        boolean result = false;
        for(int i = 0; i < 3; i++){
            Ramp r = ramps[i];
            if(r.active){
                result = RampTop(pX, pY, 40, 40, angle, r.x, r.y, r.width, r.height, r.angle);
                if(result)
                    break;
            }
        }
        return result;
    }

    boolean GroundDetect(){
        if(SAT(pX,pY,40,40,angle,0,340,640,100,0)){
            if(angle-Math.floor(angle/360)*360 <= 300 && angle-Math.floor(angle/360)*360 >= 60){
                GameOver();
            }
            return true;
        }
        else{
            return false;
        }
    }

    boolean RampDetect(){
        boolean result = false;
        for(int i = 0; i < 3; i++){
            Ramp r = ramps[i];
            if(r.active){
                if(SAT(pX, pY, 40, 40, angle, r.x,r.y,r.width,r.height,r.angle)){
                    if(angle-Math.floor(angle/360)*360 <= 300+r.angle && angle-Math.floor(angle/360)*360 >= 60+r.angle){
                        GameOver();
                    }
                    result = true;
                    break;
                }
            }
        }
        return result;
    }

    void MoveRamp(){
        for(int i = 0; i < 3; i++){
            Ramp r = ramps[i];
            if(r.active){
                r.x = Math.round(r.x) - speed;
                if(r.x < -100)
                    r.active = false;
                    SpawnRamp();
            }
        }
    }

    void DrawPlayer(Graphics2D g){
        AffineTransform old = g.getTransform();
        g.rotate(Math.toRadians(angle),pX+20,pY+20);
        g.setColor(Color.CYAN);
        g.fillRect(Math.round(pX),pY,40,40);
        g.setColor(Color.BLACK);
        g.fillRect(Math.round(pX) +35,pY+5,5,5);
        g.setTransform(old);
    }

    void DrawGround(Graphics2D g){
        g.setColor(Color.darkGray);
        g.fillRect(0,340,640,300);
    }

    void DrawRamp(Graphics2D g){
        for(int i = 0; i < 3; i++) {
            Ramp r = ramps[i];
            if(r.active) {
                AffineTransform old = g.getTransform();
                g.rotate(Math.toRadians(-30), r.x + 50, 350);
                g.setColor(Color.darkGray);
                g.fillRect(Math.round(r.x), r.y,r.width, r.height);
                g.setColor(Color.cyan);
                g.fillArc(r.x+r.width,r.y,5,5,0,360);
                g.setTransform(old);
            }
        }
    }

    void DrawScore(Graphics2D g) {
        g.setColor(Color.red);
        g.setFont(getFont());
        g.drawString("SCORE: " + Math.floorDiv(score, 10), 20, 20);
    }
    void DrawStartScreen(Graphics2D g) {
        g.setColor(Color.red);
        g.setFont(getFont());
        g.drawString("EXCITE BIKE", 150, 190);
        g.setColor(Color.orange);
        g.setFont(smallFont);
        g.drawString("(Space to START)", 200, 240);
    }

    @Override
    public void paintComponent(Graphics gg) {
        super.paintComponent(gg);
        Graphics2D g = (Graphics2D) gg;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (gameOver) {
            DrawStartScreen(g);
        } else {
            DrawPlayer(g);
            DrawGround(g);
            DrawRamp(g);
            DrawScore(g);
            //DrawNodes(g);
        }
    }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame mainFrame = new JFrame();
            mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            mainFrame.setTitle("GAME");
            mainFrame.setResizable(true);
            mainFrame.add(new Main(), BorderLayout.CENTER);
            mainFrame.pack();
            mainFrame.setLocationRelativeTo(null);
            mainFrame.setVisible(true);
        });
    }
}
