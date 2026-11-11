package com.homerios.msnegociogestionsemestre;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.homerios.msnegociogestionsemestre.repository.SemestreRepository;
import com.homerios.msnegociogestionsemestre.rest.SemestreRest;

@ExtendWith(MockitoExtension.class)
class SemestreRestTest {

	@Mock
	private SemestreRepository repository;

	@InjectMocks
	private SemestreRest rest;

	@Test
	void semestreInexistenteDevuelve404() {
		when(repository.findById(99L)).thenReturn(Optional.empty());
		ResponseStatusException error = assertThrows(ResponseStatusException.class,
				() -> rest.buscarSemestre(99L));
		assertEquals(HttpStatus.NOT_FOUND, error.getStatus());
	}
}
