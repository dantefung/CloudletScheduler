package org.cloudbus.cloudsim;

import java.util.List;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;

public class QDatacenter extends Datacenter {

	public static final int CLOUDLET_SUBMIT_FAILED = 51;	//任务提交失败
	
	public QDatacenter(String name, DatacenterCharacteristics characteristics,
			VmAllocationPolicy vmAllocationPolicy, List<Storage> storageList,
			double schedulingInterval) throws Exception {
		super(name, characteristics, vmAllocationPolicy, storageList,
				schedulingInterval);
	}

	@Override
	protected void processCloudletSubmit(SimEvent ev, boolean ack) {
		updateCloudletProcessing();

		try {
			Cloudlet cl = (Cloudlet) ev.getData();
			//Log.printLine("Datacenter ---Cloudlet #"+cl.getCloudletId()+" SUBMIT! "+cl.getExecStartTime());
			//据代码，以下注释情况不会发生。
/*			// checks whether this Cloudlet has finished or not
			if (cl.isFinished()) {
				String name = CloudSim.getEntityName(cl.getUserId());
				Log.printLine(getName() + ": Warning - Cloudlet #" + cl.getCloudletId() + " owned by " + name
						+ " is already completed/finished.");
				Log.printLine("Therefore, it is not being executed again");
				Log.printLine();

				// NOTE: If a Cloudlet has finished, then it won't be processed.
				// So, if ack is required, this method sends back a result.
				// If ack is not required, this method don't send back a result.
				// Hence, this might cause CloudSim to be hanged since waiting
				// for this Cloudlet back.
				if (ack) {
					int[] data = new int[3];
					data[0] = getId();
					data[1] = cl.getCloudletId();
					data[2] = CloudSimTags.FALSE;

					// unique tag = operation tag
					int tag = CloudSimTags.CLOUDLET_SUBMIT_ACK;
					sendNow(cl.getUserId(), tag, data);
				}

				sendNow(cl.getUserId(), CloudSimTags.CLOUDLET_RETURN, cl);

				return;
			}
*/			

			// process this Cloudlet to this CloudResource
			cl.setResourceParameter(getId(), getCharacteristics().getCostPerSecond(), getCharacteristics()
					.getCostPerBw());

			int userId = cl.getUserId();
			int vmId = cl.getVmId();

			// time to transfer the files
			double fileTransferTime = predictFileTransferTime(cl.getRequiredFiles());

			Host host = getVmAllocationPolicy().getHost(vmId, userId);
			Vm vm = host.getVm(vmId, userId);
			//Log.printLine("Qdatacenter-Cloudlet#"+cl.getCloudletId()+" Vm#"+cl.getVmId());
			//任务提交到CloudletScheduler
			CloudletScheduler scheduler = vm.getCloudletScheduler();
			double estimatedFinishTime = ((QCloudletSchedulerSpaceShared)scheduler).cloudletSubmit(cl, fileTransferTime);

			// 提交成功
			if (estimatedFinishTime > 0.0 && !Double.isInfinite(estimatedFinishTime)) {
				estimatedFinishTime += fileTransferTime;
				send(getId(), estimatedFinishTime, CloudSimTags.VM_DATACENTER_EVENT);
			}else{// 提交失败，发送消息到broker重新提交
				if(estimatedFinishTime < 0.0){
					Log.printLine("Cloudlet submit Failed! QDatacenter sends Cloudlet Vm# "+cl.vmId+" to Broker");
					send(cl.getUserId(), estimatedFinishTime, CLOUDLET_SUBMIT_FAILED,cl);
					System.exit(0);
					}
			}

			if (ack) {
				int[] data = new int[3];
				data[0] = getId();
				data[1] = cl.getCloudletId();
				data[2] = CloudSimTags.TRUE;

				// unique tag = operation tag
				int tag = CloudSimTags.CLOUDLET_SUBMIT_ACK;
				sendNow(cl.getUserId(), tag, data);
			}
		} catch (ClassCastException c) {
			Log.printLine(getName() + ".processCloudletSubmit(): " + "ClassCastException error.");
			c.printStackTrace();
		} catch (Exception e) {
			Log.printLine(getName() + ".processCloudletSubmit(): " + "Exception error.");
			e.printStackTrace();
		}

		checkCloudletCompletion();
	}

}
