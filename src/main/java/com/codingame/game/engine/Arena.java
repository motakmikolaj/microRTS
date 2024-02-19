package com.codingame.game.engine;

import java.util.*;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.stream.Collectors;
import java.util.Random;

import com.codingame.gameengine.core.MultiplayerGameManager;
import com.codingame.gameengine.core.Tooltip;

import com.codingame.gameengine.module.tooltip.TooltipModule;

import com.codingame.gameengine.module.entities.GraphicEntityModule;
import com.codingame.gameengine.module.entities.Sprite;
import com.codingame.gameengine.module.entities.Text;

import com.codingame.game.Player;
import com.codingame.game.Referee;

import static com.codingame.game.engine.Constants.*;

public class Arena {
	
	private MultiplayerGameManager<Player> gameManager;
	private GraphicEntityModule graphicEntityModule;
	private TooltipModule tooltips;
	
	// ARENA_MAP  : 3D Array [WIDTH_TILES][HEIGHT_TILES][5]:
    // ==============================
    // ownerID    : 0 = neutral, 1 = player[0], 2 = player[1]  | when passed to players (id = id-1)
    // entityType : 0 = CASTLE,  1 = BARRACKS,  2 = WORKER, 3 = LIGHT, 4 = HEAVY, 5 = RANGED
    // tilePointer: (1000x + y + 1) - tile at which entity looks at (0 means it doesnt look at anything)
    // health     : amount of health entity has
    // ifLocked   : used for movement calculations, shields against loops: -1: locked, 0 unlocked, 0 < n waiting n turns to move
    // ** entityType for neutral : 0 = empty, 1 = wall, 2 = mine, 3 = forest
    
    public int[][][]              ARENA_MAP;
    public HashMap<String, int[]> ENTITY_TYPES = new HashMap<String, int[]>();
    public ArrayList<RTSEntity>   ENTITY_LIST  = new ArrayList<RTSEntity>();
    
    public Sprite[][] ARENA_MAP_SPRITES;
    public Text[][]   ARENA_MAP_TEXT;
    public int[][]    TOOLTIPS_CHECK;
    public int paddingL = 0;
    public int paddingT = 0;
    
    public Arena(MultiplayerGameManager<Player> gameManager, GraphicEntityModule graphicEntityModule, TooltipModule tooltips){
		this.gameManager         = gameManager;
		this.graphicEntityModule = graphicEntityModule;
		this.tooltips            = tooltips;
	}
    
    // (name), (entityType/maxHP), (reach/attack/step), (goldCost/woodCost)
	private void initEntityTypes(){
		for (int i = 0; i < Constants.ENTITY_TYPES_NAMES.length; i++) {
			ENTITY_TYPES.put(Constants.ENTITY_TYPES_NAMES[i], Constants.ENTITY_TYPES_DATA[i].clone());
		}
	}
	
	private void clearTile(int i, int j){
		for (int k = 0; k < 5; k++){
			ARENA_MAP[i][j][k] = 0;
		}
	}
	private void generateArena(Random rng){
		// Main Mapping Variables & declarations
		int sizeX = (int) Math.ceil((double) Constants.WIDTH_TILES / 2);
		int sizeY = Constants.HEIGHT_TILES;
		
		int x = -1, y = -1, cx = -1, cy = -1;
		
		// Functional Variables
		int builderSpawned       = 0;
		int builderMoveDirection = 0;
		int allocatedBlocks      = 0;                  //variable used to track the percentage of the map filled
		int rootX                = sizeX - 1;          //this is where the growth starts from (random spot on the mirror axis)
		int rootY                = rng.nextInt(sizeY - 2) + 1; //this is where the growth starts from (random spot on the mirror axis)
		int stepped              = 0;                  //this is how long corridors can be
		int orthogonalAllowed    = 0;                  //orthogonal movement allowed? If not, it carves a wider cooridor on diagonal
		
		// expanding the seed
		int rootYExpansion = rng.nextInt(sizeY);
		for(int i = 0; i < rootYExpansion; i++){
			if (rootY - i > 0) {
				for (int k = 0; k < 5; k++){
					ARENA_MAP[sizeX-1][rootY - i][k] = 0;
				}
				allocatedBlocks++;
			}
			if (rootY + i < sizeY - 1) {
				for (int k = 0; k < 5; k++){
					ARENA_MAP[sizeX-1][rootY + i][k] = 0;
				}
				allocatedBlocks++;
			}
		}
		
		// The Diffusion Limited Aggregation Loop
		while (allocatedBlocks < (2*(sizeX*sizeY)/5) ){  //quit when 40% of the map is filled
			if (builderSpawned != 1){
				//spawn at random position
				cx = rng.nextInt(sizeX - 2) + 1;
				cy = rng.nextInt(sizeY - 2) + 1;
				
				//see if builder is ontop of expanded root
				if (Math.abs(rootX - cx) <= 0 && Math.abs(rootY - cy) <= rootYExpansion){
					//builder was spawned too close to root, clear that floor and respawn
					if (ARENA_MAP[cx][cy][1] != 0){
						clearTile(cx,cy);
						allocatedBlocks++;
					}
				} else {
					builderSpawned = 1;
					builderMoveDirection = rng.nextInt(8);
					stepped = 0;
				}
			
			
			//builder already spawned and knows it's direction, move builder
			} else { 
				if (builderMoveDirection == 0 && cy > 1){
					cy--; stepped++;       // North
					
				} else if (builderMoveDirection == 1 && cx < sizeX - 1){
					cx++; stepped++;       // East
					
				} else if (builderMoveDirection == 2 && cy < sizeY - 1){
					cy++; stepped++;       // South
					
				} else if (builderMoveDirection == 3 && cx > 1){
					cx++; stepped++;       // West
					
				} else if (builderMoveDirection == 4 && cx < sizeX - 1 && cy > 1){
					cy--; cx++; stepped++; // North-East
					
				} else if (builderMoveDirection == 5 && cx < sizeX - 1 && cy < sizeY - 1){
					cy++; cx++; stepped++; // South-East
					
				} else if (builderMoveDirection == 6 && cx > 1 && cy < sizeY - 1){
					cy++; cx--; stepped++; // South-West
					
				} else if (builderMoveDirection == 7 && cx > 1 && cy > 1){
					cy--; cx--; stepped++; // North-West
					
				} 
				
				// ensure that the builder is touching an existing spot
				if (cx < sizeX - 1 && cy < sizeY - 1 && cx > 1 && cy > 1 && stepped <= 5){
					if (ARENA_MAP[cx+1][cy][1] == 0){          // East
						if (ARENA_MAP[cx][cy][1] != 0){
							clearTile(cx,cy); allocatedBlocks++;
						} 
						
					} else if (ARENA_MAP[cx-1][cy][1] == 0){   // West
						if (ARENA_MAP[cx][cy][1] != 0){
							clearTile(cx,cy); allocatedBlocks++;
						} 
						
					} else if (ARENA_MAP[cx][cy+1][1] == 0){   // South
						if (ARENA_MAP[cx][cy][1] != 0){
							clearTile(cx,cy); allocatedBlocks++;
						}
						
					} else if (ARENA_MAP[cx][cy-1][1] == 0){   // North
						if (ARENA_MAP[cx][cy][1] != 0){
							clearTile(cx,cy); allocatedBlocks++;
						}
							
					} else if (ARENA_MAP[cx+1][cy-1][1] == 0){ // North-East
						if (ARENA_MAP[cx][cy][1] != 0){
							clearTile(cx,cy); allocatedBlocks++; 
							if (orthogonalAllowed == 0){
								clearTile(cx+1, cy); allocatedBlocks++;
							}
						}
						
					} else if (ARENA_MAP[cx+1][cy+1][1] == 0){ // South-East
						if (ARENA_MAP[cx][cy][1] != 0){
							clearTile(cx,cy); allocatedBlocks++; 
							if (orthogonalAllowed == 0){
								clearTile(cx+1, cy); allocatedBlocks++;
							}
						}
						
					} else if (ARENA_MAP[cx-1][cy+1][1] == 0){ // South-West
						if (ARENA_MAP[cx][cy][1] != 0){
							clearTile(cx,cy); allocatedBlocks++; 
							if (orthogonalAllowed == 0){
								clearTile(cx-1, cy); allocatedBlocks++;
							}
						}
						
					} else if (ARENA_MAP[cx-1][cy-1][1] == 0){ // North-West
						if (ARENA_MAP[cx][cy][1] != 0){
							clearTile(cx,cy); allocatedBlocks++; 
							if (orthogonalAllowed == 0){
								clearTile(cx-1, cy); allocatedBlocks++;
							}
						}
					}
			
				} else { 
					builderSpawned = 0; 
				}
			}
		}
		
		ArrayList<Integer> blockedMidTiles = new ArrayList<Integer>();
		for(int j = 1; j < sizeY-1; j++){
			if (ARENA_MAP[sizeX-1][j][1] == 1){
				blockedMidTiles.add(j);
			}
		}
		if (blockedMidTiles.size() != 0) {
			// adding extra entries between halves if they are accessible
			int extraEntries = rng.nextInt(blockedMidTiles.size());
			while(extraEntries > 0 && blockedMidTiles.size() != 0){
				int mt = rng.nextInt(blockedMidTiles.size());
				if(ARENA_MAP[sizeX-2][blockedMidTiles.get(mt)][1] == 0 || ARENA_MAP[sizeX-2][blockedMidTiles.get(mt)-1][1] == 0 || ARENA_MAP[sizeX-2][blockedMidTiles.get(mt)+1][1] == 0){
					for (int k = 0; k < 5; k++){
						ARENA_MAP[sizeX-1][blockedMidTiles.get(mt)][k] = 0;
					}
					blockedMidTiles.remove(mt);
					extraEntries -= 1;
				} else {
					blockedMidTiles.remove(mt);
				}
			}
		}
		
		// mirror the map
		for(int i = 0; i < sizeX; i++){
			for(int j = 0; j < sizeY; j++){
				for (int k = 0; k < 5; k++){
					ARENA_MAP[Constants.WIDTH_TILES - i - 1][j][k] = ARENA_MAP[i][j][k];
				}
			}
		}
	}
	
	private int generateEntities(Random rng, String entityName, int minAmount, int maxAmountIndice, int emptyTiles){
		int entityX = -1, entityY = -1, amount = 0;
		int maxAmount = Constants.MAX_GENERATION_AMOUNTS[maxAmountIndice];
		// precise amount of entities vs percentage amount of entities
		if (maxAmount <= 0) {
			maxAmount = Math.abs(maxAmount);
		} else {
			maxAmount = (int) Math.ceil((double) emptyTiles * maxAmount / 100);
		}
		// randomizing actual amount within the range
		if (minAmount >= maxAmount){
			amount = minAmount;
		} else {
			amount = rng.nextInt(maxAmount - minAmount) + minAmount;
		}
		// generate
		for(int i = 0; i < amount && emptyTiles > 0; i++){
			entityX = rng.nextInt(Constants.WIDTH_TILES);
			entityY = rng.nextInt(Constants.HEIGHT_TILES);
			
			// Mines & Forests tiles
			if (maxAmountIndice == 0 || maxAmountIndice == 1){
				// look for free tile until you find one
				while(!(ARENA_MAP[entityX][entityY][0] == 0 && ARENA_MAP[entityX][entityY][1] == 0)) {
					entityX = rng.nextInt(Constants.WIDTH_TILES);
					entityY = rng.nextInt(Constants.HEIGHT_TILES);
				}
			
			// Player tiles
			} else {
				// look for free tile until you find one (can't be right in the middle of the map of odd width, because player and enemy castle would occupy same tile)
				while(!(ARENA_MAP[entityX][entityY][0] == 0 && ARENA_MAP[entityX][entityY][1] == 0) || (entityX == Constants.WIDTH_TILES - entityX - 1)) {
					entityX = rng.nextInt(Constants.WIDTH_TILES);
					entityY = rng.nextInt(Constants.HEIGHT_TILES);
				}	
			}
			
			// add actual tile
			if (entityName == "MINE") {
				int goldAmount = rng.nextInt(Constants.MAX_GENERATION_HP_VALUES[0] - Constants.MIN_GENERATION_HP_VALUES[0]) + Constants.MIN_GENERATION_HP_VALUES[0];
				if (entityX == Constants.WIDTH_TILES - entityX - 1) {
					addGold(entityX, entityY, goldAmount);
					emptyTiles -= 1;
				} else {
					addGold(entityX, entityY, goldAmount);
					addGold(Constants.WIDTH_TILES - entityX - 1, entityY, goldAmount);
					emptyTiles -= 2;
				}
			} else if (entityName == "FOREST") {
				int woodAmount = rng.nextInt(Constants.MAX_GENERATION_HP_VALUES[1] - Constants.MIN_GENERATION_HP_VALUES[1]) + Constants.MIN_GENERATION_HP_VALUES[1];
				if (entityX == Constants.WIDTH_TILES - entityX - 1) {
					addWood(entityX, entityY, woodAmount);
					emptyTiles -= 1;
				} else {
					addWood(entityX, entityY, woodAmount);
					addWood(Constants.WIDTH_TILES - entityX - 1, entityY, woodAmount);
					emptyTiles -= 2;
				}
			} else {
				addPlayerEntity(entityX, entityY, 1, entityName);
				addPlayerEntity(Constants.WIDTH_TILES - entityX - 1, entityY, 2, entityName);
			}
		}
		return emptyTiles;
	}
	
	// =====
    // ARENA_MAP  : 3D Array [WIDTH_TILES][HEIGHT_TILES][5]: {ownerID, entityType, tilePointer, health, ifLocked}
    // TOOLTIPS_CHECK : 2D Array [WIDTH_TILES][HEIGHT_TILES]: 0 = tile has tooltip, 1 = doesn't have a tooltip
	public void initArena(Random rng) {
		
		//Constants.WIDTH_TILES  = rng.nextInt(40)+10; //10 - 50
		//Constants.HEIGHT_TILES = rng.nextInt(40)+10; //10 - 50
		
		// initialising Entity Types
		// (name), (entityType/maxHP), (reach/attack/step), (goldCost/woodCost)
		initEntityTypes(); // data defined in Constants.java
		
		// generating width and height of the map (TODO: exceed 32 x 18 map)
		Constants.WIDTH_TILES  = rng.nextInt(Constants.MAX_WIDTH_TILES  - Constants.MIN_WIDTH_TILES)  + Constants.MIN_WIDTH_TILES;
		Constants.HEIGHT_TILES = rng.nextInt(Constants.MAX_HEIGHT_TILES - Constants.MIN_HEIGHT_TILES) + Constants.MIN_HEIGHT_TILES;
		
		ARENA_MAP         = new int[Constants.WIDTH_TILES][Constants.HEIGHT_TILES][5];
		ARENA_MAP_SPRITES = new Sprite[Constants.WIDTH_TILES][Constants.HEIGHT_TILES];
		ARENA_MAP_TEXT    = new Text[Constants.WIDTH_TILES][Constants.HEIGHT_TILES];
		TOOLTIPS_CHECK    = new int[Constants.WIDTH_TILES][Constants.HEIGHT_TILES];
		
		// initialising walled ARENA_MAP for map generation
		for (int i = 0; i < Constants.WIDTH_TILES; i++) {
			for (int j = 0; j < Constants.HEIGHT_TILES; j++) {
				ARENA_MAP[i][j][0] =  0;  // neutral
				ARENA_MAP[i][j][1] =  1;  // wall
				ARENA_MAP[i][j][2] =  0;  // not pointing
				ARENA_MAP[i][j][3] = -1;  // unbreakable
				ARENA_MAP[i][j][4] = -1;  // locked
			}
		}
		
		generateArena(rng);
		
		int emptyTiles = 0;
		// initialising all walls from the generated arena
		for(int i = 0; i < Constants.WIDTH_TILES; i++){
			for(int j = 0; j < Constants.HEIGHT_TILES; j++){
				if(ARENA_MAP[i][j][0] == 0 && ARENA_MAP[i][j][1] == 1){
					addWall(i, j, -1);
				} else {
					emptyTiles += 1;
				}
			}
		}
		
		// generate all other entities
		emptyTiles = generateEntities(rng, "CASTLE",   1, 2, emptyTiles);
		emptyTiles = generateEntities(rng, "MINE",     0, 0, emptyTiles);
		emptyTiles = generateEntities(rng, "FOREST",   0, 1, emptyTiles);
		emptyTiles = generateEntities(rng, "BARRACKS", 0, 3, emptyTiles);
		emptyTiles = generateEntities(rng, "WORKER",   0, 4, emptyTiles);
		emptyTiles = generateEntities(rng, "LIGHT",    0, 5, emptyTiles);
		emptyTiles = generateEntities(rng, "HEAVY",    0, 6, emptyTiles);
		emptyTiles = generateEntities(rng, "RANGED",   0, 7, emptyTiles);
		
		// initialising empty TOOLTIPS_CHECK
		initTooltipsChecks();
		
		// =====
        // initialising starting scenario on ARENA_MAP
        //InputStream intext = ClassLoader.getSystemResourceAsStream("arena0etypes.txt");
        //String[] result = new BufferedReader(new InputStreamReader(intext)).lines().parallel().collect(Collectors.joining("\n")).split(",");
        //LinkedList<Integer> dataETypes = new LinkedList<Integer>();
        //for (String word : result) {
		//	dataETypes.addLast(Integer.valueOf(word.replaceAll("\\s+","")));
		//}
        //intext = ClassLoader.getSystemResourceAsStream("arena0health.txt");
        //result = new BufferedReader(new InputStreamReader(intext)).lines().parallel().collect(Collectors.joining("\n")).split(",");
        //LinkedList<Integer> dataHealth = new LinkedList<Integer>();
        //for (String word : result) {
		//	dataHealth.addLast(Integer.valueOf(word.replaceAll("\\s+","")));
		//}
		
		// first lines then tiles (first pick row, then pick column)
		//int tempE, tempH;
		//for (int j = 0; j < Constants.HEIGHT_TILES; j++){
		//	for (int i = 0; i < Constants.WIDTH_TILES; i++){
		//		tempE = dataETypes.getFirst();
		//		tempH = dataHealth.getFirst();
		//		// add wall
		//		if (tempE == 1){
		//			addWall(i, j, -1);
		//		// add mine
		//		} else if (tempE == 2){
		//			addGold(i, j, tempH);
		//		// add forest
		//		} else if (tempE == 3){
		//			addWood(i, j, tempH);
		//		// add castle[1]
		//		} else if (tempE == 5){
		//			addPlayerEntity(i, j, 1, "CASTLE");
		//		// add castle[2]
		//		} else if (tempE == 6){
		//			addPlayerEntity(i, j, 2, "CASTLE");
		//		}
		//		dataETypes.removeFirst();
		//		dataHealth.removeFirst();
		//	}
		//}
		
		// initialising materials for each player
		for (Player p : gameManager.getActivePlayers()) {
			p.initMaterial("GOLD", Constants.START_GOLD); 
			p.initMaterial("WOOD", 0);
		}
		
		// initialising graphical representation of ARENA_MAP
		initGraphicArena();
	}
	
	private void addToArena(int x, int y, int ownerID, int entityType, int health) {
		ARENA_MAP[x][y][0] = ownerID;
		ARENA_MAP[x][y][1] = entityType;
		ARENA_MAP[x][y][2] = 0;
		ARENA_MAP[x][y][3] = health;
		ARENA_MAP[x][y][4] = -1;
		ENTITY_LIST.add(new RTSEntity(x, y));
	}
	// ==========
	// (int x, int y, int ownerID, int entityType, int health)
	private void addWall(int x, int y, int health) {
		addToArena(x, y, 0, 1, health);
	}
	private void addGold(int x, int y, int health) {
		addToArena(x, y, 0, 2, health);
	}
	private void addWood(int x, int y, int health) {
		addToArena(x, y, 0, 3, health);
	}
	public void addPlayerEntity(int x, int y, int ownerID, String entityTypeName) {
		addToArena(x, y, ownerID, ENTITY_TYPES.get(entityTypeName)[0], ENTITY_TYPES.get(entityTypeName)[1]);
	}
	
	// adding a tooltip to an entity
	public void setEntityTooltip(int x, int y, String tile_health) {
			
		// GOLD MINE
		if (isTileMine(x,y)) {
			tooltips.setTooltipText(getTileSprite(x,y), String.format("Gold Mine\nx: %d, y: %d\n%s gold left.", x, y, tile_health));
			setTileTooltipCheck(x, y, 1);
			
		// FOREST		
		} else if (isTileForest(x,y)) {
			tooltips.setTooltipText(getTileSprite(x,y), String.format("Forest\nx: %d, y: %d\n%s wood left.", x, y, tile_health));
			setTileTooltipCheck(x, y, 1);
			
		
		} else if (!isTileNeutral(x,y)) {
			for (Player p : gameManager.getActivePlayers()) {
				if (p.getIndex() + 1 == getTileOwner(x,y)) {
					// CASTLE
					if (isTileCastle(x,y)) {
						tooltips.setTooltipText(getTileSprite(x,y), String.format("Castle\nx: %d, y: %d\nhealth: %s/%d\ngold: %d, wood: %d", x, y, tile_health, getTileMaxHP(x,y), p.gold(), p.wood()));
										
					// OTHER: BARRACKS | WORKER | LIGHT | HEAVY | RANGED
					} else {
						String tempname = Constants.ENTITY_TYPES_NAMES[getTileType(x,y)];
						tempname = tempname.substring(0, 1).toUpperCase() + tempname.substring(1).toLowerCase();
						tooltips.setTooltipText(getTileSprite(x,y), String.format("%s\nx: %d, y: %d\nhealth: %s/%d", tempname, x, y, tile_health, getTileMaxHP(x,y)));
					}
				}
			}
			setTileTooltipCheck(x, y, 1);
		}
	}
	
	// =====
    // initialising graphical representation of ARENA_MAP
    public void initGraphicArena(){
		paddingL = (32 - Constants.WIDTH_TILES)  * 30;
		paddingT = (18 - Constants.HEIGHT_TILES) * 30;
		
		if (Constants.WIDTH_TILES % 2 == 1 && Constants.HEIGHT_TILES % 2 == 1) {
			graphicEntityModule.createSprite().setImage("empty-arena-LT.png").setAnchor(0);
			
		} else if (Constants.WIDTH_TILES % 2 == 1) {
			graphicEntityModule.createSprite().setImage("empty-arena-L.png").setAnchor(0);
			
		} else if (Constants.HEIGHT_TILES % 2 == 1) {
			graphicEntityModule.createSprite().setImage("empty-arena-T.png").setAnchor(0);
			
		} else {
			graphicEntityModule.createSprite().setImage("empty-arena.png").setAnchor(0);
			
		}
        
        
        // draw initial map
        for (int i = 0; i < Constants.WIDTH_TILES; i++) {
			for (int j = 0; j < Constants.HEIGHT_TILES; j++) {
				String tile_filename = "0-0.png", tile_health = "";
				int font_size = 30;
				double di = Double.valueOf(i), dj = Double.valueOf(j), dt = Double.valueOf(Constants.TILE_SIZE);
				if (ARENA_MAP[i][j][2] == 0){
					tile_filename = String.format("%d-%d.png", ARENA_MAP[i][j][0], ARENA_MAP[i][j][1]);
					// non-empty entity
					if (ARENA_MAP[i][j][0] != 0 || ARENA_MAP[i][j][1] == 2 || ARENA_MAP[i][j][1] == 3){
						// unbreakable/infinite entity
						if (ARENA_MAP[i][j][3] == -1) {
							tile_health = "∞";
							font_size   = 50;
						// normal entity
						} else {
							tile_health = String.valueOf(ARENA_MAP[i][j][3]);
						}
					}
				}
				
				ARENA_MAP_SPRITES[i][j] = graphicEntityModule.createSprite()
						.setImage(tile_filename)
						.setAnchorX(0)
						.setAnchorY(0)
						.setX((int)(di * dt) + paddingL)
						.setY((int)(dj * dt) + paddingT);
				ARENA_MAP_TEXT[i][j] = graphicEntityModule.createText()
						.setText(tile_health)
						.setX((int)((di+0.5) * dt)  + paddingL)
						.setY((int)((dj+0.75) * dt) + paddingT)
						.setZIndex(20)
						.setFontSize(font_size)
						.setFillColor(0xffffff)
						.setAnchor(0.5);
						
				setEntityTooltip(i, j, tile_health);
						
			}
		}
	}
	
	public void initTooltipsChecks(){
		// initialising empty ARENA_MAP and empty TOOLTIPS_CHECK
		for (int i = 0; i < Constants.WIDTH_TILES; i++) {
			for (int j = 0; j < Constants.HEIGHT_TILES; j++) {
				TOOLTIPS_CHECK[i][j] = 0;
			}
		}
	}
	
	// ==============
	// get values
	// ==============
	public int[][][] getMap(){
		return ARENA_MAP;
	}
	public int[] getTile(int i, int j){
		return ARENA_MAP[i][j];
	}
	public int getTileValue(int i, int j, int k){
		return ARENA_MAP[i][j][k];
	}
	
	// ownerID, entityType, tilePointer, health, ifLocked
	public int getTileOwner(int i, int j){
		return ARENA_MAP[i][j][0];
	}
	public int getTileType(int i, int j){
		return ARENA_MAP[i][j][1];
	}
	public int getTilePointer(int i, int j){
		return ARENA_MAP[i][j][2];
	}
	public int getTileHealth(int i, int j){
		return ARENA_MAP[i][j][3];
	}
	public int getTileLockStatus(int i, int j){
		return ARENA_MAP[i][j][4];
	}
	// ==============
	public int getTileMaxHP(int i, int j){
		return Integer.valueOf(getTileEType(i,j)[1]);
	}
	
	public int getTileReach(int i, int j){
		return Integer.valueOf(getTileEType(i,j)[2]);
	}
	
	public int getTileAttack(int i, int j){
		return Integer.valueOf(getTileEType(i,j)[3]);
	}
	
	public int getTileStep(int i, int j){
		return Integer.valueOf(getTileEType(i,j)[4]);
	}
	
	public int getTileCostGold(int i, int j){
		return Integer.valueOf(getTileEType(i,j)[5]);
	}
	
	public int getTileCostWood(int i, int j){
		return Integer.valueOf(getTileEType(i,j)[6]);
	}
	
	public int getCostGold(int tileType){
		return Integer.valueOf(getEType(tileType)[5]);
	}
	
	public int getCostWood(int tileType){
		return Integer.valueOf(getEType(tileType)[6]);
	}
	
	// ==============
	public HashMap<String, int[]> getETypes(){
		return ENTITY_TYPES;
	}
	public int[] getEType(int tileType){
		return ENTITY_TYPES.get(Constants.ENTITY_TYPES_NAMES[tileType]);
	}
	public int[] getTileEType(int i, int j){
		return getEType(getTileType(i,j));
	}
	public ArrayList<RTSEntity> getEntities(){
		return ENTITY_LIST;
	}
	
	// ==============
	public Sprite[][] getSpritesMap(){
		return ARENA_MAP_SPRITES;
	}
	public Sprite getTileSprite(int i, int j){
		return ARENA_MAP_SPRITES[i][j];
	}
	
	// ==============
	public Text[][] getTextMap(){
		return ARENA_MAP_TEXT;
	}
	public String getTileTextValue(int i, int j){
		return ARENA_MAP_TEXT[i][j].getText();
	}
	
	// ==============
	public int[][] getTooltipChecks(){
		return TOOLTIPS_CHECK;
	}
	public int getTileTooltipCheck(int i, int j){
		return TOOLTIPS_CHECK[i][j];
	}
	
	// ==============
	// set values
	// ==============
	public void setTileValue(int i, int j, int k, int value){
		ARENA_MAP[i][j][k] = value;
	}
	public void setTileLockStatus(int i, int j, int value){
		ARENA_MAP[i][j][4] = value;
	}
	public void raiseTileLockStatus(int i, int j){
		ARENA_MAP[i][j][4] += 1;
	}
	// ==============
	public void harvestTile(int i, int j){
		ARENA_MAP[i][j][3] -= 1;
	}
	public void attackTile(int i, int j, int value){
		ARENA_MAP[i][j][3] -= value;
	}
	
	// ==============
	public Sprite setTileSpriteVisible(int i, int j, String text){
		return ARENA_MAP_SPRITES[i][j].setImage(text).setVisible(true);
	}
	public Sprite setTileSpriteInvisible(int i, int j, String text){
		return ARENA_MAP_SPRITES[i][j].setImage(text).setVisible(false);
	}
	public void setTileSprite(int i, int j, String text){
		ARENA_MAP_SPRITES[i][j].setImage(text);
	}
	public void setTileVisible(int i, int j){
		ARENA_MAP_SPRITES[i][j].setVisible(true);
	}
	public void setTileInvisible(int i, int j){
		ARENA_MAP_SPRITES[i][j].setVisible(false);
	}
	
	// ==============
	public Text setTileText(int i, int j, String text, int x, int y, int fontsize){
		return ARENA_MAP_TEXT[i][j].setText(text).setX(x + paddingL).setY(y + paddingT).setFontSize(fontsize);
	}
	
	// ==============
	public void setTileTooltipCheck(int i, int j, int value){
		TOOLTIPS_CHECK[i][j] = value;
	}
	
	// ==============
	// check values
	// ==============
	public boolean isTileEmpty(int i, int j){
		return getTileOwner(i,j) == 0 && getTileType(i,j) == 0;
	}
	public boolean isTileWall(int i, int j){
		return getTileOwner(i,j) == 0 && getTileType(i,j) == 1;
	}
	public boolean isTileMine(int i, int j){
		return getTileOwner(i,j) == 0 && getTileType(i,j) == 2;
	}
	public boolean isTileForest(int i, int j){
		return getTileOwner(i,j) == 0 && getTileType(i,j) == 3;
	}
	public boolean isTileCastle(int i, int j){
		return getTileOwner(i,j) != 0 && getTileType(i,j) == 0;
	}
	public boolean isTileNeutral(int i, int j){
		return getTileOwner(i,j) == 0;
	}
	public boolean checkTileOwnership(int i, int j, int owner){
		return ARENA_MAP[i][j][0] == owner;
	}
	
	// ==============
	public boolean isTileSpriteEqual(int i, int j, String text){
		return getTileSprite(i,j).getImage().equals(text);
	}
	
	public boolean isTileTextEqual(int i, int j, String text){
		return getTileTextValue(i,j).equals(text);
	}
	
	
	// Finds and returns indice of entity that is present at those coordinates.
	public int findRTSEntity (int coordx, int coordy){
		for (int i = 0; i < ENTITY_LIST.size(); i++) {
			if (ENTITY_LIST.get(i).x == coordx && ENTITY_LIST.get(i).y == coordy){
				return i;
			}
		}
		return -1;
	}
	
	// ==============
	public void cleanupDead() {
		ArrayList<RTSEntity> cleanedList = new ArrayList<RTSEntity>();
		for (RTSEntity e : ENTITY_LIST) {
			int x = e.x, y = e.y;
			// check if health == 0
			if (ARENA_MAP[x][y][3] == 0) {
				ARENA_MAP[x][y][0] = 0; ARENA_MAP[x][y][1] = 0;
				ARENA_MAP[x][y][2] = 0; ARENA_MAP[x][y][4] = 0;
			} else {
				cleanedList.add(e);
			}
		}
		ENTITY_LIST = new ArrayList<RTSEntity>(cleanedList);
	}
}
