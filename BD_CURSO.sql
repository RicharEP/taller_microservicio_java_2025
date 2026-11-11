
-- Ejecutar conectado a BD_CURSO, creada previamente por el administrador.
	
	CREATE SCHEMA IF NOT EXISTS sys;
	
	CREATE SEQUENCE IF NOT EXISTS sys.curso_id_seq
    INCREMENT 1
    START 1
    MINVALUE 1
    MAXVALUE 9223372036854775807
    CACHE 1;

ALTER SEQUENCE sys.curso_id_seq
    OWNER TO postgres;
	

CREATE TABLE IF NOT EXISTS sys.curso
(
    id_curso integer NOT NULL DEFAULT nextval('sys.curso_id_seq'::regclass),
    nombre character varying(100) COLLATE pg_catalog."default",
    credito integer,
    id_docente integer,
    CONSTRAINT curso_pkey PRIMARY KEY (id_curso)
)
TABLESPACE pg_default;

ALTER TABLE IF EXISTS sys.curso
    OWNER to postgres;
