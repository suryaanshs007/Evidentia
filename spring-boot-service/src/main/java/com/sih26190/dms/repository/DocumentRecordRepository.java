package com.sih26190.dms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.sih26190.dms.model.DocumentRecord;
import com.sih26190.dms.model.User;

public interface DocumentRecordRepository extends JpaRepository<DocumentRecord, Long> {


    List<DocumentRecord> findByUploadedBy(User uploadedBy);

    List<DocumentRecord> findByCaseId(String caseId);

    @Query("SELECT DISTINCT d.caseId FROM DocumentRecord d")
    List<String> findDistinctCaseIds();

}
