package com.supportdesk.incident.repository;
import com.supportdesk.incident.entity.AppSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppSettingRepository extends JpaRepository<AppSetting, String>
{
}
