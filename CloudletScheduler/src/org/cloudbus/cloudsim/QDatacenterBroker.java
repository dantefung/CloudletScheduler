package org.cloudbus.cloudsim;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.lists.VmList;

public class QDatacenterBroker extends DatacenterBroker {

	private VmCloudletAssigner vmCloudletAssigner;	// 给云任务分配物理机的策略

	private static final int CREATE_CLOUDLETS = 49;

	private static List<Double> delayList;			// 各波云任务提交的延迟时间列表
	private static List<Integer> numLetList;		// 各波云任务中云任务的数量
	private int currWave;							// 目前处于第几波云任务中
	private int numlets;							// 云任务的总数量
	private int numVm;								// 各物理机中虚拟机的数量
	private double lambda;							// 泊松分布的λ值
	private double numletWaveInterval;				// 两波云任务之间的时间间隔
	private static int cloudletWaitingQueueLength;	// 子任务队列的最大队长

	public QDatacenterBroker(String name,
			VmCloudletAssigner vmCloudletAssigner, int numlets, int numVm,
			double lambda, double numletWaveInterval,
			int cloudletWaitingQueueLength) throws Exception {
		super(name);
		setVmCloudletAssigner(vmCloudletAssigner);
		this.numlets = numlets;
		this.numVm = numVm;
		this.lambda = lambda;
		this.numletWaveInterval = numletWaveInterval;
		this.cloudletWaitingQueueLength = cloudletWaitingQueueLength;
		currWave = 0;
	}

	@Override
	public void processOtherEvent(SimEvent ev) {
		switch (ev.getTag()) {
		case CREATE_CLOUDLETS: // 新一波云任务的生成及到达
			int waveId = ((Integer) ev.getData()).intValue(); // 获取当前波数
			System.out.println(CloudSim.clock() + "：第" + (waveId + 1)
					+ "波cloudlet开始到达");

			submitCloudletList(createCloudlet(getId(), numLetList.get(waveId),
					waveId * 1000 + 1000)); // 生成一波云任务
			currWave++;
			if (waveId > 0) {		// 若不是第一波任务
				submitCloudlets();  // 那么不再初始化代理，直接提交新的任务
			}

			CloudSim.resumeSimulation();
			break;
		case QDatacenter.CLOUDLET_SUBMIT_FAILED:	//任务提交失败处理
			cloudletSubmitFailed(ev);
			break;
		default:
			Log.printLine(getName() + ": unknown event type");
			break;
		}
	}

	@Override
	public void startEntity() {	// 本代理开始工作
		Log.printLine(super.getName() + " is starting...");
		try {
			createCloudletWave(numlets, lambda, numletWaveInterval); // 生成各波云任务开始生成的延迟列表
		} catch (Exception e) {
			System.out.println("生成云任务队列出错！");
			e.printStackTrace();
		}
		setVmList(createVM(getId(), numVm, 0)); // 初始化虚拟机

		for (int i = 0; i < delayList.size(); i++) {
			schedule(getId(), delayList.get(i), CREATE_CLOUDLETS, i); // 安排云任务生成的任务
		}
		schedule(getId(), 0, CloudSimTags.RESOURCE_CHARACTERISTICS_REQUEST); // 请求资源，初始化本代理		
	}

	@Override
	protected void submitCloudlets() { // 向数据中心提交云任务
		// 将任务与虚拟机绑定
		Log.printLine("SSSSubmitCloudlet() size: " + getCloudletList().size());
		/*for(int i=0;i<getCloudletList().size();i++){
			Log.print(getCloudletList().get(i).getCloudletId()+" ");
			if((i+1)%10==0) Log.printLine();
		}*/

		List<Cloudlet> assignedCloudletList = getVmCloudletAssigner()
				.cloudletAssign(getCloudletList(), getVmList()); //由任务分配器分配任务到虚拟机
		
		/*
		for (Cloudlet cloudlet : assignedCloudletList)
			Log.print(cloudlet.getCloudletId() + " ");
		Log.printLine();
		for (Cloudlet cloudlet : assignedCloudletList)
			Log.print(cloudlet.getVmId() + "     ");
		Log.printLine("Broker:CloudletSubmit " + assignedCloudletList.size());
		*/
		for (Cloudlet cloudlet : assignedCloudletList) {
			Vm vm;
			if (cloudlet.getVmId() != -1) {
				vm = VmList.getById(getVmsCreatedList(), cloudlet.getVmId());
				if (vm == null) { // vm was not created
					Log.printLine(CloudSim.clock() + ": " + getName()
							+ ": Postponing execution of cloudlet "
							+ cloudlet.getCloudletId()
							+ ": bount VM not available");
					continue;
				}
				 Log.printLine(CloudSim.clock() + ": " + getName()
				 + ": Sending cloudlet " + cloudlet.getCloudletId()
				 + " to VM #" + vm.getId());
				sendNow(getVmsToDatacentersMap().get(vm.getId()),
						CloudSimTags.CLOUDLET_SUBMIT, cloudlet);
				cloudletsSubmitted++;
				getCloudletSubmittedList().add(cloudlet);
				
				/*检查任务是否重复
				 * for(Cloudlet cl:getCloudletSubmittedList()){
					if(cl.getCloudletId()==cloudlet.getCloudletId()){
						Log.printLine("SubmitList Error!cl#"+cl.getCloudletId());
						continue;
						//System.exit(0);
					}
				}*/
				
			}					
		}
		
		getCloudletList().clear(); //任务已经提交到任务分配器，要么已分配，要么进行主任务队列

	}

	@Override
	protected void processCloudletReturn(SimEvent ev) { // 处理成功执行被返回的云任务
		Cloudlet cloudlet = (Cloudlet) ev.getData();
		getCloudletReceivedList().add(cloudlet);
		// Log.printLine(CloudSim.clock() + ": " + getName() + ": Cloudlet "
		// + cloudlet.getCloudletId() + " received");
		cloudletsSubmitted--;
		List<Cloudlet> assignedCloudletList = getVmCloudletAssigner()
				.cloudletAssign(null, getVmList());	//分配一个任务
		// 从主队列调度一个任务
		if (assignedCloudletList != null) {
			
			/* Log.printLine("Cloudlet Return Assign :"+getCloudletList().size())
			 * ; for (Cloudlet cl : getCloudletList()) {
			 * Log.print(cl.getVmId()+" "); }*/
			 
			for (Cloudlet cl : assignedCloudletList) {	//实际只有一个任务
				Vm vm;
				if (cl.getVmId() != -1) {
				vm = VmList.getById(getVmsCreatedList(), cl.getVmId());
				Log.printLine(CloudSim.clock() + ": " + getName()
						+ ": Sending cloudlet " + cl.getCloudletId()
						+ " to VM #" + vm.getId());
				sendNow(getVmsToDatacentersMap().get(vm.getId()),
						CloudSimTags.CLOUDLET_SUBMIT, cl);
				cloudletsSubmitted++;
				getCloudletSubmittedList().add(cl);
				
				}else{
					//Log.printLine("assignedCloudletList Assign Error! Cloudlet#"+cl.getCloudletId());
				}

			}
		} else {
			//Log.printLine("CloudletReturn Assign NULL!");
		}		 
		
		/*//打印实际子队列长度
		 * Log.print("BrokerQueueLeft:");		
		for (Vm vm : getVmsCreatedList()) {
			Log.print(" "
					+ ((QCloudletSchedulerSpaceShared) vm
							.getCloudletScheduler()).getCloudletWaitingQueue()
							.size() + " ");
		}
		// Log.printLine();
		//打印虚拟子队列长度
		List<Integer> virQueueSize = VirtualQueueSize.getInstance()
				.getQueueSize();
		Log.print("VirtualQueue Left:");
		for (int i = 0; i < virQueueSize.size(); i++) {
			Log.print(virQueueSize.get(i) + " ");
		}
		Log.printLine();
		*/
		
		if (getCloudletList().size() == 0 && cloudletsSubmitted == 0) { // 所有已提交的任务运行完
			if (currWave < delayList.size()) {
				System.out.println("后面还有" + (delayList.size() - currWave)
						+ "波任务没有到达。。。。");
				return;
			}
			//if (getVmCloudletAssigner().getGlobalCloudletWaitingQueue().size() != 0)
				//return;
			Log.printLine(CloudSim.clock() + ": " + getName()
					+ ": All Cloudlets executed. Finishing...");
			clearDatacenters();
			finishExecution();
		} else { // 一些任务还没执行完
			if (getCloudletList().size() > 0 && cloudletsSubmitted == 0) {
				for (Vm vm : getVmsCreatedList()) {
					Log.printLine("QueueLeft:"
							+ ((QCloudletSchedulerSpaceShared) vm
									.getCloudletScheduler())
									.getCloudletWaitingQueue().size());
				}
				if (currWave < delayList.size()) {
					System.out.println("刚提交了第" + currWave + "波任务。。。。");
					return;
				}
				clearDatacenters();
				createVmsInDatacenter(0);
			}
		}

	}

	protected void cloudletSubmitFailed(SimEvent ev) {
		Cloudlet cloudlet = (Cloudlet) ev.getData();
		cloudletsSubmitted--;
		Log.printLine("\nQDatacenterBroker received CLOUDLET "
				+ cloudlet.getCloudletId() + " Failed cloudletList Size:"
				+ getCloudletList().size());
		List<Cloudlet> cloudletList = new ArrayList<Cloudlet>();
		cloudletList.add(cloudlet);
		submitCloudletList(cloudletList);
		Log.printLine("After submit cloudletList Size:"
				+ getCloudletList().size());
		submitCloudlets();
	}
	
	public VmCloudletAssigner getVmCloudletAssigner() {
		return vmCloudletAssigner;
	}

	public void setVmCloudletAssigner(VmCloudletAssigner vmCloudletAssigner) {
		this.vmCloudletAssigner = vmCloudletAssigner;
	}

	private static List<Vm> createVM(int userId, int vms, int idShift) {// 创建虚拟机
		LinkedList<Vm> list = new LinkedList<Vm>();

		long size = 10000; // image size (MB)
		int ram = 512; // vm memory (MB)
		int mips = 1000;// 250;
		long bw = 1000;
		int pesNumber = 1; // number of cpus
		String vmm = "Xen"; // VMM name

		Vm[] vm = new Vm[vms];

		for (int i = 0; i < vms; i++) {
			vm[i] = new Vm(idShift + i, userId, mips, pesNumber, ram, bw, size,
					vmm, new QCloudletSchedulerSpaceShared(idShift + i,
							cloudletWaitingQueueLength));
			list.add(vm[i]);
		}

		return list;
	}

	public static List<Cloudlet> createCloudlet(int userId, int cloudlets,
			int idShift) { // 生成云任务列表
		LinkedList<Cloudlet> list = new LinkedList<Cloudlet>();

		long length = 20000;
		long fileSize = 0;
		long outputSize = 0;
		int pesNumber = 1;
		UtilizationModel utilizationModel = new UtilizationModelFull();

		Cloudlet[] cloudlet = new Cloudlet[cloudlets];

		for (int i = 0; i < cloudlets; i++) {
			cloudlet[i] = new Cloudlet(idShift + i, length, pesNumber,
					fileSize, outputSize, utilizationModel, utilizationModel,
					utilizationModel);
			cloudlet[i].setUserId(userId);
			list.add(cloudlet[i]);
			
		}
		
		return list;
	}

	public static double f_Poisson(double lambda, int k) {// 泊松分布
		double e = 2.7182818284;
		double result;

		result = Math.pow(e, -lambda) * Math.pow(lambda, k);
		for (int i = 1; i <= k; i++) {
			result = result / i;
		}

		return result;
	}

	public static void createCloudletWave(int numlets, double lambda,
			double numletWaveInterval) throws Exception {// 生成Broker的延迟序列delayList
		int numLetWave = 100;
		ArrayList<Integer> numLet = new ArrayList<Integer>();
		int tmpCloudlets = 0;
		double tmp = numlets;

		for (int i = 0; i <= numLetWave; i++) {
			numLet.add(i, (int) (tmp * f_Poisson(lambda, i)));
			if (numLet.get(i) <= 0) {
				numLet.set(i, 1);
			}
			if ((tmpCloudlets + numLet.get(i)) > numlets) {
				numLet.set(i, numlets - tmpCloudlets);
				System.out.println("超过总任务数，调整为：numLet[" + i + "]: "
						+ numLet.get(i) + "\tlambda: " + lambda
						+ "\tf_Poisson: " + f_Poisson(lambda, i));
				break;
			}
			tmpCloudlets += numLet.get(i);
			System.out.println("numLet[" + i + "]: " + numLet.get(i)
					+ "\tlambda: " + lambda + "\tf_Poisson: "
					+ f_Poisson(lambda, i));
		}

		delayList = new LinkedList<Double>();
		numLetList = new LinkedList<Integer>();
		for (int i = 0; i < numLet.size(); i++) {
			delayList.add(i, numletWaveInterval * i);
			numLetList.add(i, numLet.get(i));
		}
	}

	/*//根据id找任务
	 * private Cloudlet findCloudletById(List<Cloudlet> cloudletList,int cloudletId){
		for (Cloudlet cl : cloudletList) {
			if (cl.getCloudletId() == cloudletId) {
				return cl;
			}
		}
		//Log.printLine("findCloudletById() find null");
		return null;
	}*/
	
	public static List<Integer> getNumLetList() {
		return numLetList;
	}

	public static void setNumLetList(List<Integer> numLetList) {
		QDatacenterBroker.numLetList = numLetList;
	}


}
