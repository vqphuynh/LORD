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
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.DataFormatException;

import prepr.Selector;
import rl.RuleInfo;
import rl.eg.Lord;
import arg.Arguments;
import evaluations.HeuristicMetricFactory.METRIC_TYPES;

public class ExtractRuleForGraph {
	private static boolean reversed_order = true;
	
	// Classification result for folds	
	public static void main(String[] args) throws Exception{
        Arguments arguments = new Arguments();
        arguments.input_directory = "data/inputs/artificial_datasets";
        arguments.metric_type = METRIC_TYPES.MESTIMATE;
        arguments.metric_arg = 0.1;
        
        System.out.println("Running ExtractRuleForGraph ...");
        
        final String filter_value = ".csv";
        File data_dir = new File(arguments.input_directory);
	    File[] data_files = data_dir.listFiles(new FilenameFilter() {
												        @Override
												        public boolean accept(File dir, String name) {
												            return name.toLowerCase().contains(filter_value);
												        }
												    });
	    Arrays.sort(data_files);
	    
	    for(File data_file : data_files) run(data_file.getAbsolutePath(), arguments);
        
        //run("data/inputs/flo_datasets/parity.csv", arguments);
		
		System.out.println("Finished.");
	}
	
	public static void run(String data_filename,
							Arguments arguments) throws IOException, DataFormatException{
		File data_file = new File(data_filename);
		File output_dir = data_file.getParentFile();
		String file_name = data_file.getName().split("\\.")[0];
		
		Lord alg = new Lord();
		alg.setThreadCount(arguments.thread_count);
		
		System.out.println(String.format("Execute algorithm %s on dataset:\n %s",
											alg.getClass().getSimpleName(), data_filename));
		
		System.out.println(String.format("Metric type: %s, Argument: %f", 
						arguments.metric_type.name(), arguments.metric_arg));
		
		long[] times = alg.fetch_information(data_filename);
		long init_time = 0;
		for(long time : times) init_time += time;
		
		System.out.println(String.format("preprocess time: %d ms", times[0]));
		System.out.println(String.format("tree building time: %d ms", times[1]));
		System.out.println(String.format("selector-nodelists generation time: %d ms", times[2]));
		System.out.println(String.format("Total init time: %d ms", init_time));
		
		long learning_time = alg.learning(arguments.metric_type, arguments.metric_arg);
		System.out.println(String.format("Learning time: %d ms", learning_time));
		
		// Class based group of rules
		double rule_count, avg_rule_length = 0;
		
		Map<Integer, List<RuleInfo>> rule_groups = new HashMap<Integer, List<RuleInfo>>();
		List<Selector> selectors = alg.getConstructingSelectors();
		change_condition_format(selectors);
		
		for(int i=alg.getPredictSelectorCount(), count=alg.getSelectorCount(); i<count; i++){
			Selector s = selectors.get(i);
			rule_groups.put(s.selectorID, new ArrayList<RuleInfo>());
		}
		
		rule_count = alg.rm.ruleList.size();
		for(RuleInfo rule :  alg.rm.ruleList){
			rule_groups.get(rule.headID).add(rule);
			avg_rule_length += rule.body.length;
		}
		avg_rule_length = avg_rule_length/rule_count;
		
		System.out.println("------------------------------------------------------------------------------------");
		System.out.println(String.format("Rule count: %.0f", rule_count));
		System.out.println(String.format("Average rule length: %f", avg_rule_length));
		System.out.println(String.format("Total runing time: %d ms", init_time+learning_time));
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(Paths.get(output_dir.getAbsolutePath(), file_name+"_rules.txt").toString()));
		if(reversed_order){
			for(List<RuleInfo> group : rule_groups.values()){
				write_dnf_rule_reversed(writer, group, selectors);
			}
		}else{
			for(List<RuleInfo> group : rule_groups.values()){
				write_dnf_rule(writer, group, selectors);
			}
		}
		
		writer.close();
		
		System.out.println("====================================================================================");
	}
	
	private static void change_condition_format(List<Selector> selectors){
		for(Selector s : selectors){
			s.condition = s.condition.replace('=', '_');
			s.condition = s.condition.substring(1, s.condition.length()-1);
		}
	}
	
	private static void write_dnf_rule(BufferedWriter writer, List<RuleInfo> group, List<Selector> selectors) throws IOException{
		if(group == null || group.size() < 1) return;
		
		StringBuilder sb = new StringBuilder(512);
		RuleInfo rule;
		
		rule = group.get(0);
		sb.setLength(0);
		for(int selector_id : rule.body){
			sb.append(selectors.get(selector_id).condition).append(" & ");
		}
		sb.setLength(sb.length()-3);
		writer.write(sb.toString());
		
		String class_value = selectors.get(rule.headID).condition.split("_")[1];
		
		for(int i=1, size=group.size(); i<size; i++){
			rule = group.get(i);
			sb.setLength(0);
			for(int selector_id : rule.body){
				sb.append(selectors.get(selector_id).condition).append(" & ");
			}
			sb.setLength(sb.length()-3);
			writer.write(" | ");
			writer.write(sb.toString());
		}
		writer.write(" => " + class_value);
		writer.newLine();
		writer.flush();
	}
	
	private static void write_dnf_rule_reversed(BufferedWriter writer, List<RuleInfo> group, List<Selector> selectors) throws IOException{
		if(group == null || group.size() < 1) return;
		
		StringBuilder sb = new StringBuilder(512);
		RuleInfo rule;
		
		rule = group.get(0);
		sb.setLength(0);
		for(int index=rule.body.length-1; index>-1; index--){
			sb.append(selectors.get(rule.body[index]).condition).append(" & ");
		}
		
		sb.setLength(sb.length()-3);
		writer.write(sb.toString());
		
		String class_value = selectors.get(rule.headID).condition.split("_")[1];
		
		for(int i=1, size=group.size(); i<size; i++){
			rule = group.get(i);
			sb.setLength(0);
			for(int index=rule.body.length-1; index>-1; index--){
				sb.append(selectors.get(rule.body[index]).condition).append(" & ");
			}
			sb.setLength(sb.length()-3);
			writer.write(" | ");
			writer.write(sb.toString());
		}
		writer.write(" => " + class_value);
		writer.newLine();
		writer.flush();
	}
}
