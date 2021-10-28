/*
 * @author Van Quoc Phuong Huynh, FAW JKU
 *
 */

package rl;

import java.util.ArrayList;
import java.util.List;

public class Matrix {
	private int[][] matrix;
	private int dim;
	
	public Matrix(int dim){
		this.dim = dim;
		matrix = new int[dim][dim];
	}
	
	public int get(int row, int col){
		return matrix[row][col];
	}
	
	public void set(int row, int col, int value){
		matrix[row][col] = value;
	}
	
	public void add(int row, int col, int amount){
		matrix[row][col] += amount;
	}
	
	public void show(){
		for(int i=0; i<dim; i++){
			for(int j=0; j<dim; j++){
				System.out.print(matrix[i][j] + " ");
			}
			System.out.println("");
		}
	}
	
	/**
	 * Sum pairs of symmetric elements through the diagonal, results are saved the upper-half portion 
	 */
	public void summary_by_diagonal_folding(){
		for(int i=0; i<dim; i++){
			for(int j=i+1; j<dim; j++){
				matrix[i][j] += matrix[j][i];
			}
		}
	}
	
	/**
	 * Sum two diagonal-upper-half portions of two matrixes, result in the calling matrix
	 * @param m another matrix
	 */
	public void summary_with_matrix(Matrix m){
		for(int i=0; i<dim; i++){
			for(int j=i+1; j<dim; j++){
				matrix[i][j] += m.get(i, j);
			}
		}
	}
	
	
	/**
	 * Return String[][] results
	 * </br>results[0] : All frequent 2selector sets from predicting attributes
	 * </br>results[1] : All frequent 2selector sets having 1 selector from target attributes
	 * </br>results[2] : results[0] + results[1]
	 * @param predict_selector_count
	 * @param min_support_count
	 * @return String[][] results
	 */
	public String[][] extract_2selector_sets(int predict_selector_count,
										int min_support_count){
		List<String> predict_2selectors = new ArrayList<String>(); 
		List<String> predict_target_2selectors = new ArrayList<String>(); 
		String[][] results = new String[3][];
		
		StringBuilder sb = new StringBuilder(20);
		int base_length;
		
		for(int i=0; i<predict_selector_count; i++){
			sb.setLength(0);
			sb.append(i).append(' ');
			base_length = sb.length();
			
			for(int j=i+1; j<predict_selector_count; j++){
				if(matrix[i][j] < min_support_count) continue;
				
				sb.setLength(base_length);
				predict_2selectors.add(sb.append(j).toString());
			}
			
			for(int j=predict_selector_count; j<dim; j++){
				if(matrix[i][j] < min_support_count) continue;
				
				sb.setLength(base_length);
				predict_target_2selectors.add(sb.append(j).toString());
			}
		}
		
		results[0] = new String[predict_2selectors.size()];
		predict_2selectors.toArray(results[0]);
				
		results[1] = new String[predict_target_2selectors.size()];
		predict_target_2selectors.toArray(results[1]);
		
		results[2] = new String[results[0].length + results[1].length];
		System.arraycopy(results[0], 0, results[2], 0, results[0].length);
		System.arraycopy(results[1], 0, results[2], results[0].length, results[1].length);
		
		return results;
	}
}
