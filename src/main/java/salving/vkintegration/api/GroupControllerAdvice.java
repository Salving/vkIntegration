package salving.vkintegration.api;

import io.swagger.v3.oas.annotations.media.ExampleObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import salving.vkintegration.service.InvalidParametersException;
import salving.vkintegration.service.UserNotFoundException;

@RestControllerAdvice
public class GroupControllerAdvice {

    @ExceptionHandler(UserNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleUserNotFound(RuntimeException e) {
        return e.getMessage();
    }

    @ExceptionHandler(InvalidParametersException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleInvalidParameters(RuntimeException e) {
        return e.getMessage();
    }

}
