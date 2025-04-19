package bots;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;

import arena.BattleBotArena;
import arena.BotInfo;
import arena.Bullet;

import java.util.*; // needed to create a list


// Base Bot class used to build your own bot

/**
 * Usefull commands
 * BattleBotArena.SEND_MESSAGE; Sends a message from the message array
 * BattleBotArena.UP,BattleBotArena.DOWN,BattleBotArena.LEFT,BattleBotArena.RIGHT Makes the bot move in a direction
 * BattleBotArena.FIREUP,BattleBotArena.FIREDOWN,BattleBotArena.FIRERIGHT,BattleBotArena.FIRELEFT
 * current is a variable that stores the current image being drawn.
 * set current to left, right, down or up to change directional image to be drawn.
 **/
public class theoBot extends Bot {
   /**
    *My name
    */
   String name;


   /**
    * My next message or null if nothing to say
    */
   String nextMessage = null;
   
   /**
    * Array of happy drone messages
    */
   private String[] messages;
   
   /**
    * Image for drawing
    */
   Image up, down, left, right, currentImage;
   
   /**
    * For deciding when it is time to change direction
    */
   private int counter = 50;
   
   /**
    * Current move
    */
   private int move = BattleBotArena.RIGHT;
   
   /**
    * My last location - used for detecting when I am stuck
    */
   private double x, y;
   
   /**
    * Draw the current Bot image
    */
   public void draw(Graphics g, int x, int y) {
      g.drawImage(currentImage, x, y, Bot.RADIUS*2, Bot.RADIUS*2, null);
   }
   
   // used to track what bots I have messaged

   public ArrayList<String> messagedBots = new ArrayList<String>();
   int deadCount = 0;
   
   
   
   /*******************************************************************************
    *********          controls movement, messages, and firing          ***********
    *******************************************************************************/
   
   public int getMove(BotInfo me, boolean shotOK, BotInfo[] liveBots, BotInfo[] deadBots, Bullet[] bullets) {
     
     //
     // 1. SHOOT - shoot every 25 frames if I can either left or right, (do not check for further options)     
     //
     
     counter++;  
      
     if (counter % 25 == 0 && shotOK)
     {
       // fire left it I am closer to the right edge
       if (me.getX() > BattleBotArena.RIGHT_EDGE/2)
       {
          return BattleBotArena.FIRELEFT;
       }
       else // fire right if I am closer to the left edge
       {
         return BattleBotArena.FIRERIGHT;
       }
     }
     
     //
     // 2a. MESSAGE - check if any bot dies and send a message to that bot, (do not check for further options)
     //
     
     if(deadCount < deadBots.length) 
     {
       deadCount++;
       nextMessage = "Loser!! " + deadBots[deadBots.length-1].getName();
       return BattleBotArena.SEND_MESSAGE;
     }
     
     //
     // 2b. MESSAGE - check if a bot is close to me and send a message, (do not check for further options)
     //
     
     for (int i = 0; i < liveBots.length; i++)
     {
        double xDiff = Math.abs(liveBots[i].getX() - me.getX());
        double yDiff = Math.abs(liveBots[i].getY() - me.getY());
        
        if (xDiff < 30 || yDiff < 30)
        {
           if (!messagedBots.contains(liveBots[i].getName()))
           {
              messagedBots.add(liveBots[i].getName());
              nextMessage = "Get away from me " + liveBots[i].getName();
              return BattleBotArena.SEND_MESSAGE;
           } 
        }
     }
     
     //
     // 3. MOVE - set next move if stuck or if counter has reached 0, and update image as appropriate
     //
     
     if (counter == 0 || (me.getX() == x && me.getY() == y))
     {
        if (move == BattleBotArena.UP)
        {
           currentImage = left;
           move = BattleBotArena.LEFT;
        }
        else if (move == BattleBotArena.LEFT)
        {
           currentImage = down;
           move = BattleBotArena.DOWN;
        }
        else if (move == BattleBotArena.DOWN)
        {
           currentImage = right;
           move = BattleBotArena.RIGHT;
        }
        else if (move == BattleBotArena.RIGHT)
        {
           currentImage =up;
           move = BattleBotArena.UP;
        }
        
        counter = 50+(int)(Math.random()*100);
     }

     // update my record of my most recent position
     x = me.getX();
     y = me.getY();
     
     return move;
   }
   
   /**
    * Construct and return my name
    */
   public String getName()
   {
      if (name == null)
         name = "AAAA";
      return name;
   }
   
   /**
    * Team Arena!
    */
   public String getTeamName() {
     return "Arena";
   }
   
   /**
    * Pick a random starting direction
    */
   public void newRound() {
     messagedBots.clear();
     move = BattleBotArena.RIGHT;
     currentImage = right;
   }
   
   /**
    * Image names
    */
   public String[] imageNames()
   {
      String[] images = {"drone_up.png","drone_down.png","drone_left.png","drone_right.png"};
      return images;
   }
   
   /**
    * Store the loaded images
    */
   public void loadedImages(Image[] images)
   {
      if (images != null)
      {
         currentImage = up = images[0];
         down = images[1];
         left = images[2];
         right = images[3];
      }
   }
   
   /**
    * Send my next message and clear out my message buffer
    */
   public String outgoingMessage()
   {
      String msg = nextMessage;
      nextMessage = null;
      return msg;
   }
   
   /**
    * Required abstract method
    */
   public void incomingMessage(int botNum, String msg)
   {
      
   }
   
}
