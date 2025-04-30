/*
 * BattleBots
 * Date: Apr 30 2025
 * Author: Nathan Ehnes ICS4U
 */

package bots;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;

import arena.BattleBotArena;
import arena.BotInfo;
import arena.Bullet;

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
   private String[] messages = { "YOOOOOO", "Drake > Kendrick", "Gregor FTW" };

   /**
    * Image for drawing
    */
   Image angry, front, left, right, danger, currentImage;

   /**
    * Counter is used to time recurring events, such as messaging
    */
   private int counter = 0;

   /**
    * Current positions initialized global to avoid TONS of parameter passing
    */
   private static double currentX, currentY;
   private static double targetX, targetY = 300;
   private static double targetBotX, targetBotY = 300;

   /**
    * Same goes for bullets array, as it is used in several methods
    */
   private static Bullet[] privBullets;

   /**
    * A number of variables are initialized global because they need to have states
    * independent of GetMove method
    * Also used in a number of methods and being global avoids excessive passing
    */
   private static boolean attackEnabled = false;
   private static int attackTicks = 0;
   private static int lastShotTicks = 0;
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
    * - dodge any incoming bullets
    *
    * - check if another bot is aligned vertically/horizontally; shoot if
    * conditions met
    * 
    * - update the target bot
    * - update my desired position relative to the bot (on a diagonal to avoid
    * being shot, preparing for attack sequence)
    * 
    * - start/continue attack if conditions are met, end attack if not met
    * - continue attack in progress
    * - attack strategy:
    * - once in attack position, strafe left/right above or below the target
    * - shorter arena height then width, so that bullets despawn faster and i can
    * fire again quicker
    * - most bots (including my own and Deo's) encounter issues dodging multiple
    * bullets
    * - fire every 10 ticks vertically, creating a wall
    * 
    * - if nothing else triggered, set my movement towards the target location
    * - if > 10 bots alive, move to an unpopulated are of the map by default
    *
    * - run method to avoid dead bots, moves up/down depending on relative location
    * - currently commented out because of issues
    * 
    * - if nothing else was already returned, return current move
    */

   /*************************************************************
    ********* controls movement, messages, and firing ***********
    ************************************************************/
   public int getMove(BotInfo me, boolean shotOK, BotInfo[] liveBots, BotInfo[] deadBots, Bullet[] bullets) {

      // update my position
      currentX = (int) me.getX();
      currentY = (int) me.getY();

      // update bullet array - my own array so I dont have to pass it around
      privBullets = bullets;

      // handles all dodging logic
      move = DodgeSequence();
      if (move != 0) {
         RunCounters();
         System.out.println("DodgeSequence return");
         return move;
      }

      // checks if another bot is aligned in x/y lines
      move = CheckForShot(me, liveBots);
      if (move != 0) {
         lastShotTicks = 0;
         RunCounters();
         System.out.println("CheckForShot return");
         return move;
      }

      // find closest bot, and position myself relative to it
      SetTargeting(liveBots);

      // if conditions met, commence attack
      attackEnabled = AttackConditionsMet(shotOK);

      // continue attack sequence, returns 0 if conditions not met
      move = Attack(shotOK);
      if (move != 0) {
         RunCounters();
         System.out.println("Attack return");
         return move;
      }

      // set targeting move
      move = MoveTo(targetX, targetY);

      // default postition if >10 bots remain
      if (liveBots.length > 10) {
         move = MoveTo(100, 380);
      }

      if (counter % 50 == 0) {

         nextMessage = (Math.random() < 0.5) ? messages[0] : messages[1];

         RunCounters();
         return BattleBotArena.SEND_MESSAGE;
      }

      // move = AvoidDeadBots(move, deadBots); //commented out due to bugs
      // if (move != 0) {
      // RunCounters();
      // return move;
      // }

      RunCounters();
      System.out.println("Default return");
      return move;
   }

   // ************************************************
   // LOGIC ENDS HERE ------- CUSTOM METHODS BELOW
   // ************************************************

   // handles all dodging events: findding, calculating, and moving if necessary
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
      inDanger = false; // default
      return 0;
   }

   // returns an array of two values, the nearest bullet index @ [0], second nearest index @ [1]
   private static int[] GetClosestBullets() {
      int bulletIndex = 0;
      int secondClosestIndex = 0;
      int[] orderedBullets = { 0 }; //array so that i can store multiple values if needed
      double closestBulletDist = 10000;
      double bulletX, bulletY;

      // iterate through arrary, saving closest distance
      for (int i = 0; i < privBullets.length; i++) {
         Bullet tempBullet = privBullets[i];
         bulletX = tempBullet.getX();
         bulletY = tempBullet.getY();
         if (ManhattanDistance(bulletX, bulletY) < closestBulletDist) {
            secondClosestIndex = bulletIndex;
            closestBulletDist = ManhattanDistance(bulletX, bulletY);
            bulletIndex = i;
         }
      }

      // fill array with either 1 or 2 elements
      if (privBullets.length > 1) {
         orderedBullets = new int[] { bulletIndex, secondClosestIndex };
      } else {
         orderedBullets = new int[] { bulletIndex };
      }
      return orderedBullets;
   }

   // actually returns dodge direction move
   private int DodgeBullet(Bullet closestBullet) {

      // extra padding space for safety
      int padding = 35;

      // if bullet moving up/down
      if (closestBullet.getYSpeed() != 0) {
         // if aligned X
         if (Math.abs((currentX + Bot.RADIUS) - closestBullet.getX()) < Bot.RADIUS + padding) {
            // select dodge direction L/R
            if (closestBullet.getX() > currentX + Bot.RADIUS || currentX > 640) {
               return BattleBotArena.LEFT;
            } else {
               return BattleBotArena.RIGHT;
            }
         }
      }
      // if moving left/right
      else if (closestBullet.getXSpeed() != 0) {
         // if aligned Y
         if (Math.abs((currentY + Bot.RADIUS) - closestBullet.getY()) < Bot.RADIUS + padding) {
            // select dodge direction U/D
            return (closestBullet.getY() > currentY + Bot.RADIUS || currentY > 440) ? BattleBotArena.UP
                  : BattleBotArena.DOWN;
         }

      }
      return 0;
   }

   // picks target bot and ideal relative position
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

   // returns closest bot index
   private static int GetClosestBot(BotInfo[] botArray) {
      int botIndex = 0;
      double closestBotDist = 1000;

      // iterate through and save closest
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
         attackingLeft = (currentX > targetBotX) ? true : false;
         attackTicks = 0;
         return true;
      } else {
         return false;
      }
   }

   // returns actual attack  move
   private int Attack(boolean shotOK) {

      // end attack if is over
      if (attackTicks > attackDuration - 1) {
         attackEnabled = false;
      }

      if (attackEnabled == true && attackTicks < attackDuration) {
         attackTicks++;
         // if can shoot (every 10 ticks)
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

   // not currently in use due to bugs
   private int AvoidDeadBots(int move, BotInfo[] deadBots) {

      if (deadBots.length == 0) {
         return 0;
      }

      int closestDeadBot = GetClosestBot(deadBots);

      double closestDeadX = deadBots[closestDeadBot].getX();
      double closestDeadY = deadBots[closestDeadBot].getY();

      double closestDeadDist = ManhattanDistance(closestDeadX, closestDeadY);

      if (closestDeadDist < 50) {
         return (targetY < currentY) ? BattleBotArena.UP : BattleBotArena.DOWN;
      }
      return 0;
   }

   // looks for another bot aligned with me and shoots at it
   private int CheckForShot(BotInfo me, BotInfo[] liveBots) {
      int shootingInterval = 10;
      for (int i = 0; i < liveBots.length; i++) {
         // if aligned vertically
         if (Math.abs(me.getX() - liveBots[i].getX()) < Bot.RADIUS) {
            // if has not fired recently
            if (lastShotTicks > shootingInterval) {
               // decide to shoot up/down
               return (me.getY() > liveBots[i].getY()) ? BattleBotArena.FIREUP : BattleBotArena.FIREDOWN;
            }
         }

         // if aligned horizontally
         if (Math.abs(me.getY() - liveBots[i].getY()) < Bot.RADIUS) {
            if (lastShotTicks > shootingInterval) {
               // decide to shoot left/right
               return (me.getX() > liveBots[i].getX()) ? BattleBotArena.FIRELEFT : BattleBotArena.FIRERIGHT;
            }
         }
      }
      return 0;
   }

   // moves to a point, prioritizing x/y in a way that moves in a diagonal
   private static int MoveTo(double moveX, double moveY) {

      if (Math.abs(currentX - moveX) > Math.abs(currentY - moveY)) {
         if (currentX < moveX) {
            return BattleBotArena.RIGHT;
         } else if (currentX > moveX) {
            return BattleBotArena.LEFT;
         }
      } else {
         if (currentY < moveY) {
            return BattleBotArena.DOWN;
         } else {
            return BattleBotArena.UP;
         }
      }
      return 0;
   }

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

   private static double ManhattanDistance(double objX, double objY) {
      return Math.abs(currentX - objX) + Math.abs(currentY - objY);
   }

   // end of GetMove stuff, in a function so that early returns dont skip logic
   private void RunCounters() {
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
         name = "gregorAI";
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