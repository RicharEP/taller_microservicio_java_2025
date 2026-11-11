package com.homerios.msnegociogestioncurso;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.homerios.msnegociogestioncurso.controller.CursoController;
import com.homerios.msnegociogestioncurso.model.Curso;
import com.homerios.msnegociogestioncurso.repository.CursoRepository;

@ExtendWith(MockitoExtension.class)
class CursoControllerTest {
	@Mock
	private CursoRepository repository;
	@InjectMocks
	private CursoController controller;

	@Test
	void devuelve404SiNoExiste() {
		when(repository.findById(9L)).thenReturn(Optional.empty());
		assertEquals(HttpStatus.NOT_FOUND, controller.busCurso(9L).getStatusCode());
		assertEquals(HttpStatus.NOT_FOUND, controller.deleteCurso(9L).getStatusCode());
		verify(repository, never()).deleteById(any());
	}

	@Test
	void altaIgnoraIdentificadorEnviadoPorCliente() {
		Curso solicitud = new Curso();
		solicitud.setId_curso(9L);
		solicitud.setNombre("Matemática");
		when(repository.save(any(Curso.class))).thenAnswer(inv -> inv.getArgument(0));
		Curso guardado = controller.crearCurso(solicitud);
		assertNull(guardado.getId_curso());
		assertEquals("Matemática", guardado.getNombre());
	}
}
