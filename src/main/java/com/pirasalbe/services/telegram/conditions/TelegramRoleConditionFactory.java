package com.pirasalbe.services.telegram.conditions;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.pirasalbe.models.UserRole;
import com.pirasalbe.models.telegram.handlers.TelegramCondition;
import com.pirasalbe.services.AdminService;
import com.pirasalbe.utils.TelegramUtils;

/**
 * Command conditions factory
 *
 * @author pirasalbe
 *
 */
@Component
public class TelegramRoleConditionFactory {

	@Autowired
	protected AdminService adminService;

	/**
	 * Generates a condition on the specified role
	 *
	 * @param role Role for the condition
	 * @return TelegramCondition
	 */
	public TelegramCondition onRole(UserRole role) {
		return update -> {
			boolean asserted = false;

			// get the user role
			Long userId = TelegramUtils.getUserId(update);
			if (userId != null) {
				UserRole authority = adminService.getAuthority(userId);

				// check if the authority level is valid
				asserted = role.getAuthorityLevel() <= authority.getAuthorityLevel();
			}

			return asserted;
		};
	}

}
