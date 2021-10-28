/*
 * @author Van Quoc Phuong Huynh, FAW JKU
 *
 */

package arg;

import prepr.DataReader.DATA_FORMATS;
import evaluations.HeuristicMetricFactory;
import evaluations.HeuristicMetricFactory.METRIC_TYPES;

public class OldArguments {
	public static int thread_count = Math.max(1, Runtime.getRuntime().availableProcessors()/2);
	public static String input_filename = "";
	public static DATA_FORMATS data_format = DATA_FORMATS.ARFF;
	public static int target_attribute_count = 1;
	public static double support_threshold = -1;
	public static double interest_threshold = -1;
	public static METRIC_TYPES metric_type = HeuristicMetricFactory.METRIC_TYPES.PRECISION;
	public static String output_filename = "";
  
	public static boolean parse(String args[]) {
		if (args.length == 0){
			printHelp();
			return false;
		}
		
		try{
			for (int i = 0; i < args.length; i += 2) {
				if (args[i].equals("-tc") || args[i].equals("--thread_count")){
					thread_count = Integer.parseInt(args[i + 1]);
				} else if (args[i].equals("-if") || args[i].equals("--input_filename")){
					input_filename = args[i + 1];
				} else if (args[i].equals("-df") || args[i].equals("--data_format")) {
					try{
						data_format = DATA_FORMATS.valueOf(args[i + 1].toUpperCase());
					}catch (IllegalArgumentException e){
						System.out.println(String.format("Invalid data format, using default format %s", data_format.name()));
					}
				} else if (args[i].equals("-tac") || args[i].equals("--target_attribute_count")) {
					target_attribute_count = Integer.parseInt(args[i + 1]);
				} else if (args[i].equals("-st") || args[i].equals("--support_threshold")) {
					support_threshold = Double.parseDouble(args[i + 1]);
				} else if (args[i].equals("-it") || args[i].equals("--interest_threshold")) {
					interest_threshold = Double.parseDouble(args[i + 1]);
				} else if (args[i].equals("-mt") || args[i].equals("--metric_type")) {
					try{
						metric_type = HeuristicMetricFactory.METRIC_TYPES.valueOf(args[i + 1].toUpperCase());
					}catch (IllegalArgumentException e){
						System.out.println(String.format("Invalid metric type, using default metric %s", metric_type.name()));
					}
				} if (args[i].equals("-of") || args[i].equals("--output_filename")){
					output_filename = args[i + 1];
				} else if (args[i].equals("-h") || args[i].equals("--help")) {
					printHelp();
					return false;
				} 
			}
		}catch(NumberFormatException e){}
		
		return checkParameters();
	}
  
	private static boolean checkParameters() {
		boolean check_result = true;
		
		if (thread_count < 1){
			System.out.println("Wrong value for the number of threads, use -tc option");
			check_result = false;
		}
		if (input_filename.equals("")){
			System.out.println("Miss the data source file name, use -if option");
			check_result = false;
		}
		if (target_attribute_count < 1){
			System.out.println("Wrong value for the number of target attributes, use -tac option");
			check_result = false;
		}
		if (support_threshold >= 1 || support_threshold <= 0){
			System.out.println("Wrong value for the support threshold, use -st option");
			check_result = false;
		}
		if (interest_threshold >= 1 || interest_threshold <= 0){
			System.out.println("Wrong value for the interest threshold, use -it option");
			check_result = false;
		}
		if (output_filename.equals("")){
			System.out.println("Miss the output file name for discovered rules, use -of option");
			check_result = false;
		}
		// Metric type and data format always has a valid value
		return check_result;
	}
  
	public static String getString() {
		StringBuilder sb = new StringBuilder(200);
		sb.append("Parameters:\n")
		.append("\t--thread_count ").append(thread_count).append('\n')
		.append("\t--input_filename ").append(input_filename).append('\n')
		.append("\t--data_format ").append(data_format.name()).append('\n')
		.append("\t--target_attribute_count ").append(target_attribute_count).append('\n')
		.append("\t--support_threshold ").append(support_threshold).append('\n')
		.append("\t--interest_threshold ").append(interest_threshold).append('\n')
		.append("\t--metric_type ").append(metric_type.name()).append('\n')
		.append("\t--output_filename ").append(output_filename).append('\n');
		
		return sb.toString();
	}
  
	public static void printHelp() {
	    System.out.println("Parameters:");
	    
	    System.out.println("\t--thread_count (-tc) number of threads to run (default value is the number of physical cores)");
	    
	    System.out.println("\t--input_filename (-if) data source file name (required)");
	    
	    System.out.println(String.format("\t--data_format (-df) data format (default format = %s)", data_format.name()));
	    
	    System.out.println("\t--target_attribute_count (-tac) the number of target attributes (default value = 1)");
	    System.out.println("\t\tTarget attributes sit at right most side of the dataset");
	    
	    System.out.println("\t--support_threshold (-st) mininum support threshold (required)");
	    System.out.println("\t\tThe value ranges in (0, 1)");
	    
	    System.out.println("\t--interest_threshold (-it) mininum interest threshold (required)");
	    System.out.println("\t\tThe value ranges in (0, 1)");
	    
	    System.out.println(String.format("\t--metric_type (-mt) type of metric to evaluate discovered rules (default value = %s)", metric_type.name()));
	    System.out.print("\t\tSupported metric types: ");
	    for(METRIC_TYPES metric : HeuristicMetricFactory.METRIC_TYPES.values()){
	    	System.out.print(metric.name() + " ");
	    }
	    System.out.println();
	    
	    System.out.println("\t--output_filename (-of) output file name for discovered rules (required)");
	    
	    System.out.println("\t--help (-h) print help");
	    
	    System.out.println("Example: -if data_filename -tac 1 -st 0.05 -it 0.85 -mt Precision -of rule_filename");
	    System.out.println();
	}
}