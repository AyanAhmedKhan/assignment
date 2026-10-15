package com.klodspot.assignment.error;

import com.klodspot.assignment.account.RegistrationException;
import com.klodspot.assignment.employee.DuplicateEmailException;
import com.klodspot.assignment.employee.Employee;
import com.klodspot.assignment.employee.EmployeeNotFoundException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(RegistrationException.class)
    public ProblemDetail invalidRegistration(RegistrationException exception) {
        var detail = problem(HttpStatus.BAD_REQUEST, exception.getMessage());
        detail.setProperty("errors", exception.getErrors());
        return detail;
    }

    @ExceptionHandler(EmployeeNotFoundException.class)
    public ProblemDetail notFound(EmployeeNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ProblemDetail duplicateEmail(DuplicateEmailException exception) {
        var detail = problem(HttpStatus.CONFLICT, exception.getMessage());
        detail.setProperty("errors", Map.of("email", exception.getMessage()));
        return detail;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail invalidBody(MethodArgumentNotValidException exception) {
        var errors = new LinkedHashMap<String, String>();
        exception
            .getBindingResult()
            .getFieldErrors()
            .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        var detail = problem(HttpStatus.BAD_REQUEST, "Please check the highlighted fields.");
        detail.setProperty("errors", errors);
        return detail;
    }

    @ExceptionHandler({ IllegalArgumentException.class, MethodArgumentTypeMismatchException.class })
    public ProblemDetail invalidParameter(Exception exception) {
        String message =
            exception instanceof MethodArgumentTypeMismatchException
                ? "A URL parameter has an invalid value."
                : exception.getMessage();
        return problem(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ProblemDetail invalidQuery(HandlerMethodValidationException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Search must be at most 120 characters.");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail unreadableBody(HttpMessageNotReadableException exception) {
        return problem(
            HttpStatus.BAD_REQUEST,
            "Invalid request body. Check the dates, numbers and allowed enum values."
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail integrityViolation(DataIntegrityViolationException exception) {
        String cause = String.valueOf(exception.getMostSpecificCause().getMessage());
        if (cause.contains("ux_app_users_")) {
            var detail = problem(HttpStatus.CONFLICT, "An account with this email already exists.");
            detail.setProperty("errors", Map.of("email", detail.getDetail()));
            return detail;
        }
        // The database constraint also catches two requests creating the same email at once.
        if (cause.contains("ux_employees_email")) {
            return duplicateEmail(new DuplicateEmailException());
        }
        log.warn("Employee write violated a database constraint", exception);
        return problem(HttpStatus.CONFLICT, "This change conflicts with an existing record.");
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail concurrentWrite(OptimisticLockingFailureException exception) {
        return problem(HttpStatus.CONFLICT, "This record changed during your request. Reload it and try again.");
    }

    private ProblemDetail problem(HttpStatus status, String detail) {
        return ProblemDetail.forStatusAndDetail(status, detail);
    }
}
