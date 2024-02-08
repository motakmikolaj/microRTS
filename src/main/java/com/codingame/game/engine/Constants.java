package com.codingame.game.engine;

public class Constants {
	// ====================
	// Base Game Data
	// ====================
	public static int WIDTH        = 1920;
	public static int HEIGHT       = 1080;
	public static int TILE_SIZE    = 60;
	public static int WIDTH_TILES  = 32;
	public static int HEIGHT_TILES = 18;
	public static int START_GOLD   = 10;
	
	public static int FRAME_DURATION      = 400;
	public static int MAX_TURNS           = 200;
	public static int MAX_FIRST_TURN_TIME = 1000;
	public static int MAX_TURN_TIME       = 50;
	
	// ====================
	// Base Entity Data
	// ====================
	// (name)
	public static String[] ENTITY_TYPES_NAMES = {"CASTLE", "BARRACKS", "WORKER", "LIGHT", "HEAVY", "RANGED"};
	
	//(entityType/maxHP), (reach/attack/step), (goldCost/woodCost)
	public static int[][]  ENTITY_TYPES_DATA  = {
		{0,10,  3,0,0,  5,5}, // CASTLE
		{1, 4,  2,0,0,  0,5}, // BARRACKS
		{2, 1,  1,1,1,  1,0}, // WORKER
		{3, 4,  1,2,2,  1,1}, // LIGHT
		{4, 8,  1,4,1,  2,1}, // HEAVY
		{5, 1,  3,1,1,  1,1}  // RANGED
	};

}
