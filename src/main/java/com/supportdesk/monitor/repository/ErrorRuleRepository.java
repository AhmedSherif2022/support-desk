package com.supportdesk.monitor.repository;
import com.supportdesk.monitor.entity.ErrorRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ErrorRuleRepository extends JpaRepository<ErrorRule, Long>
{

    Optional<ErrorRule> findByErrorCodeAndActiveTrue(String errorCode);

    List<ErrorRule> findAllByOrderByErrorCodeAsc();
}