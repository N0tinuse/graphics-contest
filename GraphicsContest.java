/*
 * File: GraphicsContest.java
 * --------------------------
 */

import java.awt.Color;

import acm.program.*;
import acm.graphics.*;
import java.awt.event.*;
import java.awt.*;
import java.util.*;
import acm.util.*;

/* GImage credits:
 * star background found at a1star.com
 */

public class GraphicsContest extends GraphicsProgram {

	private static final int PROGRAM_WIDTH = 1600;
	private static final int PROGRAM_HEIGHT = 800;
	private GImage gameArea;
	private GLabel gameTitle;
	private GLabel highScoreButton;
	private GLabel playGameButton;
	private GImage ship;

	private boolean shipMovingUp;
	private boolean shipMovingDown;
	private boolean shipMovingRight;
	private boolean shipMovingLeft;
	private int shipMovementX;
	private int shipMovementY;
	private static final int shipMovementValue = 5;
	private int shipImageConstant;
	private int shipResetCounter;

	private GOval[] bullets;
	private double[] bulletVelocities;
	private static final int BULLET_INITIAL_SIZE = 60;
	private int bulletCounter;
	private boolean bulletsPresent = false;

	private int enemyCounter;
	private RandomGenerator rgen = RandomGenerator.getInstance();
	private GRect[] enemies;
	private boolean enemiesPresent = false;
	private static final int ENEMY_INITIAL_SIZE = 1;
	private GOval[] enemyBullets;
	private double[] enemyXBulletVelocities;
	private double[] enemyYBulletVelocities;
	private int enemyBulletCounter;
	private boolean enemyBulletsPresent = false;
	
	private int lives;
	private GImage[] lifeLabels;
	private GLabel livesLabel;
	private boolean lifeLost = false;

	private int loopCounter;
	private boolean bossPresent = false;
	
	private int score;
	private GLabel scoreLabel;

	public void init() {
		setSize(PROGRAM_WIDTH, PROGRAM_HEIGHT);
		gameArea = new GImage("starbackground.gif");
		gameArea.setSize(PROGRAM_WIDTH, PROGRAM_HEIGHT);
		add(gameArea);
		addMouseListeners();

	}

	public void run() {
		/* makeInitialLabels();
		shipMovementX = 5;
		shipMovementY = 5;
		 */
		playGame();
	}

	public void mouseClicked(MouseEvent e) {
		GObject clickedLabel = getElementAt(e.getX(), e.getY());
		if (clickedLabel == playGameButton) {
			removeAll();
			playGameButton.setLocation(900,900);
			gameTitle.setLocation(900,900);
			highScoreButton.setLocation(900,900);
			add(gameArea);
			playGame();
		}
	}

	private void playGame() {
		addKeyListeners();
		setUpGame();
		while(lives != -1) {
			checkforShipCollisions();
			moveShip();
			checkforShipChange();
			if (!bossPresent) {
				normalGameProcedure(); 
			}
			scoreLabel.setLabel("Score: " + score);
			remove(ship);
			add(ship);
			loopCounter++;
			pause(5);
		}
		removeAll();
		GLabel gameOver = new GLabel("Game Over", 0, 0);
		gameOver.setFont("Sans Serif-100");
		gameOver.setColor(Color.RED);
		gameOver.setLocation(getWidth() / 2 - gameOver.getWidth() / 2, getHeight() / 2 - gameOver.getAscent() / 2);
		add(gameOver);
	}
	
	private void bossProcedure() {
		
	}

	private void normalGameProcedure() {
		if (bulletsPresent) {
			for (int i = 0; i < bullets.length; i++) {
				if (bullets[i] == null) break;
				if (bullets[i].getX() != 2000) {
					bullets[i].move(bulletVelocities[i], 0);
					bullets[i].setSize(bullets[i].getWidth() - 0.5, bullets[i].getHeight() - 0.5);
					if (bullets[i].getWidth() == 0) {
						remove(bullets[i]);
						bullets[i].setLocation(2000,900);
					}
					if (bulletCollisionChecker(bullets[i]) instanceof GRect && 3 * bulletCollisionChecker(bullets[i]).getWidth() / 5 >= bullets[i].getWidth()) {
						bulletCollisionChecker(bullets[i]).setLocation(2500, 1500);
						bullets[i].setLocation(2000,900);
						score += 50;
					} 
				}
			}
		}
		int monsterDeterminant = rgen.nextInt(5000);
		if (monsterDeterminant >= 4990) {
			spawnEnemy();
		}
		if (enemiesPresent) {
			for (int i = 0; i < enemies.length; i++) {
				if (enemies[i] == null) break;
				enemies[i].setLocation(enemies[i].getX() - 0.05, enemies[i].getY() - 0.05);
				enemies[i].setSize(enemies[i].getWidth() + 0.1, enemies[i].getHeight() + 0.1);
				int bulletDeterminant = rgen.nextInt(5000);
				if (bulletDeterminant >= 4970 && enemies[i].getWidth() < 20) {
					spawnEnemyBullet(enemies[i], ship);
				}
				if (enemyCollisionChecker(enemies[i]) == ship && enemies[i].getWidth() >= 80) {
					for (int k = 0; k < enemies.length; k++) {
						if (enemies[k] != null) {
							enemies[k].setLocation(2500, 900);
						}
					}
					processDeath();
				}
				if (enemies[i].getWidth() >= 100) {
					enemies[i].setLocation(2500,1500);
					remove(enemies[i]);
				}
			}
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
						for (int k = 0; k < enemyBullets.length; k++) {
							if (enemyBullets[k] != null) {
								enemyBullets[k].setLocation(2200, 900);
							}
						}
						processDeath();
					}
					if (enemyBullets[i].getWidth() > 60) {
						remove(enemyBullets[i]);
						enemyBullets[i].setLocation(2200,1200);
					}

				}
			}
		}
	}

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
		lifeLabels = new GImage[3];
		for (int i = 0; i < lifeLabels.length; i++) {
			lifeLabels[i] = new GImage("placeholder.jpg");
			lifeLabels[i].setLocation(getWidth() - 180 + i * 60, 0);
			lifeLabels[i].setSize(60, 60);
			add(lifeLabels[i]);
		}
		bulletCounter = 0;
		bullets = new GOval[100];
		bulletVelocities = new double[100];
		enemies = new GRect[100];
		enemyBullets = new GOval[100];
		enemyXBulletVelocities = new double[100];
		enemyYBulletVelocities = new double[100];
		enemyBulletCounter = 0;
		enemyCounter = 0;
		score = 0;
		shipResetCounter = 0;
		loopCounter = 0;
		lives = 3;
	}

	private void processDeath() {
		for (int j = 0; j < lives; j++) {
			remove(lifeLabels[j]);
		}
		lives--;
		removeAll();
		add(gameArea);
		add(scoreLabel);
		add(livesLabel);
		for (int j = 0; j < lives; j++) {
			add(lifeLabels[j]);
		}
		GLabel death = new GLabel("You died!", 0, 0);
		death.setColor(Color.RED);
		death.setFont("Sans Serif-100");
		death.setLocation(getWidth() / 2 - death.getWidth() / 2, getHeight() / 2 - death.getAscent() / 2);
		add(death);
		lifeLost = true;
		pause(2000);
		lifeLost = false;
		remove(death);
		ship.setLocation(getWidth() / 2 - ship.getWidth() / 2, getHeight() / 2 - ship.getHeight() / 2);
		add(ship);
		score -= 500;
	}
	
	private GObject enemyBulletCollisionChecker(GOval enemyBullet) {
		if (getElementAt (enemyBullet.getX(), enemyBullet.getY()) != null) {
			return (getElementAt (enemyBullet.getX(), enemyBullet.getY()));
		} else if (getElementAt (enemyBullet.getX(), enemyBullet.getY() + enemyBullet.getHeight()) != null) {
			return (getElementAt (enemyBullet.getX(), enemyBullet.getY() + enemyBullet.getHeight()));
		} else if (getElementAt (enemyBullet.getX() + enemyBullet.getWidth(), enemyBullet.getY()) != null) {
			return (getElementAt (enemyBullet.getX() + enemyBullet.getWidth(), enemyBullet.getY()));
		} else if (getElementAt (enemyBullet.getX() + enemyBullet.getWidth(), enemyBullet.getY() + enemyBullet.getHeight()) != null) {
			return (getElementAt (enemyBullet.getX() + enemyBullet.getWidth(), enemyBullet.getY() + enemyBullet.getHeight()));
		} else {
			return null;
		}
	}
	
	private GObject enemyCollisionChecker(GRect enemy) {
		if (getElementAt (enemy.getX(), enemy.getY()) != null) {
			return (getElementAt (enemy.getX(), enemy.getY()));
		} else if (getElementAt (enemy.getX(), enemy.getY() + enemy.getHeight()) != null) {
			return (getElementAt (enemy.getX(), enemy.getY() + enemy.getHeight()));
		} else if (getElementAt (enemy.getX() + enemy.getWidth(), enemy.getY()) != null) {
			return (getElementAt (enemy.getX() + enemy.getWidth(), enemy.getY()));
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

	private void spawnEnemyBullet(GRect enemy, GImage ship) {
		GOval newBullet = new GOval(enemy.getX() + enemy.getWidth() / 5, enemy.getY() + enemy.getHeight() / 5, 3 * enemy.getWidth() / 5, 3 * enemy.getHeight() / 5);
		newBullet.setColor(Color.BLUE);
		newBullet.setFilled(true);
		enemyBullets[enemyBulletCounter] = newBullet;
		enemyXBulletVelocities[enemyBulletCounter] = ((ship.getX() + ship.getWidth() / 2 - 30) - (enemy.getX() + enemy.getWidth() / 2)) / (double)480;
		enemyYBulletVelocities[enemyBulletCounter] = ((ship.getY() + ship.getHeight() / 2 - 30) - (enemy.getY() + enemy.getHeight() / 2)) / (double)480;
		add(newBullet);
		enemyBulletCounter++;
		enemyBulletsPresent = true;
		if (enemyBulletCounter == 100) enemyBulletCounter = 0;
	} 

	private void spawnEnemy() {
		GRect newEnemy = new GRect(800, 800, ENEMY_INITIAL_SIZE, ENEMY_INITIAL_SIZE);
		newEnemy.setColor(Color.PINK);
		newEnemy.setFilled(true);
		newEnemy.setLocation(rgen.nextInt(236, getWidth() - 236), rgen.nextInt(100, getHeight() - 100));
		enemies[enemyCounter] = newEnemy;
		remove(ship);
		add(enemies[enemyCounter]);
		add(ship);
		enemyCounter++;
		enemiesPresent = true;
		if (enemyCounter == 100) enemyCounter = 0;
	}

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

	private void checkforShipChange() {
		if (ship.getX() < 8 * (getWidth() - 236) && shipImageConstant != 8) shipResetCounter++;
		if (ship.getX() > 9 * (getWidth() - 236) && shipImageConstant != 8) shipResetCounter--;
		if (ship.getX() < (getWidth() - 236) / (double)17) {
			ship.setImage("Ship_00flipped.png"); 
			shipImageConstant = 0;
		} else if (ship.getX() < 2 * (getWidth() - 236) / (double)17 && ship.getX() > (getWidth() - 236) / (double)17 || shipResetCounter < -70 && shipResetCounter >= -80) {
			ship.setImage("Ship_01flipped.png"); 
			shipImageConstant = 1;
		} else if (ship.getX() < 3 * (getWidth() - 236) / (double)17 && ship.getX() > 2 * (getWidth() - 236) / (double)17 || shipResetCounter < -60 && shipResetCounter >= -70) { 
			ship.setImage("Ship_02flipped.png"); 
			shipImageConstant = 2;
		} else if (ship.getX() < 4 * (getWidth() - 236) / (double)17 && ship.getX() > 3 * (getWidth() - 236) / (double)17 || shipResetCounter < -50 && shipResetCounter >= -60) {
			ship.setImage("Ship_03flipped.png"); 
			shipImageConstant = 3;
		} else if (ship.getX() < 5 * (getWidth() - 236) / (double)17 && ship.getX() > 4 * (getWidth() - 236) / (double)17 || shipResetCounter < -40 && shipResetCounter >= -50) {
			ship.setImage("Ship_04flipped.png"); 
			shipImageConstant = 4;
		} else if (ship.getX() < 6 * (getWidth() - 236) / (double)17 && ship.getX() > 5 * (getWidth() - 236) / (double)17 || shipResetCounter < -30 && shipResetCounter >= -40) {
			ship.setImage("Ship_05flipped.png"); 
			shipImageConstant = 5;
		} else if (ship.getX() < 7 * (getWidth() - 236) / (double)17 && ship.getX() > 6 * (getWidth() - 236) / (double)17 || shipResetCounter < -20 && shipResetCounter >= -30) {
			ship.setImage("Ship_06flipped.png"); 
			shipImageConstant = 6;
		} else if (ship.getX() < 8 * (getWidth() - 236) / (double)17 && ship.getX() > 7 * (getWidth() - 236) / (double)17 || shipResetCounter < -10 && shipResetCounter >= -20) {
			ship.setImage("Ship_07flipped.png"); 
			shipImageConstant = 7;
		} else if (ship.getX() < 9 * (getWidth() - 236) / (double)17 && ship.getX() > 8 * (getWidth() - 236) / (double)17) {
			ship.setImage("Ship_08.png"); 
			shipImageConstant = 8;
		} else if (ship.getX() < 10 * (getWidth() - 236) / (double)17 && ship.getX() > 9 * (getWidth() - 236) / (double)17 || shipResetCounter <= 10 && shipResetCounter > 0) {
			ship.setImage("Ship_07.png"); 
			shipImageConstant = 9;
		} else if (ship.getX() < 11 * (getWidth() - 236) / (double)17 && ship.getX() > 10 * (getWidth() - 236) / (double)17 || shipResetCounter <= 20 && shipResetCounter > 10) {
			ship.setImage("Ship_06.png"); 
			shipImageConstant = 10;
		} else if (ship.getX() < 12 * (getWidth() - 236) / (double)17 && ship.getX() > 11 * (getWidth() - 236) / (double)17 || shipResetCounter <= 30 && shipResetCounter > 20) {
			ship.setImage("Ship_05.png"); 
			shipImageConstant = 11;
		} else if (ship.getX() < 13 * (getWidth() - 236) / (double)17 && ship.getX() > 12 * (getWidth() - 236) / (double)17 || shipResetCounter <= 40 && shipResetCounter > 30) {
			ship.setImage("Ship_04.png"); 
			shipImageConstant = 12;
		} else if (ship.getX() < 14 * (getWidth() - 236) / (double)17 && ship.getX() > 13 * (getWidth() - 236) / (double)17 || shipResetCounter <= 50 && shipResetCounter > 40) {
			ship.setImage("Ship_03.png"); 
			shipImageConstant = 13;
		} else if (ship.getX() < 15 * (getWidth() - 236) / (double)17 && ship.getX() > 14 * (getWidth() - 236) / (double)17 || shipResetCounter <= 60 && shipResetCounter > 50) {
			ship.setImage("Ship_02.png"); 
			shipImageConstant = 14;
		} else if (ship.getX() < 16 * (getWidth() - 236) / (double)17 && ship.getX() > 15 * (getWidth() - 236) / (double)17 || shipResetCounter <= 70 && shipResetCounter > 60) { 
			ship.setImage("Ship_01.png"); 
			shipImageConstant = 15;
		} else if (ship.getX() > 16 * (getWidth() - 236) / (double)17 || shipResetCounter <= 80 && shipResetCounter > 70) {
			ship.setImage("Ship_00.png"); 
			shipImageConstant = 16;
		}
	}

	private void moveShip() {
		if (shipMovingUp && shipMovingLeft) {
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

	public void keyPressed(KeyEvent e) {
		switch (e.getKeyCode()) {
		case KeyEvent.VK_UP: shipMovingUp = true; break;
		case KeyEvent.VK_DOWN: shipMovingDown = true; break;
		case KeyEvent.VK_LEFT: shipMovingLeft = true; break;
		case KeyEvent.VK_RIGHT: shipMovingRight = true; break;
		case KeyEvent.VK_SHIFT: shipMovementX = 0;
		shipMovementY = 0; break;
		}
	}

	public void keyReleased(KeyEvent e) {
		switch (e.getKeyCode()) {
		case KeyEvent.VK_UP: shipMovingUp = false; break;
		case KeyEvent.VK_DOWN: shipMovingDown = false; break;
		case KeyEvent.VK_LEFT: shipMovingLeft = false; break;
		case KeyEvent.VK_RIGHT: shipMovingRight = false; break;
		}
	}

	public void keyTyped(KeyEvent e) {
		if (e.getKeyChar() == KeyEvent.VK_SPACE) {
			if (!lifeLost) {
				GOval newBullet = new GOval (800, 800, BULLET_INITIAL_SIZE, BULLET_INITIAL_SIZE);
				newBullet.setColor(Color.GRAY);
				newBullet.setFilled(true);
				if (shipImageConstant == 0) {
					newBullet.setLocation(ship.getX() + 4 * ship.getWidth() / 5, ship.getY() + ship.getHeight() / 3);
					bulletVelocities[bulletCounter] = .8;
				} else if (shipImageConstant == 1) {
					newBullet.setLocation(ship.getX() + 4 * ship.getWidth() / 5, ship.getY() + ship.getHeight() / 3);
					bulletVelocities[bulletCounter] = .7;
				} else if (shipImageConstant == 2) {
					newBullet.setLocation(ship.getX() + 4 * ship.getWidth() / 5, ship.getY() + ship.getHeight() / 3);
					bulletVelocities[bulletCounter] = .6;
				} else if (shipImageConstant == 3) {
					newBullet.setLocation(ship.getX() + 4 * ship.getWidth() / 5, ship.getY() + ship.getHeight() / 3);
					bulletVelocities[bulletCounter] = .5;
				} else if (shipImageConstant == 4) {
					newBullet.setLocation(ship.getX() + 4 * ship.getWidth() / 5, ship.getY() + ship.getHeight() / 3);
					bulletVelocities[bulletCounter] = .4;
				} else if (shipImageConstant == 5) {
					newBullet.setLocation(ship.getX() + 4 * ship.getWidth() / 5, ship.getY() + ship.getHeight() / 3);
					bulletVelocities[bulletCounter] = .3;
				} else if (shipImageConstant == 6) {
					newBullet.setLocation(ship.getX() + 4 * ship.getWidth() / 5, ship.getY() + ship.getHeight() / 3);
					bulletVelocities[bulletCounter] = .2;
				} else if (shipImageConstant == 7) {
					newBullet.setLocation(ship.getX() + 4 * ship.getWidth() / 5, ship.getY() + ship.getHeight() / 3);
					bulletVelocities[bulletCounter] = .1;
				} else if (shipImageConstant == 8) {
					newBullet.setLocation(ship.getX() + ship.getWidth() / 2, ship.getY() + ship.getHeight() / 3);
					bulletVelocities[bulletCounter] = 0;
				} else if (shipImageConstant == 9) {
					newBullet.setLocation(ship.getX(), ship.getY() + ship.getHeight() / 3);
					bulletVelocities[bulletCounter] = -.1;
				} else if (shipImageConstant == 10) {
					newBullet.setLocation(ship.getX(), ship.getY() + ship.getHeight() / 3);
					bulletVelocities[bulletCounter] = -.2;
				} else if (shipImageConstant == 11) {
					newBullet.setLocation(ship.getX(), ship.getY() + ship.getHeight() / 3);
					bulletVelocities[bulletCounter] = -.3;
				} else if (shipImageConstant == 12) {
					newBullet.setLocation(ship.getX(), ship.getY() + ship.getHeight() / 3);
					bulletVelocities[bulletCounter] = -.4;
				} else if (shipImageConstant == 13) {
					newBullet.setLocation(ship.getX(), ship.getY() + ship.getHeight() / 3);
					bulletVelocities[bulletCounter] = -.5;
				} else if (shipImageConstant == 14) {
					newBullet.setLocation(ship.getX(), ship.getY() + ship.getHeight() / 3);
					bulletVelocities[bulletCounter] = -.6;
				} else if (shipImageConstant == 15) {
					newBullet.setLocation(ship.getX(), ship.getY() + ship.getHeight() / 3);
					bulletVelocities[bulletCounter] = -.7;
				} else if (shipImageConstant == 16) {
					newBullet.setLocation(ship.getX(), ship.getY() + ship.getHeight() / 3);
					bulletVelocities[bulletCounter] = -.8;
				}
				bullets[bulletCounter] = newBullet;
				remove(ship);
				add(bullets[bulletCounter]);
				add(ship);
				bulletCounter++;
				if (bulletCounter == 100) bulletCounter = 0;
				bulletsPresent = true;
			}
		}
	}

	/* private void makeInitialLabels() {
		gameTitle = new GLabel("STARFIGHTER 106A", 0, 0);
		gameTitle.setColor(Color.RED);
		gameTitle.setFont("Lucida-Bold-72");
		gameTitle.setLocation(getWidth() / 2 - gameTitle.getWidth() / 2, getHeight() / 5);
		add(gameTitle);
		playGameButton = new GLabel("Play Game", 0, 0);
		playGameButton.setColor(Color.WHITE);
		playGameButton.setFont("Sans Serif-36");
		playGameButton.setLocation(getWidth() / 2 - playGameButton.getWidth() / 2, getHeight() / 2 + playGameButton.getAscent()/2);
		add(playGameButton);
		highScoreButton = new GLabel ("High Scores", 0, 0);
		highScoreButton.setColor(Color.WHITE);
		highScoreButton.setFont("Sans Serif-36");
		highScoreButton.setLocation(getWidth() / 2 - highScoreButton.getWidth() / 2, 3 * getHeight() / 4);
		add(highScoreButton);
	} */



}
