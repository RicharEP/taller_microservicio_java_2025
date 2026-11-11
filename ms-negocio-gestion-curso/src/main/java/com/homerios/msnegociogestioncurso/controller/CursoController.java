package com.homerios.msnegociogestioncurso.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.homerios.msnegociogestioncurso.model.Curso;
import com.homerios.msnegociogestioncurso.repository.CursoRepository;

@RestController
public class CursoController {
	@Autowired
	private CursoRepository cursoRepository;

	@GetMapping("/cursos")
	public List<Curso> obtenerCursos() {
		return cursoRepository.findAll();
	}

	@GetMapping("/curso/{id}")
	public ResponseEntity<Curso> busCurso(@PathVariable("id") Long id) {
		return cursoRepository.findById(id).map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.notFound().build());
	}

	@PostMapping("/curso")
	@ResponseStatus(HttpStatus.CREATED)
	public Curso crearCurso(@RequestBody Curso nCurso) {
		Curso tCurso = new Curso();
		tCurso.setNombre(nCurso.getNombre());
		tCurso.setCredito(nCurso.getCredito());
		tCurso.setId_docente(nCurso.getId_docente());

		return cursoRepository.save(tCurso);
	}

	@PutMapping("/curso/{id}")
	public ResponseEntity<Curso> actualizarCurso(@PathVariable("id") Long id, @RequestBody Curso aCurso) {

		return cursoRepository.findById(id).map(curso -> {
			curso.setNombre(aCurso.getNombre());
			curso.setCredito(aCurso.getCredito());
			curso.setId_docente(aCurso.getId_docente());
			Curso actualizado = cursoRepository.save(curso);
			return ResponseEntity.ok(actualizado);
		}).orElseGet(() -> ResponseEntity.notFound().build());

	}
	/*
	 * @PutMapping("/curso/{id}") public Optional<Curso>
	 * actualizarCurso(@PathVariable("id") String id, @RequestBody Curso aCurso) {
	 * Long id_curso = Long.parseLong(id); Optional<Curso> tCurso =
	 * cursoRepository.findById(id_curso); if (tCurso.isPresent()) { Curso t_curso =
	 * tCurso.get(); t_curso.setNombre(aCurso.getNombre());
	 * t_curso.setCredito(aCurso.getCredito());
	 * t_curso.setId_docente(aCurso.getId_docente()); cursoRepository.save(t_curso);
	 * } return tCurso; }
	 */

	@DeleteMapping("/curso/{id}")
	public ResponseEntity<Void> deleteCurso(@PathVariable("id") Long id) {
		if (!cursoRepository.existsById(id)) {
			return ResponseEntity.notFound().build();
		}
		cursoRepository.deleteById(id);
		return ResponseEntity.noContent().build();
	}
}
