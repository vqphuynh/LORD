/*
 * @author Van Quoc Phuong Huynh, FAW JKU
 *
 */

package run;

public class Helper {

	public static void main(String[] args) {		
		System.out.println("To run TrainTestSplitter, use the following command for help");
		System.out.println("\tjava -cp ./lord.jar run.TrainTestSplitter");
		System.out.println();
		
		System.out.println("To discretize data, use the following command for help");
		System.out.println("\tjava -cp ./lord.jar run.DiscretizationRun");
		System.out.println();
		
		System.out.println("To run LORD, use the following command for help");
		System.out.println("\tjava -cp ./lord.jar run.LordRun");
		System.out.println();
		
		System.out.println("To run WLORD (LORD adaptation to interface with WEKA), use the following command for help");
		System.out.println("\tjava -cp ./lord.jar run.WLordRun");
	}

}
