package com.pirasalbe.models;

public class RequestAssociationInfo {

	public enum Association {
		NONE, SUBSCRIBER, CREATOR;
	}

	private boolean requestExists;

	private Association association;

	public RequestAssociationInfo(boolean requestExists, Association association) {
		this.requestExists = requestExists;
		this.association = association;
	}

	/**
	 * The request doesn't exists
	 *
	 * @return RequestAssociationInfo
	 */
	public static RequestAssociationInfo noRequest() {
		return new RequestAssociationInfo(false, Association.NONE);
	}

	/**
	 * The request exists, the association doesn't
	 *
	 * @return RequestAssociationInfo
	 */
	public static RequestAssociationInfo noAssociation() {
		return new RequestAssociationInfo(true, Association.NONE);
	}

	/**
	 * The request exists, the user is a subscriber
	 *
	 * @return RequestAssociationInfo
	 */
	public static RequestAssociationInfo subscriber() {
		return new RequestAssociationInfo(true, Association.SUBSCRIBER);
	}

	/**
	 * The request exists, the user is the creator
	 *
	 * @return RequestAssociationInfo
	 */
	public static RequestAssociationInfo creator() {
		return new RequestAssociationInfo(true, Association.CREATOR);
	}

	public boolean requestExists() {
		return requestExists;
	}

	public Association getAssociation() {
		return association;
	}

}
