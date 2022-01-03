package com.pirasalbe.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pirasalbe.models.database.Group;

/**
 * Repository to interact with Group table
 *
 * @author pirasalbe
 *
 */
public interface GroupRepository extends JpaRepository<Group, Long> {

}
