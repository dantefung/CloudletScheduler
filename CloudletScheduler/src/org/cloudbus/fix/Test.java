package org.cloudbus.fix;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.VirtualQueueSize;
import org.cloudbus.cloudsim.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.VmCloudletAssigner;
import org.cloudbus.cloudsim.VmCloudletAssignerFair;
import org.cloudbus.cloudsim.VmCloudletAssignerGreedy;
import org.cloudbus.cloudsim.VmCloudletAssignerRandom;
import org.cloudbus.cloudsim.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.utils.GenExcel;

public class Test {

	private static final int NUM_VM = 10; // 虚拟机的数目
	private static final int NUM_CLOUDLET = 2000; // 云任务的总数
	private static final double POISSON_LAMBDA = 10.0; // 泊松分布的λ值
	private static final double LETS_WAVE_INTERVAL = 200.0; // 各波云任务间的时间间隔（ms）
	private static final int MAX_LENGTH_WAITING_QUEUE = 50; // 各台虚拟机中的云任务等待队列的最长长度
	private static final double LEARNING_GAMMA = 0.5; // 强化学习算法的γ值
	private static final double LEARNING_ALPHA = 0.5; // 强化学习算法的α值
	private static final double LEARNING_EPSILON = 0.5; // 强化学习算法的ε值
	
//	private static final double LEARNING_EPSILON = 1; // 强化学习算法的ε值
	private static final int VM_MIPS[] = { 1000, 1100, 1200, 1300, 1400, 1500,
			2000, 2500, 3000, 4000 };

	public static void main(String[] args) { // 主函数
		Log.printLine("Starting...");

		try {
			int num_user = 1;
			Calendar calendar = Calendar.getInstance();
			boolean trace_flag = false;

			CloudSim.init(num_user, calendar, trace_flag);
			if (VM_MIPS.length != NUM_VM) {
				System.out.println("虚拟机MIPS数目与虚拟机数量不符，程序结束");
				System.exit(0);
			}
			int numHost = 1;
			int numVm = NUM_VM;
			int vmMips[] = VM_MIPS;
			QDatacenter datacenter0 = createDatacenter("Datacenter_0", numHost,
					numVm, vmMips);

			double gamma = LEARNING_GAMMA; // 设置强化学习算法的γ值
			double alpha = LEARNING_ALPHA; // 设置强化学习算法的α值
			double epsilon = LEARNING_EPSILON; // 设置强化学习算法的ε值
//			VmCloudletAssigner vmCloudletAssigner = new VmCloudletAssignerRandom();
			VmCloudletAssigner vmCloudletAssigner = new VmCloudletAssignerLearning(gamma, alpha, epsilon, GenExcel.getInstance());
//			VmCloudletAssigner vmCloudletAssigner = new VmCloudletAssignerGreedy();
//			VmCloudletAssigner vmCloudletAssigner = new VmCloudletAssignerFair();
			int numlets = NUM_CLOUDLET; // 设置云任务总数
			int numPe = NUM_VM; // 根据虚拟机数目设置核芯数目
			double lambda = POISSON_LAMBDA; // 设置泊松分布的λ值
			double numletWaveInterval = LETS_WAVE_INTERVAL; // 设置各波任务的时间间隔
			int cloudletWaitingQueueLength = MAX_LENGTH_WAITING_QUEUE; // 设置各虚拟机中云任务等待队列的最大长度
			QDatacenterBroker globalBroker = new QDatacenterBroker(
					"QDatacenterBroker", vmCloudletAssigner, numlets, numPe,
					lambda, numletWaveInterval, vmMips,
					cloudletWaitingQueueLength);

			VirtualQueueSize.init(NUM_VM, cloudletWaitingQueueLength); // 初始化虚拟队列
			CloudSim.startSimulation(); // 开始模拟

			GenExcel.getInstance().genExcel();
			
			List<Cloudlet> newList = new LinkedList<Cloudlet>(); // 创建记录云任务运行结果的列表
			// HashMap<Integer, Double> waitingTimeList = new HashMap<Integer,
			// Double>();
			// int numVm = datacenter0.getVmList().size();

			newList.addAll(globalBroker.getCloudletReceivedList()); // 接收处理完的云任务

			/*
			 * List<Cloudlet> sameList = globalBroker.getSameList();
			 * Log.printLine
			 * ("Print the same CLoudletList:"+(sameList==null)+" size "
			 * +sameList.size()); for(Cloudlet cl:sameList)
			 * Log.print(cl.getCloudletLength()+" "); Log.printLine();
			 */
			// for (int i = 0; i < numVm; i++) {
			// waitingTimeList.put(datacenter0.getVmList().get(i).getId(),
			// ((QCloudletSchedulerSpaceShared) datacenter0.getVmList()
			// .get(i).getCloudletScheduler()).getAverageWaitingTime());
			// }

			CloudSim.stopSimulation(); // 结束模拟
			Log.printLine("Total Cloudlets: " + numlets); // 输出云任务
			printCloudletList(newList); // 的总数

			// System.out.println("以下是每个虚拟机的平均等待时间：");
			// for (int i = 0; i < numVm; i++) {
			// System.out.println("Vm#" + i + ": " + waitingTimeList.get(i));
			// }

			Log.printLine("finished!");
		} catch (Exception e) { // 云模拟出现不可知的错误
			e.printStackTrace();
			Log.printLine("The simulation has been terminated due to an unexpected error");
		}
	}

	private static QDatacenter createDatacenter(String name, int numHost,
			int numPe, int vmMips[]) {

		List<Host> hostList = new ArrayList<Host>();
		List<Pe> peList = new ArrayList<Pe>();

		int maxMips = 0;
		for (int mip : vmMips)
			if (mip > maxMips)
				maxMips = mip;
		for (int i = 0; i < numPe; i++) {
			peList.add(new Pe(i, new PeProvisionerSimple(maxMips)));
		}

		int hostId = 0; // 设置物理机的id
		int ram = 16384; // 设置物理机内存
		long storage = 1000000; // 设置物理机存储空间
		int bw = 10000; // 设置物理机带宽

		for (int i = 0; i < numHost; i++) { // 创建物理机
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

		DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
				// 设置数据中心的特征
				arch, os, vmm, hostList, time_zone, cost, costPerMem,
				costPerStorage, costPerBw);

		QDatacenter datacenter = null;
		try {
			datacenter = new QDatacenter(name, characteristics, // 创建数据中心
					new VmAllocationPolicySimple(hostList), storageList, 0);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return datacenter;
	}

	private static void printCloudletList(List<Cloudlet> list) { // 输出云模拟后的云任务执行结果
		int size = list.size();
		Cloudlet cloudlet;
		List<Double> finishTimeEachVm = new ArrayList<Double>(); // 每台虚拟机中最后一个云任务离开等待队列的时间
		List<Double> totalWaitingTimeEachVm = new ArrayList<Double>(); // 每台虚拟机中所有云任务在等待队列中的总等待时间
		List<Double> totalUtilizingTimeEachVm = new ArrayList<Double>(); // 每台虚拟机中所有云任务消耗的总时间
		List<Integer> numCloudletsEachVm = new ArrayList<Integer>(); // 每台虚拟机中执行的云任务的总数量

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
				totalWaitingTimeEachVm.set(cloudlet.getVmId(), // 计算每台物理机中所有云任务的在队列中等待的总时间
						totalWaitingTimeEachVm.get(cloudlet.getVmId())
								+ cloudlet.getWaitingTime());
				// + cloudlet.getExecStartTime() -
				// cloudlet.getSubmissionTime());
				if (finishTimeEachVm.get(cloudlet.getVmId()) < cloudlet
						.getExecStartTime()) { // 获取每台物理机最后一个云任务离开等待队列的时间
					finishTimeEachVm.set(cloudlet.getVmId(),
							cloudlet.getExecStartTime());
				}
				totalUtilizingTimeEachVm.set(
						cloudlet.getVmId(), // 计算每台物理机中所有云任务的总消耗时间
						totalUtilizingTimeEachVm.get(cloudlet.getVmId())
								+ cloudlet.getFinishTime()
								- cloudlet.getSubmissionTime());
				numCloudletsEachVm.set(cloudlet.getVmId(), // 计算每台物理机中的云任务总数
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
			} else {

			}
		}
		for (int i = 0; i < NUM_VM; i++) { // 输出每台物理机单位时间内等待队列的平均队长
			System.out.println("Vm #"
					+ i
					+ "的平均队长："
					+ (int) (totalWaitingTimeEachVm.get(i) / finishTimeEachVm
							.get(i)) + " lets/ms");
			// System.out.println("Host #" + i + "分子：" +
			// totalWaitingTimeEachHost.get(i) + " ms");
			// System.out.println(totalWaitingTimeEachHost.get(i) /
			// numCloudletsEachHost.get(i));
			// System.out.println("Host #" + i + "分母：" +
			// finishTimeEachHost.get(i) + " ms");
		}
		for (int i = 0; i < NUM_VM; i++) { // 输出每台物理机所有任务的平均消耗时间
			System.out
					.println("Vm #"
							+ i
							+ "的所有任务平均消耗时间："
							+ (int) (totalUtilizingTimeEachVm.get(i) / numCloudletsEachVm
									.get(i)) + " ms");
		}
		for (int i = 0; i < NUM_VM; i++) { // 输出每台物理机单位时间内等待队列的平均等待时间
			System.out.println("Vm #" + i + "的平均等待时间："
					+ totalWaitingTimeEachVm.get(i) / numCloudletsEachVm.get(i)
					+ " ms");
		}

		Log.printLine("Number of Success cloudlet : " + success);
	}

	private static void printCloudletList(List<Cloudlet> list,
			List<Integer> idList) {
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
				success++;// Log.printLine("ss ");
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
			} else {

			}

			if (idList.contains(cloudlet.getCloudletId()))
				idList.remove(new Integer(cloudlet.getCloudletId()));

		}

		Log.printLine("Unsuccess CLoudlet#");
		for (int i = 0; i < idList.size(); i++) {
			Log.print(" " + idList.get(i) + " ");
			if ((i + 1) % 10 == 0)
				Log.printLine();
		}

		Log.printLine("Number of unSuccess cloudlet : " + idList.size());
		Log.printLine("Number of Success cloudlet : " + success);
	}

}
