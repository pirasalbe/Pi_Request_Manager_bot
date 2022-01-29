package com.pirasalbe.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.pirasalbe.models.database.Request;
import com.pirasalbe.models.database.RequestPK;

/**
 * Repository to interact with Request table
 *
 * @author pirasalbe
 *
 */
public interface RequestRepository extends JpaRepository<Request, RequestPK> {

	@Query("SELECT r FROM Request r WHERE link = :link")
	Request findByLink(@Param("link") String link);

	@Modifying
	@Query("DELETE FROM Request r WHERE r.id.groupId = :groupId")
	void deleteByGroupId(@Param("groupId") Long groupId);

}
