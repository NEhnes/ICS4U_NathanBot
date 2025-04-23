package bots;

import arena.BattleBotArena;
import arena.BotInfo;
import arena.Bullet;
import java.awt.Graphics;
import java.awt.Image;

public class AyushBot extends Bot {

    // ========== BOT INFORMATION ==========
    private String name = "AyushBot";
    private String nextMessage = null;
    private String[] messages = {
        "Target acquired!",
        "You can't escape!",
        "Nice try!",
        "Too slow!",
        "Calculated."
    };

    // ========== IMAGES ==========
    private Image up, down, left, right, currentImage;

    // ========== CONSTANTS ==========
    private static final int WALL_BUFFER = 50;        // Stay away from walls
    private static final int BULLET_DANGER = 150;     // Distance to start dodging
    private static final int FIRE_COOLDOWN = 15;      // Frames between shots
    private static final int STUCK_TIME = 30;         // Frames before considering stuck
    private static final int DODGE_DURATION = 20;     // How long to maintain dodge direction
    private static final int MOVE_DURATION = 50;      // How long to move in one direction

    // ========== STATE VARIABLES ==========
    private int fireCooldown = 0;          // Counts down between shots
    private double lastX = -1, lastY = -1; // Previous position for stuck detection
    private int stuckCount = 0;            // How long we've been stuck
    private int moveCount = 0;             // Frames moving in current direction
    private int dodgeTimer = 0;            // How long to keep dodging
    private int lastDodgeMove = BattleBotArena.STAY; // Remember dodge direction
    private BotInfo currentTarget = null;  // Bot we're targeting
    private boolean movingRight = true;    // Current movement direction

    public void draw(Graphics g, int x, int y) {
        g.drawImage(currentImage, x, y, Bot.RADIUS * 2, Bot.RADIUS * 2, null);
    }

    public int getMove(BotInfo me, boolean shotOK, BotInfo[] liveBots, BotInfo[] deadBots, Bullet[] bullets) {
        // Update timers
        moveCount++;
        fireCooldown--;
        if (dodgeTimer > 0) {
            dodgeTimer--;
        }

        // 2. ATTACK ENEMIES
        if (shotOK && fireCooldown <= 0) {
            int attackMove = attackNearestBot(me, liveBots);
            if (attackMove != BattleBotArena.STAY) {
                fireCooldown = FIRE_COOLDOWN;
                updateImage(attackMove);
                return attackMove;
            }
        }

        // 1. DODGE BULLETS (Highest Priority)
        int dodgeMove = dodgeBullets(me, bullets);
        if (dodgeMove != BattleBotArena.STAY) {
            updateImage(dodgeMove);
            return dodgeMove;
        }

        // 3. STRATEGIC MOVEMENT
        int moveDirection = getSmartMove(me, liveBots, deadBots);
        updateImage(moveDirection);
        return moveDirection;
    }

    private int dodgeBullets(BotInfo me, Bullet[] bullets) {
        // Continue dodging if timer is active
        if (dodgeTimer > 0) {
            return lastDodgeMove;
        }
    
        for (Bullet bullet : bullets) {
            if (bullet == null) continue;
    
            double xDiff = me.getX() + Bot.RADIUS - bullet.getX();
            double yDiff = me.getY() + Bot.RADIUS - bullet.getY();
            double distance = Math.abs(xDiff) + Math.abs(yDiff);
    
            // Check if bullet is headed towards us
            boolean bulletComingRight = bullet.getXSpeed() > 0 && xDiff > 0;
            boolean bulletComingLeft = bullet.getXSpeed() < 0 && xDiff < 0;
            boolean bulletComingDown = bullet.getYSpeed() > 0 && yDiff > 0;
            boolean bulletComingUp = bullet.getYSpeed() < 0 && yDiff < 0;
    
            if ((bulletComingRight || bulletComingLeft || bulletComingDown || bulletComingUp)
                    && distance < BULLET_DANGER) {
    
                boolean verticalDanger = Math.abs(xDiff) < Bot.RADIUS * 2;
                boolean horizontalDanger = Math.abs(yDiff) < Bot.RADIUS * 2;
    
                if (verticalDanger || horizontalDanger) {
                    int dodgeMove;
                    if (verticalDanger) {
                        // Choose safer side to dodge
                        dodgeMove = (me.getX() < BattleBotArena.RIGHT_EDGE/2) ? 
                                   BattleBotArena.RIGHT : BattleBotArena.LEFT;
                    } else {
                        dodgeMove = (me.getY() < BattleBotArena.BOTTOM_EDGE/2) ? 
                                   BattleBotArena.DOWN : BattleBotArena.UP;
                    }
                    // Start dodge timer and remember direction
                    dodgeTimer = DODGE_DURATION;
                    lastDodgeMove = dodgeMove;
                    return dodgeMove;
                }
            }
        }
        return BattleBotArena.STAY;
    }

    private int attackNearestBot(BotInfo me, BotInfo[] liveBots) {
        double closestDistance = Double.MAX_VALUE;
        BotInfo target = null;

        for (BotInfo bot : liveBots) {
            if (bot == null || bot.getName().equals(name)) {
                continue;
            }

            double distance = Math.abs(me.getX() - bot.getX())
                    + Math.abs(me.getY() - bot.getY());

            if (distance < closestDistance) {
                closestDistance = distance;
                target = bot;
            }
        }

        if (target != null) {
            currentTarget = target;
            if (Math.abs(me.getY() - target.getY()) < Bot.RADIUS * 1.5) {
                return (me.getX() < target.getX())
                        ? BattleBotArena.FIRERIGHT : BattleBotArena.FIRELEFT;
            }
            if (Math.abs(me.getX() - target.getX()) < Bot.RADIUS * 1.5) {
                return (me.getY() < target.getY())
                        ? BattleBotArena.FIREDOWN : BattleBotArena.FIREUP;
            }
        }
        return BattleBotArena.STAY;
    }

    private boolean isNearDeadBot(BotInfo me, BotInfo[] deadBots) {
        for (BotInfo bot : deadBots) {
            if (bot == null) {
                continue;
            }
            double distance = Math.abs(me.getX() - bot.getX())
                    + Math.abs(me.getY() - bot.getY());
            if (distance < Bot.RADIUS * 4) {
                return true;
            }
        }
        return false;
    }

    private int getAwayFromDeadBots(BotInfo me, BotInfo[] deadBots) {
        double closestDistance = Double.MAX_VALUE;
        BotInfo closestDeadBot = null;

        for (BotInfo bot : deadBots) {
            if (bot == null) {
                continue;
            }

            double distance = Math.abs(me.getX() - bot.getX())
                    + Math.abs(me.getY() - bot.getY());
            if (distance < closestDistance) {
                closestDistance = distance;
                closestDeadBot = bot;
            }
        }

        if (closestDeadBot != null) {
            double xDiff = me.getX() - closestDeadBot.getX();
            double yDiff = me.getY() - closestDeadBot.getY();

            if (Math.abs(xDiff) > Math.abs(yDiff)) {
                return (xDiff > 0) ? BattleBotArena.RIGHT : BattleBotArena.LEFT;
            } else {
                return (yDiff > 0) ? BattleBotArena.DOWN : BattleBotArena.UP;
            }
        }
        return BattleBotArena.STAY;
    }

    private int getSmartMove(BotInfo me, BotInfo[] liveBots, BotInfo[] deadBots) {
        // Check if stuck
        if (lastX == me.getX() && lastY == me.getY()) {
            stuckCount++;
            if (stuckCount > STUCK_TIME) {
                movingRight = !movingRight;
                stuckCount = 0;
                moveCount = 0;
            }
        } else {
            stuckCount = 0;
            lastX = me.getX();
            lastY = me.getY();
        }

        // Avoid walls
        if (me.getX() < WALL_BUFFER) {
            return BattleBotArena.RIGHT;
        }
        if (me.getX() > BattleBotArena.RIGHT_EDGE - WALL_BUFFER) {
            return BattleBotArena.LEFT;
        }
        if (me.getY() < WALL_BUFFER) {
            return BattleBotArena.DOWN;
        }
        if (me.getY() > BattleBotArena.BOTTOM_EDGE - WALL_BUFFER) {
            return BattleBotArena.UP;
        }

        // Get away from dead bots
        if (isNearDeadBot(me, deadBots)) {
            return getAwayFromDeadBots(me, deadBots);
        }

        // Change direction periodically for unpredictability
        if (moveCount > MOVE_DURATION) {
            movingRight = !movingRight;
            moveCount = 0;
        }

        // Basic patrolling movement
        return movingRight ? BattleBotArena.RIGHT : BattleBotArena.LEFT;
    }

    private void updateImage(int move) {
        switch (move) {
            case BattleBotArena.UP:
            case BattleBotArena.FIREUP:
                currentImage = up;
                break;
            case BattleBotArena.DOWN:
            case BattleBotArena.FIREDOWN:
                currentImage = down;
                break;
            case BattleBotArena.LEFT:
            case BattleBotArena.FIRELEFT:
                currentImage = left;
                break;
            case BattleBotArena.RIGHT:
            case BattleBotArena.FIRERIGHT:
                currentImage = right;
                break;
        }
    }

    public void newRound() {
        fireCooldown = 0;
        stuckCount = 0;
        moveCount = 0;
        lastX = -1;
        lastY = -1;
        dodgeTimer = 0;
        lastDodgeMove = BattleBotArena.STAY;
        movingRight = true;
        currentImage = right;
        currentTarget = null;
    }

    public String getName() {
        return name;
    }

    public String getTeamName() {
        return "Arena";
    }

    public String[] imageNames() {
        return new String[]{"roomba_up.png", "roomba_down.png",
            "roomba_left.png", "roomba_right.png"};
    }

    public void loadedImages(Image[] images) {
        if (images != null) {
            up = images[0];
            down = images[1];
            left = images[2];
            right = images[3];
            currentImage = right;
        }
    }

    public String outgoingMessage() {
        if (Math.random() < 0.01) { // 1% chance to taunt
            nextMessage = messages[(int) (Math.random() * messages.length)];
        }
        String msg = nextMessage;
        nextMessage = null;
        return msg;
    }

    public void incomingMessage(int botNum, String msg) {
        // Not used
    }
}
