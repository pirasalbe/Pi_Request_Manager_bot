package com.pirasalbe.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pirasalbe.models.database.Admin;

/**
 * Repository to interact with Admin table
 *
 * @author pirasalbe
 *
 */
public interface AdminRepository extends JpaRepository<Admin, Long> {

}
