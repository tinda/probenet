/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */

package applications;

import java.util.Random;

import core.Application;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;
import core.SimScenario;
import core.World;
import java.util.LinkedList;
import report.PARANETS_AppReport;
import routing.ParanetAdaptableFuzzySprayAndWaitRouter;

/**
 * Paranets Application
 * 
 * The corresponding <code>PARANETS_AppReport</code> class can be used to record
 * information about the application behavior.
 * 
 * @see PARANETS_AppReport
 * @author Jad Makhlouta
 */
public class PARANETS_application extends Application 
{
	/** MA or SA */
	public static final String NODE_ROLE = "role";
	/** REQUEST generation interval */
	public static final String REQUEST_INTERVAL = "interval";
	/** SA address  */
	public static final String SA_ADDRESS = "SA_Address";
	/** Seed for the app's random number generator */
	public static final String SEED = "seed";
	/** Size of the request message */
	public static final String REQUEST_SIZE = "requestSize";
	/** Size of the data messages */
	public static final String MESSAGES_RANGE = "MessageSizeRanges";

	public static final String COST_PER_SIZE = "CostsPerTechnology";
	
	public static final String ALLOWABLE_COST_PER_SIZE = "MaxCost";
	
	public static final String METHOD = "lagrange";
	
	public static final String NUM_MINIBUNDLES="num_minibundles";

	public static final String NOMINAL_TH="NominalThroughput";

	public static final String TYPE_PROPERTY = "PARANET_type";
	public static final String TYPE_REQUEST = "request";
	public static final String TYPE_DATA = "data";
	public static final String WLAN_MSG_SIZE = "wlan_msg_size";
	
	/** Application ID */
	public static final String APP_ID = "jh.PARANETS_Application";

	public class message_divisions
	{
		public double wlan,cellular, satellite;
	}

	public message_divisions solve_optimization_problem(double data_size)
	{
		message_divisions d=new message_divisions();
		if (!lagrange)	//i.e. LARA's method
		{
			double max_throuput=0, current_cost=max_cost, new_cost,new_throuput;
			for (int i=0;i<=num_minibundles;i++)
				for (int j=0;j<=num_minibundles-i;j++)
				{
					if (max_throuput<=(new_throuput=i*conditions[0].getTH_estimate()+j*conditions[1].getTH_estimate()+(num_minibundles-i-j)*conditions[2].getTH_estimate())
							&& max_cost>=(new_cost=(i*wlan_cost+j*cellular_cost+(num_minibundles-i-j)*satellite_cost)/num_minibundles*data_size))
					{
						if (new_cost>current_cost && max_throuput==new_throuput)
							continue;
						else
						{
							if ( new_cost<=max_cost)
							{
								current_cost=new_cost;
								max_throuput=new_throuput;
								d.wlan=(double)i/num_minibundles*data_size;
								d.satellite=(double)j/num_minibundles*data_size;
								d.cellular=(double)(num_minibundles-i-j)/num_minibundles*data_size;
							}
						}
					}
				}
			//System.out.println("divison: "+d.wlan+","+d.cellular+","+d.satellite+ " cost:"+current_cost);
			return d;
		}
		else			//i.e. LAGRANGE's method
		{
			double K=max_cost,S=data_size,Sc=satellite_cost,Wc=wlan_cost,Cc=cellular_cost,Ct=conditions[1].getTH_estimate(),Wt=conditions[0].getTH_estimate(),St=conditions[2].getTH_estimate();
			double Y_temp, X_temp,Z_temp,X=0,Y=0,Z=0,cost=0,max_throughput=0,cost_temp,throughput_temp;
			//** region 1 **//
			//option 2
			X_temp=(K-S*Sc)/(Wc+Ct/Wt*Cc-Sc-Ct/Wt*Sc);
			Y_temp=Ct/Wt*X_temp;
			Z_temp=S-X_temp-Y_temp;
			cost_temp=X_temp*wlan_cost+Y_temp*cellular_cost+Z_temp*satellite_cost;
			throughput_temp=S/(X_temp/Wt+Y_temp/Ct+Z_temp/St);
			if (X_temp>=0 && Y_temp>=0 && Z_temp>=0 && X_temp<=S && Y_temp<=S && Z_temp<=S &&  X_temp+ Y_temp+ Z_temp<=S+0.01 && X_temp*wlan_cost+Y_temp*cellular_cost+Z_temp*satellite_cost <=K) //&& X_temp/Wt>=Y_temp/Ct && X_temp/Wt>=Z_temp/St&&
			{
				X=X_temp;
				Y=Y_temp;
				Z=Z_temp;
				cost=cost_temp;
				max_throughput=throughput_temp;
				System.out.print("1");
			}
			//option 3
			X_temp=S/(1+Ct/Wt+St/Wt);
			Y_temp=Ct/Wt*X_temp;
			Z_temp=St/Wt*X_temp;
			if (X_temp>=0 && Y_temp>=0 && Z_temp>=0 && X_temp<=S && Y_temp<=S && Z_temp<=S &&  X_temp+ Y_temp+ Z_temp<=S+0.01 && X_temp*wlan_cost+Y_temp*cellular_cost+Z_temp*satellite_cost <=K) //&& X_temp/Wt>=Y_temp/Ct && X_temp/Wt>=Z_temp/St
			{
				cost_temp=X_temp*wlan_cost+Y_temp*cellular_cost+Z_temp*satellite_cost;
				throughput_temp=S/(X_temp/Wt+Y_temp/Ct+Z_temp/St);
				if (throughput_temp>max_throughput || (throughput_temp==max_throughput && cost>cost_temp))
				{
					X=X_temp;
					Y=Y_temp;
					Z=Z_temp;
					cost=cost_temp;
					max_throughput=throughput_temp;
					System.out.print("2");
				}
			}
			//option 4c
			Z_temp=(S*Wc-K)/(Wc-Sc);
			Y_temp=0;
			X_temp=(K-Z_temp*Sc)/Wc;
			if (X_temp>=0 && Y_temp>=0 && Z_temp>=0 && X_temp<=S && Y_temp<=S && Z_temp<=S &&  X_temp+ Y_temp+ Z_temp<=S+0.01 && X_temp*wlan_cost+Y_temp*cellular_cost+Z_temp*satellite_cost <=K) // && X_temp/Wt>=Y_temp/Ct && X_temp/Wt>=Z_temp/St
			{
				cost_temp=X_temp*wlan_cost+Y_temp*cellular_cost+Z_temp*satellite_cost;
				throughput_temp=S/(X_temp/Wt+Y_temp/Ct+Z_temp/St);
				if (throughput_temp>max_throughput || (throughput_temp==max_throughput && cost>cost_temp))
				{
					X=X_temp;
					Y=Y_temp;
					Z=Z_temp;
					cost=cost_temp;
					max_throughput=throughput_temp;
					System.out.print("3");
				}
			}
			//option 4b
			Z_temp=S/(1+Wt/St);
			Y_temp=0;
			X_temp=S-Z_temp;
			if (X_temp>=0 && Y_temp>=0 && Z_temp>=0 && X_temp<=S && Y_temp<=S && Z_temp<=S &&  X_temp+ Y_temp+ Z_temp<=S+0.01 && X_temp*wlan_cost+Y_temp*cellular_cost+Z_temp*satellite_cost <=K) //&& X_temp/Wt>=Y_temp/Ct && X_temp/Wt>=Z_temp/St
			{
				cost_temp=X_temp*wlan_cost+Y_temp*cellular_cost+Z_temp*satellite_cost;
				throughput_temp=S/(X_temp/Wt+Y_temp/Ct+Z_temp/St);
				if (throughput_temp>max_throughput || (throughput_temp==max_throughput && cost>cost_temp))
				{
					X=X_temp;
					Y=Y_temp;
					Z=Z_temp;
					cost=cost_temp;
					max_throughput=throughput_temp;
					System.out.print("4");
				}
			}
			//option 5a
			Z_temp=0;
			Y_temp=S/(1+Wt/Ct);
			X_temp=S-Y_temp;
			if (X_temp>=0 && Y_temp>=0 && Z_temp>=0 && X_temp<=S && Y_temp<=S && Z_temp<=S &&  X_temp+ Y_temp+ Z_temp<=S+0.01 && X_temp*wlan_cost+Y_temp*cellular_cost+Z_temp*satellite_cost <=K) //&& X_temp/Wt>=Y_temp/Ct && X_temp/Wt>=Z_temp/St
			{
				cost_temp=X_temp*wlan_cost+Y_temp*cellular_cost+Z_temp*satellite_cost;
				throughput_temp=S/(X_temp/Wt+Y_temp/Ct+Z_temp/St);
				if (throughput_temp>max_throughput || (throughput_temp==max_throughput && cost>cost_temp))
				{
					X=X_temp;
					Y=Y_temp;
					Z=Z_temp;
					cost=cost_temp;
					max_throughput=throughput_temp;
					System.out.print("5");
				}
			}
			//option 5b
			Z_temp=0;
			Y_temp=(K-S*Cc)/(Cc-Wc);
			X_temp=S-Y_temp;
			if (X_temp>=0 && Y_temp>=0 && Z_temp>=0 && X_temp<=S && Y_temp<=S && Z_temp<=S &&  X_temp+ Y_temp+ Z_temp<=S+0.01 && X_temp*wlan_cost+Y_temp*cellular_cost+Z_temp*satellite_cost <=K) // && X_temp/Wt>=Y_temp/Ct && X_temp/Wt>=Z_temp/St
			{
				cost_temp=X_temp*wlan_cost+Y_temp*cellular_cost+Z_temp*satellite_cost;
				throughput_temp=S/(X_temp/Wt+Y_temp/Ct+Z_temp/St);
				if (throughput_temp>max_throughput || (throughput_temp==max_throughput && cost>cost_temp))
				{
					X=X_temp;
					Y=Y_temp;
					Z=Z_temp;
					cost=cost_temp;
					max_throughput=throughput_temp;
					System.out.print("6");
				}
			}
			//option 1
			Z_temp=(K-S*Wc)/(Sc+Wc);
			Y_temp=S-Z_temp*(1+Wt/St);
			X_temp=Z_temp*Wt/St;
			if (X_temp>=0 && Y_temp>=0 && Z_temp>=0 && X_temp<=S && Y_temp<=S && Z_temp<=S &&  X_temp+ Y_temp+ Z_temp<=S+0.01 && X_temp*wlan_cost+Y_temp*cellular_cost+Z_temp*satellite_cost <=K) //&& X_temp/Wt>=Y_temp/Ct && X_temp/Wt>=Z_temp/St &&
			{
				cost_temp=X_temp*wlan_cost+Y_temp*cellular_cost+Z_temp*satellite_cost;
				throughput_temp=S/(X_temp/Wt+Y_temp/Ct+Z_temp/St);
				if (throughput_temp>max_throughput || (throughput_temp==max_throughput && cost>cost_temp) )
				{
					X=X_temp;
					Y=Y_temp;
					Z=Z_temp;
					cost=cost_temp;
					max_throughput=throughput_temp;
					System.out.print("7");
				}
			}
			//option 4d
			X_temp=K/(Wc+St*Sc/Wt);
			Y_temp=0;
			Z_temp=X_temp*St/Wt;
			if (X_temp>=0 && Y_temp>=0 && Z_temp>=0 && X_temp<=S && Y_temp<=S && Z_temp<=S &&  X_temp+ Y_temp+ Z_temp<=S+0.01 && X_temp*wlan_cost+Y_temp*cellular_cost+Z_temp*satellite_cost <=K) //&& X_temp/Wt>=Y_temp/Ct && X_temp/Wt>=Z_temp/St
			{
				cost_temp=X_temp*wlan_cost+Y_temp*cellular_cost+Z_temp*satellite_cost;
				throughput_temp=S/(X_temp/Wt+Y_temp/Ct+Z_temp/St);
				if (throughput_temp>max_throughput || (throughput_temp==max_throughput && cost>cost_temp))
				{
					X=X_temp;
					Y=Y_temp;
					Z=Z_temp;
					cost=cost_temp;
					max_throughput=throughput_temp;
					System.out.print("8");
				}
			}
			//option 5d
			X_temp=K/(Wc+Ct*Cc/Wt);
			Z_temp=0;
			Y_temp=X_temp*Ct/Wt;
			if (X_temp>=0 && Y_temp>=0 && Z_temp>=0 && X_temp<=S && Y_temp<=S && Z_temp<=S && X_temp+ Y_temp+ Z_temp<=S+0.01 && X_temp*wlan_cost+Y_temp*cellular_cost+Z_temp*satellite_cost <=K) //&& X_temp/Wt>=Y_temp/Ct && X_temp/Wt>=Z_temp/St
			{
				cost_temp=X_temp*wlan_cost+Y_temp*cellular_cost+Z_temp*satellite_cost;
				throughput_temp=S/(X_temp/Wt+Y_temp/Ct+Z_temp/St);
				if (throughput_temp>max_throughput || (throughput_temp==max_throughput && cost>cost_temp))
				{
					X=X_temp;
					Y=Y_temp;
					Z=Z_temp;
					cost=cost_temp;
					max_throughput=throughput_temp;
					System.out.print("9");
				}
			}
			//** region 2 **//
			//option 1
			Z_temp=(K-S*Wc)/(Sc-Wc+Ct/St*(Cc-Wc));
			Y_temp=Ct/St*Z_temp;
			X_temp=S-Z_temp-Y_temp;
			if (X_temp>=0 && Y_temp>=0 && Z_temp>=0 && X_temp<=S && Y_temp<=S && Z_temp<=S && X_temp+ Y_temp+ Z_temp<=S+0.01 && X_temp*wlan_cost+Y_temp*cellular_cost+Z_temp*satellite_cost <=K) //&& Y_temp/Ct>=X_temp/Wt && Y_temp/Ct>=Z_temp/St
			{
				cost_temp=X_temp*wlan_cost+Y_temp*cellular_cost+Z_temp*satellite_cost;
				throughput_temp=S/(X_temp/Wt+Y_temp/Ct+Z_temp/St);
				if (throughput_temp>max_throughput || (throughput_temp==max_throughput && cost>cost_temp))
				{
					X=X_temp;
					Y=Y_temp;
					Z=Z_temp;
					cost=cost_temp;
					max_throughput=throughput_temp;
					System.out.print("A");
				}
			}
			//option 2
			Z_temp=K/(Sc-Wt/(Wt+Ct))-(Wc+Cc*Ct/Wt)*S/(Sc*(1+Ct/Wt)-1);
			X_temp=(S-Z_temp)/(1+Ct/Wt);
			Y_temp=Ct/Wt*X_temp;
			if (X_temp>=0 && Y_temp>=0 && Z_temp>=0 && X_temp<=S && Y_temp<=S && Z_temp<=S && X_temp+ Y_temp+ Z_temp<=S+0.01 && X_temp*wlan_cost+Y_temp*cellular_cost+Z_temp*satellite_cost <=K) //&& Y_temp/Ct>=X_temp/Wt && Y_temp/Ct>=Z_temp/St
			{
				cost_temp=X_temp*wlan_cost+Y_temp*cellular_cost+Z_temp*satellite_cost;
				throughput_temp=S/(X_temp/Wt+Y_temp/Ct+Z_temp/St);
				if (throughput_temp>max_throughput || (throughput_temp==max_throughput && cost>cost_temp))
				{
					X=X_temp;
					Y=Y_temp;
					Z=Z_temp;
					cost=cost_temp;
					max_throughput=throughput_temp;
					System.out.print("B");
				}
			}
			//option 3
			X_temp=S/(1+Ct/Wt+St/Wt);
			Y_temp=Ct/Wt*X_temp;
			Z_temp=St/Ct*Y_temp;
			if (X_temp>=0 && Y_temp>=0 && Z_temp>=0 && X_temp<=S && Y_temp<=S && Z_temp<=S && X_temp+ Y_temp+ Z_temp<=S+0.01 && X_temp*wlan_cost+Y_temp*cellular_cost+Z_temp*satellite_cost <=K) //&& Y_temp/Ct>=X_temp/Wt && Y_temp/Ct>=Z_temp/St
			{
				cost_temp=X_temp*wlan_cost+Y_temp*cellular_cost+Z_temp*satellite_cost;
				throughput_temp=S/(X_temp/Wt+Y_temp/Ct+Z_temp/St);
				if (throughput_temp>max_throughput || (throughput_temp==max_throughput && cost>cost_temp))
				{
					X=X_temp;
					Y=Y_temp;
					Z=Z_temp;
					cost=cost_temp;
					max_throughput=throughput_temp;
					System.out.print("C");
				}
			}
			//option 4a
			Z_temp=S/(1+Ct/St);
			Y_temp=Ct/St*Z_temp;
			X_temp=0;
			if (X_temp>=0 && Y_temp>=0 && Z_temp>=0 && X_temp<=S && Y_temp<=S && Z_temp<=S && X_temp+ Y_temp+ Z_temp<=S+0.01 && X_temp*wlan_cost+Y_temp*cellular_cost+Z_temp*satellite_cost <=K) //&& Y_temp/Ct>=X_temp/Wt && Y_temp/Ct>=Z_temp/St
			{
				cost_temp=X_temp*wlan_cost+Y_temp*cellular_cost+Z_temp*satellite_cost;
				throughput_temp=S/(X_temp/Wt+Y_temp/Ct+Z_temp/St);
				if (throughput_temp>max_throughput || (throughput_temp==max_throughput && cost>cost_temp))
				{
					X=X_temp;
					Y=Y_temp;
					Z=Z_temp;
					cost=cost_temp;
					max_throughput=throughput_temp;
					System.out.print("D");
				}
			}
			//option 4b
			Z_temp=(K-Cc*S)/(Sc-Cc);
			Y_temp=S-Z_temp;
			X_temp=0;
			if (X_temp>=0 && Y_temp>=0 && Z_temp>=0 && X_temp<=S && Y_temp<=S && Z_temp<=S && X_temp+ Y_temp+ Z_temp<=S+0.01 && X_temp*wlan_cost+Y_temp*cellular_cost+Z_temp*satellite_cost <=K) //&& Y_temp/Ct>=X_temp/Wt && Y_temp/Ct>=Z_temp/St
			{
				cost_temp=X_temp*wlan_cost+Y_temp*cellular_cost+Z_temp*satellite_cost;
				throughput_temp=S/(X_temp/Wt+Y_temp/Ct+Z_temp/St);
				if (throughput_temp>max_throughput || (throughput_temp==max_throughput && cost>cost_temp))
				{
					X=X_temp;
					Y=Y_temp;
					Z=Z_temp;
					cost=cost_temp;
					max_throughput=throughput_temp;
					System.out.print("E");
				}
			}
			//option 4c
			Z_temp=K/(Cc*Ct/St+Sc);
			Y_temp=Ct/St*Z_temp;
			X_temp=0;
			if (X_temp>=0 && Y_temp>=0 && Z_temp>=0 && X_temp<=S && Y_temp<=S && Z_temp<=S && X_temp+ Y_temp+ Z_temp<=S+0.01 && X_temp*wlan_cost+Y_temp*cellular_cost+Z_temp*satellite_cost <=K) //&& Y_temp/Ct>=X_temp/Wt && Y_temp/Ct>=Z_temp/St
			{
				cost_temp=X_temp*wlan_cost+Y_temp*cellular_cost+Z_temp*satellite_cost;
				throughput_temp=S/(X_temp/Wt+Y_temp/Ct+Z_temp/St);
				if (throughput_temp>max_throughput || (throughput_temp==max_throughput && cost>cost_temp))
				{
					X=X_temp;
					Y=Y_temp;
					Z=Z_temp;
					cost=cost_temp;
					max_throughput=throughput_temp;
					System.out.print("F");
				}
			}
			//option 5a
			Z_temp=0;
			X_temp=S/(1+Ct/Wt);
			Y_temp=X_temp*Ct/Wt;
			if (X_temp>=0 && Y_temp>=0 && Z_temp>=0 && X_temp<=S && Y_temp<=S && Z_temp<=S &&  X_temp+ Y_temp+ Z_temp<=S+0.01 && X_temp*wlan_cost+Y_temp*cellular_cost+Z_temp*satellite_cost <=K) //&& Y_temp/Ct>=X_temp/Wt && Y_temp/Ct>=Z_temp/St
			{
				cost_temp=X_temp*wlan_cost+Y_temp*cellular_cost+Z_temp*satellite_cost;
				throughput_temp=S/(X_temp/Wt+Y_temp/Ct+Z_temp/St);
				if (throughput_temp>max_throughput || (throughput_temp==max_throughput && cost>cost_temp))
				{
					X=X_temp;
					Y=Y_temp;
					Z=Z_temp;
					cost=cost_temp;
					max_throughput=throughput_temp;
					System.out.print("G");
				}
			}
			//option 5b (although redundant but for different constraints
			Z_temp=0;
			Y_temp=(K-S*Wc)/(Cc-Wc);
			X_temp=S-Y_temp;
			if (X_temp>=0 && Y_temp>=0 && Z_temp>=0 && X_temp<=S && Y_temp<=S && Z_temp<=S && X_temp+ Y_temp+ Z_temp<=S+0.01 && X_temp*wlan_cost+Y_temp*cellular_cost+Z_temp*satellite_cost <=K) //&& Y_temp/Ct>=X_temp/Wt && Y_temp/Ct>=Z_temp/St
			{
				cost_temp=X_temp*wlan_cost+Y_temp*cellular_cost+Z_temp*satellite_cost;
				throughput_temp=S/(X_temp/Wt+Y_temp/Ct+Z_temp/St);
				if (throughput_temp>max_throughput || (throughput_temp==max_throughput && cost>cost_temp))
				{
					X=X_temp;
					Y=Y_temp;
					Z=Z_temp;
					cost=cost_temp;
					max_throughput=throughput_temp;
					System.out.print("H");
				}
			}
			//option 5c
			X_temp=K/(Wc+Wt*Cc/Ct);
			Z_temp=0;
			Y_temp=X_temp*Wt/Ct;
			if (X_temp>=0 && Y_temp>=0 && Z_temp>=0 && X_temp<=S && Y_temp<=S && Z_temp<=S &&  X_temp+ Y_temp+ Z_temp<=S+0.01 && X_temp*wlan_cost+Y_temp*cellular_cost+Z_temp*satellite_cost <=K) //&& Y_temp/Ct>=X_temp/Wt && Y_temp/Ct>=Z_temp/St
			{
				cost_temp=X_temp*wlan_cost+Y_temp*cellular_cost+Z_temp*satellite_cost;
				throughput_temp=S/(X_temp/Wt+Y_temp/Ct+Z_temp/St);
				if (throughput_temp>max_throughput || (throughput_temp==max_throughput && cost>cost_temp))
				{
					X=X_temp;
					Y=Y_temp;
					Z=Z_temp;
					cost=cost_temp;
					max_throughput=throughput_temp;
					System.out.print("I");
				}
			}
			//** region 3 **//
			//option 1
			Z_temp=(K-S*Wc)/(Ct/St*(Cc-Wc)+Sc-Wc);
			Y_temp=Ct/St*Z_temp;
			X_temp=S-Y_temp-Z_temp;
			if (X_temp>=0 && Y_temp>=0 && Z_temp>=0 && X_temp<=S && Y_temp<=S && Z_temp<=S &&  X_temp+ Y_temp+ Z_temp<=S+0.01 && X_temp*wlan_cost+Y_temp*cellular_cost+Z_temp*satellite_cost <=K) //&& Z_temp/St>=Y_temp/Ct && Z_temp/St>=X_temp/Wt
			{
				cost_temp=X_temp*wlan_cost+Y_temp*cellular_cost+Z_temp*satellite_cost;
				throughput_temp=S/(X_temp/Wt+Y_temp/Ct+Z_temp/St);
				if (throughput_temp>max_throughput || (throughput_temp==max_throughput && cost>cost_temp))
				{
					X=X_temp;
					Y=Y_temp;
					Z=Z_temp;
					cost=cost_temp;
					max_throughput=throughput_temp;
					System.out.print("J");
				}
			}
			//option 2
			Y_temp=(K-Wc*S)/(Cc+Sc*St/Ct-Wc*(1+St/Ct));
			Z_temp=St/Ct*Y_temp;
			X_temp=S-Y_temp-Z_temp;
			if (X_temp>=0 && Y_temp>=0 && Z_temp>=0 && X_temp<=S && Y_temp<=S && Z_temp<=S &&  X_temp+ Y_temp+ Z_temp<=S+0.01 && X_temp*wlan_cost+Y_temp*cellular_cost+Z_temp*satellite_cost <=K) //&& Z_temp/St>=Y_temp/Ct && Z_temp/St>=X_temp/Wt
			{
				cost_temp=X_temp*wlan_cost+Y_temp*cellular_cost+Z_temp*satellite_cost;
				throughput_temp=S/(X_temp/Wt+Y_temp/Ct+Z_temp/St);
				if (throughput_temp>max_throughput || (throughput_temp==max_throughput && cost>cost_temp))
				{
					X=X_temp;
					Y=Y_temp;
					Z=Z_temp;
					cost=cost_temp;
					max_throughput=throughput_temp;
					System.out.print("K");
				}
			}
			//option 3
			Z_temp=S/(1+Ct/St+Wt/St);
			Y_temp=Z_temp*Ct/St;
			X_temp=Z_temp*Wt/St;
			if (X_temp>=0 && Y_temp>=0 && Z_temp>=0 && X_temp<=S && Y_temp<=S && Z_temp<=S &&  X_temp+ Y_temp+ Z_temp<=S+0.01 && X_temp*wlan_cost+Y_temp*cellular_cost+Z_temp*satellite_cost <=K) //&& Z_temp/St>=Y_temp/Ct && Z_temp/St>=X_temp/Wt
			{
				cost_temp=X_temp*wlan_cost+Y_temp*cellular_cost+Z_temp*satellite_cost;
				throughput_temp=S/(X_temp/Wt+Y_temp/Ct+Z_temp/St);
				if (throughput_temp>max_throughput || (throughput_temp==max_throughput && cost>cost_temp))
				{
					X=X_temp;
					Y=Y_temp;
					Z=Z_temp;
					cost=cost_temp;
					max_throughput=throughput_temp;
					System.out.print("L");
				}
			}
			//option 4a
			Y_temp=S/(1+St/Ct);
			X_temp=0;
			Z_temp=S-Z_temp;
			if (X_temp>=0 && Y_temp>=0 && Z_temp>=0 && X_temp<=S && Y_temp<=S && Z_temp<=S &&  X_temp+ Y_temp+ Z_temp<=S+0.01 && X_temp*wlan_cost+Y_temp*cellular_cost+Z_temp*satellite_cost <=K) //&& Z_temp/St>=Y_temp/Ct && Z_temp/St>=X_temp/Wt
			{
				cost_temp=X_temp*wlan_cost+Y_temp*cellular_cost+Z_temp*satellite_cost;
				throughput_temp=S/(X_temp/Wt+Y_temp/Ct+Z_temp/St);
				if (throughput_temp>max_throughput || (throughput_temp==max_throughput && cost>cost_temp))
				{
					X=X_temp;
					Y=Y_temp;
					Z=Z_temp;
					cost=cost_temp;
					max_throughput=throughput_temp;
					System.out.print("M");
				}
			}
			//option 4b (redundant but with different constraints)
			Z_temp=(K-Cc*S)/(Sc-Cc);
			Y_temp=S-Z_temp;
			X_temp=0;
			if (X_temp>=0 && Y_temp>=0 && Z_temp>=0 && X_temp<=S && Y_temp<=S && Z_temp<=S &&  X_temp+ Y_temp+ Z_temp<=S+0.01 && X_temp*wlan_cost+Y_temp*cellular_cost+Z_temp*satellite_cost <=K) //&& Z_temp/St>=Y_temp/Ct && Z_temp/St>=X_temp/Wt
			{
				cost_temp=X_temp*wlan_cost+Y_temp*cellular_cost+Z_temp*satellite_cost;
				throughput_temp=S/(X_temp/Wt+Y_temp/Ct+Z_temp/St);
				if (throughput_temp>max_throughput || (throughput_temp==max_throughput && cost>cost_temp))
				{
					X=X_temp;
					Y=Y_temp;
					Z=Z_temp;
					cost=cost_temp;
					max_throughput=throughput_temp;
					System.out.print("N");
				}
			}
			//option 4c
			Y_temp=K/(Cc+Sc*St/Ct);
			X_temp=0;
			Z_temp=St/Ct*Y_temp;
			if (X_temp>=0 && Y_temp>=0 && Z_temp>=0 && X_temp<=S && Y_temp<=S && Z_temp<=S &&  X_temp+ Y_temp+ Z_temp<=S+0.01 && X_temp*wlan_cost+Y_temp*cellular_cost+Z_temp*satellite_cost <=K) //&& Z_temp/St>=Y_temp/Ct && Z_temp/St>=X_temp/Wt
			{
				cost_temp=X_temp*wlan_cost+Y_temp*cellular_cost+Z_temp*satellite_cost;
				throughput_temp=S/(X_temp/Wt+Y_temp/Ct+Z_temp/St);
				if (throughput_temp>max_throughput || (throughput_temp==max_throughput && cost>cost_temp))
				{
					X=X_temp;
					Y=Y_temp;
					Z=Z_temp;
					cost=cost_temp;
					max_throughput=throughput_temp;
					System.out.print("O");
				}
			}
			//option 5a
			X_temp=S/(1+St/Wt);
			Y_temp=0;
			Z_temp=S-X_temp;
			if (X_temp>=0 && Y_temp>=0 && Z_temp>=0 && X_temp<=S && Y_temp<=S && Z_temp<=S &&  X_temp+ Y_temp+ Z_temp<=S+0.01 && X_temp*wlan_cost+Y_temp*cellular_cost+Z_temp*satellite_cost <=K) //&& Z_temp/St>=Y_temp/Ct && Z_temp/St>=X_temp/Wt
			{
				cost_temp=X_temp*wlan_cost+Y_temp*cellular_cost+Z_temp*satellite_cost;
				throughput_temp=S/(X_temp/Wt+Y_temp/Ct+Z_temp/St);
				if (throughput_temp>max_throughput || (throughput_temp==max_throughput && cost>cost_temp))
				{
					X=X_temp;
					Y=Y_temp;
					Z=Z_temp;
					cost=cost_temp;
					max_throughput=throughput_temp;
					System.out.print("P");
				}
			}
			//option 5b (although redundant for different constraints)
			Z_temp=(K-S*Wc)/(Sc-Wc);
			Y_temp=0;
			X_temp=S-Z_temp;
			if (X_temp>=0 && Y_temp>=0 && Z_temp>=0 && X_temp<=S && Y_temp<=S && Z_temp<=S &&  X_temp+ Y_temp+ Z_temp<=S+0.01 && X_temp*wlan_cost+Y_temp*cellular_cost+Z_temp*satellite_cost <=K) //&& Z_temp/St>=Y_temp/Ct && Z_temp/St>=X_temp/Wt
			{
				cost_temp=X_temp*wlan_cost+Y_temp*cellular_cost+Z_temp*satellite_cost;
				throughput_temp=S/(X_temp/Wt+Y_temp/Ct+Z_temp/St);
				if (throughput_temp>max_throughput || (throughput_temp==max_throughput && cost>cost_temp))
				{
					X=X_temp;
					Y=Y_temp;
					Z=Z_temp;
					cost=cost_temp;
					max_throughput=throughput_temp;
					System.out.print("Q");
				}
			}
			//option 5c
			X_temp=K/(Wc+St*Sc/Wt);
			Y_temp=0;
			Z_temp=X_temp*St/Wt;
			if (X_temp>=0 && Y_temp>=0 && Z_temp>=0 && X_temp<=S && Y_temp<=S && Z_temp<=S &&  X_temp+ Y_temp+ Z_temp<=S+0.01 && X_temp*wlan_cost+Y_temp*cellular_cost+Z_temp*satellite_cost <=K) //&& Z_temp/St>=Y_temp/Ct && Z_temp/St>=X_temp/Wt
			{
				cost_temp=X_temp*wlan_cost+Y_temp*cellular_cost+Z_temp*satellite_cost;
				throughput_temp=S/(X_temp/Wt+Y_temp/Ct+Z_temp/St);
				if (throughput_temp>max_throughput || (throughput_temp==max_throughput && cost>cost_temp))
				{
					X=X_temp;
					Y=Y_temp;
					Z=Z_temp;
					cost=cost_temp;
					max_throughput=throughput_temp;
					System.out.print("R");
				}
			}
			//boundary states
			/*1*/
			X_temp=S;
			Y_temp=0;
			Z_temp=0;
			if (X_temp>=0 && Y_temp>=0 && Z_temp>=0 && X_temp<=S && Y_temp<=S && Z_temp<=S &&  X_temp+ Y_temp+ Z_temp<=S+0.01 && X_temp*wlan_cost+Y_temp*cellular_cost+Z_temp*satellite_cost <=K) //&& Z_temp/St>=Y_temp/Ct && Z_temp/St>=X_temp/Wt
			{
				cost_temp=X_temp*wlan_cost+Y_temp*cellular_cost+Z_temp*satellite_cost;
				throughput_temp=S/(X_temp/Wt+Y_temp/Ct+Z_temp/St);
				if (throughput_temp>max_throughput || (throughput_temp==max_throughput && cost>cost_temp))
				{
					X=X_temp;
					Y=Y_temp;
					Z=Z_temp;
					cost=cost_temp;
					max_throughput=throughput_temp;
					System.out.print("S");
				}
			}
			/*2*/
			X_temp=0;
			Y_temp=S;
			Z_temp=0;
			if (X_temp>=0 && Y_temp>=0 && Z_temp>=0 && X_temp<=S && Y_temp<=S && Z_temp<=S &&  X_temp+ Y_temp+ Z_temp<=S+0.01 && X_temp*wlan_cost+Y_temp*cellular_cost+Z_temp*satellite_cost <=K) //&& Z_temp/St>=Y_temp/Ct && Z_temp/St>=X_temp/Wt
			{
				cost_temp=X_temp*wlan_cost+Y_temp*cellular_cost+Z_temp*satellite_cost;
				throughput_temp=S/(X_temp/Wt+Y_temp/Ct+Z_temp/St);
				if (throughput_temp>max_throughput || (throughput_temp==max_throughput && cost>cost_temp))
				{
					X=X_temp;
					Y=Y_temp;
					Z=Z_temp;
					cost=cost_temp;
					max_throughput=throughput_temp;
					System.out.print("T");
				}
			}
			/*3*/
			X_temp=0;
			Y_temp=0;
			Z_temp=S;
			if (X_temp>=0 && Y_temp>=0 && Z_temp>=0 && X_temp<=S && Y_temp<=S && Z_temp<=S &&  X_temp+ Y_temp+ Z_temp<=S+0.01 && X_temp*wlan_cost+Y_temp*cellular_cost+Z_temp*satellite_cost <=K) //&& Z_temp/St>=Y_temp/Ct && Z_temp/St>=X_temp/Wt
			{
				cost_temp=X_temp*wlan_cost+Y_temp*cellular_cost+Z_temp*satellite_cost;
				throughput_temp=S/(X_temp/Wt+Y_temp/Ct+Z_temp/St);
				if (throughput_temp>max_throughput || (throughput_temp==max_throughput && cost>cost_temp))
				{
					X=X_temp;
					Y=Y_temp;
					Z=Z_temp;
					cost=cost_temp;
					max_throughput=throughput_temp;
					System.out.print("U");
				}
			}
			assert(X_temp<=S && Y_temp<=S && Z_temp<=S && X_temp+ Y_temp+ Z_temp<=S+0.01 && X_temp*wlan_cost+Y_temp*cellular_cost+Z_temp*satellite_cost <=K);
			d.wlan=X;
			d.cellular=Y;
			d.satellite=Z;
			System.out.println("divison: "+d.wlan+","+d.cellular+","+d.satellite+ " cost:"+cost);
		}

		return d;
		/*
		d.cellular=0.33;
		d.wlan=0.34;
		d.satellite=0.33;
		return d;
		*/
	}

    public class network_conditions
	{

		private class fuzzy_parameters
		{
			public double Tmin=0;
			public double Tmax=10;//just example might be changed
		}

		private class throughputMeasurements{
			public double throughput;
			public double stamp;

			public throughputMeasurements(double nominal_TH){
			    throughput=nominal_TH;
			    stamp=SimClock.getTime();
			}
		}

		private fuzzy_parameters params;
		private throughputMeasurements throughputEstimate;
		private throughputMeasurements throughputSensed;
		private throughputMeasurements throughputShared;
		
		public void setInitially(double nominal_TH)
		{
			//throughputSensedEstimate=nominal_TH;
			throughputSensed =new throughputMeasurements(nominal_TH);
			throughputEstimate =new throughputMeasurements(nominal_TH);
			throughputShared=new throughputMeasurements(nominal_TH);
			params =new fuzzy_parameters();
		}
		private double getNominal(String inter)
		{
			if (inter.equals(ParanetAdaptableFuzzySprayAndWaitRouter.INTERFACE_WLAN))
				return wlan_nominal;
			else if (inter.equals(ParanetAdaptableFuzzySprayAndWaitRouter.INTERFACE_CELLULAR))
				return cellular_nominal;
			else if (inter.equals(ParanetAdaptableFuzzySprayAndWaitRouter.INTERFACE_SATELLITE))
				return satellite_nominal;
			else
				assert true: "Illigal interface type "+ inter;
			return -1;
		}
		private double getNominal(int index_of_interface)
		{
			if (index_of_interface==0)
				return wlan_nominal;
			else if (index_of_interface==1)
				return cellular_nominal;
			else if (index_of_interface==2)
				return satellite_nominal;
			else
				assert true: "Illigal interface type "+ index_of_interface;
			return -1;
		}
		public void sensed(double sensed_TH, String intr)
		{
			System.out.println(sensed_TH+" "+intr);
			double nominal=getNominal(intr);
			double time_diff=SimClock.getTime()-throughputSensed.stamp;
			params=getFuzzyParams(time_diff);
			fuzzyHelper.Tmin=params.Tmin;
			fuzzyHelper.Tmax=params.Tmax;
			throughputEstimate.throughput=fuzzyH.getNewEstimateOriginal(throughputEstimate.throughput,sensed_TH,time_diff,nominal);
			System.out.println("["+throughputEstimate.throughput+"]"+intr);
			throughputSensed.stamp=SimClock.getTime(); //for next time
		}

		public void shared(double shared_TH,int index_of_interface)
		{
			System.out.println(shared_TH+" "+index_of_interface);
			double nominal=getNominal(index_of_interface);
			double time_diff=SimClock.getTime()-throughputShared.stamp;
			params=getFuzzyParams(time_diff);
			fuzzyHelper.Tmin=params.Tmin;
			fuzzyHelper.Tmax=params.Tmax;
			throughputEstimate.throughput=fuzzyH.getNewEstimateOriginal(throughputEstimate.throughput,shared_TH,time_diff,nominal);
			System.out.println("["+throughputEstimate.throughput+"]"+index_of_interface);
			throughputShared.stamp=SimClock.getTime(); //for next time
		}
		double standard_deviation(double average)
		{
			double dev=0;
			for (int i=0;i<tdiffs.size();i++)
					dev=dev+Math.pow((tdiffs.get(i)-average),2.0);
			dev=dev/tdiffs.size()-1;
			dev=Math.sqrt(dev);
			return dev;
		}

		private fuzzy_parameters getFuzzyParams(double tdiff_current)
		{
			tdiffs.addLast(tdiff_current);
			if (tdiffs.size()==number_of_elements)
				tdiffs.removeFirst();
			double sum=0;
			for (int i=0;i<tdiffs.size();i++)
				sum+=tdiffs.get(i);
			double average=sum/tdiffs.size();
			//get standard deviation
			fuzzy_parameters f=new fuzzy_parameters();
			double dev=standard_deviation(average);
			f.Tmin=average-2*dev;
			if (f.Tmin<0)
				f.Tmin=0;
			f.Tmax=average+2*dev;
			return f;
		}
		
		private double getNewEstimate(double old_estimate, double new_value, double time_diff,double nominal_TH) {
			old_estimate=mapThroughputToFCL(old_estimate, nominal_TH);
			new_value=mapThroughputToFCL(new_value, nominal_TH);
			time_diff=mapTimeToFCL(time_diff);

			return fuzzyH.getNewEstimate(old_estimate, new_value, time_diff);
		}
		private double mapThroughputToFCL(double throughput,double nominal_TH){
			double low_TH=nominal_TH/5;
			return 10*(throughput-low_TH)/(nominal_TH-low_TH)+2;
		}
		private double mapBackToThroughput(double throughput,double nominal_TH){
			double low_TH=nominal_TH/5;
			return (throughput-2)*(nominal_TH-low_TH)/10+low_TH;
		}
		private double mapTimeToFCL(double time){
			return 6*(time-params.Tmin)/(params.Tmax-params.Tmin)+2;
		}
		private double mapBackToTime(double time){

			return (time-2)*(params.Tmax-params.Tmin)/6+params.Tmin;
		}
		public double getTH_estimate()
		{
			return throughputEstimate.throughput;
		}
		public double last_stamp()
		{
			return (throughputSensed.stamp>throughputShared.stamp?throughputSensed.stamp:throughputShared.stamp);
		}
	}

	public network_conditions [] conditions =new network_conditions[3];

	// Private vars
	private double	lastRequest = 0;
	private double	min_interval = 500;
	private double	max_interval = 600;
	private double  current_interval=min_interval;
	private boolean SA = false;
	private int		seed = 0;
/*	private int		MA_Min=1;
	private int		MA_Max=59;*/
	static public int	SA_ID=0;
	static public double wlan_cost=0;
	static public double cellular_cost=0;
	static public double satellite_cost=0;
	static public double max_max_cost=2;
	static public double min_max_cost=0.1;
	private int		requestSize=1;
	private int		minSize=100;
	private int		maxSize=200;
	private Random	rng;
	private int msg_count=0;
	private double wlan_nominal=1000000;
	private double cellular_nominal=10000000;
	private double satellite_nominal=8000000;
	private boolean lagrange=false;
	private int num_minibundles=10;
	private double max_cost=1;
	static private int wlan_msg_size=500;
	static private int number_of_elements=15;

	private LinkedList<Double> tdiffs=new LinkedList<Double>();

	
	/** 
	 * Creates a new PARANETS Application with the given settings.
	 * 
	 * @param s	Settings to use for initializing the application.
	 */
	private fuzzyHelper fuzzyH;
	public PARANETS_application(Settings s) {
		rng = new Random(this.seed);
		if (s.contains(NODE_ROLE)){
			if ( s.getSetting(NODE_ROLE).equalsIgnoreCase("SA"))
				SA=true;
			else if ( s.getSetting(NODE_ROLE).equalsIgnoreCase("MA"))
				SA=false;
			else
				assert false: "INVALID SETTING "+NODE_ROLE;
		}
		if (s.contains(REQUEST_INTERVAL)){
			int[] interval = s.getCsvInts(REQUEST_INTERVAL,2);
			this.min_interval = interval[0];
			this.max_interval = interval[1];
			this.current_interval=this.min_interval;
		}
		if (s.contains(SEED)){
			this.seed = s.getInt(SEED);
		}
		if (s.contains(METHOD)){
			this.lagrange = s.getBoolean(METHOD);
		}
		if (s.contains(NUM_MINIBUNDLES)){
			this.num_minibundles = s.getInt(NUM_MINIBUNDLES);
		}
		if (s.contains(REQUEST_SIZE)) {
			this.requestSize = s.getInt(REQUEST_SIZE);
		}
		if (s.contains(ALLOWABLE_COST_PER_SIZE)) {
			double[] costs = s.getCsvDoubles(ALLOWABLE_COST_PER_SIZE,2);
			max_max_cost=costs[1];
			min_max_cost=costs[0];
			max_cost=random_in_range(min_max_cost, max_max_cost);
		}
		if (s.contains(MESSAGES_RANGE)) {
			int[] size = s.getCsvInts(MESSAGES_RANGE,2);
			this.minSize = size[0];
			this.maxSize = size[1];
		}
		if (s.contains(COST_PER_SIZE)){
			double[] cost = s.getCsvDoubles(COST_PER_SIZE,3);
			PARANETS_application.wlan_cost = cost[0];
			PARANETS_application.cellular_cost = cost[1];
			PARANETS_application.satellite_cost = cost[2];
		}
		/*if (s.contains(MA_RANGE)){
			int[] range = s.getCsvInts(MA_RANGE,2);
			this.MA_Min = range[0];
			this.MA_Max = range[1];
		}*/
		if (s.contains(SA_ADDRESS)){
			PARANETS_application.SA_ID = s.getInt(SA_ADDRESS);
		}
		if (s.contains(WLAN_MSG_SIZE)){
			PARANETS_application.wlan_msg_size = s.getInt(WLAN_MSG_SIZE);
		}
		if (s.contains(NOMINAL_TH)){
			double[] nominals = s.getCsvDoubles(NOMINAL_TH,3);
			for (int i=0;i<this.conditions.length;i++)
			{
				this.conditions[i]=new network_conditions();
				conditions[i].setInitially(nominals[i]);
			}
			wlan_nominal=nominals[0];
			cellular_nominal=nominals[1];
			satellite_nominal=nominals[2];
		}
		super.setAppID(APP_ID);

		fuzzyH=new fuzzyHelper();


	}
	
	/** 
	 * Copy-constructor
	 * 
	 * @param a
	 */
	public PARANETS_application(PARANETS_application a) {
		super(a);
		this.lastRequest = a.lastRequest;
		this.min_interval = a.min_interval;
		this.max_interval = a.max_interval;
		this.SA = a.SA;
		this.seed = a.seed;
		this.lagrange=a.lagrange;
		/*this.MA_Min=a.MA_Min;
		this.MA_Max=a.MA_Max;*/
		//this.SA_ID=a.SA_ID;
		this.requestSize=a.requestSize;
		this.minSize=a.minSize;
		this.maxSize=a.maxSize;
		this.num_minibundles=a.num_minibundles;
		this.current_interval=a.current_interval;
		this.rng = new Random(this.seed);
		this.msg_count=0;
		this.max_cost=random_in_range(min_max_cost, max_max_cost);
		for (int i=0;i<this.conditions.length;i++)
			this.conditions[i]=new network_conditions();
		this.conditions[0].setInitially(wlan_nominal);
		this.conditions[1].setInitially(cellular_nominal);
		this.conditions[2].setInitially(satellite_nominal);
		rng = new Random(this.hashCode());
		fuzzyH=new fuzzyHelper();
	}
	
	/** 
	 * Handles an incoming message. 
	 * 
	 * @param msg	message received by the router
	 * @param host	host to which the application instance is attached
	 */
	@Override
	public Message handle(Message msg, DTNHost host) {
		String type = (String)msg.getProperty(TYPE_PROPERTY);
		if (type==null) return msg; // Not a PARANET message
		
		// Respond with data if role is SA
		if (msg.getTo()==host && type.equalsIgnoreCase(TYPE_REQUEST))
		{
			assert (SA);
			
			//String interf=(String)msg.getProperty(ParanetAdaptableFuzzySprayAndWaitRouter.INTERFACE_PROPERTY);
			double wlan_size = (Double)msg.getProperty(ParanetAdaptableFuzzySprayAndWaitRouter.INTERFACE_WLAN);
			double cellular_size = (Double)msg.getProperty(ParanetAdaptableFuzzySprayAndWaitRouter.INTERFACE_CELLULAR);
			double satellite_size = (Double)msg.getProperty(ParanetAdaptableFuzzySprayAndWaitRouter.INTERFACE_SATELLITE);
			int size=(int)(wlan_size+cellular_size+satellite_size);
			//assert (wlan_size!=null && cellular_size !=null && satellite_size !=null);
			String id = msg.getId()+"-data";
			if ((int)wlan_size+(int)cellular_size+(int)satellite_size!=size)
				wlan_size=size-(int)cellular_size-(int)satellite_size;
			int [] sizes  ={(int)wlan_size,(int)cellular_size,(int)satellite_size};
			String [] interfaces={ParanetAdaptableFuzzySprayAndWaitRouter.INTERFACE_WLAN,
									ParanetAdaptableFuzzySprayAndWaitRouter.INTERFACE_CELLULAR,
									ParanetAdaptableFuzzySprayAndWaitRouter.INTERFACE_SATELLITE};
			super.sendEventToListeners("GotRequest",msg, msg.getFrom());
			//int size=0;
			for (int i=0;i<sizes.length;i++)
			{
				int curr_size=sizes[i];
				if (curr_size==0)
					continue;
				int number=1;
				if (interfaces[i].equals(ParanetAdaptableFuzzySprayAndWaitRouter.INTERFACE_WLAN))
						number=(int) (wlan_size / wlan_msg_size);
				for (int n=0;n<number;n++)
				{
					if (interfaces[i].equals(ParanetAdaptableFuzzySprayAndWaitRouter.INTERFACE_WLAN))
					{
						if (n==number-1)
							curr_size=(int)wlan_size - (int) ((number-1)*(int)(wlan_size/number));
						else
							curr_size=(int)(wlan_size/number);
					}
					Message m = new Message(host, msg.getFrom(), id+"-"+interfaces[i]+"."+n,curr_size );
					m.addProperty(TYPE_PROPERTY, TYPE_DATA);
					m.addProperty(ParanetAdaptableFuzzySprayAndWaitRouter.INTERFACE_PROPERTY, interfaces[i]);
					m.setAppID(APP_ID);
					host.createNewMessage(m);
					super.sendEventToListeners("SentData", m, msg.getFrom());
					//size+=sizes[i];
				}
			}
			//super.sendEventToListeners("SentData", ""+size, msg.getFrom());
		}
		
		// Received a data reply
		if (msg.getTo()==host && type.equalsIgnoreCase(TYPE_DATA) ) {
			assert(!SA);
			//String interface_=(String) msg.getProperty(ParanetAdaptableFuzzySprayAndWaitRouter.INTERFACE_PROPERTY);
			super.sendEventToListeners("GotData", msg, host);
		}
		return msg;
	}

	/** 
	 * Draws a random host from the destination range
	 * 
	 * @return host
	 */
	private DTNHost randomHost(int min,int max) {
		int destaddr = 0;
		if (max == min) {
			destaddr = min;
		}
		else
		{
			destaddr = min + rng.nextInt(max - min);
		}
		World w = SimScenario.getInstance().getWorld();
		return w.getNodeByAddress(destaddr);
	}

	private double random_in_range(double min, double max) {
		if (max == min) {
			return min;
		}
		else if (max>min)
		{
			return min + rng.nextDouble()*(max - min);
		}
		else
		{
			assert max>=min: "Invalid range";
			return -1;
		}
	}

	private int random_in_range(int min, int max) {
		if (max == min) {
			return min;
		}
		else if (max>min)
		{
			return min + rng.nextInt(max - min);
		}
		else
		{
			assert max>=min: "Invalid range";
			return -1;
		}
	}

	@Override
	public Application replicate() {
		return new PARANETS_application(this);
	}

	/** 
	 * Sends a request for data packet if this is an MA application instance.
	 * 
	 * @param host to which the application instance is attached
	 */
	@Override
	public void update(DTNHost host) {
		if (SA) return;
		double curTime = SimClock.getTime();
		if (curTime - this.lastRequest >= this.current_interval) {
			// Time to send a new request
			msg_count++;
			Message m = new Message(host, randomHost(SA_ID,SA_ID), host.getAddress()+"."+msg_count,	requestSize);
			m.addProperty(TYPE_PROPERTY, TYPE_REQUEST);
			m.addProperty(ParanetAdaptableFuzzySprayAndWaitRouter.INTERFACE_PROPERTY, ParanetAdaptableFuzzySprayAndWaitRouter.INTERFACE_CELLULAR); //can be changed later (means that request sent on the cellualar channel)
			
			int data_size=random_in_range(minSize, maxSize);
			m.addProperty("data-size", data_size );
			message_divisions d=solve_optimization_problem(data_size);
			m.addProperty(ParanetAdaptableFuzzySprayAndWaitRouter.INTERFACE_WLAN, d.wlan);
			m.addProperty(ParanetAdaptableFuzzySprayAndWaitRouter.INTERFACE_CELLULAR,d.cellular);
			m.addProperty(ParanetAdaptableFuzzySprayAndWaitRouter.INTERFACE_SATELLITE, d.satellite);
			m.setAppID(APP_ID);
			host.createNewMessage(m);
			
			// Call listeners
			super.sendEventToListeners("SentRequest", m, host);
			
			this.lastRequest = curTime;
			this.current_interval=random_in_range(min_interval, max_interval); //next interval
		}
	}
};
