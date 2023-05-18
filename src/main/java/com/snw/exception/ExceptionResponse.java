package com.snw.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Date;

@Getter
@AllArgsConstructor
public class ExceptionResponse {
	private Date timestamp;
	private String message;
	private String details;
}
