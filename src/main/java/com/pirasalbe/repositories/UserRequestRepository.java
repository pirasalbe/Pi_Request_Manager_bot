package com.pirasalbe.repositories;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.pirasalbe.models.LastRequestInfo;
import com.pirasalbe.models.database.UserRequest;
import com.pirasalbe.models.database.UserRequestPK;
import com.pirasalbe.models.request.Format;
import com.pirasalbe.models.request.Source;

/**
 * Repository to interact with UserRequest table
 *
 * @author pirasalbe
 *
 */
public interface UserRequestRepository extends JpaRepository<UserRequest, UserRequestPK> {

	@Query("SELECT count(u) "
			+ "FROM UserRequest u JOIN Request r ON u.id.messageId = r.id.messageId AND u.id.groupId = r.id.groupId "
			+ "WHERE u.id.userId = :userId AND u.date >= :from AND r.format = 'EBOOK'")
	long countUserEbookRequestsOfToday(@Param("userId") long userId, @Param("from") LocalDateTime from);

	@Query("SELECT u "
			+ "FROM UserRequest u JOIN Request r ON u.id.messageId = r.id.messageId AND u.id.groupId = r.id.groupId "
			+ "WHERE u.id.userId = :userId AND u.date >= :from AND r.format = 'EBOOK'" + "ORDER BY u.date asc")
	List<UserRequest> getUserEbookRequestsOfToday(@Param("userId") long userId, @Param("from") LocalDateTime from);

	@Query("SELECT new com.pirasalbe.models.LastRequestInfo(u.date, r.otherTags) "
			+ "FROM UserRequest u JOIN Request r ON u.id.messageId = r.id.messageId AND u.id.groupId = r.id.groupId "
			+ "WHERE u.id.userId = :userId AND r.format = 'AUDIOBOOK' AND r.status <> 'RESOLVED' "
			+ "ORDER BY u.date DESC")
	LastRequestInfo getLastAudiobookRequestOfUserInGroup(@Param("userId") long user);

	@Query("SELECT new com.pirasalbe.models.LastRequestInfo(r.resolvedDate, r.otherTags) "
			+ "FROM UserRequest u JOIN Request r ON u.id.messageId = r.id.messageId AND u.id.groupId = r.id.groupId "
			+ "WHERE u.id.userId = :userId AND r.format = 'AUDIOBOOK' AND r.status = 'RESOLVED' "
			+ "ORDER BY r.resolvedDate DESC")
	LastRequestInfo getLastAudiobookResolvedOfUserInGroup(@Param("userId") long user);

	@Modifying
	@Query("DELETE FROM UserRequest u WHERE u.groupId = :groupId")
	void deleteByGroupId(@Param("groupId") Long groupId);

	@Query("SELECT u "
			+ "FROM Request r JOIN UserRequest u ON u.id.messageId = r.id.messageId AND u.id.groupId = r.id.groupId "
			+ "WHERE u.role = 'CREATOR' AND r.status = 'NEW' AND r.id.groupId = :groupId AND r.format = :format AND r.source = :source")
	List<UserRequest> findByFilters(@Param("groupId") Long groupId, @Param("source") Source source,
			@Param("format") Format format, Sort sort);

	@Query("SELECT u "
			+ "FROM Request r JOIN UserRequest u ON u.id.messageId = r.id.messageId AND u.id.groupId = r.id.groupId "
			+ "WHERE u.role = 'CREATOR' AND r.status = 'NEW' AND r.id.groupId = :groupId AND r.format = :format")
	List<UserRequest> findByFilters(@Param("groupId") Long groupId, @Param("format") Format format, Sort sort);

	@Query("SELECT u "
			+ "FROM Request r JOIN UserRequest u ON u.id.messageId = r.id.messageId AND u.id.groupId = r.id.groupId "
			+ "WHERE u.role = 'CREATOR' AND r.status = 'NEW' AND r.id.groupId = :groupId AND r.source = :source")
	List<UserRequest> findByFilters(@Param("groupId") Long groupId, @Param("source") Source source, Sort sort);

	@Query("SELECT u "
			+ "FROM Request r JOIN UserRequest u ON u.id.messageId = r.id.messageId AND u.id.groupId = r.id.groupId "
			+ "WHERE u.role = 'CREATOR' AND r.status = 'NEW' AND r.id.groupId = :groupId")
	List<UserRequest> findByFilters(@Param("groupId") Long groupId, Sort sort);

	@Query("SELECT u "
			+ "FROM Request r JOIN UserRequest u ON u.id.messageId = r.id.messageId AND u.id.groupId = r.id.groupId "
			+ "WHERE u.role = 'CREATOR' AND r.status = 'NEW' AND r.format = :format AND r.source = :source")
	List<UserRequest> findByFilters(@Param("source") Source source, @Param("format") Format format, Sort sort);

	@Query("SELECT u "
			+ "FROM Request r JOIN UserRequest u ON u.id.messageId = r.id.messageId AND u.id.groupId = r.id.groupId "
			+ "WHERE u.role = 'CREATOR' AND r.status = 'NEW' AND r.source = :source")
	List<UserRequest> findByFilters(@Param("source") Source source, Sort sort);

	@Query("SELECT u "
			+ "FROM Request r JOIN UserRequest u ON u.id.messageId = r.id.messageId AND u.id.groupId = r.id.groupId "
			+ "WHERE u.role = 'CREATOR' AND r.status = 'NEW' AND r.format = :format")
	List<UserRequest> findByFilters(@Param("format") Format format, Sort sort);

	@Query("SELECT u "
			+ "FROM Request r JOIN UserRequest u ON u.id.messageId = r.id.messageId AND u.id.groupId = r.id.groupId "
			+ "WHERE u.role = 'CREATOR' AND r.status = 'NEW'")
	List<UserRequest> findByFilters(Sort sort);

}
