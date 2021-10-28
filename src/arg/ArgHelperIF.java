/*
 * @author Van Quoc Phuong Huynh, FAW JKU
 *
 */

package arg;

public interface ArgHelperIF {
	public void print_help();
	public void print_arguments(Arguments args);
	public boolean is_valid(Arguments args);
}
