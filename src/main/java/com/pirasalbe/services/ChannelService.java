package com.pirasalbe.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.pirasalbe.models.database.Channel;
import com.pirasalbe.repositories.ChannelRepository;

/**
 * Service that manages the channel table
 *
 * @author pirasalbe
 *
 */
@Component
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
public class ChannelService {

	@Autowired
	private ChannelRepository repository;

	public List<Channel> findAll() {
		return repository.findAll();
	}

	public boolean existsById(Long id) {
		return repository.existsById(id);
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = false)
	public void insert(Long id, String name) {
		// insert
		Channel channel = new Channel();
		channel.setId(id);
		channel.setName(name);

		repository.save(channel);
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = false)
	public void delete(Long id) {
		repository.deleteById(id);
	}

}
