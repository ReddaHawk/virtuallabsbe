package it.polito.ai.es2.exceptions;

public class StudentNotFoundException extends TeamServiceException {
    public StudentNotFoundException() {
        super("Student not found!");
    }

    public StudentNotFoundException(String message) {
        super(message);
    }
}
