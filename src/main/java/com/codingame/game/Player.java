package com.codingame.game;

import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;

import com.codingame.gameengine.core.AbstractMultiplayerPlayer;

public class Player extends AbstractMultiplayerPlayer {
	
	public HashMap<String, Integer> materials = new HashMap<String, Integer>();
	
	public void initMaterial(String name, int amount) {
		this.materials.put(name, amount);
	}
	public void addMaterial(String name, int amount) {
		if (this.materials.get(name) + amount <= 0) {
			this.materials.put(name, 0);
		} else {
			this.materials.put(name, this.materials.get(name) + amount);
		}
	}
	public int gold() {
		return this.materials.get("GOLD");
	}
	public int wood() {
		return this.materials.get("WOOD");
	}
	
    public String[] getAction() throws TimeoutException {
        return this.getOutputs().get(0).split(";");
    }
    
    @Override
    public int getExpectedOutputLines() {
        return 1;
    }
}
