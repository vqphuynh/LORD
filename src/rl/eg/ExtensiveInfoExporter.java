/*
 * @author Van Quoc Phuong Huynh, FAW JKU
 *
 */

package rl.eg;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.zip.DataFormatException;

import prepr.DataReader;
import prepr.DoubleArray;
import rl.IntHolder;
import rl.RuleInfo;
import rl.Supporter;

public class ExtensiveInfoExporter {
	private MultiThreadLord alg;
	private Classifier c;
	private int[][] selectorID_records;
	
	public ExtensiveInfoExporter(MultiThreadLord alg){
		this.alg = alg;
		this.c = alg.classifier;
		this.selectorID_records = alg.getSelectorIDRecords();
	}
	
	public void export_info(String filename) throws IOException{
		// open file in append mode
		FileWriter fw = new FileWriter(filename, true);
		BufferedWriter writer = new BufferedWriter(fw);
		
		int rule_count = this.c.ruleList.size();
		double[] lengths = new double[rule_count];
		double[] tps = new double[rule_count];
		double[] n_plus_ps = new double[rule_count];
		double[] covering_rule_counts = new double[this.selectorID_records.length];
		
		int index = 0;
		for(RuleInfo rule : this.c.ruleList){
			lengths[index] = rule.body.length;
			tps[index] = rule.p;
			n_plus_ps[index] = rule.n_plus_p;
			index++;
		}
		
		index = 0;
		for(int[] selectorID_record : this.selectorID_records){
			covering_rule_counts[index] = this.c.ruleTree.find_covering_rules(selectorID_record).size();
			index++;
		}
		
		writer.write(",max,min,mean,stdev\n");
		write_info(writer, "length", Supporter.get_statistic_info(lengths));
		write_info(writer, "coverage", Supporter.get_statistic_info(n_plus_ps));
		write_info(writer, "tp", Supporter.get_statistic_info(tps));
		write_info(writer, "covering_rule# (train set)", Supporter.get_statistic_info(covering_rule_counts));
		writer.flush();
		writer.close();
	}
	
	public void export_info(String filename, String test_filename) throws IOException, DataFormatException{		
		FileWriter fw = new FileWriter(filename, true);
		BufferedWriter writer = new BufferedWriter(fw);
		
		DataReader reader = DataReader.getDataReader(test_filename);
		reader.bind_datasource(test_filename);
		
		String[] value_record;
		IntHolder predicted_classID = new IntHolder(-1);
		DoubleArray covering_rule_counts = new DoubleArray(this.selectorID_records.length/9 + 9);
		
		while((value_record = reader.next_record()) != null){
			this.alg.predict(value_record, predicted_classID);
			covering_rule_counts.add((double) this.alg.classifier.covering_rules.size());
		}
		
		write_info(writer, "covering_rule# (test set)", Supporter.get_statistic_info(covering_rule_counts.toArray()));
		writer.flush();
		writer.close();
	}
	
	private void write_info(BufferedWriter writer, String info_name, double[] statistic_info) throws IOException{
		StringBuilder sb = new StringBuilder(512);
		
		sb.append(info_name).append(',')
		.append(statistic_info[0]).append(',')
		.append(statistic_info[1]).append(',')
		.append(statistic_info[2]).append(',')
		.append(statistic_info[3]).append('\n');
		
		writer.write(sb.toString());
	}
}
