package com.sih26190.dms.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sih26190.dms.dto.CaseSummaryResponse;
import com.sih26190.dms.repository.DocumentRecordRepository;

import lombok.RequiredArgsConstructor;

// no real case entity in the schema, just docs represented by caseId that can be accessed by the django dash at the rest api's end point, simple stuff for now
@RestController
@RequestMapping("/api/cases")
@RequiredArgsConstructor
public class CaseController {

    private final DocumentRecordRepository documentRecordRepository;

    @GetMapping
    public List<CaseSummaryResponse> listCases() {
        return documentRecordRepository.findDistinctCaseIds().stream()
                .map(this::toSummary)
                .toList();
    }

    @GetMapping("/{caseId}")
    public ResponseEntity<CaseSummaryResponse> getCase(@PathVariable String caseId) {
        long count = documentRecordRepository.findByCaseId(caseId).size();
        if (count == 0) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toSummary(caseId));
    }

    private CaseSummaryResponse toSummary(String caseId) {
        long documentCount = documentRecordRepository.findByCaseId(caseId).size();
        // title and status are placeholders, see CaseSummaryResponse
        return new CaseSummaryResponse(caseId, caseId, "OPEN", documentCount);
    }

}
