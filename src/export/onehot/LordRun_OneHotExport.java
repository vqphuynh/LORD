/*
 * @author Van Quoc Phuong Huynh, FAW JKU
 *
 */

package export.onehot;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.zip.DataFormatException;

import prepr.DataReader;
import rl.IntHolder;
import rl.RuleInfo;
import arg.ArgHelperIF;
import arg.Arguments;
import arg.LordArgHelper;
import evaluations.HeuristicMetricFactory.METRIC_TYPES;
import evaluations.ModelEvaluation;
import export.onehot.LordOneHotExpo;

/**
 * Cross-validation benchmark for multi-thread LORD algorithm. Export one-hot-encoding for examples and rules
 * </br>If a train data file is in .CSV format, LORD assumes that all attributes are nominal.
 * </br>If a train data file is in .ARFF format, LORD will discrete numerical attributes before learning from the file.
 */
public class LordRun_OneHotExport {
	// Classification result for folds
	private static List<Long> run_times;
	private static List<Long> preprocess_times;
	private static List<Double> rule_counts;
	private static List<Double> avg_rule_lengths;
	private static List<Double> recalls;			// not_weighted average recalls w.r.t. classes
	private static List<Double> precisions;			// not_weighted average precisions w.r.t. classes
	private static List<Double> macro_f1_scores;	// not_weighted average f1_scores w.r.t classes
	private static List<Double> accuracies;			// which are also micro f1 scores
	
	public static void main(String[] args) throws Exception {
		String[] input_directories = new String[]{
        		"data/inputs/mushroom",
//        		"data/inputs/flo_datasets/5",
//				"data/inputs/flo_datasets/16",
//				"data/inputs/flo_datasets/19",
//				"data/inputs/flo_datasets/24",
//				"data/inputs/flo_datasets/36",
//				"data/inputs/flo_datasets/44",
//				"data/inputs/flo_datasets/53",
//				"data/inputs/flo_datasets/57",
//				"data/inputs/flo_datasets/60",
//				"data/inputs/flo_datasets/65",
//				"data/inputs/flo_datasets/68",
//				"data/inputs/flo_datasets/69",
//				"data/inputs/flo_datasets/70",
//				"data/inputs/flo_datasets/81",
//				"data/inputs/flo_datasets/82",
//				"data/inputs/flo_datasets/85",
//				"data/inputs/flo_datasets/89",
//				"data/inputs/flo_datasets/107",
//				"data/inputs/flo_datasets/112",
//				"data/inputs/flo_datasets/118",
        };
		
		Date date = Calendar.getInstance().getTime();  
        DateFormat dateFormat = new SimpleDateFormat("_yyyy-MM-dd_hh-mm-ss");
        String strDate = dateFormat.format(date);
        
        ArgHelperIF arg_helper = new LordArgHelper();
        Arguments arguments = new Arguments();
        arguments.metric_type = METRIC_TYPES.MESTIMATE;
        arguments.metric_arg = 0.1;
        
        for(String input_dir : input_directories){
        	arguments.input_directory = input_dir;
        	Path path = Paths.get(arguments.input_directory);
        	String simple_dir_name = path.getName(path.getNameCount()-1).toString() + strDate;
        	arguments.output_directory = Paths.get("data/outputs/onehot", simple_dir_name).toString();
        	
        	arg_helper.print_arguments(arguments);
            System.out.println("Running LORD Export OneHot ...");

    		run_cross_validation(arguments);
    		
    		System.out.println("Finished.");
        }
	}
	
	/**
	 * Run cross validation.
	 * @param data_dir_path contain all pairs of train and test data sets
	 * @param output_dir_path the base output directory
	 * @throws Exception
	 */
	public static void run_cross_validation(Arguments arguments) throws Exception{
	    File data_dir = new File(arguments.input_directory);
	    File[] train_files = data_dir.listFiles(new FilenameFilter() {
												        @Override
												        public boolean accept(File dir, String name) {
												            return name.toLowerCase().contains("train");
												        }
												    });
	    Arrays.sort(train_files);
	    
	    File[] test_files = data_dir.listFiles(new FilenameFilter() {
												        @Override
												        public boolean accept(File dir, String name) {
												            return name.toLowerCase().contains("test");
												        }
												    });
	    Arrays.sort(test_files);
	    
	    if(train_files.length != test_files.length) return;
	    
	    preprocess_times = new ArrayList<Long>(test_files.length);
	    run_times = new ArrayList<Long>(test_files.length);
	    rule_counts = new ArrayList<Double>(test_files.length);
	    avg_rule_lengths = new ArrayList<Double>(test_files.length);
	    recalls = new ArrayList<Double>(test_files.length);
		precisions = new ArrayList<Double>(test_files.length);
		macro_f1_scores = new ArrayList<Double>(test_files.length);
		accuracies = new ArrayList<Double>(test_files.length);
	    
	    for(int i=0; i<train_files.length; i++){
	    	File output_dir = new File(Paths.get(arguments.output_directory, String.format("fold_%02d", i+1)).toString());
	    	output_dir.mkdirs();
	    	run(train_files[i].getAbsolutePath(),
	    			test_files[i].getAbsolutePath(),
	    			output_dir.getAbsolutePath(),
	    			arguments);
	    }
	    
	    double[] avg_results = calculate_average(run_times, 
	    										preprocess_times,
	    										rule_counts,
	    										avg_rule_lengths,
	    										recalls,
	    										precisions,
	    										macro_f1_scores,
	    										accuracies);
	    write_avg_results(avg_results, arguments);
	}
	
	private static double[] calculate_average(List<Long> run_times,
											List<Long> preprocess_times,
											List<Double> rule_counts,
											List<Double> avg_rule_lengths,
											List<Double> recalls,
											List<Double> precisions,
											List<Double> macro_f1_scores,
											List<Double> accuracies){
		double[] avg_results = new double[8];
		
		long sum_preprocess_time = 0;
		long sum_time = 0;
		double sum_rule_count = 0;
		double sum_avg_rule_length = 0;
		double sum_recalls = 0;
		double sum_precisions = 0;
		double sum_f1_scores = 0;
		double sum_accuracy = 0;
		
		int fold_count = run_times.size();
		for(int i=0; i<fold_count; i++){
			sum_preprocess_time = sum_preprocess_time + preprocess_times.get(i);
			sum_time = sum_time + run_times.get(i);
			sum_rule_count = sum_rule_count + rule_counts.get(i);
			sum_avg_rule_length = sum_avg_rule_length + avg_rule_lengths.get(i);
			sum_recalls = sum_recalls + recalls.get(i);
			sum_precisions = sum_precisions + precisions.get(i);
			sum_f1_scores = sum_f1_scores + macro_f1_scores.get(i);
			sum_accuracy = sum_accuracy + accuracies.get(i);
		}
		
		avg_results[0] = sum_preprocess_time/fold_count;
		avg_results[1] = sum_time/fold_count;
		avg_results[2] = sum_rule_count/fold_count;
		avg_results[3] = sum_avg_rule_length/fold_count;
		avg_results[4] = sum_recalls/fold_count;
		avg_results[5] = sum_precisions/fold_count;
		avg_results[6] = sum_f1_scores/fold_count;
		avg_results[7] = sum_accuracy/fold_count;
		
		return avg_results;
	}
	
	/**
	 * @param train_filename
	 * @param test_filename
	 * @param output_dir
	 * @param arguments
	 * @throws IOException
	 * @throws DataFormatException
	 */
	public static void run(String train_filename,
							String test_filename,
							String output_dir,
							Arguments arguments) throws IOException, DataFormatException{
		PrintStream out = new PrintStream(new FileOutputStream(Paths.get(output_dir, "eg_output.txt").toString()));
		System.setOut(out);
		
		LordOneHotExpo alg = new LordOneHotExpo();
		alg.setThreadCount(arguments.thread_count, true);
		
		System.out.println(String.format("Execute algorithm %s on dataset:\n %s \n %s",
											alg.getClass().getSimpleName(), train_filename, test_filename));
		
		System.out.println(String.format("Metric type: %s, Argument: %f", 
						arguments.metric_type.name(), arguments.metric_arg));
		
		long[] times = alg.fetch_information(train_filename);
		long init_time = 0;
		for(long time : times) init_time += time;
		
		System.out.println(String.format("preprocess time: %d ms", times[0]));
		System.out.println(String.format("tree building time: %d ms", times[1]));
		System.out.println(String.format("selector-nlists generation time: %d ms", times[2]));
		System.out.println(String.format("Total init time: %d ms", init_time));
		
		long learning_time = alg.learning(arguments.metric_type, arguments.metric_arg);
		System.out.println(String.format("Learning time: %d ms", learning_time));		

		// Calculate average rule length
		double rule_count, avg_rule_length = 0;
		rule_count = alg.rm.ruleList.size();
		for(RuleInfo rule :  alg.rm.ruleList){
			avg_rule_length += rule.body.length;
		}
		avg_rule_length = avg_rule_length/rule_count;
		
		System.out.println();
		System.out.println("------------------------------------------------------------------------------------");
		
		DataReader dr = DataReader.getDataReader(test_filename);
		String[] value_record;
		IntHolder predicted_classID = new IntHolder(-1);
		int[] example_selectorIDs;
		
		// Predict
		List<Integer> y_true = new ArrayList<Integer>();
		List<Integer> y_pred = new ArrayList<Integer>();
		long start = System.currentTimeMillis();
		dr.bind_datasource(test_filename);
		while((value_record = dr.next_record()) != null){
			example_selectorIDs = alg.predict(value_record, predicted_classID);
			y_pred.add(predicted_classID.value);
			y_true.add(example_selectorIDs[example_selectorIDs.length-1]);
		}
		long prediction_time = System.currentTimeMillis() - start;
		
		// Calculate performance measurements
		ModelEvaluation me = new ModelEvaluation();
		//me.fetch_prediction_result(y_true, y_pred, alg.getClassIDs());
		me.fetch_prediction_result(y_true, y_pred, null);
		double[] scores = me.get_not_weighted_f1_score();
		
		preprocess_times.add(times[0]);
		run_times.add(init_time+learning_time+prediction_time);
		rule_counts.add(rule_count);
		avg_rule_lengths.add(avg_rule_length);
		recalls.add(scores[ModelEvaluation.recall_idx]);
		precisions.add(scores[ModelEvaluation.precision_idx]);
		macro_f1_scores.add(scores[ModelEvaluation.f1_score_idx]);
		accuracies.add(me.getAccuracy());
		
		// Print on each fold
		System.out.println("\nSUMMARY:");
		System.out.println(String.format("Testing example count: %d", y_true.size()));
		System.out.println(String.format("Prediction time: %d ms", prediction_time));
		System.out.println(String.format("Total running time: %d ms", init_time+learning_time+prediction_time));
		System.out.println(String.format("Rule count: %.0f", rule_count));
		System.out.println(String.format("Average rule length: %f", avg_rule_length));
		System.out.println(String.format("Recall: %f", scores[ModelEvaluation.recall_idx]));
		System.out.println(String.format("Precision: %f", scores[ModelEvaluation.precision_idx]));
		System.out.println(String.format("Macro f1-score (not weighted): %f", scores[ModelEvaluation.f1_score_idx]));
		System.out.println(String.format("Accuracy: %f", me.getAccuracy()));
		
		
		//////////////////////Export ONE-HOT-ENCODING///////////////////////
		String example_filepath = Paths.get(output_dir, "examples_onehot.txt").toString();
		String rule_filepath = Paths.get(output_dir, "rules_onehot.txt").toString();
		alg.export_onehot(example_filepath, rule_filepath);
	}
	
	static void write_avg_results(double[] avg_results, Arguments arguments) throws IOException{
    	BufferedWriter output = new BufferedWriter(new FileWriter(
    			Paths.get(arguments.output_directory, "cross_validate_results.csv").toString()));
    	
    	StringBuilder sb = new StringBuilder(200);
    	sb.append("preprocess_time(ms), run_time(ms), avg_rule_count, avg_rule_length, avg_recall, avg_precision, avg_macro_f1_score, avg_accuracy\n")
    	.append(avg_results[0])
    	.append(", ").append(avg_results[1])
    	.append(", ").append(avg_results[2])
    	.append(", ").append(avg_results[3])
    	.append(", ").append(avg_results[4])
    	.append(", ").append(avg_results[5])
    	.append(", ").append(avg_results[6])
    	.append(", ").append(avg_results[7]).append('\n');
    	output.write(sb.toString());
    	output.flush();
    	output.close();
    }
}
