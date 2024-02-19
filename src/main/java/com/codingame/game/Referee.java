//System.err.println(Integer.toString(i) + ", " + Integer.toString(j));
//gameManager.addToGameSummary("text");
package com.codingame.game;

import com.codingame.game.engine.*;
import static com.codingame.game.engine.Constants.*;

import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Random;

import com.codingame.gameengine.core.AbstractPlayer.TimeoutException;
import com.codingame.gameengine.core.AbstractReferee;
import com.codingame.gameengine.core.MultiplayerGameManager;
import com.codingame.gameengine.core.Tooltip;

import com.codingame.gameengine.module.endscreen.EndScreenModule;
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
    @Inject private EndScreenModule endScreenModule;
    
    private Random rng;
    private Arena arena;
	
	// ==============================
	// Functions
	private void sentBasePlayerInputs() {
        for (Player p : gameManager.getActivePlayers()) {
			// SEND ENTITY_TYPE DATA:
			// entityType, maxHP, reach, attack, step, goldCost, woodCost
			p.sendInputLine(String.format("%d %d", arena.width_tiles, arena.height_tiles));
				
			for (int i = 0; i < Constants.ENTITY_TYPES_NAMES.length; i++) {
				int [] temp_etype_array = arena.getETypes().get(Constants.ENTITY_TYPES_NAMES[i]).clone();
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
			// SEND arena DATA:
			p.sendInputLine(String.format("%d %d %d %d %d", arena.getEntities().size(), p.gold(), p.wood(), gameManager.getPlayer((p.getIndex()+1) % 2).gold(), gameManager.getPlayer((p.getIndex()+1) % 2).wood()));
			for (int i = 0; i < arena.width_tiles; i++) {
				for (int j = 0; j < arena.height_tiles; j++) {
					// x | y | ownerID (-1:neutral,0:player1,1:player2) | entityType (empty, gold, worker, castle, etc.) | health
					if (!arena.isTileEmpty(i,j)) {
						String inputText = "";
						if (arena.getTileOwner(i,j) == 0) {
							inputText = String.format("%d %d %d %d %d", i, j, -1, arena.getTileType(i,j), arena.getTileHealth(i,j));
						} else {
							int tempOwnerID = (p.getIndex() + arena.getTileOwner(i,j) - 1) % 2;
							inputText = String.format("%d %d %d %d %d", i, j, tempOwnerID, arena.getTileType(i,j), arena.getTileHealth(i,j));
						}
						p.sendInputLine(inputText);
					}
				}
			}
            p.execute();
        }
    }
    private void printArena() {
		for (int i = 0; i < arena.width_tiles; i++) {
			for (int j = 0; j < arena.height_tiles; j++) {
				//arena_map[i][j] = (ownerID, entityType, tilePointer, health,  ifLocked
				String tile_filename = "0-0.png", tile_health = String.valueOf(arena.getTileHealth(i,j));
				int tile_fontsize = 30;
				
				if (tile_health.equals("-1")) tile_health   = "∞";
				if (tile_health.equals("0"))  tile_health   = "";
				if (tile_health.equals("∞"))  tile_fontsize = 50;
				
				// updating sprites
				if (arena.getTilePointer(i,j) == 0){
					tile_filename = String.format("%d-%d.png", arena.getTileOwner(i,j), arena.getTileType(i,j));
				}
				
				// check, if sprite changed
				if (!arena.isTileSpriteEqual(i, j, tile_filename)) {
					if (tile_health.equals("")) {
						graphicEntityModule.commitEntityState(0, arena.setTileSpriteInvisible(i, j, tile_filename));
					} else {
						graphicEntityModule.commitEntityState(0, arena.setTileSpriteVisible(i, j, tile_filename));
					}
				}
					
				// display health
				int newX = (int)((Double.valueOf(i)+0.5)  * Double.valueOf(Constants.TILE_SIZE));
				int newY = (int)((Double.valueOf(j)+0.75) * Double.valueOf(Constants.TILE_SIZE));
				
				// check, if text changed
				if (!arena.isTileTextEqual(i, j, tile_health)) {
					// checks if it's not a wall (don't display health of unbreakable walls)
					if (!arena.isTileWall(i,j)){
						graphicEntityModule.commitEntityState(0, arena.setTileText(i, j, tile_health, newX, newY, tile_fontsize));
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
 
        arena = new Arena(gameManager, graphicEntityModule, tooltips);
        
        rng = new Random(gameManager.getSeed());
        //rng = new Random(-2826428385550016000L);
        
        arena.initArena(rng);
        
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
		return 0 <= x && x < arena.width_tiles && 0 <= y && y < arena.height_tiles;
	}
	// Checks if there is mine/forest at those coordinates
	private boolean checkIfHarvestable(int x, int y) {
		return checkIfInBounds(x, y) && (arena.isTileMine(x,y) || arena.isTileForest(x,y));
	}
	// Checks if there is an entity at those coordinates that is owned by ownerID
    private boolean checkPlayersEntityExists(int x, int y, int ownerID) {
		return checkIfInBounds(x, y) && arena.checkTileOwnership(x, y, ownerID);
	}
	
	private boolean checkIfKnowsOrder(String orderName, int x, int y) {
		String entityName = Constants.ENTITY_TYPES_NAMES[arena.getTileType(x, y)];
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
			int tempIndex = arena.findRTSEntity(w1, w2);
			if (tempIndex == -1) { 
				p.deactivate("Unknown error: not properly initialised entity! Contact the author!");
				
			} else if (!checkIfKnowsOrder(words[0], w1, w2)) {
				p.deactivate(String.format("Incorrect order: entity doesn't know this order! \"%s\"", order));
				
			} else if (wordslen == 3) {
				//not really appliable for the modern version of the project, but still working to reset orders for entity, more of a hidden debug option
				// ===== STOP X Y ===== 
				if (words[0].equals("STOP")) {
					arena.getEntities().get(tempIndex).setCountdown(0);
					arena.getEntities().get(tempIndex).setActiveOrder("WAIT");
					arena.getEntities().get(tempIndex).clearOrders();
					arena.setTileLockStatus(w1, w2, -1); // locking entity in place
				} else {
					p.deactivate(String.format("Incorrect order: wrong syntax! \"%s\"", order));
				}
				
			} else if (wordslen == 5) {
				int w3 = Integer.valueOf(words[3]), w4 = Integer.valueOf(words[4]);
				// ===== MOVE X Y tX tY =====
				if (words[0].equals("MOVE")){
					int step = arena.getTileStep(w1,w2);
					if (checkIfInBounds(w3, w4) && ifInRange(w1, w2, w3, w4, step)) {
						arena.getEntities().get(tempIndex).addLastOrder(order);
						arena.setTileLockStatus(w1, w2, 0); // unlocking entity to potentially move
					} else {
						p.deactivate(String.format("Incorrect MOVE order: target tile out of reach! \"%s\"", order));
					}
					
				// ===== ATTACK X Y tX tY =====	
				} else if (words[0].equals("ATTACK")) {
					int reach = arena.getTileReach(w1,w2);
					if (!ifInRange(w1, w2, w3, w4, reach)) {
						p.deactivate(String.format("Incorrect ATTACK order: target tile out of reach! \"%s\"", order));
						
					} else if (!checkPlayersEntityExists(w3, w4, (p.getIndex()+1)%2 + 1)) {
						p.deactivate(String.format("Incorrect ATTACK order: target tile doesn't have entity owned by enemy player! \"%s\"", order));
						
					} else {
						arena.getEntities().get(tempIndex).addLastOrder(order);
						arena.setTileLockStatus(w1, w2, -1); // locking entity in place
					}
					
				// ===== HARVEST X Y tX tY =====
				} else if (words[0].equals("HARVEST")) {
					int reach = arena.getTileReach(w1,w2);
					if (!ifInRange(w1, w2, w3, w4, reach)) {
						p.deactivate(String.format("Incorrect HARVEST order: target tile out of reach! \"%s\"", order));
						
					} else if (!checkIfHarvestable(w3, w4)) {
						p.deactivate(String.format("Incorrect HARVEST order: target tile doesn't have harvestable resource! \"%s\"", order));
						
					} else {
						arena.getEntities().get(tempIndex).addLastOrder(order);
						arena.setTileLockStatus(w1, w2, -1); // locking entity in place
					}
					
				} else {
						p.deactivate(String.format("Incorrect order: wrong syntax! \"%s\"", order));
				}
				
			} else if (wordslen == 6) {
				int w3 = Integer.valueOf(words[3]), w4 = Integer.valueOf(words[4]);
					
				// ===== BUILD X Y tX tY eT =====
				if (words[0].equals("BUILD")){
					int reach = arena.getTileReach(w1,w2);
					if (checkIfInBounds(w3, w4) && ifInRange(w1, w2, w3, w4, reach)) {
						String tempEntityTypeValue = matchEntityTypeValue(words[5]);
						if (checkBuildEntityTypeValidity(arena.getTileType(w1,w2), Integer.valueOf(tempEntityTypeValue))) {
							arena.getEntities().get(tempIndex).addLastOrder(String.format("BUILD %s %s %s %s %s", words[1], words[2], words[3], words[4], tempEntityTypeValue));
							arena.setTileLockStatus(w1, w2, -1); // locking entity in place
							
						} else {
							p.deactivate(String.format("Incorrect BUILD order: entity can't build given entityType! \"%s\"", order));
						}
					} else {
						p.deactivate(String.format("Incorrect BUILD order: target tile out of reach! \"%s\"", order));
					}

				// ===== TRAIN X Y tX tY eT =====
				} else if (words[0].equals("TRAIN")){
					int reach = arena.getTileReach(w1,w2);
					if (checkIfInBounds(w3, w4) && ifInRange(w1, w2, w3, w4, reach)) {
						String tempEntityTypeValue = matchEntityTypeValue(words[5]);
						if (checkTrainEntityTypeValidity(arena.getTileType(w1,w2), Integer.valueOf(tempEntityTypeValue))) {
							arena.getEntities().get(tempIndex).addLastOrder(String.format("TRAIN %s %s %s %s %s", words[1], words[2], words[3], words[4], tempEntityTypeValue));
							arena.setTileLockStatus(w1, w2, -1); // locking entity in place
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
			if (arena.isTileEmpty(tX, tY)) {
				stepRoad[2] = tX;
				stepRoad[3] = tY;
				return stepRoad;
						
			// if targetTile is not empty - but other entity may move this turn, emptying it
			} else if (arena.getTileLockStatus(stepRoad[0], stepRoad[1]) == arena.getTileLockStatus(tX, tY)) {
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
		
		int reach    = arena.getTileReach(w1,w2); // Reach
		int costGold = arena.getCostGold(w5);     // targetCostGold
		int costWood = arena.getCostWood(w5);     // targetCostWood
		
		for (Player p : gameManager.getActivePlayers()) {
			if ((p.getIndex()+1) == (arena.getTileOwner(w1,w2))) {
				//addPlayerEntity(x, y, ownerID, entityTypeName)
				if (p.gold() >= costGold && p.wood() >= costWood) {
					// if targetTile is empty and in reach
					if (arena.isTileEmpty(w3,w4) && ifInRange(w1, w2, w3, w4, reach)) {
						//addPlayerEntity(x, y, ownerID, entityTypeName)
						arena.addPlayerEntity(w3, w4, arena.getTileOwner(w1,w2), Constants.ENTITY_TYPES_NAMES[w5]);
						p.addMaterial("GOLD", -costGold);
						p.addMaterial("WOOD", -costWood);
						gameManager.addToGameSummary(String.format("%s: [%d, %d] created %s at [%d, %d].\n", p.getNicknameToken(), w1, w2, Constants.ENTITY_TYPES_NAMES[w5], w3, w4));
						return 0;
						
					// if targetTile is not empty - but other entity may move this turn, emptying it
					} else if (arena.getTileLockStatus(w3,w4) == 0) {
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
		for (RTSEntity e : arena.getEntities()) {
			// only execute orders of player entities
			if (!arena.isTileNeutral(Integer.valueOf(e.x), Integer.valueOf(e.y))) {
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
						
		// xy: 0 - block, 1 - block build, 2 - build, 3 - block train, 4 - train, 5 - block move, 6 - move, 7 - none 
		// (if there is event of higher priority - dont do others - put block if there is one on the same priority)
		int [][] orderConflictArray = new int[arena.width_tiles][arena.height_tiles];
		for (int i = 0; i < arena.width_tiles; i++){
			for(int j = 0; j < arena.height_tiles; j++){
				orderConflictArray[i][j] = 7;
			}
		}
		int conflictX = -1;
		int conflictY = -1;
		
		// build orders
		for (int i = 0; i < bldQ.size(); i++){
			String order = bldQ.getFirst();
			conflictX = Integer.valueOf(order.split(" ")[3]);
			conflictY = Integer.valueOf(order.split(" ")[4]);
			if (orderConflictArray[conflictX][conflictY] <= 2){ // already other build order
				orderConflictArray[conflictX][conflictY] = 1;
				bldQ.removeFirst();
			} else { // only other non-build orders
				orderConflictArray[conflictX][conflictY] = 2;
				bldQ.removeFirst();
				bldQ.addLast(order);
			}
		}
		
		// train orders
		for (int i = 0; i < trnQ.size(); i++){
			String order = trnQ.getFirst();
			conflictX = Integer.valueOf(order.split(" ")[3]);
			conflictY = Integer.valueOf(order.split(" ")[4]);
			if (orderConflictArray[conflictX][conflictY] <= 3){ // already other order of higher priority
				trnQ.removeFirst();
			} else if (orderConflictArray[conflictX][conflictY] == 4){ // already other train order
				orderConflictArray[conflictX][conflictY] = 3;
				trnQ.removeFirst();
			} else { // only other non-train or build orders
				orderConflictArray[conflictX][conflictY] = 4;
				trnQ.removeFirst();
				trnQ.addLast(order);
			}
		}
		
		// move orders
		for (int i = 0; i < movQ.size(); i++){
			String order = movQ.getFirst();
			conflictX = Integer.valueOf(order.split(" ")[3]);
			conflictY = Integer.valueOf(order.split(" ")[4]);
			if (orderConflictArray[conflictX][conflictY] <= 5){ // already other order of higher priority
				movQ.removeFirst();
			} else if (orderConflictArray[conflictX][conflictY] == 6){ // already other move order
				orderConflictArray[conflictX][conflictY] = 5;
				movQ.removeFirst();
			} else { // only when no other order
				orderConflictArray[conflictX][conflictY] = 6;
				movQ.removeFirst();
				movQ.addLast(order);
			}
		}
		
		// execute all possible BUILD orders
		while(!bldQ.isEmpty()) {
			// ===== BUILD X Y tX tY eT =====
			String order = bldQ.getFirst();
			bldQ.removeFirst();
			conflictX = Integer.valueOf(order.split(" ")[3]);
			conflictY = Integer.valueOf(order.split(" ")[4]);
			if (orderConflictArray[conflictX][conflictY] >= 2){ // execute if not blocked
				// execute order, if possible (try again, if tile may empty before the end of the turn).
				if (executeBuildTrain(order) == 1) bl2Q.addLast(order);
			}
		}
		// execute all possible TRAIN orders
		while(!trnQ.isEmpty()) {
			// ===== TRAIN X Y tX tY eT =====
			String order = trnQ.getFirst();
			trnQ.removeFirst();
			conflictX = Integer.valueOf(order.split(" ")[3]);
			conflictY = Integer.valueOf(order.split(" ")[4]);
			if (orderConflictArray[conflictX][conflictY] >= 4){ // execute if not blocked
				// execute order, if possible (try again, if tile may empty before the end of the turn).
				if (executeBuildTrain(order) == 1) tr2Q.addLast(order);
			}
		}
		// execute all possible MOVE orders
		while(!movQ.isEmpty()) {
			// ===== MOVE X Y tX tY =====
			String order = movQ.getFirst();
			movQ.removeFirst();
			String[] words = order.split(" ");
			int w1 = Integer.valueOf(words[1]), w2 = Integer.valueOf(words[2]); // playerEntity
			int w3 = Integer.valueOf(words[3]), w4 = Integer.valueOf(words[4]); // targetEntity
			
			if (orderConflictArray[w3][w4] >= 6){ // execute if not blocked
				int step      = arena.getTileStep(w1,w2);                                                  //entityStep
				int tempIndex = arena.findRTSEntity(Integer.valueOf(words[1]), Integer.valueOf(words[2])); //entityIndex	
				
				if (tempIndex == -1) { 
					gameManager.getPlayer(arena.getTileOwner(w1, w2)-1).deactivate("Unknown error: not properly initialised or changed entity! Contact the author!");
					
				// checks if one step is needed
				} else if (ifInRange(w1, w2, w3, w4, 1)) {		
					// if targetTile is empty, in range and accessible by walking
					if (arena.isTileEmpty(w3,w4)) {
						// update arena_MAP tiles
						for (int k = 0; k < 5; k++) {
							arena.setTileValue(w3, w4, k, arena.getTileValue(w1, w2, k));
							arena.setTileValue(w1, w2, k, 0);
						}
						// update ENTITY_LIST
						arena.getEntities().get(tempIndex).setCoords(w3, w4);
						
						arena.setTileLockStatus(w3, w4, -1); //locking target tile after making move
						 
						//gameManager.addToGameSummary(String.format("%s: entity [%s, %s] moved to [%s, %s].\n", gameManager.getPlayer(arena.getTileOwner(w3, w4)-1).getNicknameToken(), words[1], words[2], words[3], words[4]));
									
					// if targetTile is not empty - but other entity may move this turn, emptying it
					} else if (arena.getTileLockStatus(w1, w2) == arena.getTileLockStatus(w3, w4)) {
						arena.raiseTileLockStatus(w1, w2); //raising the ifLocked count, may still move after others
						movQ.addLast(order);
						
					// if targetTile is not empty - but other entity has higher ifLocked count (likely a loop)
					} else if (arena.getTileLockStatus(w1, w2) < arena.getTileLockStatus(w3, w4)) {
						arena.setTileLockStatus(w1, w2, -1); //locking the tile to not cause infinite loops
						gameManager.addToGameSummary(String.format("%s: [%d, %d] failed to move to [%d, %d].\n", gameManager.getPlayer(arena.getTileOwner(w1, w2)-1).getNicknameToken(), w1, w2, w3, w4));
						
					// if targetTile is not empty and it won't empty this turn
					} else {
						arena.setTileLockStatus(w1, w2, -1); //locking the tile
						gameManager.addToGameSummary(String.format("%s: [%d, %d] failed to move to [%d, %d].\n", gameManager.getPlayer(arena.getTileOwner(w1, w2)-1).getNicknameToken(), w1, w2, w3, w4));
					}
				
				// checks if two steps are needed
				} else if (ifInRange(w1, w2, w3, w4, 2)) {
					int[] stepRoad = {w1, w2, w3, w4, w3, w4}; 
					stepRoad = findMidStep(stepRoad).clone();
					if (stepRoad[2] == -1 || stepRoad[3] == -1) {
						arena.setTileLockStatus(w1, w2, -1); //locking the tile, dropping the order, as there is no available mid-tiles
					} else {
						w3 = stepRoad[2]; 
						w4 = stepRoad[3];

						// if targetTile is empty, in range and accessible by walking
						if (arena.isTileEmpty(w3, w4)) {
							// update arena_MAP tiles
							for (int k = 0; k < 5; k++) {
								arena.setTileValue(w3, w4, k, arena.getTileValue(w1, w2, k));
								arena.setTileValue(w1, w2, k, 0);
							}
							// update ENTITY_LIST
							arena.getEntities().get(tempIndex).setCoords(w3, w4);
							
							arena.raiseTileLockStatus(w3, w4); //rising ifLocked value after making one step out of two
								 
							gameManager.addToGameSummary(String.format("%s: [%d, %d] moved to [%d, %d].\n", gameManager.getPlayer(arena.getTileOwner(w3, w4)-1).getNicknameToken(), w1, w2, w3, w4));
							
							// after successfully moving one step - add second step to queue
							movQ.addLast(String.format("MOVE %d %d %d %d", w3, w4, stepRoad[4], stepRoad[5]));
											
						// if targetTile is not empty - but other entity may move this turn, emptying it
						} else if (arena.getTileLockStatus(w1, w2) == arena.getTileLockStatus(w3, w4)) {
							arena.raiseTileLockStatus(w1, w2); // rising the ifLocked count, may still move after others
							movQ.addLast(order);               // putting back full order until the mid-tile is free
								
						// if targetTile is not empty - but other entity has higher ifLocked count (likely a loop)
						} else if (arena.getTileLockStatus(w1, w2) < arena.getTileLockStatus(w3, w4)) {
							arena.setTileLockStatus(w1, w2, -1); //locking the tile to not cause infinite loops, dropping the order
							gameManager.addToGameSummary(String.format("%s: [%d, %d] failed to move to [%d, %d].\n", gameManager.getPlayer(arena.getTileOwner(w1, w2)-1).getNicknameToken(), w1, w2, w3, w4));
							
								
						// if targetTile is not empty and it won't empty this turn
						} else {
							arena.setTileLockStatus(w1, w2, -1); //locking the tile, dropping the order
							gameManager.addToGameSummary(String.format("%s: [%d, %d] failed to move to [%d, %d].\n", gameManager.getPlayer(arena.getTileOwner(w1, w2)-1).getNicknameToken(), w1, w2, w3, w4));
						}
					}
				} // else case needed if faster entity added or rework previous case to include entities with step > 1
			}
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
 
			int reach = arena.getTileReach(w1, w2);
			
			// (ownerID, entityType, tilePointer, health,  ifLocked)
			if (arena.getTileHealth(w3, w4) != 0) {
				for (Player p : gameManager.getActivePlayers()) {
					if ((p.getIndex()+1) == arena.getTileOwner(w1, w2)) {
						// GOLD
						if (arena.isTileMine(w3, w4) && ifInRange(w1, w2, w3, w4, reach)) {
							// if mine still has resources - harvest gold
							if (arena.getTileHealth(w3, w4) != 0) {
								// if mine has finite resources
								if (arena.getTileHealth(w3, w4) > 0) {
									arena.harvestTile(w3,w4);
								}
								p.addMaterial("GOLD", 1);
								gameManager.addToGameSummary(String.format("%s: [%d, %d] got 1 GOLD from [%d, %d].\n", p.getNicknameToken(), w1, w2, w3, w4));
							}
							
						// WOOD
						} else if (arena.isTileForest(w3, w4) && ifInRange(w1, w2, w3, w4, reach)) {
							// if forest still has resources - harvest wood
							if (arena.getTileHealth(w3, w4) != 0) {
								// if forest has finite resources
								if (arena.getTileHealth(w3, w4) > 0) {
									arena.harvestTile(w3,w4);
								}
								p.addMaterial("WOOD", 1);
								gameManager.addToGameSummary(String.format("%s: [%d, %d] got 1 WOOD from [%d, %d].\n", p.getNicknameToken(), w1, w2, w3, w4));	
							}
						} else {
							gameManager.addToGameSummary(String.format("%s: [%d, %d] failed to harvest from [%d, %d].\n", p.getNicknameToken(), w1, w2, w3, w4));
						}
					}
				}
			} else {
				gameManager.addToGameSummary(String.format("%s: [%d, %d] failed to harvest from [%d, %d]. Because, it's already depleted!\n", gameManager.getPlayer(arena.getTileOwner(w1, w2)-1).getNicknameToken(), w1, w2, w3, w4));
			}
		}
		
		while(!attQ.isEmpty()) {
			// ===== ATTACK X Y tX tY =====
			String order = attQ.getFirst();
			attQ.removeFirst();
			
			String[] words = order.split(" ");
			int w1 = Integer.valueOf(words[1]), w2 = Integer.valueOf(words[2]); // playerEntity
			int w3 = Integer.valueOf(words[3]), w4 = Integer.valueOf(words[4]); // targetEntity

			int reach  = arena.getTileReach(w1, w2);
			int attack = arena.getTileAttack(w1, w2);
			
			for (Player p : gameManager.getActivePlayers()) {
				if ((p.getIndex()+1) == arena.getTileOwner(w1,w2)) {
					// attack only if in range
					if (ifInRange(w1, w2, w3, w4, reach)) {
						// if enemy entity still has remaining health
						if (arena.getTileHealth(w3,w4) != 0) {
							if (arena.getTileHealth(w3,w4) < attack) {
								attack = arena.getTileHealth(w3,w4);
							} 
							arena.attackTile(w3, w4, attack);
							gameManager.addToGameSummary(String.format("%s: [%d, %d] did %d damage to [%d, %d].\n", p.getNicknameToken(), w1, w2, attack, w3, w4));
						}
					}
				}
			}
		}
	}
	
	// updating all Tooltips
	private void updateTooltips() {
		for (int i = 0; i < arena.width_tiles; i++) {
			for (int j = 0; j < arena.height_tiles; j++) {
				
				// EMPTY TILE or WALL
				if (arena.isTileEmpty(i,j) || arena.isTileWall(i,j)) {
					// check, if tile has any tooltips
					if (arena.getTileTooltipCheck(i,j) == 1) {
						tooltips.removeTooltipText(arena.getTileSprite(i,j));
						arena.setTileInvisible(i,j); // TooltipModule doesn't update properly otherwise
						arena.setTileTooltipCheck(i, j, 0);
					}
				
				// OTHER (Entities)
				} else {
					arena.setEntityTooltip(i, j, arena.getTileTextValue(i,j));
				}
				
			}
		}
	}
	
	// checking for players who lost all of their castles
    private void checkRemainingCastles() {
		for (Player p : gameManager.getActivePlayers()) {
			int numberOfCastles = 0;
			for (RTSEntity e : arena.getEntities()) {
				int x = Integer.valueOf(e.x), y = Integer.valueOf(e.y);
				// check if entity is player's castle
				if (arena.getTileOwner(x,y) == (p.getIndex() + 1) && arena.isTileCastle(x,y)) {
					numberOfCastles += 1;
				}
			}
			if (numberOfCastles == 0) {
				p.deactivate(gameManager.getPlayer(p.getIndex()).getNicknameToken() + " has no remaining castles!");
				gameManager.addToGameSummary(p.getNicknameToken() + " has no remaining castles!");
			}
		}
	}
	
	// checking which remaining player has higher score
    private void checkRemainingScores() {
		int maxMatsScore = 0;
		for (Player p : gameManager.getActivePlayers()) {
			int numberOfMaterials = 0;
			for (RTSEntity e : arena.getEntities()) {
				int x = Integer.valueOf(e.x), y = Integer.valueOf(e.y);
				// check if entity is player's
				if (arena.getTileOwner(x,y) == (p.getIndex() + 1)) {
					numberOfMaterials += arena.getTileEType(x,y)[5];
					numberOfMaterials += arena.getTileEType(x,y)[6];
				}
			}
			numberOfMaterials += p.gold();
			numberOfMaterials += p.wood();
			p.setmatScore(numberOfMaterials);
			
			if (numberOfMaterials > maxMatsScore){
				maxMatsScore = numberOfMaterials;
			}
		}
		for (Player p : gameManager.getActivePlayers()) {
			if (maxMatsScore > p.getmatScore()){
				p.deactivate(gameManager.getPlayer(p.getIndex()).getNicknameToken() + " has worse final score!");
				gameManager.addToGameSummary(p.getNicknameToken() + " has worse final score!");
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
		arena.cleanupDead();
		
		// printing the arena
        printArena();
        
        // updating tooltips
        updateTooltips();
        
        // catching end-conditions
        checkRemainingCastles();
        
        // RuntimeException
        if (turn == Constants.MAX_TURNS) {
			for (Player p : gameManager.getActivePlayers()) {
				gameManager.addToGameSummary(p.getNicknameToken() + " exceeded turn limit.");
			}
			// catching end-conditions
			checkRemainingScores();
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
        int[] scores = { gameManager.getPlayer(0).getScore(), gameManager.getPlayer(1).getScore() };
        String[] text = new String[2];
        if(scores[0] > scores[1]) {
            gameManager.addToGameSummary(gameManager.formatSuccessMessage(gameManager.getPlayer(0).getNicknameToken() + " won!"));
            gameManager.addTooltip(gameManager.getPlayer(0), gameManager.getPlayer(0).getNicknameToken() + " won!");
            text[0] = "Won";
            text[1] = "Lost";
        } else if(scores[0] < scores[1]) {
            gameManager.addToGameSummary(gameManager.formatSuccessMessage(gameManager.getPlayer(1).getNicknameToken() + " won!"));
            gameManager.addTooltip(gameManager.getPlayer(1), gameManager.getPlayer(1).getNicknameToken() + " won!");
            text[0] = "Lost";
            text[1] = "Won";
        } else {
            gameManager.addToGameSummary(gameManager.formatErrorMessage("Game is drawn"));
            gameManager.addTooltip(gameManager.getPlayer(1), "Draw");
            text[0] = "Draw";
            text[1] = "Draw";
        }
        endScreenModule.setScores(scores, text);
    }
}
