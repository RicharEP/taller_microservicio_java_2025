


-- Ejecutar conectado a BD_SEMESTRE, creada previamente por el administrador.
	
	
CREATE SCHEMA IF NOT EXISTS sys;

CREATE SEQUENCE IF NOT EXISTS sys.semestre_detalle_id_seq
    INCREMENT 1
    START 1
    MINVALUE 1
    MAXVALUE 9223372036854775807
    CACHE 1;

ALTER SEQUENCE sys.semestre_detalle_id_seq
    OWNER TO postgres;
	
	
CREATE SEQUENCE IF NOT EXISTS sys.semestre_id_seq
    INCREMENT 1
    START 1
    MINVALUE 1
    MAXVALUE 9223372036854775807
    CACHE 1;

ALTER SEQUENCE sys.semestre_id_seq
    OWNER TO postgres;
	

CREATE TABLE IF NOT EXISTS sys.semestre
(
    id_semestre integer NOT NULL DEFAULT nextval('sys.semestre_id_seq'::regclass),
    fecha_registro character varying(100) COLLATE pg_catalog."default",
    fecha_inicio character varying(100) COLLATE pg_catalog."default",
    fecha_fin character varying(100) COLLATE pg_catalog."default",
    semestre character varying(100) COLLATE pg_catalog."default",
    CONSTRAINT id_semestre_pkey PRIMARY KEY (id_semestre)
)
TABLESPACE pg_default;

ALTER TABLE IF EXISTS sys.semestre
    OWNER to postgres;
	
	

CREATE TABLE IF NOT EXISTS sys.semestre_detalle
(
    id_semestre_det integer NOT NULL DEFAULT nextval('sys.semestre_detalle_id_seq'::regclass),
    id_semestre integer,
    id_docente integer,
    id_curso integer,
    horas_lteoricas integer,
    horas_practicas integer,
    CONSTRAINT id_semestre_det_pkey PRIMARY KEY (id_semestre_det),
    CONSTRAINT fk_semestre_id FOREIGN KEY (id_semestre)
        REFERENCES sys.semestre (id_semestre) MATCH SIMPLE
        ON UPDATE CASCADE
        ON DELETE CASCADE
)
TABLESPACE pg_default;

ALTER TABLE IF EXISTS sys.semestre_detalle
    OWNER to postgres;
