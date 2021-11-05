/*
 * @author Van Quoc Phuong Huynh, FAW JKU
 *
 */

package run;

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
import rl.eg.MultiThreadLord;
import arg.Arguments;
import arg.RLArgHelper;
import evaluations.HeuristicMetricFactory.METRIC_TYPES;

/**
 * Experiment LORD's accuracy w.r.t. the remaining percentage of best rules
 *
 */
public class LordRun_RuleListTruncation {
	private static List<Double> remaining_portions;
	private static List<Double> hit_counts;
	private static List<Double> miss_counts;
	private static List<Double> rule_counts;
	private static List<Double> avg_rule_lengths;
	private static List<Double> accuracies;
	private static List<Long> prediction_times;
	
	private static double truncate_portion_step = 0.05;
	
	public static void main(String[] args) throws Exception{
		Date date = Calendar.getInstance().getTime();  
        DateFormat dateFormat = new SimpleDateFormat("_yyyy-MM-dd_hh-mm-ss");
        String strDate = dateFormat.format(date);
        
        RLArgHelper arg_helper = new RLArgHelper();
        Arguments arguments = new Arguments();
        //arguments.input_directory = "data/inputs/adult_arff";	// uncomment this line for debugging run
        arguments.metric_type = METRIC_TYPES.MESTIMATE;
        arguments.metric_arg = 0.1;
        
        arguments.parse(args, arg_helper);
        
        if(arguments.output_directory == null && arguments.input_directory != null){
        	Path path = Paths.get(arguments.input_directory);
        	String simple_dir_name = path.getName(path.getNameCount()-1).toString() + strDate;
        	arguments.output_directory = Paths.get("data/outputs/eg_rules_truncations", simple_dir_name).toString();
        }
        
        if(!arg_helper.is_valid(arguments)){
        	arg_helper.print_help();
        	return;
        }
        
        arg_helper.print_arguments(arguments);
        System.out.println("Running LORD (Eager & Greedy) with rules truncations ...");

		run_with_truncations(arguments);
		
		System.out.println("Finished.");
	}
	
	/**
	 * @param data_dir_path contain all pairs of train and test data sets
	 * @param output_dir_path the base output directory
	 * @throws Exception
	 */
	public static void run_with_truncations(Arguments arguments) throws Exception{
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
	    
	    int i=0;
	    File output_dir = new File(Paths.get(arguments.output_directory, String.format("fold_%02d", i+1)).toString());
    	output_dir.mkdirs();
    	run(train_files[i].getAbsolutePath(),
    			test_files[i].getAbsolutePath(),
    			output_dir.getAbsolutePath(),
    			arguments);
	    
	    write_results(arguments);
	}
	
	/**
	 * @param train_filename
	 * @param test_filename
	 * @return results[0]=running time, results[1]=rule count, results[2]=accuracy performance
	 * @throws IOException
	 * @throws DataFormatException
	 */
	public static void run(String train_filename,
							String test_filename,
							String output_dir,
							Arguments arguments) throws IOException, DataFormatException{
		PrintStream out = new PrintStream(new FileOutputStream(Paths.get(output_dir, "eg_output.txt").toString()));
		System.setOut(out);
		
		MultiThreadLord alg = new MultiThreadLord(1);
		
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
		alg.classifier.sort_rules();
		
		// Print rule set
		System.out.println("------------------------------------------------------------------------------------");
		System.out.println("Rule count: " + alg.classifier.ruleList.size());
		System.out.println("Rule set: ");
		for(RuleInfo rule :  alg.classifier.ruleList){
			System.out.println(rule.content());
		}
		System.out.println("------------------------------------------------------------------------------------");
		
		int loop_count = (int) (1/truncate_portion_step);
		remaining_portions = new ArrayList<Double>(loop_count);
		hit_counts = new ArrayList<Double>(loop_count);
	    miss_counts = new ArrayList<Double>(loop_count);
	    rule_counts = new ArrayList<Double>(loop_count);
	    avg_rule_lengths = new ArrayList<Double>(loop_count);
	    accuracies = new ArrayList<Double>(loop_count);
	    prediction_times = new ArrayList<Long>(loop_count);
	    
		for(int i=0; i<loop_count; i++){
			testing_with_rules_truncations(alg, test_filename, i*truncate_portion_step);
		}
	}
	
	private static void testing_with_rules_truncations(MultiThreadLord alg,
														String test_filename,
														double truncate_portion) throws DataFormatException, IOException{
		if(truncate_portion != 0){
			alg.classifier.truncate(truncate_portion);
		}
		
		//get rule count and average rule length
		double rule_count, avg_rule_length = 0;
		if(alg.classifier.truncatedRuleList == null) alg.classifier.truncatedRuleList = alg.classifier.ruleList;
		rule_count = alg.classifier.truncatedRuleList.size();
		for(RuleInfo rule :  alg.classifier.truncatedRuleList){
			avg_rule_length += rule.body.length;
		}
		avg_rule_length = avg_rule_length/rule_count;
		
		//test
		DataReader reader = DataReader.getDataReader(test_filename);
		String[] value_record;
		IntHolder predicted_classID = new IntHolder(-1);
		int[] example_selectorIDs;
		double hit_count = 0, miss_count = 0;
		
		long start = System.currentTimeMillis();
		reader.bind_datasource(test_filename);
		while((value_record = reader.next_record()) != null){
			example_selectorIDs = alg.predict(value_record, predicted_classID);
			
			if(predicted_classID.value == example_selectorIDs[example_selectorIDs.length-1])
				hit_count++;
			else{
				miss_count++;
			}
		}
		long prediction_time = System.currentTimeMillis() - start;
		double accuracy = hit_count/(hit_count+miss_count);
		
		remaining_portions.add(1-truncate_portion);
		hit_counts.add(hit_count);
		miss_counts.add(miss_count);
		rule_counts.add(rule_count);
		avg_rule_lengths.add(avg_rule_length);
		accuracies.add(accuracy);
		prediction_times.add(prediction_time);
	}
	
	static void write_results(Arguments arguments) throws IOException{
    	BufferedWriter output = new BufferedWriter(new FileWriter(
    			Paths.get(arguments.output_directory, "results_with_rules_truncations.csv").toString()));
    	
    	StringBuilder sb = new StringBuilder(200);
    	sb.append("remaining_portions, hit_counts, miss_counts, rule_counts, avg_rule_lengths, accuracies, prediction_time(ms)\n");
    	output.write(sb.toString());
    	
    	for(int i=remaining_portions.size()-1; i>-1; i--){
    		sb.setLength(0);
    		sb.append(remaining_portions.get(i))
        	.append(", ").append(hit_counts.get(i))
        	.append(", ").append(miss_counts.get(i))
        	.append(", ").append(rule_counts.get(i))
        	.append(", ").append(avg_rule_lengths.get(i))
        	.append(", ").append(accuracies.get(i))
        	.append(", ").append(prediction_times.get(i))
        	.append('\n');
        	output.write(sb.toString());
    	}
    	output.flush();
    	output.close();
    }
}
