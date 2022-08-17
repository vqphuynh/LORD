/*
 * @author Van Quoc Phuong Huynh, FAW JKU
 *
 */

package arg;

import evaluations.HeuristicMetricFactory;
import evaluations.HeuristicMetricFactory.METRIC_TYPES;

public class Arguments {
	public int thread_count = Math.max(2, Runtime.getRuntime().availableProcessors()/2);
	public String input_filename = null;
	public String output_filename = null;
	public String input_directory = null;
	public String output_directory = null;
	public int target_attribute_count = 1;
	public double support_threshold = Double.NaN;
	public double heuristic_threshold = Double.NaN;
	public METRIC_TYPES metric_type = null;
	public double metric_arg = Double.NaN;
	public boolean discretize_attr = true;
	
	public static final String __TC = "--thread_count";
	public static final String _TC = "-tc";
	public static final String __IF = "--input_filename";
	public static final String _IF = "-if";
	public static final String __OF = "--output_filename";
	public static final String _OF = "-of";
	public static final String __ID = "--input_directory";
	public static final String _ID = "-id";
	public static final String __OD = "--output_directory";
	public static final String _OD = "-od";
	public static final String __TAC = "--target_attribute_count";
	public static final String _TAC = "-tac";
	public static final String __ST = "--support_threshold";
	public static final String _ST = "-st";
	public static final String __HT = "--heuristic_threshold";
	public static final String _HT = "-ht";
	public static final String __MT = "--metric_type";
	public static final String _MT = "-mt";
	public static final String __MA = "--metric_argument";
	public static final String _MA = "-ma";
	public static final String __DA = "--discretize_attribute";
	public static final String _DA = "-da";
	public static final String __H = "--help";
	public static final String _H = "-h";
	
  
	public void parse(String args[], ArgHelperIF helper) {
		if (args.length == 0){
			return;
		}
		
		for (int i = 0; i < args.length; i += 2) {
			if (args[i].equals(_TC) || args[i].equals(__TC)){
				try{
					thread_count = Integer.parseInt(args[i + 1]);
				}catch(NumberFormatException e){
					System.out.println(String.format("Invalid thread count, using default value: %d", thread_count));
				}
			} else if (args[i].equals(_IF) || args[i].equals(__IF)){
				input_filename = args[i + 1];
			} else if (args[i].equals(_OF) || args[i].equals(__OF)){
				output_filename = args[i + 1];
			} else if (args[i].equals(_ID) || args[i].equals(__ID)){
				input_directory = args[i + 1];
			} else if (args[i].equals(_OD) || args[i].equals(__OD)){
				output_directory = args[i + 1];
			} else if (args[i].equals(_TAC) || args[i].equals(__TAC)) {
				try{
					target_attribute_count = Integer.parseInt(args[i + 1]);
				}catch(NumberFormatException e){
					System.out.println(String.format("Invalid target attribute count, using default value: %d", target_attribute_count));
				}
			} else if (args[i].equals(_ST) || args[i].equals(__ST)) {
				try{
					support_threshold = Double.parseDouble(args[i + 1]);
				}catch(NumberFormatException e){
					System.out.println(String.format("Invalid support threshold."));
				}
			} else if (args[i].equals(_HT) || args[i].equals(__HT)) {
				try{
					heuristic_threshold = Double.parseDouble(args[i + 1]);
				}catch(NumberFormatException e){
					System.out.println("Invalid heuristic threshold.");
				}
			} else if (args[i].equals(_MT) || args[i].equals(__MT)) {
				try{
					metric_type = HeuristicMetricFactory.METRIC_TYPES.valueOf(args[i + 1].toUpperCase());
				}catch (IllegalArgumentException e){
					System.out.println("Invalid metric type.");
				}
			} else if (args[i].equals(_MA) || args[i].equals(__MA)) {
				try{
					metric_arg = Double.parseDouble(args[i + 1]);
				}catch(NumberFormatException e){
					System.out.println("Invalid metric argument.");
				}
			} else if (args[i].equals(_DA) || args[i].equals(__DA)) {
				discretize_attr = Boolean.parseBoolean(args[i + 1]);
			} else if (args[i].equals(_H) || args[i].equals(__H)) {
				helper.print_help();
			} 
		}
	}
}