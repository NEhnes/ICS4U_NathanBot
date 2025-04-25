/*
 * THIS IS MY COMPETITION BOT
 * STILL HAS A FEW BUGS, NOTABLY AN UNRELIABLE ATTACK SEQUENCE
 * COMMENTS ARE MOSTLY UPDATED, NOT 100% ACCURATE THOUGH -> NO TIME
 */

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
public class NathanBot extends Bot {
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
   private static double targetBotX, targetBotY = 300;
   private static Bullet[] privBullets; // leave as global
   private static boolean attackEnabled = false;
   private static int attackTicks = 0;
   private static boolean attackingLeft;
   private static int attackDuration = 35;
   private static boolean inDanger = false;

   /**
    * Current move
    */
   private int move = BattleBotArena.RIGHT;

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

   /*************************************************************
    ********* controls movement, messages, and firing ***********
    ************************************************************/
   public int getMove(BotInfo me, boolean shotOK, BotInfo[] liveBots, BotInfo[] deadBots, Bullet[] bullets) {

      //debugging - ignore
      if (counter % 95 == 0){
         System.out.println("Current Attack Ticks = " + attackTicks);
      }

      // update my position
      currentX = (int) me.getX();
      currentY = (int) me.getY();

      // update bullet array - my own array so I dont have to pass it around
      privBullets = bullets;

      // handles all dodging logic
      move = DodgeSequence();
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

      // find closest bot, and position myself relative to it
      SetTargeting(liveBots);

      if (counter % 51 == 0){
         System.out.println("targetBotY: " + targetBotY);
         System.out.println("currentY: " + currentY);
         System.out.println("targetBotX: " + targetBotX);
         System.out.println("currentX: " + currentX);
      }

      // if conditions met, commence attack
      attackEnabled = AttackConditionsMet(shotOK);

      // continue attack sequence, returns 0 if conditions not met
      move = Attack(shotOK);
      if (move != 0) {
         move = MoveTo(300, 100);
         RunCounters();
         return move;
      }

      move = AvoidDeadBots(move, deadBots);
      if (move != 0) {
         RunCounters();
         return move;
      }

      // set targeting move
      move = MoveTo(targetX, targetY);

      // default postition if >10 bots remain
      if (liveBots.length > 10) {
         move = MoveTo(100, 380);
      }

      if (counter % 50 == 0){

         nextMessage = (Math.random() < 0.5) ? messages [0] : messages [1];

         RunCounters();
         return BattleBotArena.SEND_MESSAGE;
      }

      RunCounters();
      return move;
   }

   // ************************************************
   // ***********---LOGIC---END---********************
   // ***********--FUNCS---BELOW--********************
   // ************************************************

















   // -------------------all good-----------
   private int DodgeSequence() {
      // Only trigger dodge sequence if there are active bullets
      if (privBullets.length != 0 && !attackEnabled) {

         Bullet closestBullet;

         // find closest bullet, calc dist.
         closestBullet = privBullets[GetClosestBullets()[0]];
         double closestBulletDist = ManhattanDistance(closestBullet.getX(), closestBullet.getY());

         // if in danger, move
         if (closestBulletDist <= 80 && InMyDirection(closestBullet) == true) {
            move = DodgeBullet(closestBullet);
            inDanger = true;
            if (move != 0) {
               return move;
            }
         }
      }
      inDanger = false;
      return 0;
   }

   // returns an array of two values, the nearest bullet index @ [0], second
   // nearest index @ [1]
   // -------------------ALL GOOD----------------
   private static int[] GetClosestBullets() {
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

   // ---------all good---------
   private int DodgeBullet(Bullet closestBullet) {

      // extra padding space for safety
      int padding = 35;

      // if bullet moving up/down
      // doesnt dodge L/R during attack phase
      if (closestBullet.getYSpeed() != 0) {
         // if aligned X
         if (Math.abs((currentX + Bot.RADIUS) - closestBullet.getX()) < Bot.RADIUS + padding) {
            // select dodge direction L/R
            return (closestBullet.getX() > currentX + Bot.RADIUS || currentX > 640) ? 
               BattleBotArena.LEFT : BattleBotArena.RIGHT;
         }
      }
      // if moving left/right
      else if (closestBullet.getXSpeed() != 0){
         // if aligned Y
         if (Math.abs((currentY + Bot.RADIUS) - closestBullet.getY()) < Bot.RADIUS + padding) {
            // select dodge direction U/D
            return (closestBullet.getY() > currentY + Bot.RADIUS || currentY > 440) ? 
               BattleBotArena.UP : BattleBotArena.DOWN;
         }

      }
      return 0;
   }

   private void SetTargeting(BotInfo[] liveBots) {
      // retrieves nearest bot
      int closestBotIndex = GetClosestBot(liveBots);
      // update target bot pos
      targetBotX = liveBots[closestBotIndex].getX();
      targetBotY = liveBots[closestBotIndex].getY();
      // sets target attack position, on a diagonal to target bot
      int xTargetingGap = 30;
      int yTargetingGap = 90;
      targetX = liveBots[closestBotIndex].getX();
      targetY = liveBots[closestBotIndex].getY();
      targetX += (targetX > currentX || currentX < 40) ? -xTargetingGap : xTargetingGap;
      targetY += (targetY > currentY || currentY > 440) ? -yTargetingGap : yTargetingGap;
   }

   // -------------------ALL GOOD----------------
   private static int GetClosestBot(BotInfo[] botArray) {
      int botIndex = 0;
      double closestBotDist = 1000;
      for (int i = 0; i < botArray.length; i++) {
         BotInfo bot = botArray[i];
         double botDistance = ManhattanDistance(bot.getX(), bot.getY());
         if (botDistance < closestBotDist) {
            closestBotDist = botDistance;
            botIndex = i;
         }
      }
      return botIndex;
   }

   private boolean AttackConditionsMet(boolean shotOK) {

      if (attackEnabled) {
         return true;
      }

      // if in the correct attack position, ± 10px tolerance
      int tolerance = 10;

      boolean okayX = Math.abs(currentX - targetX) < tolerance;
      boolean okayY = Math.abs(currentY - targetY) < tolerance;

      if (okayX && okayY && shotOK && lastShotTicks > 60) {
         attackingLeft = (currentX < targetBotX) ? false : true;
         attackTicks = 0;
         return true;
      } else {
         return false;
      }
   }

   // ----------all good------------
   private int Attack(boolean shotOK) {

      // end attack if is over
      if (attackTicks > attackDuration - 1) {
         attackEnabled = false;
      }

      if (attackEnabled == true && attackTicks < attackDuration) {
         attackTicks++;
         // if can shoot
         if (attackTicks > 10 && counter % 10 == 0 && counter != 0 && shotOK) {
            return (targetBotY < currentY) ? BattleBotArena.FIREUP : BattleBotArena.FIREDOWN;
         }
         // otherwise move
         else {
            return (attackingLeft) ? BattleBotArena.LEFT : BattleBotArena.RIGHT;
         }
      }
      return 0;
   }

   // targetX, targetY are global
   private int AvoidDeadBots(int move, BotInfo[] deadBots) {

      if (deadBots.length == 0) {
         return 0;
      }

      int closestDeadBot = GetClosestBot(deadBots);
      // System.out.println("closestDeadBot: " + deadBots[botIndex].getName());

      double closestDeadX = deadBots[closestDeadBot].getX();
      double closestDeadY = deadBots[closestDeadBot].getY();

      double closestDeadDist = ManhattanDistance(closestDeadX, closestDeadY);

      if (closestDeadDist < 50) {
         return (targetY < currentY) ? BattleBotArena.UP : BattleBotArena.DOWN;
      }
      return 0;
   }

   // -------------------ALL GOOD----------------
   private int CheckForShot(BotInfo me, BotInfo[] liveBots) {
      int shootingInterval = 10;
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

   // moves to a point, prioritizing x/y in a way that moves as a diagonal
   // -------------------ALL GOOD----------------
   private static int MoveTo(double targetX, double targetY) {

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
   private boolean InMyDirection(Bullet closestBullet) {

      // extra padding value for safety
      int padding = 20;

      boolean alignedX = Math.abs(currentX - closestBullet.getX()) < Bot.RADIUS + padding;
      boolean alignedY = Math.abs(currentY - closestBullet.getY()) < Bot.RADIUS + padding;

      if (alignedX) {
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
      if (alignedY) {
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

   // -------------------ALL GOOD----------------
   private static double ManhattanDistance(double objX, double objY) {
      return Math.abs(currentX - objX) + Math.abs(currentY - objY);
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
      if (inDanger) {
         currentImage = danger;
      }
      if (attackEnabled) {
         currentImage = angry;
      }

      counter++;
      counter = (counter > 100) ? 0 : counter; // reset counter every 100 ticks

      lastShotTicks++;
   }

   /**
    * Construct and return my name
    * 
    */
   public String getName() {
      if (name == null)
         name = "new_greg";
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
      String[] images = { "angryGregorBot.png", "gregorBotFront.png", "gregorBotLeft.png", "gregorBotRight.png",
            "dangerBotIcon.png" };
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