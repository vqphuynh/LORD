/*
 * @author Van Quoc Phuong Huynh, FAW JKU
 *
 */

package arg;

import evaluations.HeuristicMetricFactory.METRIC_TYPES;

public class WLordArgHelper implements ArgHelperIF {

	@Override
	public void print_help() {
		System.out.println("Parameters:");
		
		System.out.println(String.format("\t%s (%s): number of threads to run, default value is the number of physical cores",
				Arguments.__TC, Arguments._TC));
		
		System.out.println(String.format("\t%s (%s): metric type, default value is %s",
				Arguments.__MT, Arguments._MT, METRIC_TYPES.MESTIMATE.name()));
		
		System.out.println(String.format("\t%s (%s): metric argument",
				Arguments.__MA, Arguments._MA));
		
		System.out.println(String.format("\t%s (%s): whether doing discretization for numeric attributes, default value is true",
				Arguments.__DA, Arguments._DA));
	    
	    System.out.println(String.format("Example: %s mestimate %s 0.0",
	    		Arguments._MT, Arguments._MA));
	    System.out.println();
	}

	@Override
	public void print_arguments(Arguments args) {
		StringBuilder sb = new StringBuilder(200);
		sb.append("Parameters:\n")
		.append('\t').append(Arguments.__TC).append(' ').append(args.thread_count).append('\n')
		.append('\t').append(Arguments.__MT).append(' ').append(args.metric_type).append('\n')
		.append('\t').append(Arguments.__MA).append(' ').append(args.metric_arg).append('\n')
		.append('\t').append(Arguments.__DA).append(' ').append(args.discretize_attr).append('\n');
		
		System.out.println(sb.toString());
	}

	@Override
	public boolean is_valid(Arguments args) {
		if(args.metric_type == null) return false;
		if(Double.isNaN(args.metric_arg)) return false;
		return true;
	}
}
