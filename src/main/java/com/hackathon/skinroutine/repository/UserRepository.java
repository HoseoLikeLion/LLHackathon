package com.hackathon.skinroutine.repository;

import com.hackathon.skinroutine.domain.User;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {
}
