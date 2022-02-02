package com.pirasalbe.repositories;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.pirasalbe.models.LastRequestInfo;
import com.pirasalbe.models.database.UserRequest;
import com.pirasalbe.models.database.UserRequestPK;

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

}
