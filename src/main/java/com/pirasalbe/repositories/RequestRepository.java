package com.pirasalbe.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pirasalbe.models.database.Request;

/**
 * Repository to interact with Request table
 *
 * @author pirasalbe
 *
 */
public interface RequestRepository extends JpaRepository<Request, Long> {

}
