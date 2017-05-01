import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Random;
/*
    class that represents the state of a ball in this simulation

    in this class I am assuming that the mass of the ball grows proportional
    to the radius of the ball
 */
class Ball implements Serializable{

    public static final int MIN_INITIAL_SPEED = 1;
    public static final int MAX_INITIAL_SPEED = 10;
    public static final int MAX_RADIUS = 30;
    public static final int MIN_RADIUS = 15;

    int x;
    int y;
    int vx;
    int vy;
    int radius;
    int center_x;
    int center_y;
    Color c;
    /*
        default constructor for a ball object
    */
    public Ball(int x, int y, int vx, int vy, int radius) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.radius = radius;
        this.center_x = x + radius;
        this.center_y = y + radius;
        c = new Color((int)(Math.random()*256), (int)(Math.random()*256), (int)(Math.random()*256));
    }
    /*
        chaining constructor for ball object with a random radius
     */
    public Ball(int x, int y, int vx, int vy) {
        this(x, y, vx, vy, MIN_RADIUS + (int)(Math.random() * MAX_RADIUS));
    }
    /*
        chaining constructor for a ball object with only x and y location provided
     */
    public Ball(int x, int y) {
        this(x, y, MIN_INITIAL_SPEED + (int)(Math.random() * MAX_INITIAL_SPEED), MIN_INITIAL_SPEED + (int)(Math.random() * MAX_INITIAL_SPEED));
    }
    /*
        logic adapted from: http://stackoverflow.com/questions/345838/ball-to-ball-collision-detection-and-handling
        checks to see if a Ball b is colliding with this ball; returns true if it is and false otherwise
    */
    public boolean isColliding(Ball b){
        float xd = center_x - b.center_x;
        float yd = center_y - b.center_y;
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
        if(dotProduct > 0){
            double collisionScale = dotProduct / distSquared;
            double xCollision = xDist * collisionScale;
            double yCollision = yDist * collisionScale;
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
        center_x = x + radius;
        center_y = y + radius;

        //ball has hit the right side of border
        if(x + 2 * radius >= ServerPanel.BORDER_WIDTH + ServerPanel.BORDER_X){
            vx = -vx;
            x = (SimulationPanel.BORDER_WIDTH + SimulationPanel.BORDER_X) - (2 * radius) - 1;
        }
        //ball has hit the left side of border
        if(x  <= SimulationPanel.BORDER_X){
            vx = Math.abs(vx);
            x = SimulationPanel.BORDER_X + 1;
        }
        //ball has hit the bottom of border
        if(y + 2 * radius >= SimulationPanel.BORDER_HEIGHT+ SimulationPanel.BORDER_Y){
            vy = -vy;
            y = (SimulationPanel.BORDER_HEIGHT + SimulationPanel.BORDER_Y) - (2 * radius) - 1;
        }
        //ball has hit the top of border
        if(y  <= SimulationPanel.BORDER_Y){
            vy = Math.abs(vy);
            y = SimulationPanel.BORDER_Y + 1;
        }
    }
    /*
        string representation of a ball object
     */
    public String toString(){
        return "center_x: " + center_x + " center_y: " + center_y + " vx: " + vx + " vy: " + vy;
    }

}
/*
    superclass for panels used by the server and client
*/
class SimulationPanel extends JPanel{
    public static final int BORDER_HEIGHT = 500;
    public static final int BORDER_WIDTH = 500;
    public static final int BORDER_X = 100;
    public static final int BORDER_Y = 100;
    public static final int DELAY = 10;
}
/*
    Panel that is displayed on the server
 */
class ServerPanel extends SimulationPanel implements ActionListener, Runnable{
    private ArrayList<Ball> balls;
    javax.swing.Timer timer;
    Random r;
    ServerSocket serverSocket;
    ArrayList<Socket> clients;
    /*
        sets up the ball objects in the simulation and created a timer that determines how frequently the
        simulation updates
     */
    public ServerPanel(){
        balls = new ArrayList<Ball>();
        timer=new Timer(DELAY, this);
        timer.start();
        r = new Random();
        serverSocket = null;
        clients = new ArrayList<Socket>();
    }
    /*
        adds a ball object to the simulation
     */
    public void addBall(Ball b){
        balls.add(b);
    }
    /*
        updates each of the balls locations, checks to see if they are colliding with another ball,
        if so the collision is handled by the collide method
     */
    private void update(){
        ObjectOutputStream out = null;
        ObjectInputStream in = null;



        for(Ball b : balls){
            b.update();
        }

        for(Socket s : clients){
            try {
                out = new ObjectOutputStream(s.getOutputStream());
                out.writeObject(balls);
            }catch(IOException e){
                e.printStackTrace();
            }
        }

        for(int i = 0; i < balls.size(); i++){
            for(int j = i + 1; j < balls.size(); j++){
                if(balls.get(i).isColliding(balls.get(j))){
                    balls.get(i).collide(balls.get(j));
                }
            }
        }
    }
    /*
        takes the first ball in the ball object list and gives it a random x and y velocity
     */
    public void shootBallOne(){
        if(balls.size() >= 1) {
            balls.get(0).vx = r.nextInt(5 + 1 + 5) - 5;
            balls.get(0).vy = r.nextInt(5 + 1 + 5) - 5;
        }
    }
    /*
        takes the second ball in the ball object list and gives it a random x and y velocity
     */
    public void shootBallTwo(){
        if(balls.size() >= 2) {
            balls.get(1).vx = r.nextInt(5 + 1 + 5) - 5;
            balls.get(1).vy = r.nextInt(5 + 1 + 5) - 5;
        }
    }
    /*
        listener to respond to the timer
     */
    public void actionPerformed(ActionEvent ev) {
        if (ev.getSource() == timer) {
            update();
            repaint();
        }
    }
    /*
        paints each of the balls in the ball object list to the screen
     */
    public void paint(Graphics g) {
            g.drawRect(BORDER_X, BORDER_Y, BORDER_WIDTH, BORDER_HEIGHT);
            for(Ball b : balls){
                b.draw(g);
            }
    }

    public void run() {
        try {
            serverSocket = new ServerSocket(5003);
        }catch (IOException e){
            e.printStackTrace();
        }

        int id = 0;
        while (true) {
            try {
                Socket clientSocket = serverSocket.accept();
                clients.add(clientSocket);
            }catch(IOException e){
                e.printStackTrace();
                System.err.println("Error while accepting a client connection!");
            }
        }
    }
}
/*
    panel that is used for displaying the simulation in the client
 */
class ClientPanel extends SimulationPanel implements Runnable{
    javax.swing.Timer timer;
    Socket socket;
    ObjectOutputStream out;
    ObjectInputStream in;
    ArrayList<Ball> balls;

    public ClientPanel(){
        //timer=new Timer(SimulationPanel.DELAY, this);
        //timer.start();
        socket = null;
        out = null;
        in = null;
        balls = null;

    }

    public void paint(Graphics g) {
        g.drawRect(BORDER_X, BORDER_Y, BORDER_WIDTH, BORDER_HEIGHT);
        if (balls != null) {
            for (Ball b : balls) {
                b.draw(g);
            }
        }
    }

    public void run(){
        try {
            socket = new Socket("127.0.0.1", 5003);
        } catch (Exception e) {
            e.printStackTrace();
        }

        while(true){
            try {
                in = new ObjectInputStream(socket.getInputStream());
                balls = (ArrayList<Ball>) in.readObject();
                repaint();
            }catch(ClassNotFoundException e){
                e.printStackTrace();
            }catch(IOException e){
                e.printStackTrace();
            }
        }
    }
}
/*
    constructs the GUI, listens for network connections, listens for button events
 */
public class Simulation {

    public static final int FRAME_HEIGHT = 700;
    public static final int FRAME_WIDTH = 900;

    public static void main(String[] args) {
        final JFrame frame = new JFrame();
        final ServerPanel p = new ServerPanel();
        frame.setSize(FRAME_WIDTH, FRAME_HEIGHT);
        frame.setTitle("Physics Simulation");


        JPanel buttonPanel = new JPanel();
        final JButton startServerButton = new JButton("Start Network Server");
        JButton startClientButton = new JButton("Start Network Client");
        JButton stopButton = new JButton("Stop");
        JButton shootBallOne = new JButton("Shoot Ball 1");
        JButton shootBallTwo = new JButton("Shoot Ball 2");
        JButton addBallButton = new JButton("Add a ball");
        final JTextField massField = new JTextField("mass", 4);


        p.addBall(new Ball(200,200));
        p.addBall(new Ball(200,200));


        shootBallOne.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                p.shootBallOne();
            }
        });

        shootBallTwo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                p.shootBallTwo();
            }
        });

        startClientButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e) {
                JFrame clientFrame = new JFrame();

                JPanel buttonPanel = new JPanel();
                JButton addBallButton = new JButton("Add a ball");
                final JTextField massField = new JTextField("mass", 3);
                buttonPanel.add(addBallButton);
                buttonPanel.add(massField);

                ClientPanel clientPanel = new ClientPanel();
                clientFrame.add(buttonPanel, BorderLayout.NORTH);
                clientFrame.add(clientPanel);
                clientFrame.setSize(FRAME_WIDTH, FRAME_HEIGHT);
                clientFrame.setTitle("Client");

                addBallButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        try{
                            Integer mass = Integer.parseInt(massField.getText());
                            if(mass < 15 || mass > 30){
                                throw new NumberFormatException();
                            }
                            try {
                                Socket s = new Socket("127.0.0.1", 5003);
                                ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
                                out.writeObject(mass);
                            }catch(IOException x){
                                x.printStackTrace();
                            }
                        }catch(NumberFormatException x){
                            JOptionPane.showMessageDialog(frame, "Mass field should be an integer 15 <= x <= 30");
                        }
                    }
                });

                Thread clientThread = new Thread(clientPanel);
                clientThread.start();
                clientFrame.setVisible(true);
            }
        });

        startServerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Thread serverThread = new Thread(p);
                serverThread.start();
                startServerButton.removeActionListener(this);
            }
        });

        stopButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });

        addBallButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try{
                    int mass = Integer.parseInt(massField.getText());
                    if(mass < 15 || mass > 30){
                        throw new NumberFormatException();
                    }
                    p.addBall(new Ball(200, 200, 2, 2, mass));
                }catch(NumberFormatException x){
                    JOptionPane.showMessageDialog(frame, "Mass field should be an integer 15 <= x <= 30");
                }
            }
        });

        buttonPanel.add(startServerButton);
        buttonPanel.add(startClientButton);
        buttonPanel.add(stopButton);
        buttonPanel.add(shootBallOne);
        buttonPanel.add(shootBallTwo);
        buttonPanel.add(addBallButton);
        buttonPanel.add(massField);

        frame.add(p);
        frame.add(buttonPanel, BorderLayout.NORTH);

        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);



    }
}

