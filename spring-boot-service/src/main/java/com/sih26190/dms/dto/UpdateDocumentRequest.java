package com.sih26190.dms.dto;

import lombok.Getter;
import lombok.Setter;

//updating only edits meta data and not actual file data, since audit log must exist after deletion, this is why the foreign key was removed
@Getter
@Setter
public class UpdateDocumentRequest {

    private String caseId;
    private String documentType;

}
