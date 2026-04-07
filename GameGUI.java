/*
* Project 4.1.5: Escape Room Revisited
* 
* V1.0
* Copyright(c) 2024 PLTW to present. All rights reserved
*/
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;
import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

/**
 * A game where a player maneuvers around a gameboard to answer
 * riddles or questions, collecing prizes with correct answers.
 */
public class GameGUI extends JComponent implements KeyListener
{
  static final long serialVersionUID = 415L;

  // constants for gameboard confg
  private static final int WIDTH = 510;
  private static final int HEIGHT = 360;
  private static final int SPACE_SIZE = 60;
  private static final int GRID_W = 8;
  private static final int GRID_H = 5;
  private static final int MOVE = 60;

  // Instance variable to keep track of max level
  private static final int MAX_LEVEL = 5;

  // Quiz instance variables to keep track of questions and progress
  private ArrayList<ArrayList<String>> quiz;
  private int questionIndex = 0;
  private int questionsAnswered = 0;

  // frame and images for gameboard
  private JFrame frame;
  private Image bgImage;
  private Image prizeImage;
  private Image player;
  private Image playerQ;

  // player config
  private int currX = 15; 
  private int currY = 15;
  private boolean atPrize;
  private Point playerLoc;
  private int playerSteps;

  // walls, player level, and prizes
  private int numWalls = 8;
  private int playerLevel = 1;
  private Rectangle[] walls; 
  private Rectangle[] prizes;

  // scores, sometimes awarded as (negative) penalties
  private int goodMove = 1;
  private int offGridVal = 5; // penalty 
  private int hitWallVal = 5;  // penalty 
  private int correctAns = 10;
  private int wrongAns = 7; // penalty 
  private int score = 0; 

  /**
   * Constructor for the GameGUI class.
   * 
   * Gets the player level and the questions/answers for the game 
   * from two files on disk. Creates the gameboard with a background image,
   *  walls, prizes, and a player.
   */
  public GameGUI() throws IOException
  {
    newPlayerLevel();
    createQuiz();
    createBoard();
  }

   /**
   * Create array of questions and answers from the quiz.csv file.
   * 
   * @preconditon: The CSV file contains at least playerLevel number of questions.
   * (It may contain more unused questions.)
   * 
   * @postconditon: A 2D array is populated with one question and one answer per row.
   * 
   * @throws IOException
   */

  //createQuiz method to read questions from quiz.csv and store them (3rd requirement)
  private void createQuiz() throws IOException
  {
    //Initialize ArrayList
    quiz = new ArrayList<ArrayList<String>>();

    //Iterate through lines in quiz.csv
    Scanner sc = new Scanner(new File("quiz.csv"));
    while (sc.hasNextLine())
    {
      //Parse each line into tokens, and into a question ArrayList
      String line = sc.nextLine();

      //Each line consists of a type (1 for short answer, 2 for multiple choice), point value, question, and answer(s)
      //(Multiple choice questions have 4 choices, with the correct answer surrounded by < and >)

      String[] tokens = line.split(",");

      ArrayList<String> question = new ArrayList<String>();
      for (String s: tokens)
      {
        question.add(s);
      }

      //Add the question ArrayList into a random index of quiz, to randomize quiz questions without repetition (3rd customization)
      int randomIndex = (int)(Math.random() * (quiz.size() + 1));
      quiz.add(randomIndex, question);
    }
    sc.close();
  }

  /**
   * Update the instance variables playerLevel and numWalls
   * based on user level stored in the level.csv file.
   * 
   * @preconditon: The CSV file must contain a level of at least 1.
   * @throws IOException
   */

  //newPlayerLevel method to set player level to saved level in level.csv and increase # of walls (6th requirement)
  private void newPlayerLevel() throws IOException
  {
    /* your code here */
    Scanner sc = new Scanner(new File("level.csv"));

    int level = sc.nextInt();
    
    playerLevel = level;
    numWalls += level;
  }

  /**
   * Manage the input from the keybard: arrow keys, wasd keys, p, q, and h keys.
   * Key input is not case sensivite.
   * 
   * @param the key that was pressed
   */
  @Override
  public void keyPressed(KeyEvent e)
  {
    //Modified keypress P to pickup coin/ask question (4th requirement)
    if (e.getKeyCode() == KeyEvent.VK_P )
    {
      //Only pick up prize if there is a prize there
      if (atPrize)
      {
        //Get the current question and answer from quiz
        ArrayList<String> q = quiz.get(questionIndex);
        int type = Integer.parseInt(q.get(0));
        int points = Integer.parseInt(q.get(1));
        String question = q.get(2);
        
        String userAnswer = null;
        String correctAnswer = null;
        
        //If it's a short answer question, ask the question and get the correct answer from the csv
        if (type == 1)
        {
          userAnswer = askQuestion(question);
          correctAnswer = q.get(3).toLowerCase().trim();
        }
        //If it's a multiple choice question, format the question with the choices and get the correct answer from the csv (the one surrounded by < and >)
        else if (type == 2)
        {
          String choiceA = q.get(3).replace("<", "").replace(">", "");
          String choiceB = q.get(4).replace("<", "").replace(">", "");
          String choiceC = q.get(5).replace("<", "").replace(">", "");
          String choiceD = q.get(6).replace("<", "").replace(">", "");
          String formatted = question + "\nA. " + choiceA + "\nB. " + choiceB + "\nC. " + choiceC + "\nD. " + choiceD;
          userAnswer = askQuestion(formatted);
          correctAnswer = "";

          //Find the correct answer by looking for the choice with < and > around it
          for (int i = 3; i < 7; i++)
          {
            if (q.get(i).contains("<") && q.get(i).contains(">"))
            {
              correctAnswer = String.valueOf((char)('a' + (i - 3)));
              break;
            }
          }
        }
        
        //Pickup the prize and update score based on answer correct or not
        if (userAnswer != null && userAnswer.toLowerCase().trim().equals(correctAnswer))
        {
          pickupPrize();
          score += points;
          questionsAnswered++;
          showMessage("Correct! You collected " + points + " points.");
        }
        //Otherwise, decrease score
        else
        {
          score -= wrongAns;
          showMessage("Incorrect! You lost " + wrongAns + " points.");
        }
        
        //Move to the next question in quiz, loop over if at the end
        questionIndex = (questionIndex + 1) % quiz.size();
      }
      else
      {
        showMessage("There is no prize here to pick up!");
      }
    }

    //Modified keypress Q to quit game if all prizes collected, and show final score (5th requirement)
    if (e.getKeyCode() == KeyEvent.VK_Q)
    {
      int finalScore = score - playerSteps;

      if (questionsAnswered == playerLevel)
      {
        if (finalScore > 0)
        {
          showMessage("Congratulations! You have passed the level!");
          
          //Increase player level if they have a positive score, up to the max level
          if (playerLevel < MAX_LEVEL) playerLevel++;
        }
        else
        {
          showMessage("Unfortunately, you ended with a negative score, and will not advance!");
        }

        endGame();
      }
      else
      {
        showMessage("You cannot quit until you have collected all the prizes!");
      }
    }

    // H key: help
    if (e.getKeyCode() == KeyEvent.VK_H)
    {
      String msg = "Move player: arrows or WASD keys\n" + 
      "Jump: IJKL keys\n" +
      "Pickup prize: p\n" +
      "Quit: q\n" +
      "Help: h\n";
      showMessage(msg);
    }
    
    // Arrow and WASD keys: moved down, up, left or right
    if (e.getKeyCode() == KeyEvent.VK_DOWN || e.getKeyCode() == KeyEvent.VK_S )
    {
      score += movePlayer(0, MOVE);
    }
    if (e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_W)
    {
      score += movePlayer(0, -MOVE);
    }
    if (e.getKeyCode() == KeyEvent.VK_LEFT || e.getKeyCode() == KeyEvent.VK_A)
    {
      score += movePlayer(-MOVE, 0);
    }
    if (e.getKeyCode() == KeyEvent.VK_RIGHT || e.getKeyCode() == KeyEvent.VK_D)
    {
      score += movePlayer(MOVE, 0);
    }

    // IJKL keys: jump 2 spaces (4th customization)
    if (e.getKeyCode() == KeyEvent.VK_K)
    {
      score += movePlayer(0, 2*MOVE);
    }
    if (e.getKeyCode() == KeyEvent.VK_I)
    {
      score += movePlayer(0, -2*MOVE);
    }
    if (e.getKeyCode() == KeyEvent.VK_J)
    {
      score += movePlayer(-2*MOVE, 0);
    }
    if (e.getKeyCode() == KeyEvent.VK_L)
    {
      score += movePlayer(2*MOVE, 0);
    }
  } 

  /**
   * Manage the key release, checking if the player is at a prize.
   * 
   * @param the key that was pressed
   */
  @Override
  public void keyReleased(KeyEvent e) 
  { 
    checkForPrize();
  }

  /* override necessary but no action */
  @Override
  public void keyTyped(KeyEvent e) { }

  /**
  * Add player, prizes, and walls to the gameboard.
  */
  private void createBoard() throws IOException
  {    
    prizes = new Rectangle[playerLevel];
    createPrizes();

    walls = new Rectangle[numWalls];
    createWalls();

    bgImage = ImageIO.read(new File("grid.png"));
    prizeImage = ImageIO.read(new File("coin.png"));
    player = ImageIO.read(new File("player.png")); 
    playerQ = ImageIO.read(new File("playerQ.png")); 
    
    // save player location
    playerLoc = new Point(currX, currY);

    // create the game frame
    frame = new JFrame();
    frame.setTitle("EscapeRoom");
    frame.setSize(WIDTH, HEIGHT);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.add(this);
    frame.setVisible(true);
    frame.setResizable(false); 
    frame.addKeyListener(this);

    checkForPrize();

     showMessage("Welcome to the Escape Room. Press h to learn how to play.");
  }

  /**
   * Increment/decrement the player location by the amount designated.
   * This method checks for bumping into walls and going off the grid,
   * both of which result in a penalty.
   * 
   * @param incrx amount to move player in x direction
   * @param incry amount to move player in y direciton
   * 
   * @return penaly for hitting a wall or trying to go off the grid, goodMove otherwise
   */
  private int movePlayer(int incrx, int incry)
  {
      int newX = currX + incrx;
      int newY = currY + incry;

      // check if off grid horizontally and vertically
      if ( (newX < 0 || newX > WIDTH-SPACE_SIZE) || (newY < 0 || newY > HEIGHT-SPACE_SIZE) )
      {
        showMessage("You have tried to go off the grid!");
        return -offGridVal;
      }

      // determine if a wall is in the way
      for (Rectangle r: walls)
      {
        // this rect. location
        int startX =  (int)r.getX();
        int endX  =  (int)r.getX() + (int)r.getWidth();
        int startY =  (int)r.getY();
        int endY = (int) r.getY() + (int)r.getHeight();

        // (Note: the following if stmts could be written as huge conditional but who wants to look at that!?)
        // moving RIGHT, check to the right
        if ((incrx > 0) && (currX <= startX) && (startX <= newX) && (currY >= startY) && (currY <= endY))
        {
          showMessage("A wall is in the way.");
          return -hitWallVal;
        }
        // moving LEFT, check to the left
        else if ((incrx < 0) && (currX >= startX) && (startX >= newX) && (currY >= startY) && (currY <= endY))
        {
          showMessage("A wall is in the way.");
          return -hitWallVal;
        }
        // moving DOWN check below
        else if ((incry > 0) && (currY <= startY && startY <= newY && currX >= startX && currX <= endX))
        {
          showMessage("A wall is in the way.");
          return -hitWallVal;
        }
        // moving UP check above
        else if ((incry < 0) && (currY >= startY) && (startY >= newY) && (currX >= startX) && (currX <= endX))
        {
          showMessage("A wall is in the way.");
          return -hitWallVal;
        }     
      }

      // all is well, move player
      playerSteps++;
      currX += incrx;
      currY += incry;
      repaint();   
      return goodMove;
  }

  /**
   * Displays a dialog with a simple message and an OK button
   * 
   * @param str the message to show
   */
  private void showMessage(String str)
  {
    JOptionPane.showMessageDialog(frame,str );
  }

  /**
   * Display a dialog that asks a question and waits for an answer
   *
   * @param the question to display
   *
   * @return the text the user entered, null otherwise
   */
  private String askQuestion(String q)
  {
    return JOptionPane.showInputDialog(q.replace("\\n","\n") , JOptionPane.OK_OPTION);
  }

  /**
   * If there's a prize at the location, set atPrize to true and change player image
   *
   * @param w number of walls to create
   */
  private void checkForPrize()
  {
    double px = playerLoc.getX();
    double py = playerLoc.getY();

    for (Rectangle r: prizes)
    {
      if (r.contains(px, py))
      {
        atPrize = true;
        repaint();
        return;
      }
    }
    atPrize = false;
  }

  /**
   * Pickup a prize and score points. If no prize is in that location, it results in a penalty.
   */
  private void  pickupPrize()
  {
    double px = playerLoc.getX();
    double py = playerLoc.getY();

    for (Rectangle p: prizes)
    {
      // if location has a prize, pick it up
      if (p.getWidth() > 0 && p.contains(px, py))
      {
        p.setSize(0,0);
        atPrize = false;
        repaint();
      }
    }
  }

 /**
  * End the game, update and save the player level.
  */
  private void endGame() 
  {
    try {
      FileWriter fw = new FileWriter("level.csv");
      String s = playerLevel + "\n";
      fw.write(s);
      fw.close();
    } catch (IOException e)  { System.err.println("Could not level up."); }
  
    setVisible(false);
    frame.dispose();
  }

  /**
   * Add randomly placed prizes to be picked up.
   */
  private void createPrizes()
  {
    int s = SPACE_SIZE; 
    Random rand = new Random();
    for (int numPrizes = 0; numPrizes < playerLevel; numPrizes++)
    {
      int h = rand.nextInt(GRID_H);
      int w = rand.nextInt(GRID_W);
      Rectangle r = new Rectangle((w*s + 15),(h*s + 15), 15, 15);

       // get a rect. without a prize already there
       for (Rectangle p : prizes) {
        while (p != null && p.equals(r)) {
          h = rand.nextInt(GRID_H);
          w = rand.nextInt(GRID_W);
          r = new Rectangle((w*s + 15),(h*s + 15), 15, 15);
        }
      }
      prizes[numPrizes] = r;
    }
  }

  /**
   * Add walls to the board in random locations. Multiple walls may
   * be in the same locaiton.
   */
  private void createWalls()
  {
     int s = SPACE_SIZE; 
     Random rand = new Random();

     for (int n = 0; n < numWalls; n++)
     {
      int h = rand.nextInt(GRID_H);
      int w = rand.nextInt(GRID_W);

      Rectangle r;
      
      if (rand.nextInt(2) == 0) 
      {
        // vertical
        r = new Rectangle((w*s + s - 5),h*s, 8,s);
      }
      else
      {
        /// horizontal
        r = new Rectangle(w*s,(h*s + s - 5), s, 8);
      }

      walls[n] = r;
    }
  }

  /* 
   * Manage board elements with graphics buffer g.
   * For internal use - do not call directly, use repaint instead.
   */
  public void paintComponent(Graphics g)
  {
    super.paintComponent(g);
    Graphics2D g2 = (Graphics2D)g;

    // draw grid
    g.drawImage(bgImage, 0, 0, null);

    for (Rectangle p : prizes)
    {
      // pickedup prizes are 0 size so don't render
      if (p.getWidth() > 0) 
      {
      int px = (int)p.getX();
      int py = (int)p.getY();
      g.drawImage(prizeImage, px, py, null);
      }
    }

    // add walls
    for (Rectangle r : walls) 
    {
      g2.setPaint(new Color[]{Color.RED, Color.ORANGE, Color.YELLOW, Color.GREEN, Color.BLUE, Color.CYAN, Color.MAGENTA}[(int)(Math.random() * 7)]);
      g2.fill(r);
    }
   
    // draw player, saving its location
    if(atPrize)
    {
      g.drawImage(playerQ, currX, currY, 40,40, null);
    }
    else
    {
      g.drawImage(player, currX, currY, 40,40, null);
    }
    playerLoc.setLocation(currX, currY);

    //Something new: draw the score and steps taken (6th customization)
    g.setColor(Color.BLACK);
    g.drawString("Score: " + score, 10, 20);
    g.drawString("Steps: " + playerSteps, 10, 40);
  }

}
 