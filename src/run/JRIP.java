/*
 * @author Van Quoc Phuong Huynh, FAW JKU
 *
 */

package run;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import rl.Supporter;
import weka.classifiers.rules.JRip;
import weka.classifiers.rules.JRip.RipperRule;
import weka.classifiers.rules.Rule;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.Filter;
import weka.filters.supervised.instance.StratifiedRemoveFolds;

/**
 * Cross-validation benchmark for JRIP (RIPPER) algorithm
 *
 */
public class JRIP {
	private static String dataset = "german";	
	private static String data_filename = "data/inputs/datasets/"+dataset+".csv";
	private static int fold_count = 10;
	private static int seed = 1;
	private static int optimize_runs = 2;
	private static String output_filename_direct = "data/outputs/jrip_"+dataset+"_output_direct_opt" + optimize_runs + ".txt";
	
	public static void main(String[] args) throws Exception{
		// System.out.println("------------------------------Way 1------------------------------");
		// It's weird that way 1 (train and test instances in two separate files) gives lower accuracy than that of way 2
		// run_cross_validation(cross_validation_data_dir, output_filename);
		
		// System.out.println("------------------------------Way 2------------------------------");
		// Way 2 (train and test instances in the same file) gives higher accuracy than that of way 1
		if(args.length < 4){
			System.out.println("Parameters: <data filename> <number of folds> <seed> <optimize_run_count>");
			//run_cross_validation_direct(data_filename, fold_count, seed, optimize_runs, output_filename_direct); // uncomment for debugging run
			return;
		}
		System.out.println("Running JRIP ...");
		data_filename = args[0];
		fold_count = Integer.parseInt(args[1]);
		seed = Integer.parseInt(args[2]);
		optimize_runs = Integer.parseInt(args[3]);
		File file = new File(data_filename);
		String filename = file.getName().substring(0, file.getName().lastIndexOf('.'));
		output_filename_direct = "data/outputs/jrip_" + filename + "_output_direct_opt" + optimize_runs + ".txt";
		run_cross_validation_direct(data_filename, fold_count, seed, optimize_runs, output_filename_direct);
	}
	
	/**
	 * Run cross validation for JRip, an implementation of RIPPER in WEKA library.
	 * @param data_dir_path path to a directory which contains all pairs of train and test data sets.
	 * @param output_file text file to output results
	 * @throws Exception 
	 * @throws  
	 */
	public static void run_cross_validation(String data_dir_path, String output_file) throws Exception{
	    File data_dir = new File(data_dir_path);
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
	    
	    double[][] results = new double[train_files.length][];
	    
	    for(int i=0; i<train_files.length; i++){
	    	System.out.println("Training filename: " + train_files[i].getAbsolutePath());
	    	System.out.println("Testing filename: " + test_files[i].getAbsolutePath());
	    	results[i] = run(train_files[i].getAbsolutePath(), test_files[i].getAbsolutePath());
	    	System.out.println("------------------------------");
	    }
		
		BufferedWriter output = new BufferedWriter(new FileWriter(output_file));
		
		double[] sums = new double[5];
		for(int i=0; i<results.length; i++){
			double[] sub_results = results[i];
			for(int j=0; j<sub_results.length; j++) sums[j] += sub_results[j];
			output.write(String.format("Fold %d:\n", (i+1)));
			output.write(String.format("\tRunning time: %.0f ms\n", sub_results[0]));
			output.write(String.format("\tRule count: %.0f\n", sub_results[1]));
			output.write(String.format("\tHit count: %.0f\n", sub_results[2]));
			output.write(String.format("\tMiss count: %.0f\n", sub_results[3]));
			output.write(String.format("\tAccuracy: %f\n", sub_results[4]));
			output.newLine();
		}
		output.write("--------------------------------------------------------\n");
		output.write(String.format("Average running time: %.0f ms\n", sums[0]/results.length));
		output.write(String.format("Average rule count: %.0f\n", sums[1]/results.length));
		output.write(String.format("Micro average accuracy: %f\n", sums[2]/(sums[2]+sums[3])));
		output.write(String.format("Macro average accuracy: %f\n", sums[4]/results.length));
		output.flush();
		output.close();
	}
	
	/**
	 * 
	 * @param train_filename
	 * @param test_filename
	 * @return results[0]=running time, results[1]=rule count, results[2]=accuracy performance
	 * @throws IOException
	 * @throws Exception
	 */
	public static double[] run(String train_filename, String test_filename) throws IOException, Exception {
	    long start = System.currentTimeMillis();
	    double[] results = new double[5];
	    
	    // load training data
 		Instances train_instances = new DataSource(train_filename).getDataSet();
	    train_instances.setClassIndex(train_instances.numAttributes() - 1);

	    // build classifier
	    JRip classifier = new JRip();	// default settings
	     String[] options = {"-F", "3", "-N", "2.0", "-O", "2", "-S", "1"};
	    // "-F" number of folds
	    // "-N" Set the minimal weights of instances within a split
	    // "-O" Set the number of runs of optimizations
	    // "-S" Seed
	    classifier.setOptions(options);
	    classifier.buildClassifier(train_instances);
	    List<Rule> rule_set = classifier.getRuleset();
	    
	    // load testing data
 		Instances test_instances = new DataSource(test_filename).getDataSet();
 		test_instances.setClassIndex(test_instances.numAttributes() - 1);
	    
 		// testing
 		double hit_count = 0;
 		double miss_count = 0;
	    for (Instance instance : test_instances) {
	        double predicted_class = classifier.classifyInstance(instance);
	        if(predicted_class == instance.classValue()) hit_count++;
	        else{
	        	miss_count++;
//	        	System.out.println(String.format("Instances: %s", instance.toString()));
//	        	System.out.println(String.format("Predicted class: %f, class: %f", predicted_class, instance.classValue()));
//	        	System.out.println();
	        }
	    }
	    
	    results[0] = System.currentTimeMillis() - start;
	    results[1] = rule_set.size();
	    results[2] = hit_count;
	    results[3] = miss_count;
	    results[4] = hit_count/test_instances.size();
	    System.out.println(String.format("Hit count: %f, Miss count: %f, Accuracy: %f", hit_count, miss_count, results[4]));
	    return results;
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
													int optimize_runs,
													String output_filename) throws Exception{
		BufferedWriter output = new BufferedWriter(new FileWriter(output_filename));
		
		output.write(String.format("Cross-validation on data file: %s\n", data_filename));
		output.write(String.format("\tFold count: %d\n", folds_count));
		output.write(String.format("\tSeed: %d\n", seed));
		output.write(String.format("\tNumber of optimization runs of RIPPER: %d\n", optimize_runs));
		
		// load data  
		DataSource source = new DataSource(data_filename);
		Instances data = source.getDataSet();
		output.write(String.format("Total number of instances: %d\n\n", data.size()));

		// set class to last attribute
		if (data.classIndex() == -1)
		    data.setClassIndex(data.numAttributes() - 1);
		
		Attribute classAttribute = data.attribute(data.numAttributes() - 1);

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
		    JRip classifier = new JRip();	// default settings
		    String[] options = {"-F", "3", "-N", "2.0", "-O", ""+optimize_runs, "-S", "1"};
		    // "-F" number of folds
		    // "-N" Set the minimal weights of instances within a split
		    // "-O" Set the number of runs of optimizations
		    // "-S" Seed
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
		    for(Rule rule : classifier.getRuleset()){
		    	avg_rule_length += rule.size();
		    	output.write(((RipperRule) rule).toString(classAttribute));
		    	output.write('\n');
		    }
		    
		    int rule_count = classifier.getRuleset().size();
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
		    
		    //////////////////////Export Extensive Information///////////////////////
		    export_extensive_info(classifier, train_instances, test_instances);
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
	
	private static void export_extensive_info(JRip classifier, Instances train_instances, Instances test_instances) throws Exception{
		File file = new File(data_filename);
		String filename = file.getName().substring(0, file.getName().lastIndexOf('.'));
		String extensive_info_filename = "data/outputs/jrip_" + filename + "_extensive_info_opt" + optimize_runs + ".csv";
		
		// open file in append mode
		FileWriter fw = new FileWriter(extensive_info_filename, true);
		BufferedWriter writer = new BufferedWriter(fw);
		
		int rule_count = classifier.getRuleset().size()-1;	// Ignore the default rule (empty body)
		double[] lengths = new double[rule_count];
		
		List<Rule> rules = classifier.getRuleset();
		for(int i=0; i<rule_count; i++){
			lengths[i] = rules.get(i).size();
		}
		
		double[] tps = new double[rule_count];
		double[] n_plus_ps = new double[rule_count];
		
		/////////////////////This from WEKA does not get global coverage and tp of rules/////////////////////
//		RuleStats rulestats = classifier.getRuleStats(0);
//		double[] extensive_info;
//		for(int i=0; i<rule_count; i++){
//			// 0: coverage; 1:uncoverage; 2: true positive; 3: true negatives; 4: false positives; 5: false negatives
//			extensive_info = rulestats.getSimpleStats(i);
//			if(extensive_info == null) continue;	// The default rule (empty body) has no info
//			n_plus_ps[i] = extensive_info[0];
//			tps[i] = extensive_info[2];
//		}
		/////////////////////////////////////////////////////////////////////////////////////////////////////
		
		// This gets global coverage and tp of rules
		for(int i=0; i<rule_count; i++){
			RipperRule rule = (RipperRule) rules.get(i);
			double class_value = rule.getConsequent();
			for(Instance inst : train_instances){
				if(rule.covers(inst)){
					n_plus_ps[i] += 1;
					if(class_value == inst.classValue()) tps[i] += 1;
				}
			}
		}
		
		writer.write(",max,min,mean,stdev\n");
		write_info(writer, "length", Supporter.get_statistic_info(lengths));
		write_info(writer, "coverage", Supporter.get_statistic_info(n_plus_ps));
		write_info(writer, "tp", Supporter.get_statistic_info(tps));
		
		double[] covering_rule_counts = get_covering_rule_counts(classifier, train_instances);
		write_info(writer, "covering_rule# (train set)", Supporter.get_statistic_info(covering_rule_counts));
		covering_rule_counts = get_covering_rule_counts(classifier, test_instances);
		write_info(writer, "covering_rule# (test set)", Supporter.get_statistic_info(covering_rule_counts));
		
		writer.flush();
		writer.close();
	}
	
	private static void write_info(BufferedWriter writer, String info_name, double[] statistic_info) throws IOException{
		StringBuilder sb = new StringBuilder(512);
		
		sb.append(info_name).append(',')
		.append(statistic_info[0]).append(',')
		.append(statistic_info[1]).append(',')
		.append(statistic_info[2]).append(',')
		.append(statistic_info[3]).append('\n');
		
		writer.write(sb.toString());
	}
	
	private static double[] get_covering_rule_counts(JRip classifier, Instances instances){
		double[] covering_rule_counts = new double[instances.size()];
		List<Rule> rules = classifier.getRuleset();
		rules.remove(rules.size()-1);	// remove the default rule which has an empty body
		
		int index = 0;
		for(Instance inst : instances){
			int count = 0;
			for(Rule rule : rules){
				if(rule.covers(inst)) count++;
			}
			covering_rule_counts[index] = count;
			index++;
		}
		return covering_rule_counts;
	}
}
