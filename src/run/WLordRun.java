/*
 * @author Van Quoc Phuong Huynh, FAW JKU
 *
 */

package run;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import rl.RuleInfo;
import rl.eg.WLord;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.Filter;
import weka.filters.supervised.instance.StratifiedRemoveFolds;
import evaluations.HeuristicMetricFactory;

/**
 * Cross-validation benchmark for WLord which an implementation of LORD algorithm to interface with WEKA
 *
 */
public class WLordRun {
	private static String data_filename = "data/inputs/datasets/german.arff";
	private static int fold_count = 10;
	private static int seed = 1;
	
	private static String metric_type = HeuristicMetricFactory.METRIC_TYPES.MESTIMATE.name();
	private static String metric_arg = "0.1";
	private static String discretize_attr = "true";		// set to 'false' for 'lymphography' data set
	
	private static String output_filename_direct;
	
	public static void main(String[] args) throws Exception {
		if(args.length < 5){
			System.out.println("Parameters: <data filename> <number of folds> <seed> <metric_type> <metric_arg> <discretize_attr>");
			System.out.println();
			System.out.println("<metric_type> values: mestimate, entropy, relative_cost, ...");
			System.out.println();
			System.out.println("<discretize_attr> values: true/false, default value: true, whether numeric attributes are discretized.");
			System.out.println("Some data sets contain indices of categorical values, e.g. lymphography data set; <discretize_attr> should be set to false.");
			
			File file = new File(data_filename);
			String filename = file.getName().substring(0, file.getName().lastIndexOf('.'));
			output_filename_direct = "data/outputs/wlord_" + filename + "_" + metric_type + "_" + metric_arg + ".txt";
			//run_cross_validation_direct(data_filename, fold_count, seed, output_filename_direct); // uncomment for debugging run 
			
			return;
		}
		
		System.out.println("Running WLORD ...");
		data_filename = args[0];
		fold_count = Integer.parseInt(args[1]);
		seed = Integer.parseInt(args[2]);		
		metric_type = args[3];
		metric_arg = args[4];
		if(args.length >= 6) discretize_attr = args[5];
		
		File file = new File(data_filename);
		String filename = file.getName().substring(0, file.getName().lastIndexOf('.'));
		output_filename_direct = "data/outputs/wlord_" + filename + "_" + metric_type + "_" + metric_arg + ".txt";
		run_cross_validation_direct(data_filename, fold_count, seed, output_filename_direct);
	}
	
	/**
	 * 
	 * @param data_filename
	 * @param folds_count
	 * @param seed
	 * @param output_filename
	 * @throws Exception
	 */
	public static void run_cross_validation_direct(String data_filename,
													int folds_count,
													int seed,
													String output_filename) throws Exception{
		BufferedWriter output = new BufferedWriter(new FileWriter(output_filename));
		
		output.write(String.format("Cross-validation on data file: %s\n", data_filename));
		output.write(String.format("\tFold count: %d\n", folds_count));
		output.write(String.format("\tSeed: %d\n", seed));
		
		// load data  
		DataSource source = new DataSource(data_filename);
		Instances data = source.getDataSet();
		output.write(String.format("Total number of instances: %d\n\n", data.size()));

		// set class to last attribute
		if (data.classIndex() == -1)
		    data.setClassIndex(data.numAttributes() - 1);

		// use StratifiedRemoveFolds to randomly split the data  
		StratifiedRemoveFolds filter = new StratifiedRemoveFolds();
		filter.setNumFolds(folds_count);
		filter.setSeed(seed);
		
		double rule_count_sum = 0;
		double avg_rule_length_sum = 0;
		long running_time_sum = 0;
		double hit_count_sum = 0;
		double miss_count_sum = 0;
		double accuracy_sum = 0;
		
		for(int i=1; i<=folds_count; i++){
			long start = System.currentTimeMillis();
			
			filter.setFold(i);
			output.write(String.format("Fold: %d\n", i));
			
			// prepare and apply filter for training instances here
			filter.setInvertSelection(true);   // invert the selection to get other data 
			filter.setInputFormat(data);       // prepare the filter for the data format
			Instances train_instances = Filter.useFilter(data, filter);
			output.write(String.format("Number of training instances: %d\n", train_instances.size()));
			
			// build classifier
		    WLord classifier = new WLord();
		    String[] options = new String[] {"-mt", metric_type, "-ma", metric_arg, "-da", discretize_attr};
		    classifier.setOptions(options);
		    classifier.buildClassifier(train_instances);
			
			// apply filter for test instances here
			filter.setInvertSelection(false);  // do not invert the selection
			filter.setInputFormat(data);       // prepare the filter for the data format
			Instances test_instances = Filter.useFilter(data, filter);
			output.write(String.format("Number of testing instances: %d\n", test_instances.size()));
			
			// testing
	 		double hit_count = 0;
	 		double miss_count = 0;
		    for (Instance instance : test_instances) {
		        double predicted_class = classifier.classifyInstance(instance);
		        if(predicted_class == instance.classValue()) hit_count++;
		        else{
		        	miss_count++;
		        	//output.write(String.format("Instances: %s\n", instance.toString()));
		        	//output.write(String.format("Predicted class: %f, class: %f\n", predicted_class, instance.classValue()));
		        }
		    }
		    
		    long running_time = System.currentTimeMillis() - start;
		    
		    double avg_rule_length = 0;
		    output.write("Rule list:\n");
		    if(classifier.rm.ruleList.size() < 1000){
		    	for(RuleInfo rule : classifier.rm.ruleList){
			    	avg_rule_length += rule.body.length;
			    	output.write(rule.content());
			    	output.write('\n');
			    }
		    }else{
		    	output.write("The rule list is large, do not print.\n");
		    	for(RuleInfo rule : classifier.rm.ruleList){
			    	avg_rule_length += rule.body.length;
			    }
		    }
		    
		    int rule_count = classifier.rm.ruleList.size();
		    avg_rule_length = avg_rule_length/rule_count;
		    avg_rule_length_sum += avg_rule_length;
		    rule_count_sum += rule_count;
		    running_time_sum += running_time;
		    hit_count_sum += hit_count;
		    miss_count_sum += miss_count;
		    
		    double accuracy = hit_count/test_instances.size();
		    accuracy_sum += accuracy;
		    
		    output.write("--------------------------------\n");
		    output.write(String.format("Fold: %d\n", i));
		    output.write(String.format("Rule count: %d\n", rule_count));
		    output.write(String.format("Average rule length: %f\n", avg_rule_length));
		    output.write(String.format("Hit count: %.0f\n", hit_count));
		    output.write(String.format("Miss count: %.0f\n", miss_count));
		    output.write(String.format("Accuracy: %f\n", accuracy));
		    output.write(String.format("Running time: %d ms\n", running_time));
		    output.write("====================================================================================\n");
		    output.flush();
		}
		
		output.write("SUMMARY:\n");
		output.write(String.format("Average rule count: %f\n", rule_count_sum/folds_count));
		output.write(String.format("Average rule length: %f\n", avg_rule_length_sum/folds_count));
		output.write(String.format("Average running time: %d ms\n", running_time_sum/folds_count));
		output.write(String.format("Micro average accuracy: %f\n", hit_count_sum/(hit_count_sum+miss_count_sum)));
		output.write(String.format("Macro average accuracy: %f\n", accuracy_sum/folds_count));
		
		output.flush();
		output.close();
	}
}
