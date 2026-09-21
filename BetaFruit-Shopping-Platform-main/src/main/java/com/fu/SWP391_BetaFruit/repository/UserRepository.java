package com.fu.SWP391_BetaFruit.repository;

import com.fu.SWP391_BetaFruit.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
}
