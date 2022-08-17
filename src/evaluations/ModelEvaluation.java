/*
 * @author Van Quoc Phuong Huynh, FAW JKU
 *
 */

package evaluations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ModelEvaluation {
	private Map<Integer, Integer> classId_index = null;
	private List<Integer> classIDs = null;
	private int[] classDist_true = null;
	private int[] classDist_pred = null;
	private double[][] confusionMatrix = null;
	private double test_example_count;
	public static final int recall_idx=0, precision_idx=1, f1_score_idx=2;
	
	private double[][] scores = null;
	private double accuracy;	// equals micro f1-score = micro precision = micro recall
	private double hit_count;
	private double miss_count;
	
	/**
	 * rows for predicted classes, columns for true classes 
	 * @return
	 */
	public double[][] getConfusionMatrix() {
		return confusionMatrix;
	}
	
	public double[][] getScores() {
		return scores;
	}

	public double getAccuracy() {
		return accuracy;
	}
	
	public double getTestExampleCount(){
		return this.test_example_count;
	}
	
	public double hitCount(){
		return this.hit_count;
	}
	
	public double missCount(){
		return this.miss_count;
	}
	
	public void print_confustion_matrix(){
		if(this.confusionMatrix != null){
			System.out.println("Confusion matrix:");
			int class_count = this.classIDs.size();
			for(int i=0; i<class_count; i++){
				double[] array = this.confusionMatrix[i];
				System.out.println("class_id " + this.classIDs.get(i) + "\t" + Arrays.toString(array));
			}
		}else{
			System.out.println("No confusion matrix");
		}
	}
	
	public String get_score_string(){
		StringBuilder sb = new StringBuilder(1024);
		if(this.scores != null){
			sb.append("Accuracy: ").append(this.accuracy)
			.append("\nScores:\t\t\tRecall\tPrecision\tf1-score\n");
			int class_count = this.classIDs.size();
			for(int i=0; i<class_count; i++){
				double[] array = this.scores[i];
				sb.append("class_id ").append(this.classIDs.get(i)).append("\t\t").append(Arrays.toString(array)).append('\n');
			}
			sb.append("no-weighted avg ").append("\t").append(Arrays.toString(this.scores[class_count])).append('\n');
			sb.append("weighted avg ").append("\t\t").append(Arrays.toString(this.scores[class_count+1])).append('\n');
		}
		return sb.toString();
	}
	
	public void print_scores(){
		System.out.println(this.get_score_string());
	}
	
	public double[] get_not_weighted_f1_score(){
		return this.scores[this.classIDs.size()];
	}
	
	public double[] get_weighted_f1_score(){
		return this.scores[this.classIDs.size()+1];
	}
	
	/**
	 * @param y_true list of class id of examples
	 * @param y_pred list of predicted class id of examples
	 * @param classId_list list of all class ids, it can be null
	 */
	public void fetch_prediction_result(List<Integer> y_true, List<Integer> y_pred, List<Integer> classId_list){
		if(classId_list == null){
			this.classIDs = get_class_ids(y_true, y_pred);
		}else{
			this.classIDs = classId_list;
		}
		
		int class_count = this.classIDs.size();
		this.classId_index = new HashMap<Integer, Integer>();
		for(int index=0; index < class_count; index++){
			this.classId_index.put(this.classIDs.get(index), index);
		}
		
		this.confusionMatrix = build_confusion_matrix(y_true, y_pred);
		this.calculate_distribution(this.confusionMatrix);
		this.accuracy = this.calculate_accuracy(this.confusionMatrix);
		this.scores = this.calculate_macro_f1_scores();
	}
	
	private List<Integer> get_class_ids(List<Integer> y_true, List<Integer> y_pred){
		Set<Integer> class_set = new HashSet<Integer>();
		
		// get class list
		for(Integer classID : y_true){
			class_set.add(classID);
		}
		for(Integer classID : y_pred){
			class_set.add(classID);
		}
		
		return new ArrayList<Integer>(class_set);
	}
	
	private double[][] build_confusion_matrix(List<Integer> y_true, List<Integer> y_pred){
		// build confusion matrix
		double[][] matrix = new double[this.classIDs.size()][this.classIDs.size()];
		
		this.test_example_count = y_true.size();
		for(int i=0; i<this.test_example_count; i++){
			int class_pred_index = this.classId_index.get(y_pred.get(i));
			int class_true_index = this.classId_index.get(y_true.get(i));
			
			//rows for predicted classes, columns for true classes
			matrix[class_pred_index][class_true_index] ++;
		}
		return matrix;
	}
	
	private void calculate_distribution(double[][] confusionMatrix){
		int class_count = this.classIDs.size();
		int[] classDist_true = new int[class_count];
		int[] classDist_pred = new int[class_count];
		for(int i=0; i<class_count; i++){
			for(int j=0; j<class_count; j++){
				classDist_pred[i] += confusionMatrix[i][j];
				classDist_true[j] += confusionMatrix[i][j];
			}
		}
		this.classDist_pred = classDist_pred;
		this.classDist_true = classDist_true;
	}
	
	private double calculate_accuracy(double[][] confusionMatrix){
		int class_count = this.classIDs.size();
		double hit_count = 0;
		for(int i=0; i<class_count; i++){
			hit_count += this.confusionMatrix[i][i];
		}
		this.hit_count = hit_count;
		this.miss_count = this.test_example_count - this.miss_count;
		
		return hit_count/this.test_example_count;
	}
	
	private double[][] calculate_macro_f1_scores(){
		int class_count = this.classIDs.size();
		
		//				recall	precision	f1-score
		//class1
		//class2
		//...
		//classk
		//no-weighted
		//weighted
		double[][] scores = new double[class_count+2][3];
		for(int i=0; i<class_count; i++){
			if(this.classDist_true[i] != 0 && this.classDist_pred[i] != 0){
				// this.classDist_true[i] = tp+fn > 0, this.classDist_pred[i] = tp+fp > 0
				scores[i][recall_idx] = this.confusionMatrix[i][i]/this.classDist_true[i];
				scores[i][precision_idx] = this.confusionMatrix[i][i]/this.classDist_pred[i];
			}else if(this.classDist_true[i] == 0 && this.classDist_pred[i] == 0){
				// tp=0, fn=0, fp=0
				scores[i][recall_idx] = 1;
				scores[i][precision_idx] = 1;
			}else{
				// tp+fn = 0 xor tp+fp = 0		=>		tp=0, (fn=0 xor fp=0)
				scores[i][recall_idx] = 0;
				scores[i][precision_idx] = 0;
			}
			
			if(scores[i][recall_idx] == 0 && scores[i][precision_idx] == 0){
				scores[i][f1_score_idx] = 0;
			}else{
				scores[i][f1_score_idx] = 2 * scores[i][recall_idx] * scores[i][precision_idx] / (scores[i][recall_idx] + scores[i][precision_idx]);
			}
		}
		
		for(int i=0; i<3; i++){
			double sum = 0;
			double weighted_sum = 0;
			for(int j=0; j<class_count; j++){
				sum += scores[j][i];
				weighted_sum += scores[j][i] * this.classDist_true[j];
			}
			scores[class_count][i] = sum/class_count;
			scores[class_count+1][i] = weighted_sum/this.test_example_count;
		}
		
		return scores;
	}
}
