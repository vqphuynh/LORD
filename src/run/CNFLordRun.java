/*
 * @author Van Quoc Phuong Huynh, FAW JKU
 *
 */

package run;

import arg.Arguments;
import arg.LordArgHelper;
import evaluations.HeuristicMetricFactory.METRIC_TYPES;
import prepr.DataReader;
import rl.IntHolder;
import rl.RuleInfo;
import rl.eg.ExtensiveInfoExporter;
import rl.eg.CNFLord;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.DataFormatException;

/**
 * Cross-validation benchmark for multi-thread CNF-LORD algorithm
 * </br>If a train data file is in .CSV format, CNF-LORD assumes that all attributes are nominal.
 * </br>If a train data file is in .ARFF format, CNF-LORD will discrete numerical attributes before learning from the file.
 *
 */
public class CNFLordRun {
	// Classification result for folds
	private static List<Double> hit_counts;
	private static List<Double> miss_counts;
	private static List<Long> run_times;
	private static List<Long> preprocess_times;
	private static List<Double> rule_counts;
	private static List<Double> avg_rule_lengths;
	
	public static void main(String[] args) throws Exception{
        LordArgHelper arg_helper = new LordArgHelper();
        Arguments arguments = new Arguments();
        arguments.input_directory = "data/inputs/tic-tac-toe";	// uncomment this line for debugging run
        arguments.metric_type = METRIC_TYPES.MESTIMATE;		// default
        arguments.metric_arg = 0.1;							// default
        
        arguments.parse(args, arg_helper);

		String[] datasets = new String[] {
//				"data/inputs/anneal",
//				"data/inputs/audiology",
//				"data/inputs/autos",
//				"data/inputs/balance-scale",
//				"data/inputs/breast-cancer",
//				"data/inputs/breast-w",
//				"data/inputs/colic",
//				"data/inputs/credit-a",
//				"data/inputs/credit-g",
//				"data/inputs/diabetes",
//				"data/inputs/glass",
//				"data/inputs/heart-c",
//				"data/inputs/heart-h",
//				"data/inputs/heart-statlog",
//				"data/inputs/hepatitis",
//				"data/inputs/hypothyroid",
//				"data/inputs/iris",
//				"data/inputs/kr-vs-kp",
//				"data/inputs/labor",
//				"data/inputs/lymph",
//				"data/inputs/mushroom",
//				"data/inputs/primary-tumor",
//				"data/inputs/segment",
//				"data/inputs/sick",
//				"data/inputs/soybean",
//				"data/inputs/vote",
//				"data/inputs/vowel",
//				"data/inputs/yeast",
//				"data/inputs/zoo",
		};
		Double[] mValues = new Double[] {0.0, 0.1, 0.3, 1.0, 3.0, 10.0, 30.0, 100.0};
		for (String s : datasets) {
			for (double d : mValues) {
				arguments.input_directory = s;
				arguments.metric_arg = d;

				//if(arguments.output_directory == null && arguments.input_directory != null){
					Path path = Paths.get(arguments.input_directory);
					Date date = Calendar.getInstance().getTime();
					DateFormat dateFormat = new SimpleDateFormat("_yyyy-MM-dd_hh-mm-ss");
					String strDate = dateFormat.format(date);
					String simple_dir_name = path.getName(path.getNameCount()-1).toString() + "_m" + d + strDate;
					arguments.output_directory = Paths.get("data/outputs/eg_cv", simple_dir_name).toString();
				//}

				if(!arg_helper.is_valid(arguments)){
					arg_helper.print_help();
					return;
				}

				arg_helper.print_arguments(arguments);
				PrintStream stdout = System.out;
				System.out.println("Running CNF-LORD (Eager & Greedy) for " + s + " and m=" + d + "...");

				run_cross_validation(arguments);

				System.setOut(stdout);
			}
		}

		System.out.println("Finished.");
	}
	
	/**
	 * Run cross validation.
	 * @throws Exception
	 */
	public static void run_cross_validation(Arguments arguments) throws Exception{
	    File data_dir = new File(arguments.input_directory);
	    File[] train_files = data_dir.listFiles((dir, name) -> name.toLowerCase().contains("train"));
	    Arrays.sort(train_files);
	    
	    File[] test_files = data_dir.listFiles((dir, name) -> name.toLowerCase().contains("test"));
	    Arrays.sort(test_files);
	    
	    if(train_files.length != test_files.length) return;
	    
	    hit_counts = new ArrayList<>(test_files.length);
	    miss_counts = new ArrayList<>(test_files.length);
	    run_times = new ArrayList<>(test_files.length);
	    preprocess_times = new ArrayList<>(test_files.length);
	    rule_counts = new ArrayList<>(test_files.length);
	    avg_rule_lengths = new ArrayList<>(test_files.length);
	    
	    for(int i=0; i<train_files.length; i++){
	    	File output_dir = new File(Paths.get(arguments.output_directory, String.format("fold_%02d", i+1)).toString());
	    	output_dir.mkdirs();
	    	run(train_files[i].getAbsolutePath(),
	    			test_files[i].getAbsolutePath(),
	    			output_dir.getAbsolutePath(),
	    			arguments);
	    }
	    
	    double[] avg_results = calculate_average(hit_counts, miss_counts, run_times, preprocess_times, rule_counts, avg_rule_lengths);
	    write_avg_results(avg_results, arguments);
	}
	
	private static double[] calculate_average(List<Double> hit_counts,
												List<Double> miss_counts,
												List<Long> run_times,
												List<Long> preprocess_times,
												List<Double> rule_counts,
												List<Double> avg_rule_lengths){
		double[] avg_results = new double[5];
		double sum_accuracy = 0;
		long sum_time = 0;
		long sum_preprocess_time = 0;
		double sum_rule_count = 0;
		double sum_avg_rule_length = 0;
		int fold_count = hit_counts.size();
		
		for(int i=0; i<fold_count; i++){
			sum_accuracy = sum_accuracy + (hit_counts.get(i)/(hit_counts.get(i)+miss_counts.get(i)));
			sum_time = sum_time + run_times.get(i);
			sum_preprocess_time = sum_preprocess_time + preprocess_times.get(i);
			sum_rule_count = sum_rule_count + rule_counts.get(i);
			sum_avg_rule_length = sum_avg_rule_length + avg_rule_lengths.get(i);
		}
		
		avg_results[0] = sum_accuracy/fold_count;
		avg_results[1] = sum_rule_count/fold_count;
		avg_results[2] = sum_avg_rule_length/fold_count;
		avg_results[3] = sum_time/fold_count;
		avg_results[4] = sum_preprocess_time/fold_count;
		
		return avg_results;
	}
	
	/**
	 * @param train_filename
	 * @param test_filename
	 * @return results[0]=running time, results[1]=rule count, results[2]=accuracy performance
	 * @throws IOException
	 * @throws DataFormatException 
	 * @throws Exception
	 */
	public static void run(String train_filename,
							String test_filename,
							String output_dir,
							Arguments arguments) throws IOException, DataFormatException{
		PrintStream out = new PrintStream(new FileOutputStream(Paths.get(output_dir, "eg_output.txt").toString()));
		System.setOut(out);
		
		CNFLord alg = new CNFLord();
		alg.setThreadCount(arguments.thread_count);
		
		System.out.println(String.format("Execute algorithm %s on dataset:\n %s \n %s",
											alg.getClass().getSimpleName(), train_filename, test_filename));
		
		System.out.println(String.format("Metric type: %s, Argument: %f", 
						arguments.metric_type.name(), arguments.metric_arg));
		
		long[] times = alg.fetch_information(train_filename);
		long init_time = 0;
		for(long time : times) init_time += time;
		
		System.out.println(String.format("preprocess time: %d ms", times[0]));
		System.out.println(String.format("tree building time: %d ms", times[1]));
		System.out.println(String.format("selector-nodelists generation time: %d ms", times[2]));
		System.out.println(String.format("Total init time: %d ms", init_time));
		
		long learning_time = alg.learning(arguments.metric_type, arguments.metric_arg);
		System.out.println(String.format("Learning time: %d ms", learning_time));
		
		// Print rule set
		double rule_count, avg_rule_length = 0;
		System.out.println("------------------------------------------------------------------------------------");
		System.out.println("Rule set: ");
		
		rule_count = alg.rm.ruleList.size();
		String[] selectorConditions = alg.getConstructingSelectors().stream()
				.map(s -> "!" + s.condition)
				.toArray(String[]::new);
		for(RuleInfo rule :  alg.rm.ruleList){
			System.out.println(rule.content(selectorConditions));
			avg_rule_length += rule.body.length;
		}
		avg_rule_length = avg_rule_length/rule_count;
		
		System.out.println();
		System.out.println("------------------------------------------------------------------------------------");
		
		DataReader dr = DataReader.getDataReader(test_filename);
		String[] value_record;
		IntHolder predicted_classID = new IntHolder(-1);
		int[] example_selectorIDs;
		double hit_count = 0, miss_count = 0;
		
		long start = System.currentTimeMillis();
		dr.bind_datasource(test_filename);
		while((value_record = dr.next_record()) != null){
			example_selectorIDs = alg.predict(value_record, predicted_classID);
			
			if(predicted_classID.value == example_selectorIDs[example_selectorIDs.length-1])
				hit_count++;
			else{
				miss_count++;
				//print_details(example_selectorIDs, alg.classifier.covering_rules, alg.classifier.selected_rule, predicted_classID.value);
			}
		}
		long prediction_time = System.currentTimeMillis() - start;
		
		System.out.println("------------------------------------------------------------------------------------");
		System.out.println(String.format("Prediction time: %d ms", prediction_time));
		System.out.println(String.format("Hit count: %.0f", hit_count));
		System.out.println(String.format("Miss count: %.0f", miss_count));
		System.out.println(String.format("Rule count: %.0f", rule_count));
		System.out.println(String.format("Average rule length: %f", avg_rule_length));
		System.out.println(String.format("Accuracy: %f", hit_count/(hit_count+miss_count)));
		System.out.println(String.format("Total running time: %d ms", init_time+learning_time+prediction_time));
		
		hit_counts.add(hit_count);
		miss_counts.add(miss_count);
		run_times.add(init_time+learning_time+prediction_time);
		preprocess_times.add(times[0]);
		rule_counts.add(rule_count);
		avg_rule_lengths.add(avg_rule_length);
		
		//////////////////////Export Extensive Information///////////////////////
		// ExtensiveInfoExporter exporter = new ExtensiveInfoExporter(alg);
		// exporter.export_info(Paths.get(arguments.output_directory, "extensive_info.csv").toString());
		// exporter.export_info(Paths.get(arguments.output_directory, "extensive_info.csv").toString(), test_filename);
	}
	
	static void print_details(int[] example_selectorIDs,
										List<RuleInfo> candidate_rules,
										RuleInfo selected_rule,
										int predicted_classID){
		System.out.print("Testing example: "); System.out.println(Arrays.toString(example_selectorIDs));
		if(selected_rule == null){
			System.out.println("Selected rule: ");
			System.out.println("Covering rules: ");
			System.out.print("Predicted class ID (majority): ");System.out.println(predicted_classID);
			System.out.println();
		}else{
			System.out.print("Selected rule: "); System.out.println(selected_rule.content());
			System.out.println("Covering rules: ");
			for(RuleInfo rule : candidate_rules){
				System.out.println(rule.content());
			}
			System.out.println();
		}
	}
	
	static void write_avg_results(double[] avg_results, Arguments arguments) throws IOException{
    	BufferedWriter output = new BufferedWriter(new FileWriter(
    			Paths.get(arguments.output_directory, "cross_validate_results.csv").toString()));
    	
    	StringBuilder sb = new StringBuilder(200);
    	sb.append("avg_accuracy, avg_rule_count, avg_rule_length, run_time(ms), preprocess_time(ms)\n")
    	.append(avg_results[0])
    	.append(", ").append(avg_results[1])
    	.append(", ").append(avg_results[2])
    	.append(", ").append(avg_results[3])
    	.append(", ").append(avg_results[4]).append('\n');
    	output.write(sb.toString());
    	output.flush();
    	output.close();
    }
}
