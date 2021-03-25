package model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ameren.trigger.Job;

//@Component
public class SnapshotManager implements Serializable{
	Logger logger = LoggerFactory.getLogger(SnapshotManager.class);
	private Map<Long, SnapshotPayload> snapshots = new HashMap<>();
	private Map<Integer, Long> runSnapshotIdsMap = new HashMap<>();
//	private List<Integer> runNumber = new ArrayList<>(); 
	private List<Long> snapshotIds = new ArrayList<>();
	private Long currentSnapshotId = 0L;
	
	public SnapshotPayload getSnapshot(long snapshotId){
		if(snapshots.get(snapshotId) == null) {
			SnapshotPayload snapshot = new SnapshotPayload();
			snapshots.put(snapshotId, snapshot);
		}
		return snapshots.get(snapshotId);
	}
	
//	public SnapshotPayload getSnapshot(int runCount) {
//		return getSnapshot(runSnapshotIdsMap.get(runCount));
//		
//	}

	public Map<Long, SnapshotPayload> getSnapshots() {
		return snapshots;
	}
	
	public void loadTestSetupFinished() {
		
		traverseSnapshotIds();
		
		Set<Long> keySet = snapshots.keySet();
		this.snapshotIds = new ArrayList<>(keySet);
		Collections.sort(snapshotIds);
		for(int i = 0; i < snapshotIds.size(); i++) {
			logger.info("run {} - {}", i+1, snapshotIds.get(i));
		}
		
		currentSnapshotId = snapshotIds.get(0);
	}

	private void traverseSnapshotIds() {
		Set<Long> keySet = snapshots.keySet();
		List<Long> snapshotIdsWithOrder = new ArrayList<>(keySet);
		Collections.sort(snapshotIdsWithOrder);
		long start = snapshotIdsWithOrder.get(0);
		long end = snapshotIdsWithOrder.get(snapshotIdsWithOrder.size()-1);
		
		for(long i = start; i <= end; i++) {
			getSnapshot(i);
		}
//		return snapshotIdsWithOrder;
	}
	
	public List<Long> getSnapshotIds() {
//		Set<Long> keySet = snapshots.keySet();
//		ArrayList<Long> snapshotIds = new ArrayList<>(keySet);
//		Collections.sort(snapshotIds);
//		currentSnapshotId = snapshotIds.get(0);
		return snapshotIds;
	}

//	public Long getCurrentSnapshotId() {
//		return currentSnapshotId;
//	}

	public Long getCurrentSnapshotId(int runCount) {
		return snapshotIds.get(runCount - 1);
	}
//	
//	public void increaseCurrentSnapshotId() {
//		currentSnapshotId++;
//	}
	
}
