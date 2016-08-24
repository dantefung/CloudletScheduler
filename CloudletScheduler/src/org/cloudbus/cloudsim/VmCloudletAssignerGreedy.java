package org.cloudbus.cloudsim;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class VmCloudletAssignerGreedy extends VmCloudletAssigner {

	@Override
	public List<Cloudlet> cloudletAssign(List<Cloudlet> cloudletList,
			List<Vm> vmList) {

		if (vmList != null || vmList.size() != 0) {
			
			List<Cloudlet> toAssignCloudletList = getToAssignCloudletList(cloudletList); // 初始化等待分配任务队列
			List<Map<String, Integer>> vmWaitingQueueSizeList = initVmWaitingQueueSizeList();// 初始化虚拟机中云任务等待队列队长列表
			
			if (toAssignCloudletList.size() < 1) { // 没有等待分配的任务，返回空列表
				return null;
				// System.exit(0);
			}
	
			int maxCloudletsWaitingLength = vQueueSize.getMaxLength(); // 子任务队列最大长度
			
			//虚拟机升序排列
			Collections.sort(vmList, new VmComparator());
			//云任务单元降序排列
			Collections.sort(toAssignCloudletList, new CloudletComparator());
			
			//计算出矩阵time[i][j]
			int vmNum = vmList.size();
			int clNum = toAssignCloudletList.size();
			double[][] time = new double[clNum][vmNum];
			for(int i=0;i<clNum;i++)
				for(int j=0;j<vmNum;j++)
				{
					time[i][j] = (double)toAssignCloudletList.get(i).getCloudletLength()/vmList.get(j).getMips();
				}			
			
			double[] vmLoad = new double[vmNum];
			double[] vmTask = new double[vmNum];
			double minLoad = 0;
			int index = 0; // 被选中的虚拟机的id
			vmLoad[vmNum-1] = time[0][vmNum-1];
			vmTask[vmNum-1] = 1;
			toAssignCloudletList.get(0).setVmId(vmList.get(vmNum-1).getId());
			
			int i;
			
			for(i=1;i<clNum;i++)
			{
				minLoad = vmLoad[vmNum-1]+time[i][vmNum-1];
				index=vmNum-1;
				int mSize = maxCloudletsWaitingLength + 1;
				for(int j = vmNum-1;j>=0;j--)
				{
					if(minLoad > vmLoad[j] + time[i][j])
					{
						minLoad = vmLoad[j]+time[i][j];
						index=j;
					}
					else if(minLoad==vmLoad[j]+time[i][j] && vmTask[j]<vmTask[index])
					index = j;
				}
				vmLoad[index]+=time[i][index];
				vmTask[index]++;			
				
				mSize = vmWaitingQueueSizeList.get(index).get("size");
				
				// /////////////////////////
				for (int j = 0; j < vmNum; j++) { // 输出所有虚拟机等待队列的长度
					System.out.print(vmWaitingQueueSizeList.get(j).get("size")
							+ " ");
				}
				System.out.println();
				// //////////////////////////
				if (mSize >= maxCloudletsWaitingLength) { // 所有虚拟机的等待队列都满了
					Log.printLine("mSize=50 list(0):" + mSize);
					break;
				}
				
				if (vQueueSize.increment(index)) { // 决断是否能正确分配到被选中的虚拟机中，虚拟机的子队列队长队长加一
					vmWaitingQueueSizeList.get(index).put("size", ++mSize); // 更新虚拟机等待队列长度列表状态
					toAssignCloudletList.get(i).setVmId(index); // 将该任务分配给被选中的虚拟机
				}else { // 被选中的虚拟机的等待队列已满
					Log.printLine(index + "Index Assign Full Error!! Vm#" + index
							+ " mSize:" + mSize + " vQueueSize:"
							+ vQueueSize.getQueueSize().get(index));
					System.exit(0);
				}
			}
			
			List<Cloudlet> assignedCloudletList = getAssignedCloudletList(i-1,
					toAssignCloudletList); // 获取被成功分配的任务列表

			finishAssign(toAssignCloudletList); // 结束分配

			Log.printLine("Assign Finished! Left:"
					+ getGlobalCloudletWaitingQueue().size() + " Success:"
					+ assignedCloudletList.size());

			return assignedCloudletList;
		}
		else { // 没有可用的虚拟机
			Log.printLine("VmCloudletAssignerFair No VM Error!!");
			return null;
		}
		
	}
	
	private class CloudletComparator implements Comparator<Cloudlet>
	{   
		@Override
		public int compare(Cloudlet cl1, Cloudlet cl2)
		{
			return (int)(cl2.getCloudletLength() - cl1.getCloudletLength());
		}

	}
	 
	private class VmComparator implements Comparator<Vm>
	{
		public int compare(Vm vm1, Vm vm2)
		{
			return (int)(vm1.getMips() - vm2.getMips());
		}
	}

}
