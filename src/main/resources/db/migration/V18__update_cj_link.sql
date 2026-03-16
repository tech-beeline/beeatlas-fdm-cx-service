DROP TABLE if EXISTS cx.cj_link;
CREATE TABLE cx.cj_link (
                            id int4 NOT NULL,
                            url varchar(4000) NOT NULL,
                            descr varchar(50) NULL,
                            id_cj int4 NOT NULL,
                            CONSTRAINT pk_cjlink PRIMARY KEY (id)
);
ALTER TABLE cx.cj_link ADD CONSTRAINT fk_cj_link_cj_id FOREIGN KEY (id_cj) REFERENCES cx.cj(id) ON DELETE CASCADE;