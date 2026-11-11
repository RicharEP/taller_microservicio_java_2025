package com.homerios.msnegociogestionsemestre.rest;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.homerios.msnegociogestionsemestre.dto.CursoDTO;
import com.homerios.msnegociogestionsemestre.dto.DocenteDTO;
import com.homerios.msnegociogestionsemestre.dto.SemestreDTO;
import com.homerios.msnegociogestionsemestre.dto.SemestreDetalleDTO;
import com.homerios.msnegociogestionsemestre.entity.Semestre;
import com.homerios.msnegociogestionsemestre.entity.SemestreDetalle;
import com.homerios.msnegociogestionsemestre.repository.SemestreRepository;
import com.homerios.msnegociogestionsemestre.service.CursoService;
import com.homerios.msnegociogestionsemestre.service.DocententeService;

@RestController
public class SemestreRest {

	@Autowired
	private SemestreRepository semestrerepository;

	@Autowired
	private DocententeService docententeService;

	@Autowired
	private CursoService cursoService;

	@GetMapping("/semestres")
	public List<Semestre> obtenerSemestres() {
		return semestrerepository.findAll();
	}

	@GetMapping("/semestre/{id}")
	public SemestreDTO buscarSemestre(@PathVariable Long id) {
		Semestre t_Semestre = semestrerepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Semestre no encontrado"));
		SemestreDTO semestreDTO = new SemestreDTO();

			// se transfiere la informacion de semestre a semestre dto
			semestreDTO.setId_semestre(t_Semestre.getId_semestre());
			semestreDTO.setSemestre(t_Semestre.getSemestre());
			semestreDTO.setFecha_inicio(t_Semestre.getFecha_inicio());
			semestreDTO.setFecha_fin(t_Semestre.getFecha_fin());
			semestreDTO.setFecha_registro(t_Semestre.getFecha_registro());
			// se declara una lista para la carga del detalle de persona
			List<SemestreDetalleDTO> semestreDetallesDTO = new ArrayList<>();

			SemestreDetalleDTO semestreDetalleDTO = new SemestreDetalleDTO();

			if (t_Semestre.getSemestreDetalles() != null) {
				for (SemestreDetalle sd : t_Semestre.getSemestreDetalles()) {
					DocenteDTO docenteDTO = new DocenteDTO();
					CursoDTO cursoDTO = new CursoDTO();

					semestreDetalleDTO = new SemestreDetalleDTO();
					//semestreDetalleDTO.setIdSemestre(t_Semestre);
					semestreDetalleDTO.setHoras_lteoricas(sd.getHoras_lteoricas());
					semestreDetalleDTO.setHoras_practicas(sd.getHoras_practicas());

					if (sd.getId_docente() != null) {
						docenteDTO = docententeService.findById(sd.getId_docente());
						semestreDetalleDTO.setDocente(docenteDTO);
					}
					if (sd.getId_curso() != null) {
						cursoDTO = cursoService.findById(sd.getId_curso());
						semestreDetalleDTO.setCurso(cursoDTO);
					}
					semestreDetallesDTO.add(semestreDetalleDTO);
				}
			}

			semestreDTO.setSemestreDetalles(semestreDetallesDTO);
		return semestreDTO;
	}

}
