/*
 * @author Van Quoc Phuong Huynh, FAW JKU
 *
 */

package prepr;

/**
 * An implementation of lightweight dynamic array of double numbers
 */
public class DoubleArray {
	private static final float allocate_rate = 1.75f;
	private double[] array;
	private int size;
	
 	public DoubleArray(int capacity){
 		this.size = 0;
		this.array = new double[capacity];
	}
 	
 	public DoubleArray(){
 		this.size = 0;
		this.array = new double[16];
	}
 	
 	public int size(){
 		return this.size;
 	}
 	
 	public int capacity(){
 		return this.array.length;
 	}
 	
 	/**
 	 * Add a new number to the end of the list
 	 */
 	public void add(double num){
 		if(this.size == this.array.length){
 			// No spare room for new node, allocate new space
 			double[] new_space = new double[(int)(this.array.length*allocate_rate) + 1];
 			// Copy
 			System.arraycopy(this.array, 0, new_space, 0, size);
 			this.array = new_space;
 		}
 		// Add new number
 		this.array[size] = num;
 		this.size++;
 	}
 	
 	public void addAll(DoubleArray obj){
 		int new_size = this.size + obj.size;
 		
 		if(new_size >= this.array.length){
 			// No spare room for new node, allocate new space
 			double[] new_space = new double[(int)(new_size*allocate_rate)+1];
 			// Copy
 			System.arraycopy(this.array, 0, new_space, 0, this.size);
 			this.array = new_space;
 		}
 		// Add new numbers
		System.arraycopy(obj.array, 0, this.array, this.size, obj.size);
 		
 		this.size = new_size;
 	}
 	
 	/**
 	 * Get the number at index
 	 */
 	public double get(int index){
 		return this.array[index];
 	}
 	
 	/**
 	 * This function should only be used when being sure that there will not be any new elements added.
 	 * </br> Shrink the capacity to the size.
 	 */
 	public void shrink(){
		double[] new_space = new double[size];
		// Copy
		System.arraycopy(this.array, 0, new_space, 0, size);
		this.array = new_space;
 	}
 	
 	/**
 	 * @return double array of the size
 	 */
 	public double[] toArray(){
 		if(this.size < this.array.length) this.shrink();
 		return this.array;
 	}
}
