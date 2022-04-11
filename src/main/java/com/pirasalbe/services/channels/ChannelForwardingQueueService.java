package com.pirasalbe.services.channels;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.pirasalbe.models.SyncRequest;
import com.pirasalbe.models.database.RequestPK;

/**
 * Service that manages the channels
 *
 * @author pirasalbe
 *
 */
@Component
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
public class ChannelForwardingQueueService {

	private Queue<SyncRequest> syncQueue;

	private Queue<RequestPK> forwardQueue;

	private Queue<RequestPK> deleteQueue;

	private Queue<Long> deleteByGroupIdQueue;

	public ChannelForwardingQueueService() {
		this.syncQueue = new LinkedBlockingQueue<>();
		this.forwardQueue = new LinkedBlockingQueue<>();
		this.deleteQueue = new LinkedBlockingQueue<>();
		this.deleteByGroupIdQueue = new LinkedBlockingQueue<>();
	}

	/**
	 * Sync a channel request
	 *
	 * @param request Request to update
	 */
	public void syncRequest(SyncRequest syncRequest) {
		if (!syncQueue.contains(syncRequest)) {
			syncQueue.add(syncRequest);
		}
	}

	/**
	 * Send a request updated to the channels<br>
	 *
	 * @param request Request to update
	 */
	public void forwardRequest(RequestPK requestId) {
		forwardQueue.add(requestId);
	}

	/**
	 * Delete a request forwarded to a channel<br>
	 *
	 * @param request Request to delete
	 */
	public void deleteRequest(RequestPK requestId) {
		deleteQueue.add(requestId);
		if (forwardQueue.contains(requestId)) {
			forwardQueue.remove(requestId);
		}
	}

	public void deleteForwardedRequestsByGroupId(Long groupId) {
		deleteByGroupIdQueue.add(groupId);
	}

	public SyncRequest pollSyncQueue() {
		return syncQueue.poll();
	}

	public int syncQueueSize() {
		return syncQueue.size();
	}

	public RequestPK pollForwardQueue() {
		return forwardQueue.poll();
	}

	public int forwardQueueSize() {
		return forwardQueue.size();
	}

	public RequestPK pollDeleteQueue() {
		return deleteQueue.poll();
	}

	public int deleteQueueSize() {
		return deleteQueue.size();
	}

	public Long pollDeleteByGroupIdQueue() {
		return deleteByGroupIdQueue.poll();
	}

	public int deleteByGroupIdQueueSize() {
		return deleteByGroupIdQueue.size();
	}

}
