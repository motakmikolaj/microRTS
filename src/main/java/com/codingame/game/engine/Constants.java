package com.codingame.game.engine;

public class Constants {
	// ====================
	// Base Game Data
	// ====================
	public static final int WIDTH        = 1920;
	public static final int HEIGHT       = 1080;
	public static final int TILE_SIZE    = 60;
	public static final int START_GOLD   = 10;
	
	public static final int FRAME_DURATION      = 400;
	public static final int MAX_TURNS           = 250;
	public static final int MAX_FIRST_TURN_TIME = 1000;
	public static final int MAX_TURN_TIME       = 50;
	
	public static final int MAX_WIDTH_TILES  = 32; // 32
	public static final int MAX_HEIGHT_TILES = 18; // 18
	public static final int MIN_WIDTH_TILES  = 15; // 15 
	public static final int MIN_HEIGHT_TILES = 15; // 15
	
	// ====================
	// Base Entity Data
	// ====================
	// (name)
	public static final String[] ENTITY_TYPES_NAMES = {"CASTLE", "BARRACKS", "WORKER", "LIGHT", "HEAVY", "RANGED"};
	
	//(entityType/maxHP), (reach/attack/step), (goldCost/woodCost)
	public static final int[][]  ENTITY_TYPES_DATA  = {
		{0,10,  3,0,0,  5,5}, // CASTLE
		{1, 4,  2,0,0,  0,5}, // BARRACKS
		{2, 1,  1,1,1,  1,0}, // WORKER
		{3, 4,  1,2,2,  1,1}, // LIGHT
		{4, 8,  1,4,1,  2,1}, // HEAVY
		{5, 1,  3,1,1,  1,1}  // RANGED
	};
	
	public static final int[]  MAX_GENERATION_AMOUNTS  = {
		5,  // MINES    - 5% of free tiles
		5,  // FORESTS  - 5% of free tiles
		-2, // CASTLE   - not more than 2 (always at least 1)
		-2, // BARRACKS - not more than 2
		0,  // WORKER   - no tiles
		0,  // LIGHT    - no tiles
		0,  // HEAVY    - no tiles
		0   // RANGED   - no tiles
	};
	
	public static final int[]  MAX_GENERATION_HP_VALUES  = {
		25,  // MINES 
		25,  // FORESTS 
	};
	
	public static final int[]  MIN_GENERATION_HP_VALUES  = {
		5,  // MINES 
		5,  // FORESTS 
	};
}
