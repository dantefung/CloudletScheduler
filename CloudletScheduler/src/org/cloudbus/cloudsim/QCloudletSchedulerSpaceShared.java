package org.cloudbus.cloudsim;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.cloudbus.cloudsim.core.CloudSim;

public class QCloudletSchedulerSpaceShared extends CloudletSchedulerSpaceShared {

	private int vmId;														// 虚拟机ID
	private VirtualQueueSize virQueueSize = VirtualQueueSize.getInstance();	// 虚拟子队列队长全局变量
	private double aveWaitingTime; 											// 平均等待时间
	private Queue<ResCloudlet> cloudletWaitingQueue; 						// 每个Vm的等待任务队列
	private int cloudletWaitingQueueLength; 								// 等待队列长度

	public QCloudletSchedulerSpaceShared(int vmId, int maxLength) {
		super();
		setAverageWaitingTime(0);
		cloudletWaitingQueue = new LinkedList<ResCloudlet>();
		setVmId(vmId);
		setCloudletWaitingQueueLength(maxLength);
	}

	@Override
	public double updateVmProcessing(double currentTime, List<Double> mipsShare) {
		setCurrentMipsShare(mipsShare);
		// Log.printLine("updateVmProcessing Vm#" + getVmId());
		double timeSpam = currentTime - getPreviousTime(); //时间差
		double capacity = 0.0;
		int cpus = 0;

		for (Double mips : mipsShare) { // count the CPUs available to the VMM
			capacity += mips;
			if (mips > 0) {
				cpus++;
			}
		}
		currentCpus = cpus;
		capacity /= cpus; // average capacity of each cpu

		// 更新任务进度 each machine in the exec list has the same amount of cpu
		for (ResCloudlet rcl : getCloudletExecList()) {
			rcl.updateCloudletFinishedSoFar((long) (capacity * timeSpam
					* rcl.getNumberOfPes() * Consts.MILLION));
		}

		// 任务等待队列为空，返回
		if (getCloudletExecList().size() == 0
				&& getCloudletWaitingQueue().size() == 0) {
			setPreviousTime(currentTime);
			return 0.0;
		}

		// 任务执行完毕，结束任务
		int finished = 0;
		List<ResCloudlet> toRemove = new ArrayList<ResCloudlet>();
		for (ResCloudlet rcl : getCloudletExecList()) {
			// finished anyway, rounding issue...
			if (rcl.getRemainingCloudletLength() == 0) {
				toRemove.add(rcl);
				cloudletFinish(rcl);
				finished++;
				
				// Log.printLine(" QueueLeft:Vm#" + getVmId() + " :"
				// + getCloudletWaitingQueue().size() + " ");
			}
		}
		getCloudletExecList().removeAll(toRemove);

		// 任务等待队列非空，从队列中选取任务执行
		if (!getCloudletWaitingQueue().isEmpty()) {
			for (int i = 0; i < finished; i++) {
				toRemove.clear();
				for (ResCloudlet rcl : getCloudletWaitingQueue()) {
					if ((currentCpus - usedPes) >= rcl.getNumberOfPes()) { // 注：这里的任务都是单核的，代码才兼容。
						rcl.setCloudletStatus(Cloudlet.INEXEC);
						updateAverageWaitingTime(rcl.getCloudlet().getWaitingTime()); // 更新任务等待时间

						/*//检查子队列实际长度和虚拟子队列长度是否一致
						 * if (getCloudletWaitingQueue().size() != virQueueSize
						 * .getQueueSize().get(getVmId()).intValue()) {
						 * Log.printLine("UPDATE Vm#" + getVmId() +
						 * " Queue Error syn: act:" +
						 * getCloudletWaitingQueue().size() + " vir:" +
						 * virQueueSize.getQueueSize()
						 * .get(getVmId()).intValue()); System.exit(0); }
						 */

						virQueueSize.decrement(getVmId());	//本子队列队长减一

						/*//输出子队列实际长度和虚拟子队列长度
						 * Log.printLine(" " + getCloudletWaitingQueue().size()
						 * + "ActualLength QCloudletScheduler Update After: VM#"
						 * + getVmId() + " " +
						 * virQueueSize.getQueueSize().get(getVmId()));
						 */

						/*//检查任务Id与虚拟机Id是否一致
						 * if (rcl.getCloudlet().getVmId() != getVmId()) {
						 * Log.printLine(" " + getCloudletWaitingQueue().size()
						 * + "ActualLength QCloudletScheduler Update After: VM#"
						 * + rcl.getCloudlet().getVmId() + " " + "SchedulerVm#"
						 * + getVmId() + " " + virQueueSize.getQueueSize().get(
						 * rcl.getCloudlet().getVmId()));
						 * Log.printLine("Error VMId" +
						 * rcl.getCloudlet().getCloudletId()); 
						 * System.exit(0); }
						 */
						// Log.printLine("VM # "+rcl.getCloudlet().getVmId()+" waitingTime: "+getAverageWaitingTime());
						
						for (int k = 0; k < rcl.getNumberOfPes(); k++) {
							rcl.setMachineAndPeId(0, k); // 注：cloudsim源码有错！
						}
						getCloudletExecList().add(rcl);
						usedPes += rcl.getNumberOfPes();
						getCloudletWaitingQueue().remove(rcl);
						
						// if(getCloudletWaitingQueue().size()==49)
						// Log.printLine("Act:49 "+"vir:"+virQueueSize.getQueueSize()
						// .get(getVmId()));
						
						break;
					}
				}
			}
		}

		// 估计执行中的任务的要完成还需要的时间
		double nextEvent = Double.MAX_VALUE;
		for (ResCloudlet rcl : getCloudletExecList()) {
			double remainingLength = rcl.getRemainingCloudletLength();
			double estimatedFinishTime = currentTime
					+ (remainingLength / (capacity * rcl.getNumberOfPes()));
			if (estimatedFinishTime - currentTime < CloudSim
					.getMinTimeBetweenEvents()) {
				estimatedFinishTime = currentTime
						+ CloudSim.getMinTimeBetweenEvents();
			}
			if (estimatedFinishTime < nextEvent) {
				nextEvent = estimatedFinishTime;
			}

		}
		setPreviousTime(currentTime);
		
		// Log.printLine("FinishedupdateVmProcessing Vm#" + getVmId());	
		return nextEvent;
	}

	@Override
	public double cloudletSubmit(Cloudlet cloudlet, double fileTransferTime) {
		// 虚拟机空闲，则直接执行
		if ((currentCpus - usedPes) >= cloudlet.getNumberOfPes()) {
			ResCloudlet rcl = new ResCloudlet(cloudlet);
			updateAverageWaitingTime(cloudlet.getWaitingTime());// 更新平均等待时间
			virQueueSize.decrement(getVmId());//本子队列队长减一
			
			/*//检查子队列实际长度和虚拟子队列长度是否一致
			  if (virQueueSize.getQueueSize().get(getVmId()).intValue() > 48
					&& getCloudletWaitingQueue().size() != virQueueSize
							.getQueueSize().get(getVmId()).intValue()) {
				Log.printLine("SUBMIT Queue Error syn: act:"
						+ getCloudletWaitingQueue().size() + " vir:"
						+ virQueueSize.getQueueSize().get(getVmId()).intValue());
				//System.exit(0);
			}*/
			
			/*//输出子队列实际长度和虚拟子队列长度
			 * Log.printLine(getCloudletWaitingQueue().size() +
			 * "ActualLength QCloudletScheduler Submit After: VM#" +
			 * rcl.getCloudlet().getVmId() + " " +
			 * virQueueSize.getQueueSize().get( rcl.getCloudlet().getVmId()));
			 */
			// Log.printLine("VM # "+cloudlet.getVmId()+" waitingTime: "+getAverageWaitingTime());
			
			rcl.setCloudletStatus(Cloudlet.INEXEC);
			for (int i = 0; i < cloudlet.getNumberOfPes(); i++) {
				rcl.setMachineAndPeId(0, i);
			}
			getCloudletExecList().add(rcl);
			usedPes += cloudlet.getNumberOfPes();
		} else {// 虚拟机忙，任务进入任务等待队列
			ResCloudlet rcl = new ResCloudlet(cloudlet);
			rcl.setCloudletStatus(Cloudlet.QUEUED);
			if (addWaitingCloudlet(rcl)) // 队列未满，添加成功
				return 0.0;
			else
				return -1.0; // 队列已满，添加失败

		}

		// 估算任务完成的时间
		double capacity = 0.0;
		int cpus = 0;
		for (Double mips : getCurrentMipsShare()) {
			capacity += mips;
			if (mips > 0) {
				cpus++;
			}
		}

		currentCpus = cpus;
		capacity /= cpus;

		// use the current capacity to estimate the extra amount of
		// time to file transferring. It must be added to the cloudlet length
		double extraSize = capacity * fileTransferTime;
		long length = cloudlet.getCloudletLength();
		length += extraSize;
		cloudlet.setCloudletLength(length);
		return cloudlet.getCloudletLength() / capacity;
	}

	public boolean addWaitingCloudlet(ResCloudlet cloudlet) { // 添加任务到等待队列
		/*//检查任务与子队列队长
		 * if (getCloudletWaitingQueue().size() > 48)
		 * Log.printLine("Add WaitingCloudlet SIze>48 :Cloudlet#" +
		 * cloudlet.getCloudletId() + " to Scheduler#" + getVmId() + " size:" +
		 * getCloudletWaitingQueue().size());
		 */
		if (getCloudletWaitingQueue().size() < getCloudletWaitingQueueLength()) {
			/*//检查任务Id与虚拟机Id是否一致
			 * Log.printLine("SchedulerVM#" + getVmId() + " clVM" +
			 * cloudlet.getCloudlet().getVmId() + "add CLoudlet#" +
			 * cloudlet.getCloudlet().getCloudletId() + " Size:" +
			 * getCloudletWaitingQueue().size());
			 */
			return getCloudletWaitingQueue().offer(cloudlet);
		} else {//子队列减一，由于算法实际上这里不会执行到。
			virQueueSize.decrement(getVmId());
			Log.printLine("ERROR:VM #" + cloudlet.getCloudlet().getVmId()
					+ " add Cloudlet #" + cloudlet.getCloudletId()
					+ " FAILDED!! Queue Size :"
					+ getCloudletWaitingQueue().size());
			System.exit(0);
			return false;
		}
	}

	public ResCloudlet removeWaitingCloudlet() {
		return cloudletWaitingQueue.poll();
	}

	private void updateAverageWaitingTime(double newWaitingTime) { // 更新任务等待时间
		setAverageWaitingTime((getAverageWaitingTime()
				* getCloudletFinishedList().size() + newWaitingTime)
				/ (getCloudletFinishedList().size() + 1));
	}

	public double getAverageWaitingTime() {
		return aveWaitingTime;
	}

	public void setAverageWaitingTime(double averageWaitingTime) {
		this.aveWaitingTime = averageWaitingTime;
	}

	public Queue<ResCloudlet> getCloudletWaitingQueue() {
		return cloudletWaitingQueue;
	}

	public int getCloudletWaitingQueueLength() {
		return cloudletWaitingQueueLength;
	}

	public void setCloudletWaitingQueueLength(int cloudletWaitingQueueLength) {
		this.cloudletWaitingQueueLength = cloudletWaitingQueueLength;
	}

	public int getVmId() {
		return vmId;
	}

	public void setVmId(int vmId) {
		this.vmId = vmId;
	}

}
