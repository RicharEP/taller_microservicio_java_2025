package com.homerios.msnegocioproducto.exception;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.homerios.msnegocioproducto.dto.ErrorResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(RecursoNoEncontradoException.class)
	public ResponseEntity<ErrorResponse> noEncontrado(RecursoNoEncontradoException ex) {
		return responder(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	@ExceptionHandler(ReglaNegocioException.class)
	public ResponseEntity<ErrorResponse> reglaNegocio(ReglaNegocioException ex) {
		return responder(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
	}

	@ExceptionHandler(ObjectOptimisticLockingFailureException.class)
	public ResponseEntity<ErrorResponse> conflictoConcurrencia() {
		return responder(HttpStatus.CONFLICT, "El producto fue modificado por otra operación, reintente");
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> validacion(MethodArgumentNotValidException ex) {
		List<String> detalles = ex.getBindingResult().getFieldErrors().stream()
				.map(e -> e.getField() + ": " + e.getDefaultMessage()).toList();
		return ResponseEntity.badRequest().body(ErrorResponse.of(400, "Datos de entrada inválidos", detalles));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorResponse> cuerpoIlegible() {
		return responder(HttpStatus.BAD_REQUEST, "El cuerpo de la petición no es un JSON válido en UTF-8");
	}

	private ResponseEntity<ErrorResponse> responder(HttpStatus status, String mensaje) {
		return ResponseEntity.status(status).body(ErrorResponse.of(status.value(), mensaje));
	}
}
