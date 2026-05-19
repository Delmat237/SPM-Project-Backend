package com.techwave.auth.admin.repository;

import com.techwave.auth.admin.model.SystemSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SystemSettingRepository extends JpaRepository<SystemSetting, Long> {

    Optional<SystemSetting> findBySettingKey(String key);

    List<SystemSetting> findAllByOrderByCategoryAscSettingKeyAsc();

    List<SystemSetting> findByCategory(String category);
}
