
package com.snw.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class NoRecordFoundException extends RuntimeException {
	public NoRecordFoundException() {
		super("Entity with the ID given not found.");
	}

	public NoRecordFoundException(String message) {
		super(message);
	}

	public NoRecordFoundException(Throwable cause) {
		super(cause);
	}

	public NoRecordFoundException(String message,
								  Throwable cause) {
		super(message, cause);
	}

	public NoRecordFoundException(String message,
								  Throwable cause,
								  boolean enableSuppression,
								  boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
