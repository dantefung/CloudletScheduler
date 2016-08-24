package org.cloudbus.cloudsim;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.strategy.VmCloudletAssignerLearning;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

public class Test {

	private static final int NUM_VM = 10;                   //虚拟机的数目
//	private static final int NUM_VM = 1000;
	private static final int NUM_CLOUDLET = 10000;          //云任务的总数
//	private static final int NUM_CLOUDLET = 100;          //云任务的总数
	//private static final int NUM_CLOUDLET = 20000;          //云任务的总数
	private static final double POISSON_LAMBDA = 10.0;      //泊松分布的λ值
	private static final double LETS_WAVE_INTERVAL = 200.0; //各波云任务间的时间间隔（ms）
	private static final int MAX_LENGTH_WAITING_QUEUE = 50; //各台虚拟机中的云任务等待队列的最长长度
//	private static final double LEARNING_GAMMA = 0.5;       //强化学习算法的γ值
//	private static final double LEARNING_ALPHA = 0.5;       //强化学习算法的α值	
//	private static final double LEARNING_EPSILON = 0.01;    //强化学习算法的ε值
	
	private static final double LEARNING_ALPHA = 0.999999999999999;       //强化学习算法的α值
	private static final double LEARNING_GAMMA = 0.99999999999999;       //强化学习算法的γ值
	private static final double LEARNING_EPSILON = 0.000000099999999999;
	
	

	public static void main(String[] args) {                //主函数
		Log.printLine("Starting...");

		try {
			int num_user = 1;
			Calendar calendar = Calendar.getInstance();
			boolean trace_flag = false;

			CloudSim.init(num_user, calendar, trace_flag);

			int numHost = 1;    //设置物理机数目
			int numVm = NUM_VM; //设置虚拟机数目
			QDatacenter datacenter0 = createDatacenter("Datacenter_0", numHost, //创建数据中心
					numVm);

			double gamma = LEARNING_GAMMA;     //设置强化学习算法的γ值
			double alpha = LEARNING_ALPHA;     //设置强化学习算法的α值
			double epsilon = LEARNING_EPSILON; //设置强化学习算法的ε值
//			VmCloudletAssigner vmCloudletAssigner = new VmCloudletAssignerGreedy(); //设置需要使用的云任务分配算法
			VmCloudletAssigner vmCloudletAssigner = new VmCloudletAssignerLearning(gamma, alpha, epsilon); //设置需要使用的云任务分配算法
//			VmCloudletAssigner vmCloudletAssigner = new VmCloudletAssignerFair();
//			VmCloudletAssigner vmCloudletAssigner = new VmCloudletAssignerRandom();
			int numlets = NUM_CLOUDLET;     //设置云任务总数
			int numPe = NUM_VM;             //根据虚拟机数目设置核芯数目
			double lambda = POISSON_LAMBDA; //设置泊松分布的λ值
			double numletWaveInterval = LETS_WAVE_INTERVAL;            //设置各波任务的时间间隔
			int cloudletWaitingQueueLength = MAX_LENGTH_WAITING_QUEUE; //设置各虚拟机中云任务等待队列的最大长度
			QDatacenterBroker globalBroker = new QDatacenterBroker(    //创建云任务代理
					"QDatacenterBroker", vmCloudletAssigner, numlets, numPe,
					lambda, numletWaveInterval, cloudletWaitingQueueLength);

			VirtualQueueSize.init(NUM_VM, cloudletWaitingQueueLength); //初始化虚拟队列
			CloudSim.startSimulation(); //开始模拟

			List<Cloudlet> newList = new LinkedList<Cloudlet>(); //创建记录云任务运行结果的列表
			//HashMap<Integer, Double> waitingTimeList = new HashMap<Integer, Double>();
			// int numVm = datacenter0.getVmList().size();

			newList.addAll(globalBroker.getCloudletReceivedList()); //接收处理完的云任务
			
			/*List<Cloudlet> sameList = globalBroker.getSameList();
			Log.printLine("Print the same CLoudletList:"+(sameList==null)+" size "+sameList.size());
			for(Cloudlet cl:sameList)
				Log.print(cl.getCloudletLength()+" ");
			Log.printLine();*/
			// for (int i = 0; i < numVm; i++) {
			// waitingTimeList.put(datacenter0.getVmList().get(i).getId(),
			// ((QCloudletSchedulerSpaceShared) datacenter0.getVmList()
			// .get(i).getCloudletScheduler()).getAverageWaitingTime());
			// }
			
			CloudSim.stopSimulation(); //结束模拟
			Log.printLine("Total Cloudlets: "+ numlets); //输出云任务
			printCloudletList(newList);                  //的总数
			
//			doAnalysis(globalBroker);
			
			// System.out.println("以下是每个虚拟机的平均等待时间：");
			// for (int i = 0; i < numVm; i++) {
			// System.out.println("Vm#" + i + ": " + waitingTimeList.get(i));
			// }

			Log.printLine("finished!");
		} catch (Exception e) { //云模拟出现不可知的错误
			e.printStackTrace();
			Log.printLine("The simulation has been terminated due to an unexpected error");
		}
	}

	private static QDatacenter createDatacenter(String name, int numHost, //创建数据中心
			int numPe) {

		List<Host> hostList = new ArrayList<Host>(); //物理机列表
		List<Pe> peList = new ArrayList<Pe>();       //核芯列表

		int mips = 1000; //设置核芯的计算能力

		for (int i = 0; i < numPe; i++) { //创建核芯
			peList.add(new Pe(i, new PeProvisionerSimple(mips)));
		}

		int hostId = 0;         //设置物理机的id
		int ram = 16384;        //设置物理机内存
		long storage = 1000000; //设置物理机存储空间
		int bw = 10000;         //设置物理机带宽

		for (int i = 0; i < numHost; i++) { //创建物理机
			hostList.add(new Host(hostId, new RamProvisionerSimple(ram),
					new BwProvisionerSimple(bw), storage, peList,
					new VmSchedulerTimeShared(peList)));

			hostId++;
		}

		String arch = "x86"; // system architecture
		String os = "Linux"; // operating system
		String vmm = "Xen";
		double time_zone = 10.0; // time zone this resource located
		double cost = 3.0; // the cost of using processing in this resource
		double costPerMem = 0.05; // the cost of using memory in this resource
		double costPerStorage = 0.1; // the cost of using storage in this
										// resource
		double costPerBw = 0.1; // the cost of using bw in this resource
		LinkedList<Storage> storageList = new LinkedList<Storage>(); 

		DatacenterCharacteristics characteristics = new DatacenterCharacteristics( //设置数据中心的特征
				arch, os, vmm, hostList, time_zone, cost, costPerMem,
				costPerStorage, costPerBw);

		QDatacenter datacenter = null;
		try {
			datacenter = new QDatacenter(name, characteristics,              //创建数据中心
					new VmAllocationPolicySimple(hostList), storageList, 0);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return datacenter;
	}

	private static void printCloudletList(List<Cloudlet> list) { //输出云模拟后的云任务执行结果
		int size = list.size();
		Cloudlet cloudlet;
		List<Double> finishTimeEachVm = new ArrayList<Double>();         //每台虚拟机中最后一个云任务离开等待队列的时间
		List<Double> totalWaitingTimeEachVm = new ArrayList<Double>();   //每台虚拟机中所有云任务在等待队列中的总等待时间
		List<Double> totalUtilizingTimeEachVm = new ArrayList<Double>(); //每台虚拟机中所有云任务消耗的总时间
		List<Integer> numCloudletsEachVm = new ArrayList<Integer>();     //每台虚拟机中执行的云任务的总数量
		
		for (int i = 0; i < NUM_VM; i++) {
			finishTimeEachVm.add(0.0);
			totalWaitingTimeEachVm.add(0.0);
			totalUtilizingTimeEachVm.add(0.0);
			numCloudletsEachVm.add(0);
		}

		String indent = "    ";
		int success = 0;
		Log.printLine();
		Log.printLine("========== OUTPUT ==========");
		Log.printLine("Cloudlet ID" + indent + "STATUS" + indent
				+ "Data center ID" + indent + "VM ID" + indent + indent
				+ "Time" + indent + "Submission Time" + indent + "Start Time"
				+ indent + "Finish Time");

		DecimalFormat dft = new DecimalFormat("###.##");
		for (int i = 0; i < size; i++) {
			cloudlet = list.get(i);
			// Log.print(indent + cloudlet.getCloudletId() + indent + indent);

			if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS) {
				success++;
				totalWaitingTimeEachVm.set(cloudlet.getVmId(),                                //计算每台物理机中所有云任务的在队列中等待的总时间
						totalWaitingTimeEachVm.get(cloudlet.getVmId()) 
						+ cloudlet.getWaitingTime());
//						+ cloudlet.getExecStartTime() - cloudlet.getSubmissionTime());
				if (finishTimeEachVm.get(cloudlet.getVmId()) < cloudlet.getExecStartTime()) { //获取每台物理机最后一个云任务离开等待队列的时间
					finishTimeEachVm.set(cloudlet.getVmId(), cloudlet.getExecStartTime());
				}
				totalUtilizingTimeEachVm.set(cloudlet.getVmId(),                              //计算每台物理机中所有云任务的总消耗时间
						totalUtilizingTimeEachVm.get(cloudlet.getVmId()) 
						+ cloudlet.getFinishTime() - cloudlet.getSubmissionTime());
				numCloudletsEachVm.set(cloudlet.getVmId(),                                    //计算每台物理机中的云任务总数
						numCloudletsEachVm.get(cloudlet.getVmId()) + 1);
				/*
				 * Log.printLine( indent + indent + cloudlet.getResourceId() +
				 * indent + indent + indent + cloudlet.getVmId() + indent +
				 * indent + indent + dft.format(cloudlet.getActualCPUTime()) +
				 * indent + indent + indent +
				 * dft.format(cloudlet.getSubmissionTime()) + indent + indent +
				 * dft
				 * .format(cloudlet.getExecStartTime()-cloudlet.getSubmissionTime
				 * ())+ indent + indent + indent +
				 * dft.format(cloudlet.getFinishTime()));
				 */
			}else{
				
			}								
		}
		for (int i = 0; i < NUM_VM; i++) {                                                                //输出每台物理机单位时间内等待队列的平均队长
			System.out.println("Vm #" + i + "的平均队长：" + (int) (totalWaitingTimeEachVm.get(i) / finishTimeEachVm.get(i)) + " lets/ms");
//			System.out.println("Host #" + i + "分子：" + totalWaitingTimeEachHost.get(i) + " ms");
//			System.out.println(totalWaitingTimeEachHost.get(i) / numCloudletsEachHost.get(i));
//			System.out.println("Host #" + i + "分母：" + finishTimeEachHost.get(i) + " ms");
		}
		for (int i = 0; i < NUM_VM; i++) {                                                                //输出每台物理机所有任务的平均消耗时间
			System.out.println("Vm #" + i + "的所有任务平均消耗时间：" 
					+ (int) (totalUtilizingTimeEachVm.get(i) / numCloudletsEachVm.get(i)) + " ms");
		}
		for (int i = 0; i < NUM_VM; i++) {                                                                //输出每台物理机单位时间内等待队列的平均等待时间
			System.out.println("Vm #" + i + "的平均等待时间：" + totalWaitingTimeEachVm.get(i) / numCloudletsEachVm.get(i) + " ms");
		}		
		


		Log.printLine("Number of Success cloudlet : " + success);
	}
	
	
	private static void doAnalysis(DatacenterBroker broker)
	{
		List<Vm> vmlist = broker.getVmList();
		List<Cloudlet> cloudletlist = broker.getCloudletReceivedList();
		double[] time_vm = new double[vmlist.size()];
		double[] time_cloudlet = new double[cloudletlist.size()];
		for(int i=0;i<time_vm.length;i++)
		{
			time_vm[i] = 0;
		}
		double totalTime = 0;
		double avgTime_vm = 0;
		double avgTime_cloudlet = 0;
		double sigma2_vm = 0;
		double sigma2_cloudlet = 0;
		int clIndex = 0;
		for(clIndex = 0; clIndex < cloudletlist.size(); clIndex++)
		{
			Cloudlet cl = cloudletlist.get(clIndex);
			int vmId = cl.getVmId();
			Vm vm = null;
			int vmIndex = 0;
			for(vmIndex=0; vmIndex<vmlist.size(); vmIndex++)
			{
				vm = vmlist.get(vmIndex);
				if(vm.getId() == vmId) break;
			}
			double time = cl.getCloudletLength()/vm.getMips();// 计算vm完成该任务所需要的时间
			totalTime += time;// 所有虚拟机完成所有任务总耗时
			time_vm[vmIndex] += time;// 每台虚拟机完成自己的所有作业总耗时
			time_cloudlet[clIndex] = time;// 每个任务在自己所在的虚拟机上被处理完成所用时间
		}
		avgTime_vm = totalTime / vmlist.size();// 每台虚拟机处理作业的平均用时
		avgTime_cloudlet = totalTime / cloudletlist.size();// 每个作业被完成平均用时
		
		/*  标准差是反映一组数据离散程度最常用的一种量化形式，是表示精确度的重要指标。说起标准差首先得搞清楚它出现的目的。
		 *  我们使用方法去检测它，但检测方法总是有误差的，所以检测值并不是其真实值。
		 *  检测值与真实值之间的差距就是评价检测方法最有决定性的指标。但是真实值是多少，不得而知。
		 *  因此怎样量化检测方法的准确性就成了难题。这也是临床工作质控的目的：保证每批实验结果的准确可靠。
	     *	虽然样本的真实值是不可能知道的，但是每个样本总是会有一个真实值的，不管它究竟是多少。可以想象，一个好的检测方法，
         *	其检测值应该很紧密的分散在真实值周围。如果不紧密，与真实值的距离就会大，准确性当然也就不好了，不可能想象离散度大的方法，
         *	会测出准确的结果。因此，离散度是评价方法的好坏的最重要也是最基本的指标。
		 * */
		
		//////////////////////////////////////////////sigama计算///////////////////////////////////////////////////////////
		// σ用来描述任一过程参数的平均值的分布或离散程度。 详见sigama的计算公式
		for (int i=0; i<vmlist.size(); i++)// vmlist.size() 样本个数
		{
			sigma2_vm += (time_vm[i] - avgTime_vm) * (time_vm[i] - avgTime_vm);// 样本值-样本均值
		}
		sigma2_vm /= vmlist.size();// 方差
		
		for (int i=0; i<cloudletlist.size(); i++)
		{
			sigma2_cloudlet += (time_cloudlet[i] - avgTime_cloudlet) * (time_cloudlet[i] - avgTime_cloudlet) ;
		}
		
		sigma2_cloudlet /= cloudletlist.size();
		//////////////////////////////////////////////////////////////////////////////////////////////////////////
		DecimalFormat dft = new DecimalFormat("###.##");
		dft.setRoundingMode(RoundingMode.HALF_UP);
//		System.out.println("totalTime:" + dft.format(totalTime));
		System.out.println("avgTime_cloudlet:" + dft.format(avgTime_cloudlet));
		System.out.println("sigma cloudlet:" + dft.format(Math.sqrt(sigma2_cloudlet)));
		System.out.println("avgTime_vm:" + dft.format(avgTime_vm));
		System.out.println("sigma vm:" + dft.format(Math.sqrt(sigma2_vm)));
	}
	
	private static void printCloudletList(List<Cloudlet> list,List<Integer> idList) {
		int size = list.size();
		Cloudlet cloudlet;

		String indent = "    ";
		int success = 0;
		Log.printLine();
		Log.printLine("========== OUTPUT ==========");
		Log.printLine("Cloudlet ID" + indent + "STATUS" + indent
				+ "Data center ID" + indent + "VM ID" + indent + indent
				+ "Time" + indent + "Submission Time" + indent + "Start Time"
				+ indent + "Finish Time");

		DecimalFormat dft = new DecimalFormat("###.##");
		Log.printLine("Failed Cloudlet");
		for (int i = 0; i < size; i++) {
			cloudlet = list.get(i);
			// Log.print(indent + cloudlet.getCloudletId() + indent + indent);

			if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS) {
				success++;//Log.printLine("ss ");
				/*
				 * Log.print("SUCCESS");
				 * 
				 * Log.printLine( indent + indent + cloudlet.getResourceId() +
				 * indent + indent + indent + cloudlet.getVmId() + indent +
				 * indent + indent + dft.format(cloudlet.getActualCPUTime()) +
				 * indent + indent + indent +
				 * dft.format(cloudlet.getSubmissionTime()) + indent + indent +
				 * dft
				 * .format(cloudlet.getExecStartTime()-cloudlet.getSubmissionTime
				 * ())+ indent + indent + indent +
				 * dft.format(cloudlet.getFinishTime()));
				 */
			}else{
				
			}
			
			if(idList.contains(cloudlet.getCloudletId()))
				idList.remove(new Integer(cloudlet.getCloudletId()));
				
		}
		
		Log.printLine("Unsuccess CLoudlet#");
		for(int i = 0; i < idList.size(); i++){
			Log.print(" "+idList.get(i)+" ");
			if((i+1)%10==0)
				Log.printLine();
		}
		
		Log.printLine("Number of unSuccess cloudlet : "+idList.size());
		Log.printLine("Number of Success cloudlet : " + success);
	}	
	
}
