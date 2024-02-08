package com.codingame.game.engine;

import java.util.*;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

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
    
    public int[][][]              ARENA_MAP    = new int[Constants.WIDTH_TILES][Constants.HEIGHT_TILES][5];
    public HashMap<String, int[]> ENTITY_TYPES = new HashMap<String, int[]>();
    public ArrayList<RTSEntity>   ENTITY_LIST  = new ArrayList<RTSEntity>();
    
    public Sprite[][] ARENA_MAP_SPRITES = new Sprite[Constants.WIDTH_TILES][Constants.HEIGHT_TILES];
    public Text[][]   ARENA_MAP_TEXT    = new Text[Constants.WIDTH_TILES][Constants.HEIGHT_TILES];
    public int[][]    TOOLTIPS_CHECK    = new int[Constants.WIDTH_TILES][Constants.HEIGHT_TILES];
    
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
	
	// =====
    // ARENA_MAP  : 3D Array [WIDTH_TILES][HEIGHT_TILES][5]: {ownerID, entityType, tilePointer, health, ifLocked}
    // TOOLTIPS_CHECK : 2D Array [WIDTH_TILES][HEIGHT_TILES]: 0 = tile has tooltip, 1 = doesn't have a tooltip
	public void initArena() {
		
		// initialising Entity Types
		// (name), (entityType/maxHP), (reach/attack/step), (goldCost/woodCost)
		initEntityTypes(); // data defined in Constants.java
		
		
		// initialising empty ARENA_MAP and empty TOOLTIPS_CHECK
		for (int i = 0; i < Constants.WIDTH_TILES; i++) {
			for (int j = 0; j < Constants.HEIGHT_TILES; j++) {
				for (int k = 0; k < 5; k++) {
					ARENA_MAP[i][j][k] = 0;
				}
			}
		}
		
		// initialising empty TOOLTIPS_CHECK
		initTooltipsChecks();
		
		// =====
        // initialising starting scenario on ARENA_MAP
        InputStream intext = ClassLoader.getSystemResourceAsStream("arena0etypes.txt");
        String[] result = new BufferedReader(new InputStreamReader(intext)).lines().parallel().collect(Collectors.joining("\n")).split(",");
        LinkedList<Integer> dataETypes = new LinkedList<Integer>();
        for (String word : result) {
			dataETypes.addLast(Integer.valueOf(word.replaceAll("\\s+","")));
		}
        intext = ClassLoader.getSystemResourceAsStream("arena0health.txt");
        result = new BufferedReader(new InputStreamReader(intext)).lines().parallel().collect(Collectors.joining("\n")).split(",");
        LinkedList<Integer> dataHealth = new LinkedList<Integer>();
        for (String word : result) {
			dataHealth.addLast(Integer.valueOf(word.replaceAll("\\s+","")));
		}
		
		// first lines then tiles (first pick row, then pick column)
		int tempE, tempH;
		for (int j = 0; j < Constants.HEIGHT_TILES; j++){
			for (int i = 0; i < Constants.WIDTH_TILES; i++){
				tempE = dataETypes.getFirst();
				tempH = dataHealth.getFirst();
				// add wall
				if (tempE == 1){
					addWall(i, j, -1);
				// add mine
				} else if (tempE == 2){
					addGold(i, j, tempH);
				// add forest
				} else if (tempE == 3){
					addWood(i, j, tempH);
				// add castle[1]
				} else if (tempE == 5){
					addPlayerEntity(i, j, 1, "CASTLE");
				// add castle[2]
				} else if (tempE == 6){
					addPlayerEntity(i, j, 2, "CASTLE");
				}
				dataETypes.removeFirst();
				dataHealth.removeFirst();
			}
		}
		
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
			tooltips.setTooltipText(getTileSprite(x,y), "Gold Mine\n x: " 
								+ String.valueOf(x) + ", y: " 
								+ String.valueOf(y) + "\n"
								+ tile_health + " gold left.");
			setTileTooltipCheck(x, y, 1);
			
		// FOREST		
		} else if (isTileForest(x,y)) {
			tooltips.setTooltipText(getTileSprite(x,y), "Forest\nx: " 
								+ String.valueOf(x) + ", y: " 
								+ String.valueOf(y) + "\n"
								+ tile_health + " wood left.");
			setTileTooltipCheck(x, y, 1);
			
		
		} else if (!isTileNeutral(x,y)) {
			for (Player p : gameManager.getActivePlayers()) {
				if (p.getIndex() + 1 == getTileOwner(x,y)) {
					// CASTLE
					if (isTileCastle(x,y)) {
						tooltips.setTooltipText(getTileSprite(x,y), "Castle\nx: " 
										+ String.valueOf(x) + ", y: " 
										+ String.valueOf(y) + "\nhealth: " 
										+ tile_health + "\ngold: "
										+ String.valueOf(p.gold()) + ", wood: "
										+ String.valueOf(p.wood()));
										
					// OTHER: BARRACKS | WORKER | LIGHT | HEAVY | RANGED
					} else {
						String tempname = Constants.ENTITY_TYPES_NAMES[getTileType(x,y)];
						if (tempname.equals("BARRACKS")) tempname = "Barracks";
						if (tempname.equals("WORKER"))   tempname = "Worker";
						if (tempname.equals("LIGHT"))    tempname = "Light";
						if (tempname.equals("HEAVY"))    tempname = "Heavy";
						if (tempname.equals("RANGED"))   tempname = "Ranged";
						tooltips.setTooltipText(getTileSprite(x,y), tempname + "\nx: " 
								+ String.valueOf(x) + ", y: " 
								+ String.valueOf(y) + "\nhealth: " 
								+ tile_health);	
				
					}
				}
			}
			setTileTooltipCheck(x, y, 1);
		}
	}
	
	// =====
    // initialising graphical representation of ARENA_MAP
    public void initGraphicArena(){
        graphicEntityModule.createSprite().setImage("empty-arena.png").setAnchor(0);
        for (int i = 0; i < Constants.WIDTH_TILES; i++) {
			for (int j = 0; j < Constants.HEIGHT_TILES; j++) {
				String tile_filename = "0-0.png", tile_health = "";
				int font_size = 30;
				double di = Double.valueOf(i), dj = Double.valueOf(j), dt = Double.valueOf(Constants.TILE_SIZE);
				if (ARENA_MAP[i][j][2] == 0){
					tile_filename = Integer.toString(ARENA_MAP[i][j][0]) + "-" + Integer.toString(ARENA_MAP[i][j][1]) + ".png";
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
						.setX((int)(di * dt))
						.setY((int)(dj * dt));
				ARENA_MAP_TEXT[i][j] = graphicEntityModule.createText()
						.setText(tile_health)
						.setX((int)((di+0.5) * dt))
						.setY((int)((dj+0.75) * dt))
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
		return ARENA_MAP_TEXT[i][j].setText(text).setX(x).setY(y).setFontSize(fontsize);
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
	public int findRTSEntity (int x, int y){
		for (int i = 0; i < ENTITY_LIST.size(); i++) {
			if (ENTITY_LIST.get(i).x == x && ENTITY_LIST.get(i).y == y){
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
