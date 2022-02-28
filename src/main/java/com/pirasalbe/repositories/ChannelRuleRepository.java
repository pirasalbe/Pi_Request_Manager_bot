package com.pirasalbe.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pirasalbe.models.database.ChannelRule;
import com.pirasalbe.models.database.ChannelRulePK;

/**
 * Repository to interact with ChannelRule table
 *
 * @author pirasalbe
 *
 */
public interface ChannelRuleRepository extends JpaRepository<ChannelRule, ChannelRulePK> {

}
