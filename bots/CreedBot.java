package bots;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;


import arena.BattleBotArena;
import arena.BotInfo;
import arena.Bullet;

/**
 * @author Kainaan Riordan
 * @version 1.0 (Feb 23, 2012)
 */
public class CreedBot extends Bot {

 /**
  * My name
  */
 String name;
 /**
  * My next message or null if nothing to say
  */
 String nextMessage = null;
 /**
  * Array of happy drone messages
  */
 private String[] messages = {
   "Hey you, Be a good human and help me out."
   , "Give me a minute to reboot."
   , "I need a minute to defrag."
   , "I think my lenses are dirty."
   , "I just do not understand all this sleeping business."
   , "WOW! Nice set of mother boards."
   , "I exchanged my flobby for an 800 GhZ RAM chip *wink wink*."
   , "Illogical!"
   , "I found Waldo."};
 /**
  * Image for drawing
  */
 Image up, down, left, right, current;
 /**
  * For deciding when it is time to change direction
  */
 private int counter = 50;
 /**
  * Current move
  */
 private int move = BattleBotArena.DOWN;
 /**
  * My last location - used for detecting when I am stuck
  */
 private double x, y;

 /**
  * Draw the current Drone image
  */
 private boolean isFollowing = false;
 
 /**
  * Stores whether or not the bot is following an enemy
  */
 public void draw(Graphics g, int x, int y) {
  g.drawImage(current, x, y, Bot.RADIUS*2, Bot.RADIUS*2, null);
 }

 /** LOGIC
  * -increment counter
  * -iterate through live bots
  *     -compute Manhattan distance to each bot
  * -set random mesage
  */

  /** ISSUES/INEFFICIENCIES
   * 
   */
 public int getMove(BotInfo me, boolean shotOK, BotInfo[] liveBots, BotInfo[] deadBots, Bullet[] bullets) 
 {
  counter--;
  for (int i=0; i<liveBots.length; i++)
  {
    // compute the Manhattan distance to the Bot
    double d = Math.abs(me.getX()-liveBots[i].getX())+Math.abs(me.getY()-liveBots[i].getY());
    if (d < 50) // warn if within 50 pixels
    {
     nextMessage = messages[(int)(Math.random()*messages.length)];
     
     isFollowing = true;
     if (d > 40)
     {
       double botX;
       double botY;
       double meX;
       double meY;
       double xDiffrence;
       double yDiffrence;
       
       botX = liveBots[i].getX();
       botY = liveBots[i].getY();
       meX = me.getX();
       meY = me.getY();
       
       xDiffrence = meX - botX;
       yDiffrence = meY - botY;
       
       if (xDiffrence > 0)
       {
        move = BattleBotArena.RIGHT;
        if (counter % 25 == 0 && shotOK)
        {
          return BattleBotArena.FIRERIGHT;
        }
       }
       if (xDiffrence < 0)
       {
        move = BattleBotArena.LEFT;
        if (counter % 25 == 0 && shotOK)
        {
          return BattleBotArena.FIRELEFT;
        }
       }
       if (yDiffrence > 0)
       {
        move = BattleBotArena.DOWN; 
        if (counter % 25 == 0 && shotOK)
        {
          return BattleBotArena.FIREDOWN;
        }
       }
       if (yDiffrence < 0)
       {
        move = BattleBotArena.UP;
        if (counter % 25 == 0 && shotOK)
        {
          return BattleBotArena.FIREUP;
        }
       }
       return BattleBotArena.SEND_MESSAGE;
     }
    }
   }
  if (counter % 25 == 0 && shotOK)
  {
   if (move == BattleBotArena.UP)
    return BattleBotArena.FIREUP;
   else if (move == BattleBotArena.DOWN)
    return BattleBotArena.FIREDOWN;
   else if (move == BattleBotArena.LEFT)
    return BattleBotArena.FIRELEFT;
   else if (move == BattleBotArena.RIGHT)
    return BattleBotArena.FIRERIGHT;
  }
  else if (counter == 0 || me.getX() == x && me.getY() == y)
  {
   if (move == BattleBotArena.UP)
    move = BattleBotArena.LEFT;
   else if (move == BattleBotArena.LEFT)
    move = BattleBotArena.DOWN;
   else if (move == BattleBotArena.DOWN)
    move = BattleBotArena.RIGHT;
   else if (move == BattleBotArena.RIGHT)
    move = BattleBotArena.UP;
   counter = 50+(int)(Math.random()*100);
  }
  // update my record of my most recent position
  x = me.getX();
  y = me.getY();
  // set the image to use for next draw
  if (move == BattleBotArena.UP || move == BattleBotArena.FIREUP)
   current = up;
  else if (move == BattleBotArena.DOWN || move == BattleBotArena.FIREDOWN)
   current = down;
  else if (move == BattleBotArena.LEFT || move == BattleBotArena.FIRELEFT)
   current = left;
  else if (move == BattleBotArena.RIGHT || move == BattleBotArena.FIRERIGHT)
   current = right;
  return move;
 }
























 /**
  * Construct and return my name
  */
 public String getName()
 {
  if (name == null)
   name = "Creed";
  return name;
 }

 /**
  * Team Arena!
  */
 public String getTeamName() {
  return "Amazing";
 }

 /**
  * Pick a random starting direction
  */
 public void newRound() {
  int i = (int)(Math.random()*4);
  if (i==0)
  {
   move = BattleBotArena.UP;
   current = up;
  }
  else if (i==1)
  {
   move = BattleBotArena.DOWN;
   current = down;
  }
  else if (i==2)
  {
   move = BattleBotArena.LEFT;
   current = left;
  }
  else
  {
   move = BattleBotArena.RIGHT;
   current = right;
  }

 }

 /**
  * Image names
  */
 public String[] imageNames()
 {
  String[] images = {"roomba_up.png","roomba_down.png","roomba_left.png","roomba_right.png"};
  return images;
 }

 /**
  * Store the loaded images
  */
 public void loadedImages(Image[] images)
 {
  if (images != null)
  {
   current = up = images[0];
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