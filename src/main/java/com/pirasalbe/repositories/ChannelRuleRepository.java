package com.pirasalbe.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.pirasalbe.models.ChannelRuleType;
import com.pirasalbe.models.database.ChannelRule;
import com.pirasalbe.models.database.ChannelRulePK;

/**
 * Repository to interact with ChannelRule table
 *
 * @author pirasalbe
 *
 */
public interface ChannelRuleRepository extends JpaRepository<ChannelRule, ChannelRulePK> {

	@Modifying
	@Query("DELETE FROM ChannelRule c WHERE c.id.channelId = :channelId")
	void deleteByChannelId(@Param("channelId") Long channelId);

	@Query("SELECT c FROM ChannelRule c WHERE c.id.channelId = :channelId")
	List<ChannelRule> getByChannelId(@Param("channelId") Long channelId);

	@Query("SELECT c FROM ChannelRule c WHERE c.id.channelId = :channelId AND c.id.type = :type")
	List<ChannelRule> getByChannelIdAndType(@Param("channelId") Long channelId, @Param("type") ChannelRuleType type);

}
