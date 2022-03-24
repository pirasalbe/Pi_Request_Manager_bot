package com.pirasalbe.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pirasalbe.models.database.Channel;

/**
 * Repository to interact with Channel table
 *
 * @author pirasalbe
 *
 */
public interface ChannelRepository extends JpaRepository<Channel, Long> {

}
