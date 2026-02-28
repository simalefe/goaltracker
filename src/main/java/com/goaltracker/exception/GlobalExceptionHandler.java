package com.goaltracker.exception;

import com.goaltracker.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Determines whether the request expects a JSON (API) response.
     */
    private boolean isApiRequest(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        String uri = request.getRequestURI();
        return uri.startsWith("/api/") ||
               uri.startsWith("/actuator/") ||
               (accept != null && accept.contains("application/json"));
    }

    // ---- Validation Errors (400) ----
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        List<ApiResponse.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ApiResponse.FieldError(
                        fe.getField(),
                        fe.getDefaultMessage(),
                        fe.getRejectedValue()))
                .toList();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(
                        ErrorCode.VALIDATION_ERROR.name(),
                        ErrorCode.VALIDATION_ERROR.getDefaultMessage(),
                        fieldErrors));
    }

    // ---- Auth Exceptions ----
    @ExceptionHandler(EmailAlreadyExistsException.class)
    public Object handleEmailExists(HttpServletRequest req, EmailAlreadyExistsException ex) {
        if (isApiRequest(req)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error(ErrorCode.EMAIL_ALREADY_EXISTS.name(), ex.getMessage()));
        }
        return errorModelAndView("auth/register", ex.getMessage());
    }

    @ExceptionHandler(UsernameAlreadyExistsException.class)
    public Object handleUsernameExists(HttpServletRequest req, UsernameAlreadyExistsException ex) {
        if (isApiRequest(req)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error(ErrorCode.USERNAME_ALREADY_EXISTS.name(), ex.getMessage()));
        }
        return errorModelAndView("auth/register", ex.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public Object handleInvalidCredentials(HttpServletRequest req, InvalidCredentialsException ex) {
        if (isApiRequest(req)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(ErrorCode.INVALID_CREDENTIALS.name(), ex.getMessage()));
        }
        return errorModelAndView("auth/login", ex.getMessage());
    }

    @ExceptionHandler(AccountDisabledException.class)
    public Object handleDisabled(HttpServletRequest req, AccountDisabledException ex) {
        if (isApiRequest(req)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(ErrorCode.ACCOUNT_DISABLED.name(), ex.getMessage()));
        }
        return errorModelAndView("auth/login", ex.getMessage());
    }

    @ExceptionHandler(AccountLockedException.class)
    public Object handleLocked(HttpServletRequest req, AccountLockedException ex) {
        if (isApiRequest(req)) {
            return ResponseEntity.status(423)
                    .body(ApiResponse.error("ACCOUNT_LOCKED", ex.getMessage()));
        }
        return errorModelAndView("auth/login", ex.getMessage());
    }

    @ExceptionHandler({InvalidTokenException.class, TokenExpiredException.class})
    public Object handleTokenError(HttpServletRequest req, RuntimeException ex) {
        String code = ex instanceof TokenExpiredException ? ErrorCode.TOKEN_EXPIRED.name() : ErrorCode.INVALID_TOKEN.name();
        if (isApiRequest(req)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(code, ex.getMessage()));
        }
        ModelAndView mav = new ModelAndView("auth/verify-email");
        mav.addObject("errorMessage", ex.getMessage());
        return mav;
    }

    // ---- 404 Not Found ----
    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public Object handleNotFound(HttpServletRequest request, Exception ex) {
        if (isApiRequest(request)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(ErrorCode.NOT_FOUND.name(), ErrorCode.NOT_FOUND.getDefaultMessage()));
        }
        ModelAndView mav = new ModelAndView("error/404");
        mav.setStatus(HttpStatus.NOT_FOUND);
        return mav;
    }

    // ---- Catch-All (500) ----
    @ExceptionHandler(Exception.class)
    public Object handleAll(HttpServletRequest request, Exception ex) {
        log.error("Beklenmeyen hata: {} — {}", request.getRequestURI(), ex.getMessage(), ex);
        if (isApiRequest(request)) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(ErrorCode.INTERNAL_ERROR.name(), ErrorCode.INTERNAL_ERROR.getDefaultMessage()));
        }
        ModelAndView mav = new ModelAndView("error/500");
        mav.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        return mav;
    }

    private ModelAndView errorModelAndView(String viewName, String errorMessage) {
        ModelAndView mav = new ModelAndView(viewName);
        mav.addObject("errorMessage", errorMessage);
        return mav;
    }
}
