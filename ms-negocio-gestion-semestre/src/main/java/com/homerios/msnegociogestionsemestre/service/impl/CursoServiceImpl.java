package com.homerios.msnegociogestionsemestre.service.impl;

import org.springframework.stereotype.Component;

import com.homerios.msnegociogestionsemestre.dto.CursoDTO;
import com.homerios.msnegociogestionsemestre.service.CursoService;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.client.RestTemplate;

@RequiredArgsConstructor
@Component
public class CursoServiceImpl implements CursoService {

	private static final Logger log = LogManager.getLogger(CursoServiceImpl.class);

	// @Autowired(required=true)
	private RestTemplate restTemplate = new RestTemplate();

	@Autowired
	private DiscoveryClient discoveryClient;

	private String serviceName = "ms-negocio-gestion-curso";

	@Override
	public CursoDTO findById(Long id) {
		CursoDTO curso = new CursoDTO();
		String url = this.getURI();

		log.info("url {}", url);
		if (url != null) {
			url = url + "/curso/" + id;
			ResponseEntity<CursoDTO> cursoDTO = restTemplate.getForEntity(url, CursoDTO.class);
			if (cursoDTO.getStatusCode() == HttpStatus.OK) {
				if (cursoDTO.hasBody()) {
					curso = cursoDTO.getBody();
				}
				log.info("docenteDTO  DocententeServiceImpl {}", cursoDTO);
			}
		}
		if (url == null) {
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Servicio de cursos no disponible");
		}
		return curso;
	}

	private String getURI() {

		if (Objects.isNull(discoveryClient)) {
			log.info("discoveryClient is null");
			return null;
		}
		List<ServiceInstance> instances = discoveryClient.getInstances(serviceName);

		if (Objects.isNull(instances) || instances.isEmpty()) {
			log.info("instances is null");
			return null;
		}

		log.info("instances.get(0).getHost() => {}", instances.get(0).getHost());

		String uri = instances.get(0).getUri().toString();
		log.info("uri => {}", uri);

		return uri;
	}

}
