import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.awt.event.*;
/*
    class that represents the state of a ball in this simulation

    in this class I am assuming that the mass of the ball grows proportional
    to the radius of the ball
 */
class Ball{

    int x;
    int y;
    int vx;
    int vy;
    int radius;
    Color c;

    private Ball(int x, int y, int vx, int vy, int radius) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.radius = radius;
        c = new Color((int)(Math.random()*256), (int)(Math.random()*256), (int)(Math.random()*256));
    }
    /*
        logic adapted from: http://stackoverflow.com/questions/345838/ball-to-ball-collision-detection-and-handling

        checks to see if a Ball b is colliding with this ball; returns true if it is and false otherwise
    */
    public boolean isColliding(Ball b){
        float xd = x - b.x;
        float yd = y - b.y;
        float sumRadius = radius + b.radius;
        float sqrRadius = sumRadius * sumRadius;
        float distSqr = (xd * xd) + (yd * yd);

        if (distSqr <= sqrRadius)
        {
            return true;
        }
        return false;
    }
    /*
        draws the ball to the screen at its current location and fills it with color
     */
    public void draw(Graphics g){
        g.drawOval(x, y, 2 * radius, 2 * radius);
        g.setColor(c);
        g.fillOval(x, y, 2 * radius, 2 * radius);
    }
    /*
        changes the vx and vy of both balls involved in a collision

        logic adapted from:
        https://gamedev.stackexchange.com/questions/20516/ball-collisions-sticking-together
     */
    public void collide(Ball b){
        double xDist = x - b.x;
        double yDist = y - b.y;
        double distSquared = xDist*xDist + yDist*yDist;
        double xVelocity = b.vx - vx;
        double yVelocity = b.vy - vy;
        double dotProduct = xDist*xVelocity + yDist*yVelocity;
        //Neat vector maths, used for checking if the objects moves towards one another.
        if(dotProduct > 0){
            double collisionScale = dotProduct / distSquared;
            double xCollision = xDist * collisionScale;
            double yCollision = yDist * collisionScale;
            //The Collision vector is the speed difference projected on the Dist vector,
            //thus it is the component of the speed difference needed for the collision.
            double combinedMass = radius + b.radius;
            double collisionWeightA = 2 * b.radius / combinedMass;
            double collisionWeightB = 2 * radius / combinedMass;
            vx += collisionWeightA * xCollision;
            vy += collisionWeightA * yCollision;
            b.vx -= collisionWeightB * xCollision;
            b.vy -= collisionWeightB * yCollision;
        }
    }
    /*
        updates the x and y of the ball based on the velocity in the x and y directions, if the ball hits a
        border it is reflected off
    */
    public void update(){
        x += vx;
        y += vy;

        //ball has hit the right side of border
        if(x + 2 * radius >= Panel.BORDER_WIDTH + Panel.BORDER_X){
            vx = -vx;
            x = (Panel.BORDER_WIDTH + Panel.BORDER_X) - (2 * radius) - 1;
        }
        //ball has hit the left side of border
        if(x  <= Panel.BORDER_X){
            vx = Math.abs(vx);
            x = Panel.BORDER_X + 1;
        }
        //ball has hit the bottom of border
        if(y + 2 * radius >= Panel.BORDER_HEIGHT+ Panel.BORDER_Y){
            vy = -vy;
            y = (Panel.BORDER_HEIGHT + Panel.BORDER_Y) - (2 * radius) - 1;
        }
        //ball has hit the top of border
        if(y  <= Panel.BORDER_Y){
            vy = Math.abs(vy);
            y = Panel.BORDER_Y + 1;
        }
    }

    public static Ball randomBallFactory(){
        return new Ball((int) (50 + (int)(Math.random() * 500)),
                (int)  (50 + (int)(Math.random() * 500)),
                1 + (int)(Math.random() * 5),
                1 + (int)(Math.random() * 5),
                30 + (int)(Math.random() * 50));
    }
    /*
        string representation of a ball object
     */
    public String toString(){
        return "x: " + x + " y: " + y + " vx: " + vx + " vy: " + vy;
    }

}

class Panel extends JPanel implements ActionListener{

    public static final int BORDER_HEIGHT = 500;
    public static final int BORDER_WIDTH = 500;
    public static final int BORDER_X = 100;
    public static final int BORDER_Y = 100;
    public static final int DELAY = 10;

    private ArrayList<Ball> balls;
    javax.swing.Timer timer;

    public Panel(){
        balls = new ArrayList<Ball>();
        timer=new Timer(DELAY, this);
        timer.start();
    }

    public void addBall(Ball b){
        balls.add(b);
    }

    private void update(){
        for(Ball b : balls){
            b.update();
        }

        for(int i = 0; i < balls.size(); i++){
            for(int j = i + 1; j < balls.size(); j++){
                if(balls.get(i).isColliding(balls.get(j))){
                    balls.get(i).collide(balls.get(j));
                }
            }
        }
    }

    public void actionPerformed(ActionEvent ev) {
        if (ev.getSource() == timer) {
            update();
            repaint();
        }
    }

    public void paint(Graphics g) {
            g.drawRect(BORDER_X, BORDER_Y, BORDER_WIDTH, BORDER_HEIGHT);
            for(Ball b : balls){
                b.draw(g);
            }
    }
}

public class Simulation {

    public static void main(String[] args) {

        JFrame frame = new JFrame();
        frame.setSize(700, 700);
        Panel p = new Panel();
        frame.add(p);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        p.addBall(Ball.randomBallFactory());
        p.addBall(Ball.randomBallFactory());
        p.addBall(Ball.randomBallFactory());
        p.addBall(Ball.randomBallFactory());
    }
}

