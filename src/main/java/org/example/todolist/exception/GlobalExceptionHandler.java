package org.example.todolist.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(EntityNotFoundException.class)
    public String handleNotFound(EntityNotFoundException ex, HttpServletRequest request, Model model) {
        log.debug("Entity not found on {}: {}", request.getRequestURI(), ex.getMessage());
        model.addAttribute("message", ex.getMessage());
        return "error/404";
    }

    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(AccessDeniedBusinessException.class)
    public String handleAccessDenied(AccessDeniedBusinessException ex, HttpServletRequest request, Model model) {
        log.warn("Access denied on {}: {}", request.getRequestURI(), ex.getMessage());
        model.addAttribute("message", ex.getMessage());
        return "error/403";
    }

    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    @ExceptionHandler({BusinessException.class})
    public String handleBusiness(BusinessException ex, HttpServletRequest request, Model model) {
        log.info("Business rule violation on {}: {}", request.getRequestURI(), ex.getMessage());
        model.addAttribute("message", ex.getMessage());
        return "error/422";
    }

    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public String handleBindingErrors(Exception ex, HttpServletRequest request, Model model) {
        log.info("Validation error on {}", request.getRequestURI(), ex);
        model.addAttribute("message", "The submitted data is invalid. Please review the highlighted fields.");
        return "error/422";
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(OptimisticLockingAppException.class)
    public String handleOptimisticLocking(OptimisticLockingAppException ex, HttpServletRequest request, Model model) {
        log.warn("Optimistic locking failure on {}: {}", request.getRequestURI(), ex.getMessage());
        model.addAttribute("message", ex.getMessage());
        return "error/409";
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public String handleUnexpected(Exception ex, HttpServletRequest request, Model model) {
        log.error("Unexpected error on {}", request.getRequestURI(), ex);
        model.addAttribute("message", "Something went wrong. Please try again later.");
        return "error/500";
    }
}
