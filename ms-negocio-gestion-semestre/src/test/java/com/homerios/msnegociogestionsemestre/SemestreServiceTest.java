package com.homerios.msnegociogestionsemestre;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Example;

import com.homerios.msnegociogestionsemestre.entity.Semestre;
import com.homerios.msnegociogestionsemestre.entity.SemestreDetalle;
import com.homerios.msnegociogestionsemestre.repository.SemestreRepository;
import com.homerios.msnegociogestionsemestre.service.ServiceException;
import com.homerios.msnegociogestionsemestre.service.impl.SemestreServiceImpl;

@ExtendWith(MockitoExtension.class)
class SemestreServiceTest {

	@Mock
	private SemestreRepository repository;

	@InjectMocks
	private SemestreServiceImpl service;

	@Test
	void guardarVinculaDetalles() throws ServiceException {
		Semestre semestre = new Semestre();
		SemestreDetalle detalle = new SemestreDetalle();
		semestre.setSemestreDetalles(List.of(detalle));
		when(repository.save(semestre)).thenReturn(semestre);

		assertSame(semestre, service.save(semestre));
		assertSame(semestre, detalle.getIdSemestre());
	}

	@Test
	void actualizarRequiereRegistroExistente() {
		Semestre semestre = new Semestre();
		semestre.setId_semestre(7L);
		assertThrows(ServiceException.class, () -> service.update(semestre));
	}

	@Test
	void eliminarIndicaSiExiste() throws ServiceException {
		Semestre semestre = new Semestre();
		semestre.setId_semestre(7L);
		assertFalse(service.delete(semestre));
		when(repository.existsById(7L)).thenReturn(true);
		assertTrue(service.delete(semestre));
		verify(repository).deleteById(7L);
	}

	@Test
	void buscarPorEjemploDevuelveResultados() throws ServiceException {
		Semestre filtro = new Semestre();
		filtro.setSemestre("2026");
		when(repository.findAll(any(Example.class))).thenReturn(List.of(filtro));
		assertEquals(List.of(filtro), service.findByLikeObject(filtro));
	}
}
