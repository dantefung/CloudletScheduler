package org.cloudbus.cloudsim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public abstract class VmCloudletAssigner {

	protected VirtualQueueSize vQueueSize = VirtualQueueSize.getInstance(); // 虚拟子队列长度全局变量
	protected static Queue<Cloudlet> globalCloudletWaitingQueue = new LinkedList<Cloudlet>(); //主任务队列

	public abstract List<Cloudlet> cloudletAssign(List<Cloudlet> cloudletList, //云任务分配策略
			List<Vm> vmList);
	
	public static Queue<Cloudlet> getGlobalCloudletWaitingQueue() { //获取全局任务等待队列
		return globalCloudletWaitingQueue;
	}

	public static void setGlobalCloudletWaitingQueue( //设置全局任务等待队列
			Queue<Cloudlet> globalCloudletWaitingQueue) {
		VmCloudletAssigner.globalCloudletWaitingQueue = globalCloudletWaitingQueue;
	}

	protected List<Cloudlet> getToAssignCloudletList(List<Cloudlet> cloudletList) { //生成等待分配的云任务列表
		List<Cloudlet> toAssignCloudletList = new ArrayList<Cloudlet>(); //等待配的云任务列表
		if (cloudletList != null) { //等待分配的云任务数量不为0
			System.out.println("分配cloudletList中的任务 " + cloudletList.size());
			
			if (getGlobalCloudletWaitingQueue().size() != 0) { //全局任务等待队列不为空

				// Log.printLine("Global Cloudlet VMid "
				// + getGlobalCloudletWaitingQueue().size());
				// for (Cloudlet cloudlet : getGlobalCloudletWaitingQueue())
				// {
				// Log.print(cloudlet.getVmId() + " ");
				// }
				// Log.printLine();
				
				// Error:toAssignCloudletList.addAll(getGlobalCloudletWaitingQueue());
				// getGlobalCloudletWaitingQueue().clear();

				for (int i = 0; i < getGlobalCloudletWaitingQueue().size(); i++) //把全局云任务等待队列中
					toAssignCloudletList                                         //的任务放入
							.add(getGlobalCloudletWaitingQueue().poll());        //等待分配的云任务列表
			}

			/*int repeated = 0;
			for (Cloudlet cl : toAssignCloudletList) {
				for (int i = 0; i < cloudletList.size(); i++) {
					if (cl.getCloudletId() == cloudletList.get(i)
							.getCloudletId()) {
						Log.printLine("CLoudlet Repeated!!"
								+ cl.getCloudletId());
						repeated++;
					}
				}
			}

			if (repeated > 0) {
				Log.printLine("Repeated!!exit " + repeated);
				System.exit(0);
			}*/

			toAssignCloudletList.addAll(cloudletList); // 添加提交的任务为待分配任务

		} else {// cloudletList为null 从主队列中分配一个任务
			// System.out.println("从主队列中分配一个任务");
			if (getGlobalCloudletWaitingQueue().size() != 0) {
				toAssignCloudletList.add(getGlobalCloudletWaitingQueue()
						.poll());
			} else { //所有队列中都没有未分配的任务
				//Log.printLine("getGlobalCloudletWaitingQueue().size()=0 return null");
				return toAssignCloudletList;
			}
		}
		return toAssignCloudletList;
	}
	
	protected List<Map<String, Integer>> initVmWaitingQueueSizeList() { //初始化虚拟机等待队列长度列表
		List<Integer> virQueueSize = vQueueSize.getQueueSize();
		List<Map<String, Integer>> vmWaitingQueueSizeList = new ArrayList<Map<String, Integer>>();
		Map<String, Integer> queueSize;
		for (int i = 0; i < virQueueSize.size(); i++) {
			queueSize = new HashMap<String, Integer>();
			queueSize.put("id", i);
			queueSize.put("size", virQueueSize.get(i));
			vmWaitingQueueSizeList.add(queueSize);
			// Log.print(virQueueSize.get(i) + " ");
		}
		return vmWaitingQueueSizeList;
	}
	
	protected List<Cloudlet> getAssignedCloudletList(int success, List<Cloudlet> toAssignCloudletList) { //生成成功分配任务列表
		List<Cloudlet> assignedCloudletList = new ArrayList<Cloudlet>();// 成功分配任务列表
		for (int j = 0; j < success; j++)//前success个任务成功分配
			assignedCloudletList.add(toAssignCloudletList.get(j));
		toAssignCloudletList.removeAll(assignedCloudletList);// 删除成功分配任务
		return assignedCloudletList;
	}
	
	protected void finishAssign(List<Cloudlet> toAssignCloudletList){ //任务分配结束
		for (int j = 0; j < toAssignCloudletList.size(); j++) {	//未成功分配的任务重新塞回主任务队列
			getGlobalCloudletWaitingQueue().offer(
					toAssignCloudletList.get(j));
			//输出未成功分配任务
			// Log.print(toAssignCloudletList.get(j)
			// .getCloudletId() + " ");
			// if ((j + 1) % 10 == 0)
			// Log.printLine();

		}

	}

}
