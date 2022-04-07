package com.smart.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.web.bind.annotation.RequestParam;

import com.smart.entities.User;

public interface UserRepository extends JpaRepository<User,Integer>{
	
	@Query("select u from User u where u.email=:email")
	public User getUserByUserName(@RequestParam("email") String email);

}
