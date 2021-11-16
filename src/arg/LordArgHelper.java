/*
 * @author Van Quoc Phuong Huynh, FAW JKU
 *
 */

package arg;

import evaluations.HeuristicMetricFactory.METRIC_TYPES;

public class LordArgHelper implements ArgHelperIF {

	@Override
	public void print_help() {
		System.out.println("Parameters:");
		
		System.out.println(String.format("\t%s (%s): number of threads to run, default value is the number of physical cores",
				Arguments.__TC, Arguments._TC));
	    
		System.out.println(String.format("\t%s (%s): input directory of test and training files for cross-validation",
				Arguments.__ID, Arguments._ID));
	    
		System.out.println(String.format("\t%s (%s): output directory for results, auto-generated if not specified",
				Arguments.__OD, Arguments._OD));
		
		System.out.println(String.format("\t%s (%s): metric type, default value is %s",
				Arguments.__MT, Arguments._MT, METRIC_TYPES.MESTIMATE.name()));
		
		System.out.println(String.format("\t%s (%s): metric argument",
				Arguments.__MA, Arguments._MA));
	    
	    System.out.println(String.format("Example: %s <input_directory> %s mestimate %s 0.0",
	    		Arguments._ID, Arguments._MT, Arguments._MA));
	    System.out.println();
	}

	@Override
	public void print_arguments(Arguments args) {
		StringBuilder sb = new StringBuilder(200);
		sb.append("Parameters:\n")
		.append('\t').append(Arguments.__TC).append(' ').append(args.thread_count).append('\n')
		.append('\t').append(Arguments.__ID).append(' ').append(args.input_directory).append('\n')
		.append('\t').append(Arguments.__OD).append(' ').append(args.output_directory).append('\n')
		.append('\t').append(Arguments.__TAC).append(' ').append(args.target_attribute_count).append('\n')
		.append('\t').append(Arguments.__MT).append(' ').append(args.metric_type).append('\n')
		.append('\t').append(Arguments.__MA).append(' ').append(args.metric_arg).append('\n');
		
		System.out.println(sb.toString());
	}

	@Override
	public boolean is_valid(Arguments args) {
		if(args.input_directory == null) return false;
		if(args.output_directory == null) return false;
		if(args.metric_type == null) return false;
		if(Double.isNaN(args.metric_arg)) return false;
		return true;
	}
}
