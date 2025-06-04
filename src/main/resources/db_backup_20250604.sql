--
-- PostgreSQL database dump
--

-- Dumped from database version 15.13
-- Dumped by pg_dump version 15.13

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: account; Type: TABLE; Schema: public; Owner: emsp
--

CREATE TABLE public.account (
    email character varying(255) NOT NULL,
    contract_id character varying(255) NOT NULL,
    status character varying(20) NOT NULL,
    last_updated timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT account_contract_id_check CHECK (((contract_id)::text ~ '^[A-Z0-9]{32}$'::text))
);


ALTER TABLE public.account OWNER TO emsp;

--
-- Name: card; Type: TABLE; Schema: public; Owner: emsp
--

CREATE TABLE public.card (
    uid character varying(255) NOT NULL,
    visible_number character varying(255) NOT NULL,
    status character varying(20) NOT NULL,
    account_email character varying(255),
    last_updated timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.card OWNER TO emsp;

--
-- Name: flyway_schema_history; Type: TABLE; Schema: public; Owner: emsp
--

CREATE TABLE public.flyway_schema_history (
    installed_rank integer NOT NULL,
    version character varying(50),
    description character varying(200) NOT NULL,
    type character varying(20) NOT NULL,
    script character varying(1000) NOT NULL,
    checksum integer,
    installed_by character varying(100) NOT NULL,
    installed_on timestamp without time zone DEFAULT now() NOT NULL,
    execution_time integer NOT NULL,
    success boolean NOT NULL
);


ALTER TABLE public.flyway_schema_history OWNER TO emsp;

--
-- Data for Name: account; Type: TABLE DATA; Schema: public; Owner: emsp
--

COPY public.account (email, contract_id, status, last_updated) FROM stdin;
\.


--
-- Data for Name: card; Type: TABLE DATA; Schema: public; Owner: emsp
--

COPY public.card (uid, visible_number, status, account_email, last_updated) FROM stdin;
\.


--
-- Data for Name: flyway_schema_history; Type: TABLE DATA; Schema: public; Owner: emsp
--

COPY public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) FROM stdin;
1	1	init tables	SQL	V1__init_tables.sql	750672708	emsp	2025-06-03 20:49:24.799373	37	t
\.


--
-- Name: account account_pkey; Type: CONSTRAINT; Schema: public; Owner: emsp
--

ALTER TABLE ONLY public.account
    ADD CONSTRAINT account_pkey PRIMARY KEY (email);


--
-- Name: card card_pkey; Type: CONSTRAINT; Schema: public; Owner: emsp
--

ALTER TABLE ONLY public.card
    ADD CONSTRAINT card_pkey PRIMARY KEY (uid);


--
-- Name: flyway_schema_history flyway_schema_history_pk; Type: CONSTRAINT; Schema: public; Owner: emsp
--

ALTER TABLE ONLY public.flyway_schema_history
    ADD CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank);


--
-- Name: flyway_schema_history_s_idx; Type: INDEX; Schema: public; Owner: emsp
--

CREATE INDEX flyway_schema_history_s_idx ON public.flyway_schema_history USING btree (success);


--
-- Name: card card_account_email_fkey; Type: FK CONSTRAINT; Schema: public; Owner: emsp
--

ALTER TABLE ONLY public.card
    ADD CONSTRAINT card_account_email_fkey FOREIGN KEY (account_email) REFERENCES public.account(email) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--

