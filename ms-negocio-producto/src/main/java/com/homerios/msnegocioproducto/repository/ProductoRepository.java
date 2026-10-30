package com.homerios.msnegocioproducto.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.homerios.msnegocioproducto.model.Producto;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {

	boolean existsByCodigo(String codigo);

	List<Producto> findByActivoTrue();

	@Query("select p from Producto p where p.activo = true and p.stockActual <= p.stockMinimo order by p.stockActual")
	List<Producto> findConStockBajo();
}
