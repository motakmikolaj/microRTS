//System.err.println(Integer.toString(i) + ", " + Integer.toString(j));
//gameManager.addToGameSummary("text");
package com.codingame.game;

import com.codingame.game.engine.*;
import static com.codingame.game.engine.Constants.*;

import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.LinkedList;

import com.codingame.gameengine.core.AbstractPlayer.TimeoutException;
import com.codingame.gameengine.core.AbstractReferee;
import com.codingame.gameengine.core.MultiplayerGameManager;
import com.codingame.gameengine.core.Tooltip;

import com.codingame.gameengine.module.tooltip.TooltipModule;

import com.codingame.gameengine.module.entities.GraphicEntityModule;
import com.codingame.gameengine.module.entities.Sprite;
import com.codingame.gameengine.module.entities.Text;

import com.codingame.view.AnimatedEventModule;
import com.codingame.view.ViewerEvent;
import com.google.inject.Inject;


public class Referee extends AbstractReferee {
    @Inject private MultiplayerGameManager<Player> gameManager;
    @Inject private GraphicEntityModule graphicEntityModule;
    @Inject private AnimatedEventModule animatedEventModule;
    @Inject TooltipModule tooltips;
    
    private Arena ARENA;
	
	// ==============================
	// Functions
	private void sentBasePlayerInputs() {
        for (Player p : gameManager.getActivePlayers()) {
			// SEND ENTITY_TYPE DATA:
			// entityType, maxHP, reach, attack, step, goldCost, woodCost
			p.sendInputLine(String.format("%d %d %d", Constants.WIDTH_TILES, Constants.HEIGHT_TILES, Constants.START_GOLD));
				
			for (int i = 0; i < Constants.ENTITY_TYPES_NAMES.length; i++) {
				int [] temp_etype_array = ARENA.getETypes().get(Constants.ENTITY_TYPES_NAMES[i]).clone();
				String basePlayerInputs = String.valueOf(temp_etype_array[0]);
				//no size, no creationTime (a place to expand the project)
				for (int j = 1; j < 7; j++) {
					basePlayerInputs += " " + String.valueOf(temp_etype_array[j]);
				}
				p.sendInputLine(basePlayerInputs);
			}
        }
    }
    
    private void sendPlayerInputs() {
        //List<Player> allPlayers = gameManager.getPlayers();
        for (Player p : gameManager.getActivePlayers()) {
			// SEND ARENA DATA:
			p.sendInputLine(String.valueOf(ARENA.getEntities().size()));
			for (int i = 0; i < Constants.WIDTH_TILES; i++) {
				for (int j = 0; j < Constants.HEIGHT_TILES; j++) {
					// x | y | ownerID (-1:neutral,0:player1,1:player2) | entityType (empty, gold, worker, castle, etc.) | health
					if (!ARENA.isTileEmpty(i,j)) {
						String inputText = "";
						if (ARENA.getTileOwner(i,j) == 0) {
							inputText = String.format("%d %d %d %d %d", i, j, -1, ARENA.getTileType(i,j), ARENA.getTileHealth(i,j));
						} else {
							int tempOwnerID = (p.getIndex() + ARENA.getTileOwner(i,j) - 1) % 2;
							inputText = String.format("%d %d %d %d %d", i, j, tempOwnerID, ARENA.getTileType(i,j), ARENA.getTileHealth(i,j));
						}
						p.sendInputLine(inputText);
					}
				}
			}
            p.execute();
        }
    }
    private void printArena() {
		for (int i = 0; i < Constants.WIDTH_TILES; i++) {
			for (int j = 0; j < Constants.HEIGHT_TILES; j++) {
				//ARENA_MAP[i][j] = (ownerID, entityType, tilePointer, health,  ifLocked
				String tile_filename = "0-0.png", tile_health = String.valueOf(ARENA.getTileHealth(i,j));
				int tile_fontsize = 30;
				
				if (tile_health.equals("-1")) tile_health   = "∞";
				if (tile_health.equals("0"))  tile_health   = "";
				if (tile_health.equals("∞"))  tile_fontsize = 50;
				
				// updating sprites
				if (ARENA.getTilePointer(i,j) == 0){
					tile_filename = String.format("%d-%d.png", ARENA.getTileOwner(i,j), ARENA.getTileType(i,j));
				}
				
				// check, if sprite changed
				if (!ARENA.isTileSpriteEqual(i, j, tile_filename)) {
					if (tile_health.equals("")) {
						graphicEntityModule.commitEntityState(0, ARENA.setTileSpriteInvisible(i, j, tile_filename));
					} else {
						graphicEntityModule.commitEntityState(0, ARENA.setTileSpriteVisible(i, j, tile_filename));
					}
				}
					
				// display health
				int newX = (int)((Double.valueOf(i)+0.5)  * Double.valueOf(Constants.TILE_SIZE));
				int newY = (int)((Double.valueOf(j)+0.75) * Double.valueOf(Constants.TILE_SIZE));
				
				// check, if text changed
				if (!ARENA.isTileTextEqual(i, j, tile_health)) {
					// checks if it's not a wall (don't display health of unbreakable walls)
					if (!ARENA.isTileWall(i,j)){
						graphicEntityModule.commitEntityState(0, ARENA.setTileText(i, j, tile_health, newX, newY, tile_fontsize));
					}
				}
			}
		}
	}
	
    @Override
    public void init() {
		// setting Game Variables
        gameManager.setFrameDuration(Constants.FRAME_DURATION); 
        gameManager.setMaxTurns(Constants.MAX_TURNS);
        gameManager.setFirstTurnMaxTime(Constants.MAX_FIRST_TURN_TIME);
        gameManager.setTurnMaxTime(Constants.MAX_TURN_TIME);
  
  
        ARENA = new Arena(gameManager, graphicEntityModule, tooltips);
        ARENA.initArena();
        
		// =====
        // sending all base data
		sentBasePlayerInputs();
    }
    
    // =====
    // Checks if eType1 can build eType2
	private boolean checkBuildEntityTypeValidity(int eType1, int eType2) { 
		// non-existent/mismatched eType2
		if (eType2 == -1) {
			return false;
		// WORKER: BUILD CASTLE | BUILD BARRACKS
		} else if (eType1 == 2) { 
			return (eType2 == 0 || eType2 == 1); 
		// non-existent/mismatched eType1
		} else {
			return false;
		}
	}
	// Checks if eType1 can train eType2
	private boolean checkTrainEntityTypeValidity(int eType1, int eType2) {
		// non-existent/mismatched eType2
		if (eType2 == -1) {
			return false;
		// CASTLE: TRAIN WORKER
		} else if (eType1 == 0) {
			return (eType2 == 2); 
		// BARRACKS: TRAIN LIGHT | TRAIN HEAVY | TRAIN RANGED
		} else if (eType1 == 1) { 
			return (eType2 == 3 || eType2 == 4 || eType2 == 5); 
		// non-existent/mismatched eType1
		} else {
			return false;
		}
	}
	// Checks if eType exists and gives it back in numerical representation (e.g. "CASTLE"/"0" => "0")
	private String matchEntityTypeValue(String eType) {
		for (int i = 0; i < Constants.ENTITY_TYPES_NAMES.length; i++) {
			if (eType.equals(String.valueOf(i)) || eType.equals(Constants.ENTITY_TYPES_NAMES[i])) {
				return String.valueOf(i);
			}
		}
		return "-1";
	}
	// Checks if coordinates are in arena bounds
    private boolean checkIfInBounds(int x, int y) {
		return 0 <= x && x < Constants.WIDTH_TILES && 0 <= y && y < Constants.HEIGHT_TILES;
	}
	// Checks if there is mine/forest at those coordinates
	private boolean checkIfHarvestable(int x, int y) {
		return checkIfInBounds(x, y) && (ARENA.isTileMine(x,y) || ARENA.isTileForest(x,y));
	}
	// Checks if there is an entity at those coordinates that is owned by ownerID
    private boolean checkPlayersEntityExists(int x, int y, int ownerID) {
		return checkIfInBounds(x, y) && ARENA.checkTileOwnership(x, y, ownerID);
	}
	
	private boolean checkIfKnowsOrder(String orderName, int x, int y) {
		String entityName = Constants.ENTITY_TYPES_NAMES[ARENA.getTileType(x, y)];
		if (entityName.equals("CASTLE"))   return orderName.equals("TRAIN");
		if (entityName.equals("BARRACKS")) return orderName.equals("TRAIN");
		if (entityName.equals("WORKER"))   return orderName.equals("BUILD") || orderName.equals("MOVE") || orderName.equals("ATTACK") || orderName.equals("HARVEST");
		if (entityName.equals("LIGHT"))    return orderName.equals("MOVE")  || orderName.equals("ATTACK");
		if (entityName.equals("HEAVY"))    return orderName.equals("MOVE")  || orderName.equals("ATTACK");
		if (entityName.equals("RANGED"))   return orderName.equals("MOVE")  || orderName.equals("ATTACK");
		return false;
	}
	
	// ==============================
	// checking validity of the order and adding it into the queue
    private void checkOrder(Player p, String order) {
		String[] words = order.split(" ");
		int wordslen = words.length;
		
		if (wordslen == 1) {
			// ===== WAIT =====
			if (!words[0].equals("WAIT")) p.deactivate(String.format("Incorrect order: wrong syntax! \"%s\"", order));
		
		} else if (wordslen < 1 || wordslen == 2 || wordslen == 4 || wordslen > 6) {
			p.deactivate(String.format("Incorrect order: wrong amount of arguments! \"%s\"", order));
			
		} else if (checkPlayersEntityExists(Integer.valueOf(words[1]), Integer.valueOf(words[2]), p.getIndex()+1)) {
			// ===== ORDER X Y ... =====
			int w1 = Integer.valueOf(words[1]), w2 = Integer.valueOf(words[2]);
			int tempIndex = ARENA.findRTSEntity(w1, w2);
			if (tempIndex == -1) { 
				p.deactivate("Unknown error: not properly initialised entity! Contact the author!");
				
			} else if (!checkIfKnowsOrder(words[0], w1, w2)) {
				p.deactivate(String.format("Incorrect order: entity doesn't know this order! \"%s\"", order));
				
			} else if (wordslen == 3) {
				//not really appliable for the modern version of the project, but still working to reset orders for entity, more of a hidden debug option
				// ===== STOP X Y ===== 
				if (words[0].equals("STOP")) {
					ARENA.getEntities().get(tempIndex).actionCountdown = 0;
					ARENA.getEntities().get(tempIndex).activeOrder = "WAIT";
					ARENA.getEntities().get(tempIndex).clearOrders();
					ARENA.setTileLockStatus(w1, w2, -1); // locking entity in place
				} else {
					p.deactivate(String.format("Incorrect order: wrong syntax! \"%s\"", order));
				}
				
			} else if (wordslen == 5) {
				int w3 = Integer.valueOf(words[3]), w4 = Integer.valueOf(words[4]);
				// ===== MOVE X Y tX tY =====
				if (words[0].equals("MOVE")){
					int step = ARENA.getTileStep(w1,w2);
					if (checkIfInBounds(w3, w4) && ifInRange(w1, w2, w3, w4, step)) {
						ARENA.getEntities().get(tempIndex).addLastOrder(order);
						ARENA.setTileLockStatus(w1, w2, 0); // unlocking entity to potentially move
					} else {
						p.deactivate(String.format("Incorrect MOVE order: target tile out of reach! \"%s\"", order));
					}
					
				// ===== ATTACK X Y tX tY =====	
				} else if (words[0].equals("ATTACK")) {
					int reach = ARENA.getTileReach(w1,w2);
					if (!ifInRange(w1, w2, w3, w4, reach)) {
						p.deactivate(String.format("Incorrect ATTACK order: target tile out of reach! \"%s\"", order));
						
					} else if (!checkPlayersEntityExists(w3, w4, (p.getIndex()+1)%2 + 1)) {
						p.deactivate(String.format("Incorrect ATTACK order: target tile doesn't have entity owned by enemy player! \"%s\"", order));
						
					} else {
						ARENA.getEntities().get(tempIndex).addLastOrder(order);
						ARENA.setTileLockStatus(w1, w2, -1); // locking entity in place
					}
					
				// ===== HARVEST X Y tX tY =====
				} else if (words[0].equals("HARVEST")) {
					int reach = ARENA.getTileReach(w1,w2);
					if (!ifInRange(w1, w2, w3, w4, reach)) {
						p.deactivate(String.format("Incorrect HARVEST order: target tile out of reach! \"%s\"", order));
						
					} else if (!checkIfHarvestable(w3, w4)) {
						p.deactivate(String.format("Incorrect HARVEST order: target tile doesn't have harvestable resource! \"%s\"", order));
						
					} else {
						ARENA.getEntities().get(tempIndex).addLastOrder(order);
						ARENA.setTileLockStatus(w1, w2, -1); // locking entity in place
					}
					
				} else {
						p.deactivate(String.format("Incorrect order: wrong syntax! \"%s\"", order));
				}
				
			} else if (wordslen == 6) {
				int w3 = Integer.valueOf(words[3]), w4 = Integer.valueOf(words[4]);
					
				// ===== BUILD X Y tX tY eT =====
				if (words[0].equals("BUILD")){
					int reach = ARENA.getTileReach(w1,w2);
					if (checkIfInBounds(w3, w4) && ifInRange(w1, w2, w3, w4, reach)) {
						String tempEntityTypeValue = matchEntityTypeValue(words[5]);
						if (checkBuildEntityTypeValidity(ARENA.getTileType(w1,w2), Integer.valueOf(tempEntityTypeValue))) {
							ARENA.getEntities().get(tempIndex).addLastOrder(String.format("BUILD %s %s %s %s %s", words[1], words[2], words[3], words[4], tempEntityTypeValue));
							ARENA.setTileLockStatus(w1, w2, -1); // locking entity in place
							
						} else {
							p.deactivate(String.format("Incorrect BUILD order: entity can't build given entityType! \"%s\"", order));
						}
					} else {
						p.deactivate(String.format("Incorrect BUILD order: target tile out of reach! \"%s\"", order));
					}

				// ===== TRAIN X Y tX tY eT =====
				} else if (words[0].equals("TRAIN")){
					int reach = ARENA.getTileReach(w1,w2);
					if (checkIfInBounds(w3, w4) && ifInRange(w1, w2, w3, w4, reach)) {
						String tempEntityTypeValue = matchEntityTypeValue(words[5]);
						if (checkTrainEntityTypeValidity(ARENA.getTileType(w1,w2), Integer.valueOf(tempEntityTypeValue))) {
							ARENA.getEntities().get(tempIndex).addLastOrder(String.format("TRAIN %s %s %s %s %s", words[1], words[2], words[3], words[4], tempEntityTypeValue));
							ARENA.setTileLockStatus(w1, w2, -1); // locking entity in place
						} else {
							p.deactivate(String.format("Incorrect TRAIN order: entity can't train given entityType! \"%s\"", order));
						}
					} else {
						p.deactivate(String.format("Incorrect TRAIN order: target tile out of reach! \"%s\"", order));
					}
					
				// ===== UNKNOWN X Y tX tY eT =====
				} else {
						p.deactivate(String.format("Incorrect order: wrong syntax! \"%s\"", order));
				}
			
			// ===== word.length > 6 =====
			} else {
				p.deactivate(String.format("Incorrect order: wrong amount of arguments! \"%s\"", order));
			}
		
		// ===== ORDER X Y ... | wrong X or Y =====
		} else {
			p.deactivate(String.format("Incorrect order: tile doesn't have entity owned by player! \"%s\"", order));
		}
	}
	
	
	
	private int[] findMidStep (int[] stepRoad) {
		// STEP == 2
		// check direction
		// X X X X X
		// X 0 N 0 X
		// X W T E X
		// X 0 S 0 X
		// X X X X X
			
		int diffX = (stepRoad[4] - stepRoad[0])/2;
		int diffY = (stepRoad[5] - stepRoad[1])/2;
		int waitX = -1;
		int waitY = -1;
		int tX, tY;
		
		for (int i = -1; i < 2; i++){
			if (diffX == 0) {
				tX = stepRoad[4] + i;
				tY = stepRoad[5] - diffY;
			} else if (diffY == 0) {
				tX = stepRoad[4] - diffX;
				tY = stepRoad[5] + i;
			} else {
				tX = stepRoad[4] - diffX;
				tY = stepRoad[5] - diffY;
			}
			// if targetTile is empty, in range and accessible by walking
			if (ARENA.isTileEmpty(tX, tY)) {
				stepRoad[2] = tX;
				stepRoad[3] = tY;
				return stepRoad;
						
			// if targetTile is not empty - but other entity may move this turn, emptying it
			} else if (ARENA.getTileLockStatus(stepRoad[0], stepRoad[1]) == ARENA.getTileLockStatus(tX, tY)) {
				waitX = tX;
				waitY = tY;
			}
			// if targetTile is not empty and/or other entity has higher ifLocked count (likely a loop) = keep other values of waitX and waitY
		}
		stepRoad[2] = waitX;
		stepRoad[3] = waitY;
		return stepRoad;
	}
	
	private boolean ifInRange(int x, int y, int tx, int ty, int reach) {
		return (x-reach) <= tx && tx <= (x+reach) && (y-reach) <= ty && ty <= (y+reach);
	}
	
	private int executeBuildTrain(String order){
		String[] words = order.split(" ");
		int w1 = Integer.valueOf(words[1]), w2 = Integer.valueOf(words[2]); // builder/trainer entity
		int w3 = Integer.valueOf(words[3]), w4 = Integer.valueOf(words[4]); // targetEntity
		int w5 = Integer.valueOf(words[5]);                                 // targetEntityType
		
		int reach    = ARENA.getTileReach(w1,w2); // Reach
		int costGold = ARENA.getCostGold(w5);     // targetCostGold
		int costWood = ARENA.getCostWood(w5);     // targetCostWood
		
		for (Player p : gameManager.getActivePlayers()) {
			if ((p.getIndex()+1) == (ARENA.getTileOwner(w1,w2))) {
				//addPlayerEntity(x, y, ownerID, entityTypeName)
				if (p.gold() >= costGold && p.wood() >= costWood) {
					// if targetTile is empty and in reach
					if (ARENA.isTileEmpty(w3,w4) && ifInRange(w1, w2, w3, w4, reach)) {
						//addPlayerEntity(x, y, ownerID, entityTypeName)
						ARENA.addPlayerEntity(w3, w4, ARENA.getTileOwner(w1,w2), Constants.ENTITY_TYPES_NAMES[w5]);
						p.addMaterial("GOLD", -costGold);
						p.addMaterial("WOOD", -costWood);
						gameManager.addToGameSummary(String.format("%s: entity [%s, %s] created: %s at [%s, %s].\n", p.getNicknameToken(), words[1], words[2], Constants.ENTITY_TYPES_NAMES[w5], words[3], words[4]));
						return 0;
						
					// if targetTile is not empty - but other entity may move this turn, emptying it
					} else if (ARENA.getTileLockStatus(w3,w4) == 0) {
						return 1;
					}
					
				// insufficient resources
				} else {
					gameManager.addToGameSummary(String.format("%s [Warning] has insufficient resources to create new entity: %s!\n", p.getNicknameToken(), Constants.ENTITY_TYPES_NAMES[w5]));
					return 2;
				}
			}
		}
		// return non-possible creation as default return
		return 2;
	}
	
	private void executeOrders() {
		//String[] words = order.split(" ");
		LinkedList<String> bldQ = new LinkedList<String>();
		LinkedList<String> trnQ = new LinkedList<String>();
		LinkedList<String> movQ = new LinkedList<String>();
		
		LinkedList<String> bl2Q = new LinkedList<String>();
		LinkedList<String> tr2Q = new LinkedList<String>();
		
		LinkedList<String> hrvQ = new LinkedList<String>();
		LinkedList<String> attQ = new LinkedList<String>();
		// ==============================
		// splitting orders in executive priority: build > train > move > [remaining build > train] > harvest > attack
		for (RTSEntity e : ARENA.getEntities()) {
			// only execute orders of player entities
			if (!ARENA.isTileNeutral(Integer.valueOf(e.x), Integer.valueOf(e.y))) {
				// check if entity has orders
				if (!e.ifOrdersEmpty()) {
					String order = e.popFirstOrder();
					switch(order.split(" ")[0]) {
						case "BUILD":
							bldQ.addLast(order);
							break;
						case "TRAIN":
							trnQ.addLast(order);
							break;
						case "MOVE":
							movQ.addLast(order);
							break;
						case "HARVEST":
							hrvQ.addLast(order);
							break;
						case "ATTACK":
							attQ.addLast(order);
							break;
						default:
							gameManager.addToGameSummary("[Error] ExecuteOrders: This shouldn't happen!\n");
					}
				}
			}
		}
		// execute all possible BUILD orders
		while(!bldQ.isEmpty()) {
			// ===== BUILD X Y tX tY eT =====
			String order = bldQ.getFirst();
			bldQ.removeFirst();
			// execute order, if possible (try again, if tile may empty before the end of the turn).
			if (executeBuildTrain(order) == 1) bl2Q.addLast(order);
		}
		// execute all possible TRAIN orders
		while(!trnQ.isEmpty()) {
			// ===== TRAIN X Y tX tY eT =====
			String order = trnQ.getFirst();
			trnQ.removeFirst();
			// execute order, if possible (try again, if tile may empty before the end of the turn).
			if (executeBuildTrain(order) == 1) tr2Q.addLast(order);
		}
		// execute all possible MOVE orders
		while(!movQ.isEmpty()) {
			// ===== MOVE X Y tX tY =====
			String order = movQ.getFirst();
			movQ.removeFirst();
			
			String[] words = order.split(" ");
			int w1 = Integer.valueOf(words[1]), w2 = Integer.valueOf(words[2]); // playerEntity
			int w3 = Integer.valueOf(words[3]), w4 = Integer.valueOf(words[4]); // targetEntity
 
			int step      = ARENA.getTileStep(w1,w2);                                                  //entityStep
			int tempIndex = ARENA.findRTSEntity(Integer.valueOf(words[1]), Integer.valueOf(words[2])); //entityIndex	
			
			// checks if one step is needed
			if (ifInRange(w1, w2, w3, w4, 1)) {		
				// if targetTile is empty, in range and accessible by walking
				if (ARENA.isTileEmpty(w3,w4)) {
					// update ARENA_MAP tiles
					for (int k = 0; k < 5; k++) {
						ARENA.setTileValue(w3, w4, k, ARENA.getTileValue(w1, w2, k));
						ARENA.setTileValue(w1, w2, k, 0);
					}
					// update ENTITY_LIST
					ARENA.getEntities().get(tempIndex).x = w3; 
					ARENA.getEntities().get(tempIndex).y = w4;
					
					ARENA.setTileLockStatus(w3, w4, -1); //locking target tile after making move
					 
					gameManager.addToGameSummary(String.format("%s: entity [%s, %s] moved to [%s, %s].\n", gameManager.getPlayer(ARENA.getTileOwner(w3, w4)-1).getNicknameToken(), words[1], words[2], words[3], words[4]));
								
				// if targetTile is not empty - but other entity may move this turn, emptying it
				} else if (ARENA.getTileLockStatus(w1, w2) == ARENA.getTileLockStatus(w3, w4)) {
					ARENA.raiseTileLockStatus(w1, w2); //raising the ifLocked count, may still move after others
					movQ.addLast(order);
					
				// if targetTile is not empty - but other entity has higher ifLocked count (likely a loop)
				} else if (ARENA.getTileLockStatus(w1, w2) < ARENA.getTileLockStatus(w3, w4)) {
					ARENA.setTileLockStatus(w1, w2, -1); //locking the tile to not cause infinite loops
					
				// if targetTile is not empty and it won't empty this turn
				} else {
					ARENA.setTileLockStatus(w1, w2, -1); //locking the tile
				}
			
			// checks if two steps are needed
			} else if (ifInRange(w1, w2, w3, w4, 2)) {
				int[] stepRoad = {w1, w2, w3, w4, w3, w4}; 
				stepRoad = findMidStep(stepRoad).clone();
				if (stepRoad[2] == -1) {
					ARENA.setTileLockStatus(w1, w2, -1); //locking the tile, dropping the order, as there is no available mid-tiles
				} else {
					w3 = stepRoad[2]; 
					w4 = stepRoad[3];

					// if targetTile is empty, in range and accessible by walking
					if (ARENA.isTileEmpty(w3, w4)) {
						// update ARENA_MAP tiles
						for (int k = 0; k < 5; k++) {
							ARENA.setTileValue(w3, w4, k, ARENA.getTileValue(w1, w2, k));
							ARENA.setTileValue(w1, w2, k, 0);
						}
						// update ENTITY_LIST
						ARENA.getEntities().get(tempIndex).x = w3; 
						ARENA.getEntities().get(tempIndex).y = w4;
						
						ARENA.raiseTileLockStatus(w3, w4); //rising ifLocked value after making one step out of two
							 
						gameManager.addToGameSummary(String.format("%s: entity [%s, %s] moved to [%d, %d].\n", gameManager.getPlayer(ARENA.getTileOwner(w3, w4)-1).getNicknameToken(), words[1], words[2], w3, w4));
						
						// after successfully moving one step - add second step to queue
						movQ.addLast(String.format("MOVE %d %d %d %d", w3, w4, stepRoad[4], stepRoad[5]));
										
					// if targetTile is not empty - but other entity may move this turn, emptying it
					} else if (ARENA.getTileLockStatus(w1, w2) == ARENA.getTileLockStatus(w3, w4)) {
						ARENA.raiseTileLockStatus(w1, w2); // rising the ifLocked count, may still move after others
						movQ.addLast(order);               // putting back full order until the mid-tile is free
							
					// if targetTile is not empty - but other entity has higher ifLocked count (likely a loop)
					} else if (ARENA.getTileLockStatus(w1, w2) < ARENA.getTileLockStatus(w3, w4)) {
						ARENA.setTileLockStatus(w1, w2, -1); //locking the tile to not cause infinite loops, dropping the order
							
					// if targetTile is not empty and it won't empty this turn
					} else {
						ARENA.setTileLockStatus(w1, w2, -1); //locking the tile, dropping the order
					}
				}
			} // else case needed if faster entity added or rework previous case to include entities with step > 1
		}
		// execute all remaining BUILD orders
		while(!bl2Q.isEmpty()) {
			// ===== BUILD X Y tX tY eT =====
			String order = bl2Q.getFirst();
			bl2Q.removeFirst();
			int resultBuild = executeBuildTrain(order);
		}
		// execute all remaining TRAIN orders
		while(!tr2Q.isEmpty()) {
			// ===== TRAIN X Y tX tY eT =====
			String order = tr2Q.getFirst();
			tr2Q.removeFirst();
			int resultTrain = executeBuildTrain(order);
		}
		while(!hrvQ.isEmpty()) {
			// ===== HARVEST X Y tX tY =====
			String order = hrvQ.getFirst();
			hrvQ.removeFirst();
			
			String[] words = order.split(" ");
			int w1 = Integer.valueOf(words[1]), w2 = Integer.valueOf(words[2]); // playerEntity
			int w3 = Integer.valueOf(words[3]), w4 = Integer.valueOf(words[4]); // targetEntity
 
			int reach = ARENA.getTileReach(w1, w2);
			
			// (ownerID, entityType, tilePointer, health,  ifLocked)
			if (ARENA.getTileHealth(w3, w4) != 0) {
				for (Player p : gameManager.getActivePlayers()) {
					if ((p.getIndex()+1) == ARENA.getTileOwner(w1, w2)) {
						// GOLD
						if (ARENA.isTileMine(w3, w4) && ifInRange(w1, w2, w3, w4, reach)) {
							// if mine still has resources - harvest gold
							if (ARENA.getTileHealth(w3, w4) != 0) {
								// if mine has finite resources
								if (ARENA.getTileHealth(w3, w4) > 0) {
									ARENA.harvestTile(w3,w4);
								}
								p.addMaterial("GOLD", 1);
								gameManager.addToGameSummary(String.format("%s: entity [%s, %s] got 1 GOLD from [%s, %s].\n", p.getNicknameToken(), words[1], words[2], words[3], words[4]));
							}
							
						// WOOD
						} else if (ARENA.isTileForest(w3, w4) && ifInRange(w1, w2, w3, w4, reach)) {
							// if forest still has resources - harvest wood
							if (ARENA.getTileHealth(w3, w4) != 0) {
								// if forest has finite resources
								if (ARENA.getTileHealth(w3, w4) > 0) {
									ARENA.harvestTile(w3,w4);
								}
								p.addMaterial("WOOD", 1);
								gameManager.addToGameSummary(String.format("%s: entity [%s, %s] got 1 WOOD from [%s, %s].\n", p.getNicknameToken(), words[1], words[2], words[3], words[4]));	
							}
						} else {
							gameManager.addToGameSummary(String.format("%s: entity [%s, %s] failed to harvest from [%s, %s].\n", p.getNicknameToken(), words[1], words[2], words[3], words[4]));
						}
					}
				}
			} else {
				gameManager.addToGameSummary(String.format("%s: entity [%s, %s] failed to harvest from [%s, %s]. Because, it's already depleted!\n", gameManager.getPlayer(ARENA.getTileOwner(w1, w2)-1).getNicknameToken(), words[1], words[2], words[3], words[4]));
			}
		}
		
		while(!attQ.isEmpty()) {
			// ===== ATTACK X Y tX tY =====
			String order = attQ.getFirst();
			attQ.removeFirst();
			
			String[] words = order.split(" ");
			int w1 = Integer.valueOf(words[1]), w2 = Integer.valueOf(words[2]); // playerEntity
			int w3 = Integer.valueOf(words[3]), w4 = Integer.valueOf(words[4]); // targetEntity

			int reach  = ARENA.getTileReach(w1, w2);
			int attack = ARENA.getTileAttack(w1, w2);
			
			for (Player p : gameManager.getActivePlayers()) {
				if ((p.getIndex()+1) == ARENA.getTileOwner(w1,w2)) {
					// attack only if in range
					if (ifInRange(w1, w2, w3, w4, reach)) {
						// if enemy entity still has remaining health
						if (ARENA.getTileHealth(w3,w4) != 0) {
							if (ARENA.getTileHealth(w3,w4) < attack) {
								attack = ARENA.getTileHealth(w3,w4);
							} 
							ARENA.attackTile(w3, w4, attack);
							gameManager.addToGameSummary(String.format("%s: entity [%s, %s] took %d damage from [%s, %s].\n", p.getNicknameToken(), words[1], words[2], attack, words[3], words[4]));
						}
					}
				}
			}
		}
	}
	
	// updating all Tooltips
	private void updateTooltips() {
		for (int i = 0; i < Constants.WIDTH_TILES; i++) {
			for (int j = 0; j < Constants.HEIGHT_TILES; j++) {
				
				// EMPTY TILE or WALL
				if (ARENA.isTileEmpty(i,j) || ARENA.isTileWall(i,j)) {
					// check, if tile has any tooltips
					if (ARENA.getTileTooltipCheck(i,j) == 1) {
						tooltips.removeTooltipText(ARENA.getTileSprite(i,j));
						ARENA.setTileInvisible(i,j); // TooltipModule doesn't update properly otherwise
						ARENA.setTileTooltipCheck(i, j, 0);
					}
				
				// OTHER (Entities)
				} else {
					ARENA.setEntityTooltip(i, j, ARENA.getTileTextValue(i,j));
				}
				
			}
		}
	}
	
	// checking for players who lost all of their castles
    private void checkRemainingCastles() {
		for (Player p : gameManager.getActivePlayers()) {
			int numberOfCastles = 0;
			for (RTSEntity e : ARENA.getEntities()) {
				int x = Integer.valueOf(e.x), y = Integer.valueOf(e.y);
				// check if entity is player's castle
				if (ARENA.getTileOwner(x,y) == (p.getIndex() + 1) && ARENA.isTileCastle(x,y)) {
					numberOfCastles += 1;
				}
			}
			if (numberOfCastles == 0) {
				p.deactivate(gameManager.getPlayer(p.getIndex()).getNicknameToken() + " has no remaining castles!");
				gameManager.addToGameSummary(p.getNicknameToken() + " has no remaining castles!");
			}
		}
	}
		
    @Override
    public void gameTurn(int turn) {
        sendPlayerInputs();
        
        // taking players' orders
        for (Player p : gameManager.getActivePlayers()) {
			try {
				String[] playerOutput = p.getAction();
				for (String order : playerOutput) {
					checkOrder(p, order);
				}
			} catch (TimeoutException e) {
				p.deactivate("Timeout!");
				gameManager.addToGameSummary(" [Error] Something bad happened! Timeout!\n");
			} catch (NumberFormatException e) {
				p.deactivate("Eliminated! Wrong input!");
				gameManager.addToGameSummary(p.getNicknameToken() + " [Error] mismatched input.\n");
			}
		}
		
		// executing players' orders
		executeOrders();
		
		// checking for dead/depleted entities
		ARENA.cleanupDead();
		
		// printing the arena
        printArena();
        
        // updating tooltips
        updateTooltips();
        
        // catching end-conditions
        checkRemainingCastles();
        
        // RuntimeException
        if (turn == Constants.MAX_TURNS) {
			for (Player p : gameManager.getActivePlayers()) {
				p.deactivate(p.getNicknameToken() + " exceeded turn limit.");
				gameManager.addToGameSummary(p.getNicknameToken() + " exceeded turn limit.");
			}
		}
        
        if (gameManager.getActivePlayers().size() < 2) {
			for (Player p : gameManager.getActivePlayers()) {
				gameManager.addToGameSummary(p.getNicknameToken() + " won!");
			}
			gameManager.addToGameSummary("GAME OVER");
            gameManager.endGame();   
        }
        
		
    }

    @Override
    public void onEnd() {
        for (Player p : gameManager.getPlayers()) {
            p.setScore(p.isActive() ? 1 : 0);
        }
    }
}
