package com.homerios.msnegociogestionsemestre.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.homerios.msnegociogestionsemestre.entity.Semestre;
import com.homerios.msnegociogestionsemestre.repository.SemestreRepository;
import com.homerios.msnegociogestionsemestre.service.SemestreService;
import com.homerios.msnegociogestionsemestre.service.ServiceException;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
@Transactional(rollbackFor = ServiceException.class)
public class SemestreServiceImpl implements SemestreService {

	private final SemestreRepository semestreRepository;

	@Override
	@Transactional(readOnly = true)
	public List<Semestre> findByLikeObject(Semestre t) throws ServiceException {
		try {
			if (t == null) {
				return semestreRepository.findAll();
			}
			ExampleMatcher matcher = ExampleMatcher.matchingAll()
					.withIgnoreNullValues()
					.withIgnorePaths("semestreDetalles")
					.withStringMatcher(ExampleMatcher.StringMatcher.CONTAINING)
					.withIgnoreCase();
			return semestreRepository.findAll(Example.of(t, matcher));
		} catch (Exception e) {
			throw new ServiceException("No se pudieron consultar los semestres", e);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Semestre> findById(Long id) throws ServiceException {
		try {
			return semestreRepository.findById(id);
		} catch (Exception e) {
			throw new ServiceException("No se pudo consultar el semestre " + id, e);
		}
	}

	@Override
	public Semestre save(Semestre t) throws ServiceException {
		if (t == null || t.getId_semestre() != null) {
			throw new ServiceException("Un semestre nuevo no debe tener identificador");
		}
		try {
			vincularDetalles(t);
			return semestreRepository.save(t);
		} catch (Exception e) {
			throw new ServiceException("No se pudo guardar el semestre", e);
		}
	}

	@Override
	public Semestre update(Semestre t) throws ServiceException {
		if (t == null || t.getId_semestre() == null) {
			throw new ServiceException("Se requiere el identificador del semestre");
		}
		try {
			if (!semestreRepository.existsById(t.getId_semestre())) {
				throw new ServiceException("No existe el semestre " + t.getId_semestre());
			}
			vincularDetalles(t);
			return semestreRepository.save(t);
		} catch (ServiceException e) {
			throw e;
		} catch (Exception e) {
			throw new ServiceException("No se pudo actualizar el semestre", e);
		}
	}

	@Override
	public Boolean delete(Semestre t) throws ServiceException {
		if (t == null || t.getId_semestre() == null) {
			throw new ServiceException("Se requiere el identificador del semestre");
		}
		try {
			if (!semestreRepository.existsById(t.getId_semestre())) {
				return false;
			}
			semestreRepository.deleteById(t.getId_semestre());
			return true;
		} catch (Exception e) {
			throw new ServiceException("No se pudo eliminar el semestre", e);
		}
	}

	private void vincularDetalles(Semestre semestre) {
		if (semestre.getSemestreDetalles() != null) {
			semestre.getSemestreDetalles().forEach(detalle -> detalle.setIdSemestre(semestre));
		}
	}
}
