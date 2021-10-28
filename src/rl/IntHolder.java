/*
 * @author Van Quoc Phuong Huynh, FAW JKU
 *
 */

package rl;

public class IntHolder {
	public int value;
	
	public IntHolder(int value){
		this.value = value;
	}
	
	/**
	 * Add the current value with the amount in a synchronization way
	 * @param amount
	 */
	public synchronized void syncAdd(int amount){
		value += amount;
	}
	
	/**
	 * Get value
	 * @return value
	 */
	public synchronized int syncGet(){
		return value;
	}
}
