
-- Ejecutar conectado a BD_DOCENTE, creada previamente por el administrador.
	

CREATE SCHEMA IF NOT EXISTS sys;


CREATE SEQUENCE IF NOT EXISTS sys.docente_id_seq
    INCREMENT 1
    START 1
    MINVALUE 1
    MAXVALUE 9223372036854775807
    CACHE 1;

ALTER SEQUENCE sys.docente_id_seq
    OWNER TO postgres;
	



CREATE TABLE IF NOT EXISTS sys.docente
(
    id_docente integer NOT NULL DEFAULT nextval('sys.docente_id_seq'::regclass),
    nombre character varying(100) COLLATE pg_catalog."default",
    ap_paterno character varying(100) COLLATE pg_catalog."default",
    ap_materno character varying(100) COLLATE pg_catalog."default",
    url character varying(100) COLLATE pg_catalog."default",
    correo character varying(100) COLLATE pg_catalog."default",
    direccion character varying(100) COLLATE pg_catalog."default",
    dni character varying(8) COLLATE pg_catalog."default",
    CONSTRAINT docente_pkey PRIMARY KEY (id_docente)
)
TABLESPACE pg_default;

ALTER TABLE IF EXISTS sys.docente
    OWNER to postgres;
