package org.cloudbus.cloudsim;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class VmCloudletAssignerLearning extends VmCloudletAssigner { //强化学习策略

	private VirtualQueueSize vQueueSize = VirtualQueueSize.getInstance();
	
	private static BufferedWriter bw;
	private static String[][] result=new String[10][10];
	private static int count=0;
	
	private static double gamma;   //强化学习算法的γ值
	private static double alpha;   //强化学习算法的α值
	private static double epsilon; //强化学习算法的ε值
	private static Map<String, Map<Integer, Double>> QList = new HashMap<String, Map<Integer, Double>>(); //Q值表

	public VmCloudletAssignerLearning( double gamma, double alpha, double epsilon) {
		this.gamma = gamma;
		this.alpha = alpha;
		this.epsilon = epsilon;
	}

	@Override
	public List<Cloudlet> cloudletAssign(List<Cloudlet> cloudletList, List<Vm> vmList) {
		if (vmList != null || vmList.size() != 0) {
			List<Cloudlet> toAssignCloudletList = getToAssignCloudletList(cloudletList); //初始化等待分配任务队列
			if (toAssignCloudletList.size() < 1) { //没有等待分配的任务，返回空列表
				return null;
//				System.exit(0);
			}

			int m = vmList.size();	//虚拟机数
			int n = toAssignCloudletList.size();	//即将分配任务列表数
			int maxCloudletsWaitingLength = vQueueSize.getMaxLength();	//子任务队列最大长度
			List<Map<String, Integer>> vmWaitingQueueSizeList = initVmWaitingQueueSizeList();//初始化虚拟子队列队长列表

			/*
			 * Log.printLine("Queue size Print Before n=" + n); for (int i = 0;i
			 * < m; i++) { Log.print(vmWaitingQueueSizeList.get(i).get("size") +
			 * " "); } Log.printLine("\nvirQueueSize"); for (int i = 0; i < m;
			 * i++) { Log.print(virQueueSize.get(i) + " "); }
			 */
			
			int i;
			int numFreeVm = m;//空闲的vm数
			List<Map<String, Integer>> tmpSizeList = updateTmpSizeList(-1, numFreeVm, vmWaitingQueueSizeList);//临时队列
			for (i = 0; i < n; i++) { //分配任务到适合的虚拟机
				int index = createAction(numFreeVm, tmpSizeList);
				int mSize = tmpSizeList.get(index).get("size");
				if (mSize >= maxCloudletsWaitingLength) {// 若选择的队列满了，去掉这个队列，重新选择
					
					if (numFreeVm > 1) {//如果空闲的队列数还可以减为1或以上，则更新临时队列，即抛掉已满的队列
						tmpSizeList = updateTmpSizeList(index, numFreeVm--, tmpSizeList);
//						System.out.println(numFreeVm);
						i--;
						continue;
					}
					else { //所有虚拟机的等待队列都满了
						Log.printLine("mSize=50 list(0):" + mSize);
						break;
					}
					
//					//寻找最空的队列作为要分配云任务的目的vm
//					for (int j = 0, tmp = maxCloudletsWaitingLength + 1; j < m; j++) {
//						if (tmp > vmWaitingQueueSizeList.get(j).get("size")) {
//							tmp = vmWaitingQueueSizeList.get(j).get("size");
//							index = j;
//						}
//					}
//					mSize = vmWaitingQueueSizeList.get(0).get("size");

//					//非排序手法获取最空队列的mSize
//					mSize = vmWaitingQueueSizeList.get(index).get("size");
//					if (mSize >= maxCloudletsWaitingLength) {
//						Log.printLine("mSize=50 list(0):" + mSize);
//						break;
//					}

				}

				/*
				 * Log.printLine("\nLOOP I:" + i); for (int j = 0; j < m; j++) {
				 * Log.print(vmWaitingQueueSizeList.get(j).get("size") + " "); }
				 */

				// System.out.println("一个云任务分配Vm成功");
//				int id = vmWaitingQueueSizeList.get(index).get("id");
				int id = tmpSizeList.get(index).get("id"); //被选中的虚拟机的id

				if (vQueueSize.increment(id)) { //决断是否能正确分配到被选中的虚拟机中，虚拟机的子队列队长队长加一
					tmpSizeList.get(index).put("size", ++mSize); //更新临时虚拟机等待队列长度列表状态
					for (int j = 0; j < m; j++) {                //更新虚拟机等待队列长度列表状态
						if (vmWaitingQueueSizeList.get(j).get("id") == tmpSizeList.get(index).get("id")) {
							vmWaitingQueueSizeList.get(j).put("size", mSize);
							index = j;
							break;
						}
					}
					toAssignCloudletList.get(i).setVmId(id); //将该任务分配给被选中的虚拟机

					updateQList(index, m, vmList, vmWaitingQueueSizeList); //更新Q值表
					/*
					 * Log.printLine("Cloudlet#" +
					 * toAssignCloudletList.get(i).getCloudletId() + " vmid" +
					 * toAssignCloudletList.get(i).getVmId() + "VM#" + id +
					 * " size:" + vQueueSize.getQueueSize().get(id)); /* if
					 * (mSize == 50) Log.printLine("size==50 Vm#" + id +
					 * " Cloudlet#" +
					 * toAssignCloudletList.get(i).getCloudletId() + " itsVmid "
					 * + toAssignCloudletList.get(i).getVmId());
					 */
					
					// Log.printLine("Two Sizes:"
					// + vQueueSize.getQueueSize().get(id) + " "
					// + vmWaitingQueueSizeList.get(index).get("size"));
				} else { //被选中的虚拟机的等待队列已满
					Log.printLine(index + "Index Assign Full Error!! Vm#" + id
							+ " mSize:" + mSize + " vQueueSize:"
							+ vQueueSize.getQueueSize().get(id));
					System.exit(0);
				}

			}
			
			List<Cloudlet> assignedCloudletList = getAssignedCloudletList(i, toAssignCloudletList); //获取被成功分配的任务列表

			finishAssign(toAssignCloudletList); //结束分配

			Log.printLine("Assign Finished! Left:"
					+ getGlobalCloudletWaitingQueue().size() + " Success:"
					+ assignedCloudletList.size());
			
			return assignedCloudletList;

		} else { //没有可用的虚拟机
			Log.printLine("VmCloudletAssignerLearning No VM Error!!");
			return null;
		}
	}
	
	private int createAction(int numVm, List<Map<String, Integer>> vmWaitingQueueSizeList) { //生成选择虚拟机的动作，即获得想要的虚拟机号
		int current_action;        //生成的动作，即要选择的虚拟机
		int x = randomInt(0, 100); //生成随机数[0,100]
		String state_idx = createState_idx(numVm, vmWaitingQueueSizeList); //根据各虚拟机等待队列当前状态状态生成的Q值表行号
		if (!QList.containsKey(state_idx)) { //若Q值表中不存在这一行，则初始化这一行
			initRowOfQList(state_idx, numVm);
		}
		
//		/////////////////
//		System.out.println("\n输出state_idx\n" + state_idx);
//		/////////////////
		
		//根据随机数x选择生成动作的方式
		if (((double) x / 100) < (1 - epsilon)) { //生成动作方式1：利用(exploit)
			int umax = 0;
			double tmp = -1.0;
			for (int i = 0; i < numVm; i++) { //选择当前状态行中Q值最大的列号，即要选择的虚拟机号
				if (tmp < QList.get(state_idx).get(i)) {
					tmp = QList.get(state_idx).get(i);
					umax = i;
				}
			}
			if (tmp == -1) { //利用动作没有正常进行
				System.out.println("exploit没有正常进行。。！");
				System.exit(0);
			}
			current_action = umax;
		}
		else{ //生成动作方式2：学习(explore)
			current_action = randomInt(0, numVm - 1); //随机生成动作
		}
		return current_action;
	}
	
	private void updateQList(int action_idx, int numVm, List<Vm> vmList, List<Map<String, Integer>> vmWaitingQueueSizeList) { //更新Q值表
		double reward = 1.0 / ((QCloudletSchedulerSpaceShared) vmList                      //由被选中的虚拟机中的任务平均等待时间的倒数生成的reward值
				.get(action_idx).getCloudletScheduler()).getAverageWaitingTime();
		String state_idx = createLastState_idx(action_idx, numVm, vmWaitingQueueSizeList); //没有将当前任务分配到虚拟机队列时的状态行号
		String next_state_idx = createState_idx(numVm, vmWaitingQueueSizeList);            //将当前任务分配到虚拟机队列后的状态行号
		
//		/////////////////
//		System.out.println("\n输出state_idx\n" + state_idx);
//		System.out.println("\n输出next_state_idx\n" + next_state_idx);
//		/////////////////
		
		if (!QList.containsKey(next_state_idx)) { //若更新后的行不存在于Q值表中，则初始化它
			initRowOfQList(next_state_idx, numVm);
		}
		double QMaxNextState = -1.0;
		for (int i = 0; i < numVm; i++) { //获取更新后的状态行的最大值
			if (QMaxNextState < QList.get(next_state_idx).get(i)) {
				QMaxNextState = QList.get(next_state_idx).get(i);
			}
		}
		double QValue = QList.get(state_idx).get(action_idx) //Q值表Q值更新的主要公式
				+ alpha * (reward + gamma * QMaxNextState - QList.get(state_idx).get(action_idx));
		QList.get(state_idx).put(action_idx, QValue);
	}
	
	private int randomInt(int min, int max) { // random[min,max] 可取min,可取max
		if (min == max) {
			return min;
		}
		Random random = new Random();
		return random.nextInt(max) % (max - min + 1) + min;
	}
	
	private String createLastState_idx(int action_idx, int numVm, //生成更新前的状态行行号
			List<Map<String, Integer>> vmWaitingQueueSizeList) {
		String state_idx = "";
		for (int i = 0; i < numVm; i++) { //根据虚拟机数目获得列数并循环处理
			if (i == action_idx) { //若该行为变化的行，则获取它变化前的状态
				state_idx += "-" + (vmWaitingQueueSizeList.get(i).get("size").intValue() - 1);
			}
			else { //没有变化的行
				state_idx += "-" + vmWaitingQueueSizeList.get(i).get("size").intValue();
			}
		}
		return state_idx;
	}
	
	private String createState_idx(int numVm, List<Map<String, Integer>> vmWaitingQueueSizeList) { //生成当前状态行行号
		String state_idx = "";
		for (int i = 0; i < numVm; i++) { //根据虚拟机数目获得列数并循环处理
			state_idx += "-" + vmWaitingQueueSizeList.get(i).get("size").intValue();
		}
		return state_idx;
	}
	
	private void initRowOfQList(String state_idx, int numColumn) { //初始化Q值表的行
		QList.put(state_idx, new HashMap<Integer, Double>());
		for (int i = 0; i < numColumn; i++) {
			QList.get(state_idx).put(i, 0.0); //赋初值为0
		}
	}
	
	private List<Map<String, Integer>> updateTmpSizeList(int index, int numFreeVm, //更新临时虚拟机等待队列长度列表的状态
			List<Map<String, Integer>> originSizeList) {
		List<Map<String, Integer>> tmp = new ArrayList<Map<String, Integer>>();
		for (int j = 0; j < numFreeVm; j++) {
			if (index == -1 || originSizeList.get(j).get("id") != originSizeList.get(index).get("id")) { //将被选中的虚拟机（等待队列已满的虚拟机）
				tmp.add(originSizeList.get(j));                                                          //从该临时列表中去掉
			}
		}
		return tmp;
	}

}
