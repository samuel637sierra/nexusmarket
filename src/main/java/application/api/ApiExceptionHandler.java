package application.api;

import application.api.ApiViews.ErrorView;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorView> handleInvalidArgument(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(new ErrorView("invalid_request", safeMessage(exception)));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorView> handleConflict(IllegalStateException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorView("conflict", safeMessage(exception)));
    }

    private static String safeMessage(RuntimeException exception) {
        return exception.getMessage() == null ? "La solicitud no pudo procesarse" : exception.getMessage();
    }
}