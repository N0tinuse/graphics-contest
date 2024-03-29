/*
 * File: GraphicsContest.java
 * --------------------------
 * Name: Will Meyer
 * Section Leader: Will Monroe
 * Entry: Starfaux 106A
 */

import java.awt.Color;
import acm.program.*;
import acm.graphics.*;
import java.awt.event.*;
import java.applet.*;
import acm.util.*;
import java.io.*;
import acm.io.*;

/* credits:
 * star background found at http://www.digitalbusstop.com/cool-animated-gifs/
 * arwing image from http://s14.zetaboards.com/Nintendo_Rp_and_more/topic/90827/1/ - originally from Starfox 64
 * millennium falcon image from http://www.scifiscoop.com/news/star-wars-limited-edition-only-500-of-these-millennium-falcons/ - originally from Star Wars
 * tie fighter image from http://sourwineblog.blogspot.com/2010_06_01_archive.html - originally from Star Wars
 * starship enterprise image from http://www.startrek-wallpapers.com/Star-Trek-TNG/Starship-Enterprise-NCC-1701-D-Background/ - originally from Star Trek
 * mehran's head from http://robotics.stanford.edu/~sahami/bio.html
 * explosion gif from http://djsmileyface.info/Photos.html
 * explosion sound from http://soundbible.com/456-Explosion-2.html
 * starfox sounds from http://www.starfox64.baldninja.com/sf64snds.htm - originally from Starfox 64
 * corneria theme from http://www.themesongshut.com/Super-Smash-Bros-Melee-Planet-Corneria-Theme-Song.html - originally from Super Smash Bros. Melee
 * warning siren from http://policeinterceptor.com/navysounds.htm
 * everything else: images = toys from my toy cabinet/ sounds = sound files I had on my computer
 */

public class GraphicsContest extends GraphicsProgram {

	//specify width and height of program.
	private static final int PROGRAM_WIDTH = 1600;
	private static final int PROGRAM_HEIGHT = 800;
	
	//gameArea = background; ship = the ship in the foreground.
	private GImage gameArea;
	private GImage ship;

	//instance variables used for ship movement, determining image, etc.
	private boolean shipMovingUp;
	private boolean shipMovingDown;
	private boolean shipMovingRight;
	private boolean shipMovingLeft;
	private int shipMovementX;
	private int shipMovementY;
	private static final int shipMovementValue = 5;
	private int shipImageConstant;

	//instance variables used for bullets fired from the ship.
	private GOval[] bullets;
	private double[] bulletVelocities;
	private static final int BULLET_INITIAL_SIZE = 60;
	private int bulletCounter;
	private boolean bulletsPresent = false;

	//instance variables used for enemies/their bullets and graphics. also I stuck the rgen in here for some reason.
	private int enemyCounter;
	private RandomGenerator rgen = RandomGenerator.getInstance();
	private GImage[] enemies;
	private int[] enemyImageValues;
	private boolean enemiesPresent = false;
	private GOval[] enemyBullets;
	private double[] enemyXBulletVelocities;
	private double[] enemyYBulletVelocities;
	private int enemyBulletCounter;
	private boolean enemyBulletsPresent = false;
	private GImage enemyExplosion = new GImage ("explosion.png");
	private int explosionCounter;
	
	//ivars for lives and their labels.
	private int lives;
	private GImage[] lifeLabels;
	private GLabel livesLabel;
	private boolean lifeLost = false;
	
	//ivars for bosses
	private GLabel bossApproachLabel;
	private int loopCounter;
	private boolean bossPresent = false;
	private GImage boss;
	private int[] bossHealth = new int[5];
	private int bossCounter;
	private GLabel bossHealthLabel;
	private GRect bossHealthBarOutside;
	private GRect bossHealthBarInside;
	private double bossDestinationX;
	private double bossDestinationY;
	private int currentBossHealth;
	
	/* activate cheat to skip to boss. useful if you're like me and want to skip the level to play the end...
	 * hammer on the 1 key to skip to the boss.
	 */
	private int bossSkipCounter;
	
	//ivars for barrel roll.
	private boolean leftBarrelRollInitialized = false;
	private boolean rightBarrelRollInitialized = false;
	private int barrelRollTimeCounter;
	private boolean performLeftBarrelRoll = false;
	private boolean performRightBarrelRoll = false;
	private GImage barrelRollArrows = new GImage("barrelrollarrows.png");
	
	//audio clips.
	AudioClip doABarrelRoll = MediaTools.loadAudioClip("barrelroll.wav");
	AudioClip laser = MediaTools.loadAudioClip("laser.wav");
	AudioClip enemyLaser = MediaTools.loadAudioClip("enemylaser.wav");
	AudioClip explosion = MediaTools.loadAudioClip("explosion.wav");
	AudioClip corneriaTheme = MediaTools.loadAudioClip("corneriatheme.wav");
	AudioClip warningClip = MediaTools.loadAudioClip("warning.wav");
	
	//ivars for scores/high scores
	private int score;
	private GLabel scoreLabel;
	private int[] highScores;
	private String[] highScoreNames;
	

	//sets the size of the program at 1600, 800. Also adds background and reads in high scores from a file.
	public void init() {
		setSize(PROGRAM_WIDTH, PROGRAM_HEIGHT);
		gameArea = new GImage("starfield.gif");
		gameArea.setSize(PROGRAM_WIDTH, PROGRAM_HEIGHT);
		add(gameArea);
		highScoreNames = new String[10];
		highScores = new int[10];
		highScoreReader();
	}

	public void run() {
		makeInitialLabels();
		playGame();
	}

	private void playGame() {
		addKeyListeners();
		corneriaTheme.loop();
		setUpGame();
		//this loop actually plays the game. 
		while(lives > -1) {
			checkforShipCollisions();
			moveShip();
			checkforShipChange();
			//initializes boss after a minute spent in each level
			if (loopCounter == 12000) {
				initializeBoss();
			}
			//determines which type of gameplay to execute
			if (!bossPresent) {
				normalGameProcedure(); 
			} else if (bossPresent) {
				bossProcedure();
			}
			scoreLabel.setLabel("Score: " + score);
			//makes sure ship is always in the foreground
			remove(ship);
			add(ship);
			pause(5);
		}
		//only executed when loop breaks
		removeAll();
		processGameOver();
		
	}

	//either sets a new high score or displays the table.
	private void processGameOver() {
		if (score > highScores[9]) setHighScore();
		else {
			add(gameArea);
			GLabel gameOver = new GLabel("Game Over", 0, 0);
			gameOver.setFont("Sans Serif-100");
			gameOver.setColor(Color.RED);
			gameOver.setLocation(getWidth() / 2 - gameOver.getWidth() / 2, getHeight() / 2 - gameOver.getAscent() / 2);
			add(gameOver);
			pause(3000);
			remove(gameOver);
			highScoreDisplay();
		}
	}
	
	/* checks to see if barrel roll is currently being performed - if it is, it counts to a certain number, 
	 * then resets all variables relating to barrel rolls
	 */
	private void barrelRollChecker() {
		if (performLeftBarrelRoll || performRightBarrelRoll) {
			barrelRollTimeCounter++;
			if (barrelRollTimeCounter > 50) {
				performLeftBarrelRoll = false;
				performRightBarrelRoll = false;
				leftBarrelRollInitialized = false;
				rightBarrelRollInitialized = false;
				barrelRollTimeCounter = 0;
				remove(barrelRollArrows);
			}
		}
		
	}

	//switches from normal game procedure to boss procedure
	/* removes all bullets from screen, sets boss image, chooses first boss destination,
	 * makes labels relating to boss, flashes warning screen, then adds the boss.
	 */
	private void initializeBoss() {
		loopCounter = 0;
		bossPresent = true;
		removeAll();
		removeEnemiesandEnemyBullets();
		currentBossHealth = bossHealth[bossCounter];
		switch (bossCounter) {
		case 0: boss = new GImage("tiefighter.png");
				break;
		case 1: boss.setImage("millenniumfalcon.png");
				break;
		case 2: boss.setImage("arwingfacingleft.png");
				break;
		case 3: boss.setImage("enterprise.png");
				break;
		case 4: boss.setImage("mehranfacingleft.png");
				break;
		}
		//controls boss movement - picks a random coordinate on the screen to move to
		bossDestinationX = rgen.nextInt(0, (int)getWidth() - (int)boss.getWidth());
		bossDestinationY = rgen.nextInt(0, (int)getHeight() - (int)boss.getHeight());
		bossHealthLabel = new GLabel("BOSS: ", 0, 0);
		bossHealthLabel.setColor(Color.RED);
		bossHealthLabel.setFont("Sans Serif-36");
		bossHealthLabel.setLocation(getWidth() / 2 - bossHealthLabel.getWidth(), bossHealthLabel.getAscent());
		bossHealthBarOutside = new GRect(getWidth() / 2, 0, 300, 50);
		bossHealthBarOutside.setColor(Color.BLUE);
		bossHealthBarInside = new GRect(getWidth() / 2 + 1, 1, 298, 48);
		bossHealthBarInside.setColor(Color.GREEN);
		bossHealthBarInside.setFilled(true);
		add(gameArea);
		add(scoreLabel);
		add(livesLabel);
		for (int j = 0; j < lives; j++) {
			add(lifeLabels[j]);
		}
		add(ship);
		//flashes multiple times - thus the repeated pauses below
		GLabel warning = new GLabel("WARNING!", 0, 0);
		warning.setFont("Sans Serif-100");
		warning.setColor(Color.RED);
		warning.setLocation(getWidth() / 2 - warning.getWidth() / 2, getHeight() / 2 - warning.getAscent() / 2);
		add(warning);
		warningClip.play();
		pause(250);
		remove(warning);
		pause(250);
		add(warning);
		pause(250);
		remove(warning);
		pause(250);
		add(warning);
		pause(250);
		remove(warning);
		pause(250);
		add(warning);
		pause(250);
		remove(warning);
		pause(250);
		add(warning);
		pause(250);
		remove(warning);
		pause(250);
		add(warning);
		pause(200);
		remove(warning);
		warningClip.stop();
		add(bossHealthLabel);
		add(bossHealthBarOutside);
		add(bossHealthBarInside);
		boss.setLocation(getWidth() / 2 - boss.getWidth() / 2, getHeight() / 2 - boss.getHeight() / 2);
		add(boss);
	}
	
	//gets rid of enemies/bullets by removing them from the canvas and setting their location far offscreen so they don't collide with anything.
	private void removeEnemiesandEnemyBullets() {
		for (int k = 0; k < enemies.length; k++) {
			if (enemies[k] != null) {
				remove(enemies[k]);
				enemies[k].setLocation(2500, 3000);
			}
		}
		for (int k = 0; k < enemyBullets.length; k++) {
			if (enemyBullets[k] != null) {
				remove(enemyBullets[k]);
				enemyBullets[k].setLocation(2200, 3000);
				enemyXBulletVelocities[k] = 0;
				enemyYBulletVelocities[k] = 0;
			}
		}
	}
	
	private void bossProcedure() {
		//same code here as normal - checks to see if the bullets collide with anything. takes away boss health bar if they do.
		if (bulletsPresent) {
			for (int i = 0; i < bullets.length; i++) {
				if (bullets[i] == null) break;
				if (bullets[i].getX() != 2000) {
					bullets[i].setSize(bullets[i].getWidth() - 0.5, bullets[i].getHeight() - 0.5);
					bullets[i].move(bulletVelocities[i], 0);
					if (bullets[i].getWidth() == 0) {
						remove(bullets[i]);
						bullets[i].setLocation(2000,3000);
					}
					if (bulletCollisionChecker(bullets[i]) == boss && bullets[i].getWidth() <= 20) {
						remove(bullets[i]);
						bullets[i].setLocation(2000,3000);
						score += 20;
						currentBossHealth -= 1;
						bossHealthBarInside.setSize(298 * currentBossHealth / bossHealth[bossCounter], bossHealthBarInside.getHeight());
						if (bossHealthBarInside.getWidth() <= 2 * 298 / (double)3 && bossHealthBarInside.getWidth() > 298 / (double)3) bossHealthBarInside.setColor(Color.YELLOW);
						if (bossHealthBarInside.getWidth() <= 298 / (double)3) bossHealthBarInside.setColor(Color.RED);
					}
					
				}
			}
		}
		//controls boss movement speed
		if ((int)boss.getX() > bossDestinationX) {
			if (bossCounter == 0 || bossCounter == 3 || bossCounter == 4) {
				boss.move(-1, 0);
			} else if (bossCounter == 1) {
				boss.move(-1, 0);
				if (boss.getX() > bossDestinationX) boss.move(-1, 0);
			} else if (bossCounter == 2) {
				boss.move(-1, 0);
				if (boss.getX() > bossDestinationX) boss.move(-1, 0);
				if (boss.getX() > bossDestinationX) boss.move(-1, 0);
			} 
		}
		if ((int)boss.getX() < bossDestinationX) {
			if (bossCounter == 0 || bossCounter == 3 || bossCounter == 4) {
				boss.move(1, 0);
			} else if (bossCounter == 1) {
				boss.move(1, 0);
				if (boss.getX() < bossDestinationX) boss.move(1, 0);
			} else if (bossCounter == 2) {
				boss.move(1, 0);
				if (boss.getX() < bossDestinationX) boss.move(1, 0);
				if (boss.getX() < bossDestinationX) boss.move(1, 0);
			} 
		}
		if ((int)boss.getY() > bossDestinationY) {
			if (bossCounter == 0 || bossCounter == 3 || bossCounter == 4) {
				boss.move(0, -1);
			} else if (bossCounter == 1) {
				boss.move(0, -1);
				if (boss.getY() > bossDestinationY) boss.move(0, -1);
			} else if (bossCounter == 2) {
				boss.move(0, -1);
				if (boss.getY() > bossDestinationY) boss.move(0, -1);
				if (boss.getY() > bossDestinationY) boss.move(0, -1);
			} 
		}
		if ((int)boss.getY() < bossDestinationY) {
			if (bossCounter == 0 || bossCounter == 3 || bossCounter == 4) {
				boss.move(0, 1);
			} else if (bossCounter == 1) {
				boss.move(0, 1);
				if (boss.getY() < bossDestinationY) boss.move(0, 1);
			} else if (bossCounter == 2) {
				boss.move(0, 1);
				if (boss.getY() < bossDestinationY) boss.move(0, 1);
				if (boss.getY() < bossDestinationY) boss.move(0, 1);
			} 
		}
		//chooses new random destination when the boss hits a certain coordinate
		if ((int)boss.getX() == bossDestinationX && (int)boss.getY() == bossDestinationY) {
			bossDestinationX = rgen.nextInt(0, (int)getWidth() - (int)boss.getWidth());
			bossDestinationY = rgen.nextInt(0, (int)getHeight() - (int)boss.getHeight());
		}
		//flips images of arwing/mehran as they move around the screen
		if (bossCounter == 2) {
			if (boss.getX() < getWidth() / 2 + boss.getWidth() / 2 && boss.getX() > getWidth() / 2  - boss.getWidth() / 2) {
				if (boss.getX() < bossDestinationX) boss.setImage("arwingfacingright.png");
				if (boss.getX() > bossDestinationX) boss.setImage("arwingfacingleft.png");
			} else if (boss.getX() <= getWidth() / 2 - boss.getWidth() / 4) {
				boss.setImage("arwingfacingright.png");
			} else if (boss.getX() >= getWidth() / 2 + boss.getWidth() / 4){
				boss.setImage("arwingfacingleft.png");
			}
		}
		if (bossCounter == 4) {
			if (boss.getX() < getWidth() / 2 + boss.getWidth() / 2 && boss.getX() > getWidth() / 2  - boss.getWidth() / 2) {
				if (boss.getX() < bossDestinationX) boss.setImage("mehranfacingright.png");
				if (boss.getX() > bossDestinationX) boss.setImage("mehranfacingleft.png");
			} else if (boss.getX() <= getWidth() / 2 - boss.getWidth() / 4) {
				boss.setImage("mehranfacingright.png");
			} else if (boss.getX() >= getWidth() / 2 + boss.getWidth() / 4){
				boss.setImage("mehranfacingleft.png");
			}
		}
		//similar stretch of code here to normal procedure - randomly spawns bullets and moves the ones already present.
		int bulletDeterminant = rgen.nextInt(500);
		if (bulletDeterminant >= 500 - 2 * (bossCounter+1)) {
			spawnBossBullet(boss, ship);
		}
		if (enemyBulletsPresent) {
			for (int i = 0; i < enemyBullets.length; i++) {
				if (enemyBullets[i] == null) break;
				if (enemyBullets[i].getX() != 2200) {
					enemyBullets[i].move(enemyXBulletVelocities[i], enemyYBulletVelocities[i]);
					enemyBullets[i].setSize(enemyBullets[i].getWidth() + 0.2, enemyBullets[i].getHeight() + 0.2);
					if (enemyBullets[i].getWidth() >= 16 && enemyBullets[i].getWidth() < 24) {
						enemyBullets[i].setColor(Color.CYAN);
					}
					if (enemyBullets[i].getWidth() >= 24 && enemyBullets[i].getWidth() < 32) {
						enemyBullets[i].setColor(Color.YELLOW);
					}
					if (enemyBullets[i].getWidth() >= 32 && enemyBullets[i].getWidth() < 40) {
						enemyBullets[i].setColor(Color.ORANGE);
					}
					if (enemyBullets[i].getWidth() >= 40) {
						enemyBullets[i].setColor(Color.RED);
					}
					if (enemyBulletCollisionChecker(enemyBullets[i]) == ship && enemyBullets[i].getWidth() >= 40) {
						enemyBullets[i].setLocation(2200, 3000);
						removeEnemiesandEnemyBullets();
						processDeath();
						add(boss);
						add(bossHealthLabel);
						add(bossHealthBarOutside);
						add(bossHealthBarInside);
					}
					if (enemyBullets[i].getWidth() > 60) {
						remove(enemyBullets[i]);
						enemyBullets[i].setLocation(2200,1200);
					}

				}
			}
		}
		barrelRollChecker();
		//processes boss death with an explosion
		if (currentBossHealth == 0) {
			//last boss - game over
			if (bossCounter == 4) {
				removeAll();
				removeEnemiesandEnemyBullets();
				GImage bossExplosion = new GImage("explosion.gif");
				bossExplosion.setSize(boss.getWidth(), 200 * boss.getWidth() / (double)142);
				bossExplosion.setLocation(boss.getX() + boss.getWidth() / 2 - bossExplosion.getWidth() / 2, boss.getY() + boss.getHeight() / 2 - bossExplosion.getHeight() / 2);
				explosion.play();
				add(gameArea);
				add(scoreLabel);
				add(livesLabel);
				add(bossExplosion);
				GLabel youWin = new GLabel("YOU WIN!", 0, 0);
				youWin.setFont("Sans Serif-100");
				youWin.setColor(Color.RED);
				youWin.setLocation(getWidth() / 2 - youWin.getWidth() / 2, getHeight() / 2 - youWin.getAscent() / 2);
				add(youWin);
				pause(2000);
				removeAll();
				lives = -1;
			} else levelUp();
		}
	}

	//removes everything, initiates explosion, flashes a level up label for a while, then resumes normal gameplay
	private void levelUp() {
		removeAll();
		removeEnemiesandEnemyBullets();
		GImage bossExplosion = new GImage("explosion.gif");
		bossExplosion.setSize(boss.getWidth(), 200 * boss.getWidth() / (double)142);
		bossExplosion.setLocation(boss.getX() + boss.getWidth() / 2 - bossExplosion.getWidth() / 2, boss.getY() + boss.getHeight() / 2 - bossExplosion.getHeight() / 2);
		explosion.play();
		add(gameArea);
		add(scoreLabel);
		add(livesLabel);
		enemyExplosion.setLocation(5000,1600);
		add(enemyExplosion);
		for (int j = 0; j < lives; j++) {
			add(lifeLabels[j]);
		}
		add(bossExplosion);
		bossPresent = false;
		boss.setLocation(4000, 1500);
		bossCounter++;
		explosionCounter = 0;
		GLabel newLevel = new GLabel("LEVEL " + (bossCounter+1), 0, 0);
		newLevel.setFont("Sans Serif-100");
		newLevel.setColor(Color.RED);
		newLevel.setLocation(getWidth() / 2 - newLevel.getWidth() / 2, getHeight() / 2 - newLevel.getAscent() / 2);
		add(ship);
		add(newLevel);
		pause(2000);
		remove(bossExplosion);
		remove(newLevel);
	}
	
	//how the game is played normally - i.e. when boss is not present
	private void normalGameProcedure() {
		//adds label when boss is approaching
		if (loopCounter == 8600) {
			add(bossApproachLabel);
			bossApproachLabel.setLabel("ANOMALY DETECTED!");
			bossApproachLabel.setLocation(getWidth() / 2 - bossApproachLabel.getWidth() / 2, bossApproachLabel.getY());
		}
		if (loopCounter >= 9000) { 
			bossApproachLabel.setLabel("DISTANCE TO ANOMALY: " + (72000 - loopCounter * 6));
			bossApproachLabel.setLocation(getWidth() / 2 - bossApproachLabel.getWidth() / 2, bossApproachLabel.getY());
		}
		//checks for bullet collisions and removes ones that collide/are too small
		if (bulletsPresent) {
			for (int i = 0; i < bullets.length; i++) {
				if (bullets[i] == null) break;
				if (bullets[i].getX() != 2000) {
					bullets[i].move(bulletVelocities[i], 0);
					bullets[i].setSize(bullets[i].getWidth() - 0.5, bullets[i].getHeight() - 0.5);
					if (bullets[i].getWidth() == 0) {
						remove(bullets[i]);
						bullets[i].setLocation(2000,3000);
						bulletVelocities[i] = 0;
					}
					if (bulletCollisionChecker(bullets[i]) instanceof GImage && bulletCollisionChecker(bullets[i]) != ship && bulletCollisionChecker(bullets[i]) != gameArea && bulletCollisionChecker(bullets[i]) != barrelRollArrows  && bulletCollisionChecker(bullets[i]) != enemyExplosion && 3 * bulletCollisionChecker(bullets[i]).getHeight() / 5 >= bullets[i].getWidth()) {
						enemyExplosion.setSize(bulletCollisionChecker(bullets[i]).getWidth(), 200 * bulletCollisionChecker(bullets[i]).getWidth() / (double)142);
						enemyExplosion.setLocation((bulletCollisionChecker(bullets[i]).getX() + bulletCollisionChecker(bullets[i]).getWidth() / 2 - enemyExplosion.getWidth() / 2), bulletCollisionChecker(bullets[i]).getY() + bulletCollisionChecker(bullets[i]).getHeight() / 2 - enemyExplosion.getHeight() / 2);
						bulletCollisionChecker(bullets[i]).setLocation(2500, 1500);						
						explosionCounter = 1;
						remove(bullets[i]);
						bullets[i].setLocation(2000,3000);
						bulletVelocities[i] = 0;
						score += 50;
					} 
				}
			}
		}
		//randomly spawns enemy
		int enemyDeterminant = rgen.nextInt(500);
		if (enemyDeterminant >= 499 - 1 * bossCounter) {
			spawnEnemy();
		}
		if (enemiesPresent) {
			for (int i = 0; i < enemies.length; i++) {
				if (enemies[i] == null) break;
				//resizes enemy image based on image file
				if (enemyImageValues[i] == 1) {
					enemies[i].setLocation(enemies[i].getX() - 0.0705, enemies[i].getY() - 0.05);
					enemies[i].setSize(enemies[i].getWidth() + 0.141, enemies[i].getHeight() + 0.1);
				} else if (enemyImageValues[i] == 2) {
					enemies[i].setLocation(enemies[i].getX() - 0.0775, enemies[i].getY() - 0.05);
					enemies[i].setSize(enemies[i].getWidth() + 0.155, enemies[i].getHeight() + 0.1);
				} else if (enemyImageValues[i] == 3) {
					enemies[i].setLocation(enemies[i].getX() - 0.0505, enemies[i].getY() - 0.05);
					enemies[i].setSize(enemies[i].getWidth() + 0.101, enemies[i].getHeight() + 0.1);
				} else if (enemyImageValues[i] == 4) {
					enemies[i].setLocation(enemies[i].getX() - 0.049, enemies[i].getY() - 0.05);
					enemies[i].setSize(enemies[i].getWidth() + 0.098, enemies[i].getHeight() + 0.1);
				} else if (enemyImageValues[i] == 5) {
					enemies[i].setLocation(enemies[i].getX() - 0.152, enemies[i].getY() - 0.05);
					enemies[i].setSize(enemies[i].getWidth() + 0.314, enemies[i].getHeight() + 0.1);
				} else if (enemyImageValues[i] == 6) {
					enemies[i].setLocation(enemies[i].getX() - 0.0385, enemies[i].getY() - 0.05);
					enemies[i].setSize(enemies[i].getWidth() + 0.077, enemies[i].getHeight() + 0.1);
				} else if (enemyImageValues[i] == 7) {
					enemies[i].setLocation(enemies[i].getX() - 0.0385, enemies[i].getY() - 0.05);
					enemies[i].setSize(enemies[i].getWidth() + 0.077, enemies[i].getHeight() + 0.1);
				} 
				//spawns enemy bullets randomly
				int bulletDeterminant = rgen.nextInt(500);
				if (bulletDeterminant >= 499 - 1 * bossCounter && enemies[i].getHeight() < 20) {
					spawnEnemyBullet(enemies[i], ship);
				}
				if (enemyCollisionChecker(enemies[i]) == ship && enemies[i].getHeight() >= 80) {
					enemies[i].setLocation(2500, 3000);
					removeEnemiesandEnemyBullets();
					remove(ship);
					processDeath();
				}
				if (enemies[i].getHeight() >= 100) {
					enemies[i].setLocation(2500,1500);
					remove(enemies[i]);
				}
			}
		}
		if (enemyBulletsPresent) {
			for (int i = 0; i < enemyBullets.length; i++) {
				if (enemyBullets[i] == null) break;
				if (enemyBullets[i].getX() != 2200) {
					//resizes and changes color - only red bullets can hit the ship
					enemyBullets[i].move(enemyXBulletVelocities[i], enemyYBulletVelocities[i]);
					enemyBullets[i].setSize(enemyBullets[i].getWidth() + 0.2, enemyBullets[i].getHeight() + 0.2);
					if (enemyBullets[i].getWidth() >= 16 && enemyBullets[i].getWidth() < 24) {
						enemyBullets[i].setColor(Color.CYAN);
					}
					if (enemyBullets[i].getWidth() >= 24 && enemyBullets[i].getWidth() < 32) {
						enemyBullets[i].setColor(Color.YELLOW);
					}
					if (enemyBullets[i].getWidth() >= 32 && enemyBullets[i].getWidth() < 40) {
						enemyBullets[i].setColor(Color.ORANGE);
					}
					if (enemyBullets[i].getWidth() >= 40) {
						enemyBullets[i].setColor(Color.RED);
					}
					if (enemyBulletCollisionChecker(enemyBullets[i]) == ship && enemyBullets[i].getWidth() >= 40) {
						enemyBullets[i].setLocation(2200, 3000);
						removeEnemiesandEnemyBullets();
						processDeath();
					}
					if (enemyBullets[i].getWidth() > 60) {
						remove(enemyBullets[i]);
						enemyBullets[i].setLocation(2200,1200);
						enemyXBulletVelocities[i] = 0;
						enemyYBulletVelocities[i] = 0;
					}

				}
			}
		}
		loopCounter++;
		barrelRollChecker();
		//times how long an enemy explosion remains onscreen
		if (explosionCounter > 0) {
			explosionCounter++;
			if (explosionCounter > 40) {
				enemyExplosion.setLocation(5000, 2000);
				explosionCounter = 0;
			}
		}
	}
	
	//initiates game by adding images, initializing arrays, etc.
	private void setUpGame() {
		ship = new GImage("Ship_08.png");
		ship.setLocation(getWidth() / 2 - ship.getWidth() / 2, getHeight() / 2 - 3 * ship.getHeight() / 7);
		add(ship);
		scoreLabel = new GLabel ("Score: " + score, 0, 0);
		scoreLabel.setColor(Color.WHITE);
		scoreLabel.setFont("Sans Serif-36");
		scoreLabel.setLocation(20, scoreLabel.getAscent());
		add(scoreLabel);
		livesLabel = new GLabel("Lives: ", 0, 0);
		livesLabel.setColor(Color.WHITE);
		livesLabel.setFont("Sans Serif-36");
		livesLabel.setLocation(getWidth() - livesLabel.getWidth() - 180, livesLabel.getAscent());
		add(livesLabel);
		barrelRollArrows.setLocation(5000, 2000);
		add(barrelRollArrows);
		enemyExplosion.setLocation(5000, 2000);
		add(enemyExplosion);
		bossApproachLabel = new GLabel("DISTANCE TO ANOMALY: " + 72000, 0, 0);
		bossApproachLabel.setColor(Color.RED);
		bossApproachLabel.setFont("Sans Serif-36");
		bossApproachLabel.setLocation(getWidth() / 2 - bossApproachLabel.getWidth() / 2, bossApproachLabel.getAscent());
		lifeLabels = new GImage[3];
		for (int i = 0; i < lifeLabels.length; i++) {
			lifeLabels[i] = new GImage("placeholder.png");
			lifeLabels[i].setLocation(getWidth() - 180 + i * 60, 0);
			lifeLabels[i].setSize(90, 50);
			add(lifeLabels[i]);
		}
		bulletCounter = 0;
		//there are never 50 of these objects on screen at once, hopefully, although it gets close in level 5.
		bullets = new GOval[50];
		bulletVelocities = new double[50];
		enemies = new GImage[50];
		enemyBullets = new GOval[50];
		enemyXBulletVelocities = new double[50];
		enemyYBulletVelocities = new double[50];
		enemyImageValues = new int[50];
		enemyBulletCounter = 0;
		enemyCounter = 0;
		score = 0;
		loopCounter = 0;
		bossCounter = 0;
		explosionCounter = 0;
		bossSkipCounter = 0;
		bossHealth[0] = 40;
		bossHealth[1] = 60;
		bossHealth[2] = 80;
		bossHealth[3] = 150;
		bossHealth[4] = 400;
		lives = 3;
	}
	
	//procedure followed when ship collides with a bullet/enemy.
	/* removes everything on screen, readds essentials, relocates ship to center of screen and flashes
	 * then resumes normal gameplay
	 */
	private void processDeath() {
		lives--;
		removeAll();
		add(gameArea);
		add(scoreLabel);
		add(livesLabel);
		explosionCounter = 0;
		enemyExplosion.setLocation(5000, 4000);
		add(enemyExplosion);
		if (loopCounter >= 8600) add(bossApproachLabel);
		for (int j = 0; j < lives; j++) {
			add(lifeLabels[j]);
		}
		GImage death = new GImage("explosion.gif");
		death.setSize(ship.getWidth(), 200 * ship.getWidth() / (double)142);
		death.setLocation(ship.getX() + ship.getWidth() / 2  - death.getWidth() / 2, ship.getY() + ship.getHeight() / 2 - death.getHeight() / 2);
		add(death);
		explosion.play();
		lifeLost = true;
		pause(1500);
		remove(death);
		if (lives > -1) {
		pause(300);
		ship.setLocation(getWidth() / 2 - ship.getWidth() / 2, getHeight() / 2 - ship.getHeight() / 2);
		ship.setImage("Ship_08.png");
		add(ship);
		pause(300);
		remove(ship);
		pause(300);
		add(ship);
		pause(300);
		remove(ship);
		pause(300);
		add(ship);
		shipMovementX = 0;
		shipMovementY = 0; 
		lifeLost = false;
		}
	}
	
	/* these next methods are collision checkers similar to the one used for the ball in breakout; they don't work perfectly
	 * but work most of the time.
	 */
	private GObject enemyBulletCollisionChecker(GOval enemyBullet) {
		if (getElementAt (enemyBullet.getX() + enemyBullet.getWidth() / 2, enemyBullet.getY()) != null) {
			return (getElementAt (enemyBullet.getX() + enemyBullet.getWidth() / 2, enemyBullet.getY()));
		} else if (getElementAt (enemyBullet.getX() + enemyBullet.getWidth() / 2, enemyBullet.getY() + enemyBullet.getHeight()) != null) {
			return (getElementAt (enemyBullet.getX() + enemyBullet.getWidth() / 2, enemyBullet.getY() + enemyBullet.getHeight()));
		} else if (getElementAt (enemyBullet.getX() + enemyBullet.getWidth(), enemyBullet.getY() + enemyBullet.getHeight() / 2) != null) {
			return (getElementAt (enemyBullet.getX() + enemyBullet.getWidth(), enemyBullet.getY() + enemyBullet.getHeight() / 2));
		} else if (getElementAt (enemyBullet.getX(), enemyBullet.getY() + enemyBullet.getHeight() / 2) != null) {
			return (getElementAt (enemyBullet.getX(), enemyBullet.getY() + enemyBullet.getHeight() / 2));
		} else if (getElementAt (enemyBullet.getX(), enemyBullet.getY()) != null) {
			return (getElementAt (enemyBullet.getX(), enemyBullet.getY()));
		} else if (getElementAt (enemyBullet.getX() + enemyBullet.getWidth(), enemyBullet.getY()) != null) {
			return (getElementAt (enemyBullet.getX() + enemyBullet.getWidth(), enemyBullet.getY()));
		} else if (getElementAt (enemyBullet.getX(), enemyBullet.getY() + enemyBullet.getHeight()) != null) {
			return (getElementAt (enemyBullet.getX(), enemyBullet.getY() + enemyBullet.getHeight()));
		} else if (getElementAt (enemyBullet.getX() + enemyBullet.getWidth(), enemyBullet.getY() + enemyBullet.getHeight()) != null) {
			return (getElementAt (enemyBullet.getX() + enemyBullet.getWidth(), enemyBullet.getY() + enemyBullet.getHeight()));
		} else {
			return null;
		}
	}
	
	private GObject enemyCollisionChecker(GImage enemy) {
		if (getElementAt (enemy.getX() + enemy.getWidth() / 2, enemy.getY()) != null) {
			return (getElementAt (enemy.getX() + enemy.getWidth() / 2, enemy.getY()));
		} else if (getElementAt (enemy.getX() + enemy.getWidth() / 2, enemy.getY() + enemy.getHeight()) != null) {
			return (getElementAt (enemy.getX() + enemy.getWidth() / 2, enemy.getY() + enemy.getHeight()));
		} else if (getElementAt (enemy.getX() + enemy.getWidth(), enemy.getY() + enemy.getHeight() / 2) != null) {
			return (getElementAt (enemy.getX() + enemy.getWidth(), enemy.getY() + enemy.getHeight() / 2));
		} else if (getElementAt (enemy.getX(), enemy.getY() + enemy.getHeight() / 2) != null) {
			return (getElementAt (enemy.getX(), enemy.getY() + enemy.getHeight() / 2));
		} else if (getElementAt (enemy.getX(), enemy.getY()) != null) {
			return (getElementAt (enemy.getX(), enemy.getY()));
		} else if (getElementAt (enemy.getX() + enemy.getWidth(), enemy.getY()) != null) {
			return (getElementAt (enemy.getX() + enemy.getWidth(), enemy.getY()));
		} else if (getElementAt (enemy.getX(), enemy.getY() + enemy.getHeight()) != null) {
			return (getElementAt (enemy.getX(), enemy.getY() + enemy.getHeight()));
		} else if (getElementAt (enemy.getX() + enemy.getWidth(), enemy.getY() + enemy.getHeight()) != null) {
			return (getElementAt (enemy.getX() + enemy.getWidth(), enemy.getY() + enemy.getHeight()));
		} else {
			return null;
		}
	}

	private GObject bulletCollisionChecker(GOval bullet) {
		if (getElementAt (bullet.getX(), bullet.getY()) != null) {
			return (getElementAt (bullet.getX(), bullet.getY()));
		} else if (getElementAt (bullet.getX(), bullet.getY() + bullet.getHeight()) != null) {
			return (getElementAt (bullet.getX(), bullet.getY() + bullet.getHeight()));
		} else if (getElementAt (bullet.getX() + bullet.getWidth(), bullet.getY()) != null) {
			return (getElementAt (bullet.getX() + bullet.getWidth(), bullet.getY()));
		} else if (getElementAt (bullet.getX() + bullet.getWidth(), bullet.getY() + bullet.getHeight()) != null) {
			return (getElementAt (bullet.getX() + bullet.getWidth(), bullet.getY() + bullet.getHeight()));
		} else {
			return null;
		}
	}
	
	//same code as the enemy bullet except boss bullets start at the middle of the boss image...
	//unless it's the enterprise, where bullets spawn from the two guns, or mehran, where they come from his mouth
	private void spawnBossBullet(GImage boss, GImage ship) {
		GOval newBullet = new GOval(boss.getX() + boss.getWidth() / 2, boss.getY() + boss.getHeight() / 2, 1, 1);
		if (bossCounter == 3) {
			int gunDeterminer = rgen.nextInt(2);
			if (gunDeterminer == 0) newBullet.setLocation(boss.getX() + 325, boss.getY() + 200);
			if (gunDeterminer == 1) newBullet.setLocation(boss.getX() + 675, boss.getY() + 200);
		}
		if (bossCounter == 4) newBullet.setLocation(boss.getX() + boss.getWidth() / 2, boss.getY() + 395);
		newBullet.setColor(Color.BLUE);
		newBullet.setFilled(true);
		enemyBullets[enemyBulletCounter] = newBullet;
		if (bossCounter != 3 && bossCounter != 4) {
			enemyXBulletVelocities[enemyBulletCounter] = ((ship.getX() + ship.getWidth() / 2 - 30) - (boss.getX() + boss.getWidth() / 2)) / (double)320;
			enemyYBulletVelocities[enemyBulletCounter] = ((ship.getY() + ship.getHeight() / 2 - 30) - (boss.getY() + boss.getHeight() / 2)) / (double)320;
		} else if (bossCounter == 4) {
			enemyXBulletVelocities[enemyBulletCounter] = ((ship.getX() + ship.getWidth() / 2 - 30) - (boss.getX() + boss.getWidth() / 2)) / (double)320;
			enemyYBulletVelocities[enemyBulletCounter] = ((ship.getY() + ship.getHeight() / 2 - 30) - (boss.getY() + 395)) / (double)320;
		} else if (bossCounter == 3) {
			enemyXBulletVelocities[enemyBulletCounter] = ((ship.getX() + ship.getWidth() / 2 - 30) - newBullet.getX()) / (double)320;
			enemyYBulletVelocities[enemyBulletCounter] = ((ship.getY() + ship.getHeight() / 2 - 30) - newBullet.getY()) / (double)320;
		}
		add(enemyBullets[enemyBulletCounter]);
		enemyLaser.play();
		enemyBulletCounter++;
		enemyBulletsPresent = true;
		if (enemyBulletCounter == 50) enemyBulletCounter = 0;
	} 
	
	//makes new enemy bullet and sets its velocity based on relative position to the ship
	private void spawnEnemyBullet(GImage enemy, GImage ship) {
		GOval newBullet = new GOval(enemy.getX() + enemy.getWidth() / 2, enemy.getY() + enemy.getHeight() / 2, 3 * enemy.getHeight() / 5, 3 * enemy.getHeight() / 5);
		newBullet.setLocation(enemy.getX() + enemy.getWidth() / 2 - newBullet.getWidth() / 2, enemy.getY() + enemy.getHeight() / 2 - newBullet.getHeight() / 2);
		newBullet.setColor(Color.BLUE);
		newBullet.setFilled(true);
		enemyBullets[enemyBulletCounter] = newBullet;
		enemyXBulletVelocities[enemyBulletCounter] = ((ship.getX() + ship.getWidth() / 2 - 30) - (enemy.getX() + enemy.getWidth() / 2)) / (double)320;
		enemyYBulletVelocities[enemyBulletCounter] = ((ship.getY() + ship.getHeight() / 2 - 30) - (enemy.getY() + enemy.getHeight() / 2)) / (double)320;
		add(newBullet);
		enemyLaser.play();
		enemyBulletCounter++;
		enemyBulletsPresent = true;
		if (enemyBulletCounter == 50) enemyBulletCounter = 0;
	} 

	//randomly sets enemy image/location on screen
	private void spawnEnemy() {
		int enemyDeterminant = rgen.nextInt(7);
		GImage newEnemy = new GImage("placeholder.png");
		switch(enemyDeterminant) {
		case 0: newEnemy.setImage("enemy01.png");
				newEnemy.setSize(1.41, 1);
				enemyImageValues[enemyCounter] = 1;
				break;
		case 1: newEnemy.setImage("enemy02.png");
				newEnemy.setSize(1.55, 1);
				enemyImageValues[enemyCounter] = 2;
				break;
		case 2: newEnemy.setImage("enemy03.png");
				newEnemy.setSize(1.01, 1);
				enemyImageValues[enemyCounter] = 3;
				break;
		case 3: newEnemy.setImage("enemy04.png");
				newEnemy.setSize(.98, 1);
				enemyImageValues[enemyCounter] = 4;
				break;
		case 4: newEnemy.setImage("enemy05.png");
				newEnemy.setSize(3.14, 1);
				enemyImageValues[enemyCounter] = 5;
				break;
		case 5: newEnemy.setImage("enemy06.png");
				newEnemy.setSize(.77, 1);
				enemyImageValues[enemyCounter] = 6;
				break;
		case 6: newEnemy.setImage("enemy07.png");
				newEnemy.setSize(.77, 1);
				enemyImageValues[enemyCounter] = 7;
				break;
		}
		newEnemy.setLocation(rgen.nextInt(236, getWidth() - 236), rgen.nextInt(100, getHeight() - 100));
			enemies[enemyCounter] = (newEnemy);
			remove(ship);
			add(enemies[enemyCounter]);
			add(ship);
			enemyCounter++;
			enemiesPresent = true;
			if (enemyCounter == 50) enemyCounter = 0;
	}

	//stops the ship from moving past the game's boundaries.
	private void checkforShipCollisions() {
		if (ship.getX() - shipMovementX < 0) {
			ship.setLocation(0, ship.getY());
			shipMovementX = 0;
		}
		if (ship.getY() - shipMovementY < 0) {
			ship.setLocation(ship.getX(), 0);
			shipMovementY = 0;
		}
		if (ship.getX() + shipMovementX > getWidth() - ship.getWidth()) {
			ship.setLocation(getWidth() - ship.getWidth(), ship.getY());
			shipMovementX = 0;
		}
		if (ship.getY() + shipMovementY > getHeight() - ship.getHeight()) {
			ship.setLocation(ship.getX(), getHeight() - ship.getHeight());
			shipMovementY = 0;
		}
	}

	//sets the image of the ship based on where it is on screen/whether a barrel roll is being performed.
	private void checkforShipChange() {
		if (performLeftBarrelRoll || performRightBarrelRoll) {
			remove(barrelRollArrows);
			barrelRollArrows.setLocation(ship.getX() + ship.getWidth() / 2 - 133, ship.getY() + ship.getHeight() / 2 - 134.5);
			add(barrelRollArrows);
			ship.setImage("barrelroll.png");
		} else if (ship.getX() < (getWidth() - 236) / (double)17) {
			ship.setImage("Ship_00flipped.png"); 
			shipImageConstant = 0;
		} else if (ship.getX() < 2 * (getWidth() - 236) / (double)17 && ship.getX() > (getWidth() - 236) / (double)17) {
			ship.setImage("Ship_01flipped.png"); 
			shipImageConstant = 1;
		} else if (ship.getX() < 3 * (getWidth() - 236) / (double)17 && ship.getX() > 2 * (getWidth() - 236) / (double)17) { 
			ship.setImage("Ship_02flipped.png"); 
			shipImageConstant = 2;
		} else if (ship.getX() < 4 * (getWidth() - 236) / (double)17 && ship.getX() > 3 * (getWidth() - 236) / (double)17) {
			ship.setImage("Ship_03flipped.png"); 
			shipImageConstant = 3;
		} else if (ship.getX() < 5 * (getWidth() - 236) / (double)17 && ship.getX() > 4 * (getWidth() - 236) / (double)17) {
			ship.setImage("Ship_04flipped.png"); 
			shipImageConstant = 4;
		} else if (ship.getX() < 6 * (getWidth() - 236) / (double)17 && ship.getX() > 5 * (getWidth() - 236) / (double)17) {
			ship.setImage("Ship_05flipped.png"); 
			shipImageConstant = 5;
		} else if (ship.getX() < 7 * (getWidth() - 236) / (double)17 && ship.getX() > 6 * (getWidth() - 236) / (double)17) {
			ship.setImage("Ship_06flipped.png"); 
			shipImageConstant = 6;
		} else if (ship.getX() < 8 * (getWidth() - 236) / (double)17 && ship.getX() > 7 * (getWidth() - 236) / (double)17) {
			ship.setImage("Ship_07flipped.png"); 
			shipImageConstant = 7;
		} else if (ship.getX() < 9 * (getWidth() - 236) / (double)17 && ship.getX() > 8 * (getWidth() - 236) / (double)17) {
			ship.setImage("Ship_08.png"); 
			shipImageConstant = 8;
		} else if (ship.getX() < 10 * (getWidth() - 236) / (double)17 && ship.getX() > 9 * (getWidth() - 236) / (double)17) {
			ship.setImage("Ship_07.png"); 
			shipImageConstant = 9;
		} else if (ship.getX() < 11 * (getWidth() - 236) / (double)17 && ship.getX() > 10 * (getWidth() - 236) / (double)17) {
			ship.setImage("Ship_06.png"); 
			shipImageConstant = 10;
		} else if (ship.getX() < 12 * (getWidth() - 236) / (double)17 && ship.getX() > 11 * (getWidth() - 236) / (double)17) {
			ship.setImage("Ship_05.png"); 
			shipImageConstant = 11;
		} else if (ship.getX() < 13 * (getWidth() - 236) / (double)17 && ship.getX() > 12 * (getWidth() - 236) / (double)17) {
			ship.setImage("Ship_04.png"); 
			shipImageConstant = 12;
		} else if (ship.getX() < 14 * (getWidth() - 236) / (double)17 && ship.getX() > 13 * (getWidth() - 236) / (double)17) {
			ship.setImage("Ship_03.png"); 
			shipImageConstant = 13;
		} else if (ship.getX() < 15 * (getWidth() - 236) / (double)17 && ship.getX() > 14 * (getWidth() - 236) / (double)17) {
			ship.setImage("Ship_02.png"); 
			shipImageConstant = 14;
		} else if (ship.getX() < 16 * (getWidth() - 236) / (double)17 && ship.getX() > 15 * (getWidth() - 236) / (double)17) { 
			ship.setImage("Ship_01.png"); 
			shipImageConstant = 15;
		} else if (ship.getX() > 16 * (getWidth() - 236) / (double)17) {
			ship.setImage("Ship_00.png"); 
			shipImageConstant = 16;
		}
	}
	
	//actually moves the ship around.
	private void moveShip() {
		if (performLeftBarrelRoll) {
			shipMovementX = -shipMovementValue * 2;
			shipMovementY = 0;
		} else if (performRightBarrelRoll) {
			shipMovementX = shipMovementValue * 2;
			shipMovementY = 0;
		} else if (shipMovingUp && shipMovingLeft) {
			shipMovementX = -shipMovementValue;
			shipMovementY = -shipMovementValue;
		} else if (shipMovingUp && shipMovingRight) {
			shipMovementX = shipMovementValue;
			shipMovementY = -shipMovementValue;
		} else if (shipMovingDown && shipMovingLeft) {
			shipMovementX = -shipMovementValue;
			shipMovementY = shipMovementValue;
		} else if (shipMovingDown && shipMovingRight) {
			shipMovementX = shipMovementValue;
			shipMovementY = shipMovementValue;
		} else if (shipMovingUp && shipMovingDown) {
			shipMovementX = 0;
			shipMovementY = 0; 
		} else if (shipMovingLeft && shipMovingRight) {
			shipMovementX = 0;
			shipMovementY = 0; 
		} else if (shipMovingUp) {
			shipMovementX = 0;
			shipMovementY = -shipMovementValue; 
		} else if (shipMovingDown) {
			shipMovementX = 0;
			shipMovementY = shipMovementValue; 
		} else if (shipMovingLeft) {
			shipMovementX = -shipMovementValue;
			shipMovementY = 0; 
		} else if (shipMovingRight) {
			shipMovementX = shipMovementValue;
			shipMovementY = 0; 
		} 
		ship.move(shipMovementX, shipMovementY);
	}
	
	//control ship movements.
	public void keyPressed(KeyEvent e) {
		if (!lifeLost) {
			switch (e.getKeyCode()) {
			case KeyEvent.VK_UP: shipMovingUp = true; 
			leftBarrelRollInitialized = false;
			rightBarrelRollInitialized = false;
			break;
			case KeyEvent.VK_DOWN: shipMovingDown = true; 
			leftBarrelRollInitialized = false;
			rightBarrelRollInitialized = false;
			break;
			case KeyEvent.VK_LEFT: shipMovingLeft = true;
			rightBarrelRollInitialized = false;
			if (!leftBarrelRollInitialized) {
				leftBarrelRollInitialized = true;
			} else if (leftBarrelRollInitialized) {
				doABarrelRoll.play();
				performLeftBarrelRoll = true;
				leftBarrelRollInitialized = false;
				rightBarrelRollInitialized = false;
			}
			break;
			case KeyEvent.VK_RIGHT: shipMovingRight = true; 
			leftBarrelRollInitialized = false;
			if (!rightBarrelRollInitialized) {
				rightBarrelRollInitialized = true;
			} else if (rightBarrelRollInitialized){
				doABarrelRoll.play();
				performRightBarrelRoll = true;
				leftBarrelRollInitialized = false;
				rightBarrelRollInitialized = false;
			}
			break;
			case KeyEvent.VK_SHIFT: shipMovementX = 0;
									shipMovementY = 0; 
									break;
			}
		}
	}
	
	//resets boolean corresponding to key press
	public void keyReleased(KeyEvent e) {
		switch (e.getKeyCode()) {
		case KeyEvent.VK_UP: shipMovingUp = false; break;
		case KeyEvent.VK_DOWN: shipMovingDown = false; break;
		case KeyEvent.VK_LEFT: shipMovingLeft = false; break;
		case KeyEvent.VK_RIGHT: shipMovingRight = false; break;
		}
	}

	//controls shooting bullets from the ship, also sets velocity of bullet based on ship position
	public void keyTyped(KeyEvent e) {
		if (e.getKeyChar() == KeyEvent.VK_SPACE) {
			if (!lifeLost) {
				GOval newBullet = new GOval (800, 800, BULLET_INITIAL_SIZE, BULLET_INITIAL_SIZE);
				newBullet.setColor(Color.GRAY);
				newBullet.setFilled(true);
				if (shipImageConstant == 0) {
					newBullet.setLocation(ship.getX() + ship.getWidth() - 50, ship.getY() + ship.getHeight() / 3);
					bulletVelocities[bulletCounter] = 1.35;
				} else if (shipImageConstant == 1) {
					newBullet.setLocation(ship.getX() + ship.getWidth() - 50, ship.getY() + ship.getHeight() / 3);
					bulletVelocities[bulletCounter] = 1.2;
				} else if (shipImageConstant == 2) {
					newBullet.setLocation(ship.getX() + ship.getWidth() - 50, ship.getY() + ship.getHeight() / 3);
					bulletVelocities[bulletCounter] = 1.05;
				} else if (shipImageConstant == 3) {
					newBullet.setLocation(ship.getX() + ship.getWidth() - 50, ship.getY() + ship.getHeight() / 3);
					bulletVelocities[bulletCounter] = .9;
				} else if (shipImageConstant == 4) {
					newBullet.setLocation(ship.getX() + ship.getWidth() - 50, ship.getY() + ship.getHeight() / 3);
					bulletVelocities[bulletCounter] = .75;
				} else if (shipImageConstant == 5) {
					newBullet.setLocation(ship.getX() + ship.getWidth() - 50, ship.getY() + ship.getHeight() / 3);
					bulletVelocities[bulletCounter] = .6;
				} else if (shipImageConstant == 6) {
					newBullet.setLocation(ship.getX() + ship.getWidth() - 50, ship.getY() + ship.getHeight() / 3);
					bulletVelocities[bulletCounter] = .45;
				} else if (shipImageConstant == 7) {
					newBullet.setLocation(ship.getX() + ship.getWidth() - 50, ship.getY() + ship.getHeight() / 3);
					bulletVelocities[bulletCounter] = .3;
				} else if (shipImageConstant == 8) {
					newBullet.setLocation(ship.getX() + ship.getWidth() / 2 - 30, ship.getY() + ship.getHeight() / 2 - 30);
					bulletVelocities[bulletCounter] = .15;
				} else if (shipImageConstant == 9) {
					newBullet.setLocation(ship.getX(), ship.getY() + ship.getHeight() / 3);
					bulletVelocities[bulletCounter] = 0;
				} else if (shipImageConstant == 10) {
					newBullet.setLocation(ship.getX(), ship.getY() + ship.getHeight() / 3);
					bulletVelocities[bulletCounter] = -.15;
				} else if (shipImageConstant == 11) {
					newBullet.setLocation(ship.getX(), ship.getY() + ship.getHeight() / 3);
					bulletVelocities[bulletCounter] = -.3;
				} else if (shipImageConstant == 12) {
					newBullet.setLocation(ship.getX(), ship.getY() + ship.getHeight() / 3);
					bulletVelocities[bulletCounter] = -.45;
				} else if (shipImageConstant == 13) {
					newBullet.setLocation(ship.getX(), ship.getY() + ship.getHeight() / 3);
					bulletVelocities[bulletCounter] = -.6;
				} else if (shipImageConstant == 14) {
					newBullet.setLocation(ship.getX(), ship.getY() + ship.getHeight() / 3);
					bulletVelocities[bulletCounter] = -.75;
				} else if (shipImageConstant == 15) {
					newBullet.setLocation(ship.getX(), ship.getY() + ship.getHeight() / 3);
					bulletVelocities[bulletCounter] = -.9;
				} else if (shipImageConstant == 16) {
					newBullet.setLocation(ship.getX(), ship.getY() + ship.getHeight() / 3);
					bulletVelocities[bulletCounter] = -1.05;
				}
				bullets[bulletCounter] = newBullet;
				remove(ship);
				add(bullets[bulletCounter]);
				add(ship);
				laser.play();
				bulletCounter++;
				if (bulletCounter == 50) bulletCounter = 0;
				bulletsPresent = true;
			}
		}
		if (e.getKeyChar() == KeyEvent.VK_1) {
			bossSkipCounter++;
			if (bossSkipCounter == 9) {
				loopCounter = 12000;
				bossSkipCounter = 0;
			}
		}
	}
	
	//these methods set up the high score table
	private void setHighScore() {
		removeAll();
		add(gameArea);
		addHighScoreTable();
	}
	
	private void addHighScoreTable() {
		highScoreModification();
		highScoreRewrite();
		highScoreDisplay();
	}
	
	//reads high scores from file
	private void highScoreReader() {
		try {
			BufferedReader rd = new BufferedReader(new FileReader("HighScores.txt"));
			int lineNumber = 0;
			while(true) {
				String line = rd.readLine();
				if (line == null) break;
				int stringBreakPoint = line.indexOf(":");
				highScoreNames[lineNumber] = line.substring(0, stringBreakPoint);
				highScores[lineNumber] = Integer.parseInt(line.substring(stringBreakPoint+1));
				lineNumber++;
			}
			rd.close();
		} catch (IOException ex) {
			throw new ErrorException(ex);
		}
	}

	//reassigns high scores if the user gets one
	private void highScoreModification() {
		IODialog dialog = new IODialog();
		String highScore = dialog.readLine("High Score! Enter your initials:");
		if (score >= highScores[9]){
			for (int i = 0; i < 10; i++) {
				if (score >= highScores[i]) {
					for (int j = 9; j > i; j--) {
						highScoreNames[j] = highScoreNames[j-1];
						highScores[j] = highScores[j-1];
					}
					highScoreNames[i] = highScore;
					highScores[i] = score;
					break;
				}
			}
		}
		highScoreRewrite();
	}
	
	// writes high scores to the high score text file
	private void highScoreRewrite() {
		try {
			PrintWriter wr = new PrintWriter(new FileWriter("HighScores.txt"));
			for (int lineNumber = 0; lineNumber < 10; lineNumber++) {
				wr.println(highScoreNames[lineNumber] + ":" + highScores[lineNumber]);
			}
			wr.close();
		} catch (IOException ex) {
			throw new ErrorException(ex);
		}
	}
	
	//displays high scores in table on screen
	private void highScoreDisplay() {
		GLabel highScoreTitle = new GLabel("High Scores");
		highScoreTitle.setColor(Color.WHITE);
		highScoreTitle.setFont("Serif-36");
		highScoreTitle.setLocation(getWidth() / 2 - highScoreTitle.getWidth() / 2, getHeight() / 4 - highScoreTitle.getAscent() * 2);
		add(highScoreTitle);
		for (int i = 0; i < 10; i++) {
			GLabel highScoreLabel = new GLabel((i+1) + "." + highScoreNames[i] + "    " + highScores[i]);
			highScoreLabel.setColor(Color.WHITE);
			highScoreLabel.setFont("Serif-36");
			highScoreLabel.setLocation(getWidth() / 2 - highScoreLabel.getWidth() / 2, getHeight() / 4 + i * 50);
			add(highScoreLabel);
		}
	}

	//makes labels displayed at program's start
	private void makeInitialLabels() {
		GLabel gameTitle = new GLabel("STARFAUX 106A", 0, 0);
		gameTitle.setColor(Color.RED);
		gameTitle.setFont("Lucida-Bold-72");
		gameTitle.setLocation(getWidth() / 2 - gameTitle.getWidth() / 2, getHeight() / 5);
		add(gameTitle);
		GLabel instructions1 = new GLabel("Controls");
		GLabel instructions2 = new GLabel("Space = Shoot");
		GLabel instructions3 = new GLabel("Arrow Keys = Move");
		GLabel instructions4 = new GLabel("Shift = Hold Ship Stationary");
		GLabel instructions5 = new GLabel("Double tap left or right to do a barrel roll!");
		instructions1.setColor(Color.GREEN);
		instructions2.setColor(Color.GREEN);
		instructions3.setColor(Color.GREEN);
		instructions4.setColor(Color.GREEN);
		instructions5.setColor(Color.GREEN);
		instructions1.setFont("Serif-30");
		instructions2.setFont("Serif-30");
		instructions3.setFont("Serif-30");
		instructions4.setFont("Serif-30");
		instructions5.setFont("Serif-30");
		instructions1.setLocation(getWidth() / 2 - instructions1.getWidth() / 2, getHeight() / 5 + 100);
		instructions2.setLocation(getWidth() / 2 - instructions2.getWidth() / 2, getHeight() / 5 + 150);
		instructions3.setLocation(getWidth() / 2 - instructions3.getWidth() / 2, getHeight() / 5 + 200);
		instructions4.setLocation(getWidth() / 2 - instructions4.getWidth() / 2, getHeight() / 5 + 250);
		instructions5.setLocation(getWidth() / 2 - instructions5.getWidth() / 2, getHeight() / 5 + 300);
		add(instructions1);
		add(instructions2);
		add(instructions3);
		add(instructions4);
		add(instructions5);
		GLabel startingLabel = new GLabel("STARTS IN 3", 0, 0);
		startingLabel.setColor(Color.RED);
		startingLabel.setFont("Lucida-Bold-72");
		startingLabel.setLocation(getWidth() / 2 - startingLabel.getWidth() / 2, 4 * getHeight() / 5);
		add(startingLabel);
		for (int i = 10; i > 0; i--){
			startingLabel.setLabel("STARTS IN " + i);
			pause(1000);
		}
		remove(instructions1);
		remove(instructions2);
		remove(instructions3);
		remove(instructions4);
		remove(instructions5);
		remove(startingLabel);
		remove(gameTitle);
	} 



}
