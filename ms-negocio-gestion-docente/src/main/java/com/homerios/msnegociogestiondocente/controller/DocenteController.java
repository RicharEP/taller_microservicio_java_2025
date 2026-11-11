package com.homerios.msnegociogestiondocente.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.homerios.msnegociogestiondocente.model.Docente;
import com.homerios.msnegociogestiondocente.repository.DocenteRepository;

@RestController
public class DocenteController {
	@Autowired
	private DocenteRepository docenteRepository;
	
	@GetMapping("/docentes")
	public List<Docente> obtenerDocentes() {
		return docenteRepository.findAll();
	}

	@GetMapping("/docente/{id}")
	public ResponseEntity<Docente> busDocente(@PathVariable("id") Long id) {
		return docenteRepository.findById(id).map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.notFound().build());
	}

	@PostMapping("/docente")
	@ResponseStatus(HttpStatus.CREATED)
	public Docente crearDocente(@RequestBody Docente nDocente) {
		Docente tDocente = new Docente();
		tDocente.setNombre(nDocente.getNombre());
		tDocente.setAp_paterno(nDocente.getAp_paterno());
		tDocente.setAp_materno(nDocente.getAp_materno());
		tDocente.setUrl(nDocente.getUrl());
		tDocente.setCorreo(nDocente.getCorreo());
		tDocente.setDireccion(nDocente.getDireccion());
		tDocente.setDni(nDocente.getDni());

		return docenteRepository.save(tDocente);
	}

	@PutMapping("/docente/{id}")
	public ResponseEntity<Docente> actualizarDocente(@PathVariable("id") Long id, @RequestBody Docente aDocente) {
		return docenteRepository.findById(id).map(t_Docente -> {
			t_Docente.setNombre(aDocente.getNombre());
			t_Docente.setAp_paterno(aDocente.getAp_paterno());
			t_Docente.setAp_materno(aDocente.getAp_materno());
			t_Docente.setUrl(aDocente.getUrl());
			t_Docente.setCorreo(aDocente.getCorreo());
			t_Docente.setDireccion(aDocente.getDireccion());
			t_Docente.setDni(aDocente.getDni());
			return ResponseEntity.ok(docenteRepository.save(t_Docente));
		}).orElseGet(() -> ResponseEntity.notFound().build());

	}

	@DeleteMapping("/docente/{id}")
	public ResponseEntity<Void> deleteDocente(@PathVariable("id") Long id) {
		if (!docenteRepository.existsById(id)) {
			return ResponseEntity.notFound().build();
		}
		docenteRepository.deleteById(id);
		return ResponseEntity.noContent().build();
	}
}
