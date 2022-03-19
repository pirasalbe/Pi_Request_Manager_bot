package com.pirasalbe.repositories;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.pirasalbe.models.database.Request;
import com.pirasalbe.models.database.RequestPK;
import com.pirasalbe.models.request.RequestStatus;

/**
 * Repository to interact with Request table
 *
 * @author pirasalbe
 *
 */
public interface RequestRepository extends JpaRepository<Request, RequestPK> {

	@Query("SELECT r FROM Request r WHERE r.id.groupId = :groupId AND r.userId = :userId AND r.link = :link")
	Request findByUniqueKey(@Param("groupId") Long groupId, @Param("userId") Long userId, @Param("link") String link);

	@Query("SELECT r " + "FROM Request r "
			+ "WHERE r.userId = :userId AND r.requestDate >= :from AND r.format = 'EBOOK' "
			+ "ORDER BY r.requestDate asc")
	List<Request> getUserEbookRequestsOfToday(@Param("userId") long userId, @Param("from") LocalDateTime from);

	@Query("SELECT r " + "FROM Request r " + "WHERE r.userId = :userId AND r.format = 'EBOOK' "
			+ "ORDER BY r.requestDate DESC")
	List<Request> getLastEbookRequestOfUser(@Param("userId") long user, Pageable pageable);

	@Query("SELECT r " + "FROM Request r "
			+ "WHERE r.userId = :userId AND r.format = 'AUDIOBOOK' AND r.status <> 'RESOLVED' "
			+ "ORDER BY r.requestDate DESC")
	List<Request> getLastAudiobookRequestOfUser(@Param("userId") long user, Pageable pageable);

	@Query("SELECT r " + "FROM Request r "
			+ "WHERE r.userId = :userId AND r.format = 'AUDIOBOOK' AND r.status = 'RESOLVED' "
			+ "ORDER BY r.resolvedDate DESC")
	List<Request> getLastAudiobookResolvedOfUser(@Param("userId") long user, Pageable pageable);

	@Modifying
	@Query("DELETE FROM Request r WHERE r.id.groupId = :groupId")
	void deleteByGroupId(@Param("groupId") Long groupId);

	@Query("SELECT r FROM Request r WHERE r.status = :status AND r.requestDate < :requestDate")
	List<Request> findOldByStatus(@Param("requestDate") LocalDateTime requestDate,
			@Param("status") RequestStatus status);

	@Modifying
	@Query("DELETE FROM Request r WHERE r.status = :status AND r.requestDate < :requestDate")
	void deleteOldByStatus(@Param("requestDate") LocalDateTime requestDate, @Param("status") RequestStatus status);

}
