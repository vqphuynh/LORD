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
import weka.filters.unsupervised.attribute.NumericToNominal;

/**
 * Cross-validation benchmark for JRIP (RIPPER) algorithm with discretized data sets
 * </br>Train set and test set stored in one file, if they are in two separate files, JRIP's accuracy will be much lower
 *
 */
public class JRIP_DiscretizedData {	
	private static String data_dir_path = "data/inputs/german_labeled";
	private static double train_portion = 0.9;	// train set and test set stored in one file, 
	private static int optimize_runs = 2;
	
	public static void main(String[] args) throws Exception{
		if(args.length < 3){
			System.out.println("Parameters: <data directory> <train portion> <optimize_run_count>");
			//run_cross_validation(data_dir_path, train_portion, optimize_runs);	// uncomment for debugging run
			return;
		}
		System.out.println("Running JRIP_DiscretizedData ...");
		data_dir_path = args[0];
		train_portion = Double.parseDouble(args[1]);
		optimize_runs = Integer.parseInt(args[2]);
		
		run_cross_validation(data_dir_path, train_portion, optimize_runs);
	}
		
	public static void run_cross_validation(String data_dir_path,
											double train_portion,
											int optimize_runs) throws Exception{
		File data_dir = new File(data_dir_path);
	    File[] files = data_dir.listFiles(new FilenameFilter() {
												        @Override
												        public boolean accept(File dir, String name) {
												            return name.toLowerCase().contains("fold");
												        }
												    });
	    Arrays.sort(files);
	    
	    String output_filename = "data/outputs/jrip_" + data_dir.getName() + "_output_direct_opt" + optimize_runs + ".txt";
		BufferedWriter output = new BufferedWriter(new FileWriter(output_filename));
		
		output.write(String.format("Cross-validation on data files in directory: %s\n", data_dir_path));
		output.write(String.format("\tPortion of train examples: %f\n", train_portion));
		output.write(String.format("\tNumber of optimization runs of RIPPER: %d\n", optimize_runs));
		
		double rule_count_sum = 0;
		double avg_rule_length_sum = 0;
		long running_time_sum = 0;
		double hit_count_sum = 0;
		double miss_count_sum = 0;
		double accuracy_sum = 0;
		
		int folds_count = files.length;
		for(int i=0; i<folds_count; i++){
			long start = System.currentTimeMillis();
			
			output.write(String.format("Fold: %d\n", i+1));
			
			// load training data
	 		Instances data = new DataSource(files[i].getAbsolutePath()).getDataSet();
	 		int class_index = data.numAttributes() - 1;
	 		if (data.classIndex() == -1)
			    data.setClassIndex(class_index);
			Attribute classAttribute = data.attribute(class_index);
			
			// if class attribute is recognized by weka as a numeric attribute, it will raise an exception
			if(classAttribute.isNumeric()){
				System.out.print("convert class attribute from numeric to nominal");
				Instances converted_data;
				NumericToNominal convert = new NumericToNominal();
		        convert.setAttributeIndicesArray(new int[]{class_index});
		        convert.setInputFormat(data);
		        converted_data = Filter.useFilter(data, convert);
		        data = converted_data;
		        data.setClassIndex(class_index);
		        classAttribute = data.attribute(class_index);
			}
			
	 		int train_count = (int) Math.round(data.size()*train_portion);
	 		int test_count = data.size()-train_count;
	 		
	 		// split train and test sets.
	 		// it is weird that if train and test sets are stored in two separate files, the accuracy is lower.
	 		// so train and test sets are in the same file then split them on the fly
			Instances train_instances = new Instances(data, 0, train_count);
			Instances test_instances = new Instances(data, train_count, test_count);
			
			output.write(String.format("Number of training instances: %d\n", train_instances.size()));
			output.write(String.format("Number of testing instances: %d\n", test_instances.size()));
			
			// build classifier
		    JRip classifier = new JRip();	// default settings
		    String[] options = {"-F", "3", "-N", "2.0", "-O", ""+optimize_runs, "-S", "1"};
		    // "-F" number of folds
		    // "-N" Set the minimal weights of instances within a split
		    // "-O" Set the number of runs of optimizations
		    // "-S" Seed
		    classifier.setOptions(options);
		    classifier.buildClassifier(train_instances);
			
			// testing
	 		double hit_count = 0;
	 		double miss_count = 0;
		    for (Instance instance : test_instances) {
		        double predicted_class = classifier.classifyInstance(instance);
		        if(predicted_class == instance.classValue()) hit_count++;
		        else{
		        	miss_count++;
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
		    output.write(String.format("Fold: %d\n", (i+1)));
		    output.write(String.format("Rule count: %d\n", rule_count));
		    output.write(String.format("Average rule length: %f\n", avg_rule_length));
		    output.write(String.format("Hit count: %.0f\n", hit_count));
		    output.write(String.format("Miss count: %.0f\n", miss_count));
		    output.write(String.format("Accuracy: %f\n", accuracy));
		    output.write(String.format("Running time: %d ms\n", running_time));
		    output.write("====================================================================================\n");
		    output.flush();
		    
		    //////////////////////Export Extensive Information///////////////////////
			String extensive_info_filename = "data/outputs/jrip_" + data_dir.getName() + "_extensive_info_opt" + optimize_runs + ".csv";
		    export_extensive_info(classifier, train_instances, test_instances, extensive_info_filename);
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
	
	private static void export_extensive_info(JRip classifier,
												Instances train_instances,
												Instances test_instances,
												String extensive_info_filename) throws Exception{		
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
