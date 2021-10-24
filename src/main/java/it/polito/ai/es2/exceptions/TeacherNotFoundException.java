package it.polito.ai.es2.exceptions;

public class TeacherNotFoundException extends TeamServiceException {
    public TeacherNotFoundException() {
        super("Teacher not found!");
    }

    public TeacherNotFoundException(String message) {
        super(message);
    }
}
