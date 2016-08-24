package org.cloudbus.cloudsim;

import java.util.List;
import java.util.Map;
import java.util.Random;

public class VmCloudletAssignerRandom extends VmCloudletAssigner { //随机分配策略

	@Override
	public List<Cloudlet> cloudletAssign(List<Cloudlet> cloudletList,
			List<Vm> vmList) {
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

			/*//打印虚拟子队列长度
			 * Log.printLine("Queue size Print Before n=" + n); for (int i = 0;i
			 * < m; i++) { Log.print(vmWaitingQueueSizeList.get(i).get("size") +
			 * " "); } Log.printLine("\nvirQueueSize"); for (int i = 0; i < m;
			 * i++) { Log.print(virQueueSize.get(i) + " "); }
			 */
			
			int i;
			for (i = 0; i < n; i++) {	        //对所有即将分配的任务
				int index = randomInt(0, m);	//随机选择一个虚拟机
				int mSize = vmWaitingQueueSizeList.get(index).get("size");
				if (mSize >= maxCloudletsWaitingLength) {// 若随机的队列满了，往最空的队列
					// 选择队列长度最小的
					for (int j = 0, tmp = maxCloudletsWaitingLength + 1; j < m; j++) {
						if (tmp > vmWaitingQueueSizeList.get(j).get("size")) {
							tmp = vmWaitingQueueSizeList.get(j).get("size");
							index = j;
						}
					}

					mSize = vmWaitingQueueSizeList.get(index).get("size");
					if (mSize >= maxCloudletsWaitingLength) {//所有子队列已满，跳出循环
						//Log.printLine("mSize=50 list(0):" + mSize);
						break;
					}

				}

				/*//输出各子队列长度
				 * Log.printLine("\nLOOP I:" + i); for (int j = 0; j < m; j++) {
				 * Log.print(vmWaitingQueueSizeList.get(j).get("size") + " "); }
				 */

				// System.out.println("一个云任务分配Vm成功");
				int id = vmWaitingQueueSizeList.get(index).get("id");//虚拟机ID
				if (vQueueSize.increment(id)) {//虚拟机的子队列队长队长加一
					vmWaitingQueueSizeList.get(index).put("size", ++mSize);
					toAssignCloudletList.get(i).setVmId(id);//设置任务的虚拟机ID
					
					/*//输出任务ID，虚拟机ID
					 * Log.printLine("Cloudlet#" +
					 * toAssignCloudletList.get(i).getCloudletId() + " vmid" +
					 * toAssignCloudletList.get(i).getVmId() + "VM#" + id +
					 * " size:" + vQueueSize.getQueueSize().get(id)); /* if
					 * (mSize == 50) Log.printLine("size==50 Vm#" + id +
					 * " Cloudlet#" +
					 * toAssignCloudletList.get(i).getCloudletId() + " itsVmid "
					 * + toAssignCloudletList.get(i).getVmId());
					 */
					
					//比较临时子队列和虚拟机子队列
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
			Log.printLine("VmCloudletAssignerRandom No VM Error!!");
			return null;
		}

	}

	private int randomInt(int min, int max) { // random[min,max] 可取min,可取max
		Random random = new Random();
		return random.nextInt(max) % (max - min + 1) + min;
	}

}
