
package com.snw.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;
import java.util.Map;

@Getter
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class FinancialBoundaryException extends RuntimeException {

	Map<String, List<String>> constraintViolationMap;

	public FinancialBoundaryException() {
		super("Boundary Violation (0 - 1,000,000)");
	}

	public FinancialBoundaryException(String message) {
		super(message);
	}

	public FinancialBoundaryException(Throwable cause) {
		super(cause);
	}

	public FinancialBoundaryException(Map<String, List<String>> constraints) {
		this.constraintViolationMap = constraints;
	}

	public FinancialBoundaryException(String message,
                                     Throwable cause) {
		super(message, cause);
	}

	public FinancialBoundaryException(String message,
                                     Throwable cause,
                                     boolean enableSuppression,
                                     boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}


}
