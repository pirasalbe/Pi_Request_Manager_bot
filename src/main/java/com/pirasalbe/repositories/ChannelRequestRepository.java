package com.pirasalbe.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.pirasalbe.models.database.ChannelRequest;
import com.pirasalbe.models.database.ChannelRequestPK;

/**
 * Repository to interact with Channel Request table
 *
 * @author pirasalbe
 *
 */
public interface ChannelRequestRepository extends JpaRepository<ChannelRequest, ChannelRequestPK> {

	@Modifying
	@Query("DELETE FROM ChannelRequest r WHERE r.id.channelId = :channelId")
	void deleteByChannelId(@Param("channelId") Long channelId);

	@Query("SELECT r FROM ChannelRequest r WHERE r.id.channelId = :channelId AND r.requestGroupId = :requestGroupId AND r.requestMessageId = :requestMessageId")
	ChannelRequest findByUniqueKey(@Param("channelId") Long channelId, @Param("requestGroupId") Long requestGroupId,
			@Param("requestMessageId") Long requestMessageId);

	@Query("SELECT r FROM ChannelRequest r WHERE r.requestGroupId = :requestGroupId AND r.requestMessageId = :requestMessageId")
	List<ChannelRequest> findByRequest(@Param("requestGroupId") Long requestGroupId,
			@Param("requestMessageId") Long requestMessageId);

	@Query("SELECT r FROM ChannelRequest r WHERE r.requestGroupId = :requestGroupId")
	List<ChannelRequest> findByGroupId(@Param("requestGroupId") Long requestGroupId);

	@Query("SELECT r FROM ChannelRequest r WHERE r.id.channelId = :channelId")
	List<ChannelRequest> findByChannelId(@Param("channelId") Long channelId);
}
