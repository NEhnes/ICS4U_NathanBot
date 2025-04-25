package bots;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;

import arena.BattleBotArena;
import arena.BotInfo;
import arena.Bullet;
//
// Base Bot class used to build your own bot

/**
 * Usefull commands
 * BattleBotArena.SEND_MESSAGE; Sends a message from the message array
 * BattleBotArena.UP,BattleBotArena.DOWN,BattleBotArena.LEFT,BattleBotArena.RIGHT
 * Makes the bot move in a direction
 * BattleBotArena.FIREUP,BattleBotArena.FIREDOWN,BattleBotArena.FIRERIGHT,BattleBotArena.FIRELEFT
 * current is a variable that stores the current image being drawn.
 * set current to left, right, down or up to change directional image to be
 * drawn.
 **/
public class NathanBot2 extends Bot {
   /**
    * My name
    */
   String name = null;

   /**
    * My next message or null if nothing to say
    */
   String nextMessage = null;

   /**
    * Array of happy drone messages
    */
   private String[] messages = { "YOOOOOO", "Drake > Kendrick", "I am content", "I like to vaccuum",
         "La la la la la...", "I like squares" };

   /**
    * Image for drawing
    */
   Image angry, front, left, right, danger, currentImage;

   /**
    * For deciding when it is time to change direction
    */
   private static int counter = 0;
   private static int lastShotTicks = 0;
   private static double currentX, currentY; // LEAVE AS GLOBAL!
   private static double targetX, targetY = 300; // leave as global
   private static Bullet[] privBullets; // leave as global
   private static Bullet closestBullet; // leave as global
   private static double closestDist = 1000; // leave as global, for time being
   private static boolean attackEnabled = false;
   private static int attackTicks = 0;
   private static boolean attackDir;
   private static int attackDuration = 35;
   private static boolean inDanger = false;

   /**
    * Current move
    */
   private int move = BattleBotArena.RIGHT;

   /**
    * My last location - used for detecting when I am stuck
    */
   private double lastX, lastY;
   private double lastTargetX, lastTargetY;
   private static int stuckTicks = 0;

   /**
    * Draw the current Bot image
    */
   public void draw(Graphics g, int x, int y) {
      g.drawImage(currentImage, x, y, Bot.RADIUS * 2, Bot.RADIUS * 2, null);
   }

   /**
    * ##### Psuedo Logic #####
    * 
    * - update global copy of my position variables
    * - update global copy of bullets array
    * 
    * - determine whether in survival mode or not
    * - when greater than __ bots alive, play it safer
    * 
    * - dodge any incoming bullets
    * - does not run if midway through attack --> can change in dodge func. later
    * - interferes
    * 
    * - update the target bot
    * 
    * - run attacking stuff
    * - end if over
    * - continue attack in progress
    * 
    * - checks if bot is aligned for an opportunistic kill
    * 
    * - adjusts target position relative to target bot
    * - in preparation for attack
    * 
    * - checks whether conditions are met to commence an attack
    * 
    * - if nothing else was already returned, move towards target bot
    */

   /*******************************************************************************
    ********* controls movement, messages, and firing ***********
    *******************************************************************************/

   // _______________ADD BORDER / DEAD BOT AVOIDANCE______________
   // __________IF # of live bots > 8, SURVIVAL MODE_______
   // _______else, enable attacking__________
   public int getMove(BotInfo me, boolean shotOK, BotInfo[] liveBots, BotInfo[] deadBots, Bullet[] bullets) {

      // update my position
      currentX = (int) me.getX();
      currentY = (int) me.getY();

      // update bullet array - my own array so I dont have to pass it around
      privBullets = bullets;

      // disable survival mode - working
      if (liveBots.length <= 8) {
         // enable attack
      }

      // doesn't dodge during attackEnabled --- adjust later so that xdirection is
      // static in attackEnabled, y still oddges though
      if (!attackEnabled) {
         move = DodgeSequence();
         if (move != 0) {
            RunCounters();
            return move;
         }
         inDanger = false;
      }

      // retrieves nearest bot
      int targetBotIndex = TargetBotIndex(liveBots);
      // update target bot pos
      targetX = liveBots[targetBotIndex].getX();
      targetY = liveBots[targetBotIndex].getY();

      // end attack if is over
      if (attackTicks > attackDuration - 1) {
         attackTicks = 0;
         attackEnabled = false;
      }

      // continue attack sequence, returns 0 if conditions not met
      move = Attack(shotOK);
      if (move != 0) {
         RunCounters();
         return move;
      }

      // checks if another bot is aligned in x/y lines
      move = CheckForShot(me, liveBots);
      if (move != 0) {
         lastShotTicks = 0;
         RunCounters();
         return move;
      }

      // sets target attack position, on a diagonal to target bot
      int xTargetingGap = 30;
      int yTargetingGap = 95;
      targetX += (targetX > currentX || currentX < 40) ? -xTargetingGap : xTargetingGap;
      targetY += (targetY > currentY || currentY > 440) ? -yTargetingGap : yTargetingGap;

      // check if in attack position, commence attack
      if (!attackEnabled) { // does not run if attack is in progress
         int tolerance = 40;
         // if in the correct attack position, ± 20px tolerance
         if ((targetX - tolerance <= currentX && targetX + tolerance >= currentX)
               && (targetY - tolerance <= currentY && targetY + tolerance >= currentY) && shotOK && lastShotTicks < 40) {
            attackEnabled = true;
            attackDir = (currentX < targetX) ? false : true;
            attackTicks = 0;
         } else {
            attackEnabled = false;
         }
      }

      move = MoveTo(me, targetX, targetY);

      int backupMove = move;

      move = AvoidDeadBots(move, deadBots);

      if(move == 0){
         move = backupMove;
      }

      if (liveBots.length > 10){
         move = MoveTo(me, 100, 380);
      }

      RunCounters();
      return move;
   }

   // ************************************************
   // ***********---LOGIC---END---********************
   // ***********--FUNCS---BELOW--********************
   // ************************************************

   // targetX, targetY are global
   private int AvoidDeadBots(int move, BotInfo[] deadBots) {

      //find closest dead bot index
      if (deadBots.length != 0) {
         int botIndex = 0;
         closestDist = 1000;
         for (int i = 0; i < deadBots.length; i++) {
            BotInfo bot = deadBots[i];
            targetX = (int) bot.getX();
            targetY = (int) bot.getY();
            if (ManhattanDistance(targetX, targetY) < closestDist) {
               closestDist = ManhattanDistance(targetX, targetY);
               botIndex = i;
            }
         }
         // System.out.println("closestDeadBot: " + deadBots[botIndex].getName());

         if (closestDist < 50){
            return (targetY < currentY) ? BattleBotArena.UP : BattleBotArena.DOWN;
         }
      }
      

      // if targetX, targetY falls within dead bot radius
      // change target position to

      return 0;
   }

   // ----------all good------------
   private int Attack(boolean shotOK) {
      if (attackEnabled == true && attackTicks < attackDuration) {
         attackTicks++;
         // if can shoot
         if (attackTicks > 10 && counter % 10 == 0 && counter != 0 && shotOK) {
            return (targetY < currentY) ? BattleBotArena.FIREUP : BattleBotArena.FIREDOWN;
         }
         // otherwise move
         else {
            return (attackDir) ? BattleBotArena.LEFT : BattleBotArena.RIGHT;
         }
      }
      return 0;
   }

   // -------------------all good-----------
   private int DodgeSequence() {
      // Only trigger dodge sequence if there are active bullets
      if (privBullets.length != 0) {

         // find closest bullet, calc dist.
         closestBullet = privBullets[GetClosestBullet()[0]];
         double closestBulletDist = ManhattanDistance(closestBullet.getX(), closestBullet.getY());

         // if in danger, move
         if (closestBulletDist <= 80 && InMyDirection() == true) {
            // currentImage = up; // danger image
            move = DodgeBullet(closestBullet);
            inDanger = true;
            if (move != 0) {
               return move;
            }
         }
      }
      return 0;
   }

   // SEEMS LIKE IS WORKING
   private int DodgeBullet(Bullet closestbullet) { // issue wth dodging bullets from left

      int padding = 35;
      // if moving up/down
      if (closestbullet.getYSpeed() != 0) {
         // if aligned X
         if (Math.abs((currentX + Bot.RADIUS) - closestBullet.getX()) < Bot.RADIUS + padding) {
            // System.out.println("X DANGER");
            // if x danger
            if (closestBullet.getX() > currentX) {
               return BattleBotArena.LEFT;
            } else {
               return BattleBotArena.RIGHT;
            }
         }
      }
      // if moving left/right
      else {
         // if aligned Y
         if (Math.abs((currentY + Bot.RADIUS) - closestBullet.getY()) < Bot.RADIUS + padding) {
            // System.out.println("Y DANGER");
            // if y danger
            if (closestBullet.getY() > currentY) {
               return BattleBotArena.UP;
            } else {
               return BattleBotArena.DOWN;
            }
         }

      }
      return 0;
   }

   // -------------------ALL GOOD----------------
   private int CheckForShot(BotInfo me, BotInfo[] liveBots) {
      int shootingInterval = 20;
      for (int i = 0; i < liveBots.length; i++) {
         // if aligned vertically
         if (Math.abs(me.getX() - liveBots[i].getX()) < Bot.RADIUS) {
            // if has not fired recently
            if (lastShotTicks > shootingInterval) {
               // if below target
               if (me.getY() > liveBots[i].getY()) {
                  return BattleBotArena.FIREUP;
               } else // above bot
               {
                  return BattleBotArena.FIREDOWN;
               }
            }
         }

         // if aligned horizontally
         if (Math.abs(me.getY() - liveBots[i].getY()) < Bot.RADIUS) {
            if (lastShotTicks > shootingInterval) {
               if (me.getX() > liveBots[i].getX()) {
                  return BattleBotArena.FIRELEFT;
               } else {
                  return BattleBotArena.FIRERIGHT;
               }
            }
         }
      }
      return 0;
   }

   // -------------------ALL GOOD----------------
   private void RunCounters() { // in a function so that early returns dont skip logic
      // set images
      switch (move) {
         case 1 -> currentImage = front;
         case 2 -> currentImage = front;
         case 3 -> currentImage = left;
         case 4 -> currentImage = right;
         default -> currentImage = front;
      }
      if (inDanger){
         currentImage = danger;
      }
      if (attackEnabled){
         currentImage = angry;
      }

      counter++;
      counter = (counter > 100) ? 0 : counter; // reset counter every 100 ticks

      lastShotTicks++;

      lastX = currentX;
      lastY = currentY;
      lastTargetX = targetX;
      lastTargetY = targetY;
   }

   // -------------------ALL GOOD----------------
   private static double ManhattanDistance(double objX, double objY) {
      return Math.abs(currentX - objX) + Math.abs(currentY - objY);
   }

   // -------------------ALL GOOD----------------
   private int oppositeMove(int move) {
      switch (move) {
         case BattleBotArena.LEFT -> move = BattleBotArena.RIGHT;
         case BattleBotArena.RIGHT -> move = BattleBotArena.LEFT;
         case BattleBotArena.UP -> move = BattleBotArena.DOWN;
         case BattleBotArena.DOWN -> move = BattleBotArena.UP;
      }
      return move;
   }

   // returns an array of two values, the nearest bullet index @ [0], second
   // nearest index @ [1]
   // -------------------ALL GOOD----------------
   private static int[] GetClosestBullet() {
      int bulletIndex = 0;
      int secondClosestIndex = 0;
      int[] orderedBullets = { 0 };
      double closestBulletDist = 10000;

      for (int i = 0; i < privBullets.length; i++) {
         Bullet tempBullet = privBullets[i];
         targetX = tempBullet.getX();
         targetY = tempBullet.getY();
         if (ManhattanDistance(targetX, targetY) < closestBulletDist) {
            secondClosestIndex = bulletIndex;
            closestBulletDist = ManhattanDistance(targetX, targetY);
            bulletIndex = i;
         }
      }

      if (privBullets.length > 1) {
         orderedBullets = new int[] { bulletIndex, secondClosestIndex };
      } else {
         orderedBullets = new int[] { bulletIndex };
      }
      return orderedBullets;
   }

   // moves to a point, prioritizing x/y in a way that moves as a diagonal
   // -------------------ALL GOOD----------------
   private static int MoveTo(BotInfo me, double targetX, double targetY) {

      if (Math.abs(currentX - targetX) > Math.abs(currentY - targetY)) {
         if (currentX < targetX) {
            return BattleBotArena.RIGHT;
         } else if (currentX > targetX) {
            return BattleBotArena.LEFT;
         }
      } else {
         if (currentY < targetY) {
            return BattleBotArena.DOWN;
         } else {
            return BattleBotArena.UP;
         }
      }
      return 0;
   }

   // -------------------ALL GOOD----------------
   private static int TargetBotIndex(BotInfo[] liveBots) {
      int botIndex = 0;
      closestDist = 1000;
      for (int i = 0; i < liveBots.length; i++) {
         BotInfo bot = liveBots[i];
         targetX = (int) bot.getX();
         targetY = (int) bot.getY();
         if (ManhattanDistance(targetX, targetY) < closestDist) {
            closestDist = ManhattanDistance(targetX, targetY);
            botIndex = i;
         }
      }
      // System.out.println("Targeted bot is: " + liveBots[botIndex].getName());
      return botIndex;
   }

   // -------------------ALL GOOD----------------
   private boolean InMyDirection() {
      int padding = 20;

      // if aligned X
      if (Math.abs(currentX - closestBullet.getX()) < Bot.RADIUS + padding) {
         // if moving in y direction
         if (closestBullet.getYSpeed() != 0) {
            // if above and bullet moving up
            if (currentY < closestBullet.getY() && closestBullet.getYSpeed() < 0) {
               return true;
            }
            // if below and bullet moving down
            else if (currentY > closestBullet.getY() && closestBullet.getYSpeed() > 0) {
               return true;
            }
         }
      }

      // if aligned Y
      if (Math.abs(currentY - closestBullet.getY()) < Bot.RADIUS + padding) {
         // if moving in x direction
         if (closestBullet.getXSpeed() != 0) {
            // if left and bullet moving left
            if (currentX < closestBullet.getX() && closestBullet.getXSpeed() < 0) {
               return true;
            }
            // if right and bullet moving right
            else if (currentX > closestBullet.getX() && closestBullet.getXSpeed() > 0) {
               return true;
            }
         }
      }
      return false;
   }

   /**
    * Construct and return my name
    * 
    */
   public String getName() {
      if (name == null)
         name = "new_ver";
      return name;
   }

   /**
    * Team Arena!
    */
   public String getTeamName() {
      return "Arena";
   }

   /**
    * Set value for the start of each round
    */
   public void newRound() {
      currentImage = front;
      // this is a good place to do things like:
      // - reset any custom counters or list
      // - set a starting direction and image
   }

   /**
    * Image names
    */
   public String[] imageNames() {
      String[] images = { "angryGregorBot.png", "gregorBotFront.png", "gregorBotLeft.png", "gregorBotRight.png", "dangerBotIcon.png"};
      return images;
   }

   /**
    * Store the loaded images
    */
   public void loadedImages(Image[] images) {
      if (images != null) {
         currentImage = angry = images[0];
         front = images[1];
         left = images[2];
         right = images[3];
         danger = images[4];
      }
   }

   /**
    * Send my next message and clear out my message buffer
    */
   public String outgoingMessage() {
      String msg = nextMessage;
      nextMessage = null;
      return msg;
   }

   /**
    * Required abstract method
    */
   public void incomingMessage(int botNum, String msg) {

   }

}