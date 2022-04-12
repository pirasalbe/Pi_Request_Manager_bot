package com.pirasalbe.models;

public enum ContributorAction {

	CONFIRM("confirm", "cfm"), DONE("mark as done", "dne"), IN_PROGRESS("mark as in progress", "inp"),
	PENDING("mark as pending", "pdg"), PAUSE("mark as paused", "pse"), CANCEL("cancel", "cnl"), REMOVE("remove", "rmv");

	public static final String FORCE_DELETE = "rm";

	private String description;
	private String code;

	private ContributorAction(String description, String code) {
		this.description = description;
		this.code = code;
	}

	public String getDescription() {
		return description;
	}

	public String getCode() {
		return code;
	}

	public static ContributorAction getByCode(String code) {
		ContributorAction result = null;

		ContributorAction[] values = ContributorAction.values();
		for (int i = 0; i < values.length && result == null; i++) {
			ContributorAction action = values[i];

			if (action.getCode().equalsIgnoreCase(code)) {
				result = action;
			}
		}

		return result;
	}
}
