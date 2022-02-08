package com.pirasalbe.repositories;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.pirasalbe.models.database.Request;
import com.pirasalbe.models.database.RequestPK;
import com.pirasalbe.models.request.Format;
import com.pirasalbe.models.request.Source;

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

	@Query("SELECT r " + "FROM Request r "
			+ "WHERE r.userId = :userId AND r.format = 'AUDIOBOOK' AND r.status <> 'RESOLVED' "
			+ "ORDER BY r.requestDate DESC")
	Request getLastAudiobookRequestOfUserInGroup(@Param("userId") long user);

	@Query("SELECT r " + "FROM Request r "
			+ "WHERE r.userId = :userId AND r.format = 'AUDIOBOOK' AND r.status = 'RESOLVED' "
			+ "ORDER BY r.resolvedDate DESC")
	Request getLastAudiobookResolvedOfUserInGroup(@Param("userId") long user);

	@Modifying
	@Query("DELETE FROM Request r WHERE r.id.groupId = :groupId")
	void deleteByGroupId(@Param("groupId") Long groupId);

	@Query("SELECT r " + "FROM Request r "
			+ "WHERE r.status = 'NEW' AND r.id.groupId = :groupId AND r.format = :format AND r.source = :source")
	List<Request> findByFilters(@Param("groupId") Long groupId, @Param("source") Source source,
			@Param("format") Format format, Sort sort);

	@Query("SELECT r " + "FROM Request r "
			+ "WHERE r.status = 'NEW' AND r.id.groupId = :groupId AND r.format = :format")
	List<Request> findByFilters(@Param("groupId") Long groupId, @Param("format") Format format, Sort sort);

	@Query("SELECT r " + "FROM Request r "
			+ "WHERE r.status = 'NEW' AND r.id.groupId = :groupId AND r.source = :source")
	List<Request> findByFilters(@Param("groupId") Long groupId, @Param("source") Source source, Sort sort);

	@Query("SELECT r " + "FROM Request r " + "WHERE r.status = 'NEW' AND r.id.groupId = :groupId")
	List<Request> findByFilters(@Param("groupId") Long groupId, Sort sort);

	@Query("SELECT r " + "FROM Request r " + "WHERE r.status = 'NEW' AND r.format = :format AND r.source = :source")
	List<Request> findByFilters(@Param("source") Source source, @Param("format") Format format, Sort sort);

	@Query("SELECT r " + "FROM Request r " + "WHERE r.status = 'NEW' AND r.source = :source")
	List<Request> findByFilters(@Param("source") Source source, Sort sort);

	@Query("SELECT r " + "FROM Request r " + "WHERE r.status = 'NEW' AND r.format = :format")
	List<Request> findByFilters(@Param("format") Format format, Sort sort);

	@Query("SELECT r " + "FROM Request r " + "WHERE r.status = 'NEW'")
	List<Request> findByFilters(Sort sort);

}
