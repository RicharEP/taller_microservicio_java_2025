package com.homerios.msnegociogestiondocente;

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

import com.homerios.msnegociogestiondocente.controller.DocenteController;
import com.homerios.msnegociogestiondocente.model.Docente;
import com.homerios.msnegociogestiondocente.repository.DocenteRepository;

@ExtendWith(MockitoExtension.class)
class DocenteControllerTest {
	@Mock
	private DocenteRepository repository;
	@InjectMocks
	private DocenteController controller;

	@Test
	void devuelve404SiNoExiste() {
		when(repository.findById(9L)).thenReturn(Optional.empty());
		assertEquals(HttpStatus.NOT_FOUND, controller.busDocente(9L).getStatusCode());
		assertEquals(HttpStatus.NOT_FOUND, controller.actualizarDocente(9L, new Docente()).getStatusCode());
		assertEquals(HttpStatus.NOT_FOUND, controller.deleteDocente(9L).getStatusCode());
		verify(repository, never()).deleteById(any());
	}

	@Test
	void altaIgnoraIdentificadorEnviadoPorCliente() {
		Docente solicitud = new Docente();
		solicitud.setId_docente(9L);
		solicitud.setNombre("Ana");
		when(repository.save(any(Docente.class))).thenAnswer(inv -> inv.getArgument(0));
		Docente guardado = controller.crearDocente(solicitud);
		assertNull(guardado.getId_docente());
		assertEquals("Ana", guardado.getNombre());
	}
}
