package com.codingame.game.engine;

import java.util.*;

public class RTSEntity {
	public int x;
	public int y;
	public int actionCountdown;
	public String activeOrder;
	LinkedList<String> orders = new LinkedList<String>();
	
	public RTSEntity(int x, int y) {
		this.x = x;
		this.y = y;
		this.actionCountdown = 0;
		this.activeOrder = "WAIT";
	}
	public void setCoords(int x, int y){
		this.x = x;
		this.y = y;
	}
	public void setCountdown(int value){
		this.actionCountdown = value;
	}
	public void countDown(){
		this.actionCountdown -= 1;
	}
	public void setActiveOrder(String order){
		this.activeOrder = order;
	}
	
	public void addFirstOrder(String order) {
		orders.addFirst(order);
	}
	public void addLastOrder(String order) {
		orders.addLast(order);
	}
	public String getOrder(int i) {
		return orders.get(i);
	}
	public String popFirstOrder() {
		String tempOrder = orders.getFirst();
		orders.removeFirst();
		return tempOrder;
	}
	public void clearOrders() {
		orders.clear();
	}
	public boolean ifOrdersEmpty() {
		return orders.isEmpty();
	}
	public List<String> getOrders() {
		return orders;
	}  
}
