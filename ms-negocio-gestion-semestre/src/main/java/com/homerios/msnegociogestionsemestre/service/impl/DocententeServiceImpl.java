package com.homerios.msnegociogestionsemestre.service.impl;

import java.util.List;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import com.homerios.msnegociogestionsemestre.dto.DocenteDTO;
import com.homerios.msnegociogestionsemestre.service.DocententeService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Component
public class DocententeServiceImpl implements DocententeService {

	private static final Logger log = LogManager.getLogger(DocententeServiceImpl.class);

	// @Autowired(required=true)
	private RestTemplate restTemplate = new RestTemplate();

	@Autowired
	private DiscoveryClient discoveryClient;

	private String serviceName = "ms-negocio-gestion-docente";

	@Override
	public DocenteDTO findById(Long id) {
		DocenteDTO docente = new DocenteDTO();
		String url = this.getURI();
		
		log.info("url {}", url);
		if (url != null) {
			url = url + "/docente/" + id;
			ResponseEntity<DocenteDTO> docenteDTO = restTemplate.getForEntity(url, DocenteDTO.class);
			if (docenteDTO.getStatusCode() == HttpStatus.OK) {
				if (docenteDTO.hasBody()) {
					docente = docenteDTO.getBody();
				}
				log.info("docenteDTO  DocententeServiceImpl {}", docenteDTO);
			}
		}
		if (url == null) {
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Servicio de docentes no disponible");
		}
		return docente;
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
