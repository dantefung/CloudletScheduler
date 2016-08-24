package org.cloudbus.cloudsim;

import java.util.List;
import java.util.Map;

public class VmCloudletAssignerFair extends VmCloudletAssigner { // 公平分配策略

	@Override
	public List<Cloudlet> cloudletAssign(List<Cloudlet> cloudletList,
			List<Vm> vmList) {
		if (vmList != null || vmList.size() != 0) {
			List<Cloudlet> toAssignCloudletList = getToAssignCloudletList(cloudletList); // 初始化等待分配任务队列
			if (toAssignCloudletList.size() < 1) { // 没有等待分配的任务，返回空列表
				return null;
				// System.exit(0);
			}

			int m = vmList.size(); // 虚拟机数
			int n = toAssignCloudletList.size(); // 即将分配任务列表数
			int maxCloudletsWaitingLength = vQueueSize.getMaxLength(); // 子任务队列最大长度
			List<Map<String, Integer>> vmWaitingQueueSizeList = initVmWaitingQueueSizeList();// 初始化虚拟子队列队长列表

			/*
			 * Log.printLine("Queue size Print Before n=" + n); for (int i = 0;i
			 * < m; i++) { Log.print(vmWaitingQueueSizeList.get(i).get("size") +
			 * " "); } Log.printLine("\nvirQueueSize"); for (int i = 0; i < m;
			 * i++) { Log.print(virQueueSize.get(i) + " "); }
			 */

			int i;
			for (i = 0; i < n; i++) { // 分配任务到适合的虚拟机
				int index = 0;
				int mSize = maxCloudletsWaitingLength + 1;
				for (int j = 0; j < m; j++) {
					if (mSize > vmWaitingQueueSizeList.get(j).get("size")) { // 寻找最空的虚拟机队列
						mSize = vmWaitingQueueSizeList.get(j).get("size");
						index = j;
					}
				}
				// /////////////////////////
				for (int j = 0; j < m; j++) { // 输出所有虚拟机等待队列的长度
					System.out.print(vmWaitingQueueSizeList.get(j).get("size")
							+ " ");
				}
				System.out.println();
				// //////////////////////////
				if (mSize >= maxCloudletsWaitingLength) { // 所有虚拟机的等待队列都满了
					Log.printLine("mSize=50 list(0):" + mSize);
					break;
				}

				/*
				 * Log.printLine("\nLOOP I:" + i); for (int j = 0; j < m; j++) {
				 * Log.print(vmWaitingQueueSizeList.get(j).get("size") + " "); }
				 */

				// System.out.println("一个云任务分配Vm成功");
				int id = vmWaitingQueueSizeList.get(index).get("id"); // 被选中的虚拟机的id

				if (vQueueSize.increment(id)) { // 决断是否能正确分配到被选中的虚拟机中，虚拟机的子队列队长队长加一
					vmWaitingQueueSizeList.get(index).put("size", ++mSize); // 更新虚拟机等待队列长度列表状态
					toAssignCloudletList.get(i).setVmId(id); // 将该任务分配给被选中的虚拟机

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
				} else { // 被选中的虚拟机的等待队列已满
					Log.printLine(index + "Index Assign Full Error!! Vm#" + id
							+ " mSize:" + mSize + " vQueueSize:"
							+ vQueueSize.getQueueSize().get(id));
					System.exit(0);
				}

			}

			List<Cloudlet> assignedCloudletList = getAssignedCloudletList(i,
					toAssignCloudletList); // 获取被成功分配的任务列表

			finishAssign(toAssignCloudletList); // 结束分配

			Log.printLine("Assign Finished! Left:"
					+ getGlobalCloudletWaitingQueue().size() + " Success:"
					+ assignedCloudletList.size());

			return assignedCloudletList;

		} else { // 没有可用的虚拟机
			Log.printLine("VmCloudletAssignerFair No VM Error!!");
			return null;
		}
	}

}
