package com.hantu.identity_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.hantu.identity_service.entity.InvalidatedToken;
import java.util.Date;
import java.util.List;

@Repository
public interface InvalidatedTokenRepository extends JpaRepository<InvalidatedToken, String> {

    List<InvalidatedToken> findAllByExpirationTimeBefore(Date expirationTime);

}
