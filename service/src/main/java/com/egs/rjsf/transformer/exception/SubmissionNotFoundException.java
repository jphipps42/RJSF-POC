package com.egs.rjsf.transformer.exception;

public class SubmissionNotFoundException extends RuntimeException {
    public SubmissionNotFoundException(long submissionId) {
        super("Submission not found with id: " + submissionId);
    }
}
