-- Write that the 2.2->2.3 has started
UPDATE ObmInfo SET obminfo_value='2.2.x->2.3.0' WHERE obminfo_name='db_version';
-- -----------------------------------------------------------------------------

--
-- Table structure for table `addressbook`
--
CREATE TABLE addressbook (
  id         integer NOT NULL,
  domain_id  integer NOT NULL,
  timeupdate timestamp without time zone,
  timecreate timestamp without time zone,
  userupdate integer default NULL,
  usercreate integer default NULL,
  origin     varchar(255) NOT NULL,
  owner      integer default NULL,
  name       varchar(64) NOT NULL,
  is_default    boolean default false,
  syncable   boolean default true
);

--
-- Addressbook id sequence
--
CREATE SEQUENCE addressbook_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;

ALTER SEQUENCE addressbook_id_seq OWNED BY addressbook.id;
ALTER TABLE addressbook ALTER COLUMN id SET DEFAULT nextval('addressbook_id_seq'::regclass);

--
-- addressbook indexes
--
ALTER TABLE ONLY addressbook
    ADD CONSTRAINT addressbook_pkey PRIMARY KEY (id);
CREATE INDEX addressbook_domain_id_fkey ON addressbook (domain_id);
CREATE INDEX addressbook_userupdate_fkey ON addressbook (userupdate);
CREATE INDEX addressbook_usercreate_fkey ON addressbook (usercreate);
CREATE INDEX addressbook_owner_fkey ON addressbook (owner);

--
-- addressbook fkey
--
ALTER TABLE ONLY addressbook
    ADD CONSTRAINT addressbook_domain_id_domain_id_fkey FOREIGN KEY (domain_id) REFERENCES domain(domain_id) ON UPDATE CASCADE ON DELETE CASCADE;
ALTER TABLE ONLY addressbook
    ADD CONSTRAINT addressbook_userupdate_userobm_id_fkey FOREIGN KEY (userupdate) REFERENCES userobm(userobm_id) ON UPDATE CASCADE ON DELETE SET NULL;
ALTER TABLE ONLY addressbook
    ADD CONSTRAINT addressbook_usercreate_userobm_id_fkey FOREIGN KEY (usercreate) REFERENCES userobm(userobm_id) ON UPDATE CASCADE ON DELETE SET NULL;
ALTER TABLE ONLY addressbook
    ADD CONSTRAINT addressbook_owner_userobm_id_fkey FOREIGN KEY (owner) REFERENCES userobm(userobm_id) ON UPDATE CASCADE ON DELETE SET NULL;


--
-- Table structure for table `addressbookentity`
--
CREATE TABLE addressbookentity (
  addressbookentity_entity_id      integer NOT NULL,
  addressbookentity_addressbook_id integer NOT NULL
);

ALTER TABLE ONLY addressbookentity
    ADD CONSTRAINT addressbookentity_pkey PRIMARY KEY (addressbookentity_entity_id, addressbookentity_addressbook_id);

--
-- addressbookentity indexes
--
CREATE INDEX addressbookentity_addressbook_id_addressbook_id_fkey ON addressbookentity (addressbookentity_addressbook_id);
CREATE INDEX addressbookentity_entity_id_entity_id_fkey ON addressbookentity (addressbookentity_entity_id);

--
-- addressbookentity fkey
--
ALTER TABLE ONLY addressbookentity
    ADD CONSTRAINT addressbookentity_addressbook_id_addressbook_id_fkey FOREIGN KEY (addressbookentity_addressbook_id) REFERENCES addressbook(id) ON UPDATE CASCADE ON DELETE CASCADE;
ALTER TABLE ONLY addressbookentity
    ADD CONSTRAINT addressbookentity_entity_id_entity_id_fkey FOREIGN KEY (addressbookentity_entity_id) REFERENCES entity(entity_id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Contact update
--
ALTER TABLE Contact ADD COLUMN contact_addressbook_id integer default NULL;
CREATE INDEX contact_addressbook_id_addressbook_id_fkey ON contact (contact_addressbook_id);

ALTER TABLE Contact ADD CONSTRAINT contact_addressbook_id_addressbook_id_fkey FOREIGN KEY (contact_addressbook_id) REFERENCES addressbook(id) ON UPDATE CASCADE ON DELETE CASCADE;

ALTER TABLE Contact DROP COLUMN contact_privacy;


--
-- Table structure for table `DeletedAddressbook`
--
CREATE TABLE deletedaddressbook (
  addressbook_id integer NOT NULL,
  user_id        integer NOT NULL,
  timestamp      timestamp without time zone,
  origin         varchar(255) NOT NULL
);

--
-- deletedaddressbook indexes
--
ALTER TABLE ONLY deletedaddressbook
  ADD CONSTRAINT deletedaddressbook_pkey PRIMARY KEY (addressbook_id);


--
-- Table structure for table SyncedAddressbook
--
CREATE TABLE syncedaddressbook (
  user_id        integer NOT NULL,
  addressbook_id integer NOT NULL,
  timestamp      timestamp without time zone NOT NULL DEFAULT now()
);

--
-- addressbook indexes
--
ALTER TABLE ONLY syncedaddressbook
  ADD CONSTRAINT syncedaddressbook_pkey PRIMARY KEY (user_id, addressbook_id);

ALTER TABLE ONLY syncedaddressbook
    ADD CONSTRAINT syncedaddressbook_user_id_userobm_id_fkey FOREIGN KEY (user_id) REFERENCES userobm(userobm_id) ON UPDATE CASCADE ON DELETE CASCADE;

ALTER TABLE ONLY syncedaddressbook
    ADD CONSTRAINT syncedaddressbook_addressbook_id_addressbook_id_fkey FOREIGN KEY (addressbook_id) REFERENCES addressbook(id) ON UPDATE CASCADE ON DELETE CASCADE;


DROP TABLE IF EXISTS SynchedContact;

-- EventTag
CREATE TABLE eventtag ( 
  eventtag_id integer NOT NULL, 
  eventtag_user_id integer NOT NULL, 
  eventtag_label character varying(128) DEFAULT ''::character varying,
  eventtag_color character(7) default NULL 
);

CREATE SEQUENCE eventtag_eventtag_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;

ALTER SEQUENCE eventtag_eventtag_id_seq OWNED BY eventtag.eventtag_id;
ALTER TABLE eventtag ALTER COLUMN eventtag_id SET DEFAULT nextval('eventtag_eventtag_id_seq'::regclass);
ALTER TABLE ONLY eventtag ADD CONSTRAINT eventtag_pkey PRIMARY KEY (eventtag_id);
ALTER TABLE event ADD COLUMN event_tag_id integer default NULL;
ALTER TABLE ONLY event ADD CONSTRAINT event_tag_id_eventtag_id_fkey FOREIGN KEY (event_tag_id) REFERENCES eventtag(eventtag_id) ON UPDATE CASCADE ON DELETE SET NULL;
ALTER TABLE ONLY eventtag ADD CONSTRAINT eventtag_user_id_userobm_id_fkey FOREIGN KEY (eventtag_user_id) REFERENCES userobm(userobm_id) ON UPDATE CASCADE ON DELETE SET NULL;

-- EventTemplate
CREATE TABLE eventtemplate (
    eventtemplate_id integer NOT NULL,
    eventtemplate_domain_id integer NOT NULL,
    eventtemplate_timeupdate timestamp without time zone,
    eventtemplate_timecreate timestamp without time zone DEFAULT now(),
    eventtemplate_userupdate integer DEFAULT NULL,
    eventtemplate_usercreate integer DEFAULT NULL,
    eventtemplate_owner integer,
    eventtemplate_name character varying(255) DEFAULT NULL::character varying,
    eventtemplate_title character varying(255) DEFAULT NULL::character varying,
    eventtemplate_location character varying(100) DEFAULT NULL::character varying,
    eventtemplate_category1_id integer,
    eventtemplate_priority integer,
    eventtemplate_privacy integer,
    eventtemplate_date timestamp without time zone,
    eventtemplate_duration integer DEFAULT 0 NOT NULL,
    eventtemplate_allday boolean DEFAULT false,
    eventtemplate_repeatkind character varying(20) DEFAULT 'none' NOT NULL,
    eventtemplate_repeatfrequence integer,
    eventtemplate_repeatdays character varying(7) DEFAULT NULL::character varying,
    eventtemplate_endrepeat timestamp without time zone,
    eventtemplate_allow_documents boolean DEFAULT false,
    eventtemplate_alert integer DEFAULT 0 NOT NULL,
    eventtemplate_description text,
    eventtemplate_properties text,
    eventtemplate_tag_id integer default NULL,
    eventtemplate_user_ids text default NULL,
    eventtemplate_contact_ids text default NULL,
    eventtemplate_resource_ids text default NULL,
    eventtemplate_document_ids text default NULL,
    eventtemplate_group_ids text default NULL
);

CREATE SEQUENCE eventtemplate_eventtemplate_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;
    
ALTER SEQUENCE eventtemplate_eventtemplate_id_seq OWNED BY eventtemplate.eventtemplate_id;
ALTER TABLE eventtemplate ALTER COLUMN eventtemplate_id SET DEFAULT nextval('eventtemplate_eventtemplate_id_seq'::regclass);
ALTER TABLE ONLY eventtemplate ADD CONSTRAINT eventtemplate_pkey PRIMARY KEY (eventtemplate_id);
CREATE INDEX eventtemplate_category1_id_fkey ON eventtemplate (eventtemplate_category1_id);
CREATE INDEX eventtemplate_domain_id_fkey ON eventtemplate (eventtemplate_domain_id);
CREATE INDEX eventtemplate_owner_fkey ON eventtemplate (eventtemplate_owner);
CREATE INDEX eventtemplate_usercreate_fkey ON eventtemplate (eventtemplate_usercreate);
CREATE INDEX eventtemplate_userupdate_fkey ON eventtemplate (eventtemplate_userupdate);
ALTER TABLE ONLY eventtemplate ADD CONSTRAINT eventtemplate_category1_id_eventcategory1_id_fkey FOREIGN KEY (eventtemplate_category1_id) REFERENCES eventcategory1(eventcategory1_id) ON UPDATE CASCADE ON DELETE SET NULL;
ALTER TABLE ONLY eventtemplate ADD CONSTRAINT eventtemplate_domain_id_domain_id_fkey FOREIGN KEY (eventtemplate_domain_id) REFERENCES domain(domain_id) ON UPDATE CASCADE ON DELETE CASCADE;
ALTER TABLE ONLY eventtemplate ADD CONSTRAINT eventtemplate_owner_userobm_id_fkey FOREIGN KEY (eventtemplate_owner) REFERENCES userobm(userobm_id) ON UPDATE CASCADE ON DELETE CASCADE;
ALTER TABLE ONLY eventtemplate ADD CONSTRAINT eventtemplate_usercreate_userobm_id_fkey FOREIGN KEY (eventtemplate_usercreate) REFERENCES userobm(userobm_id) ON UPDATE CASCADE ON DELETE SET NULL;
ALTER TABLE ONLY eventtemplate ADD CONSTRAINT eventtemplate_userupdate_userobm_id_fkey FOREIGN KEY (eventtemplate_userupdate) REFERENCES userobm(userobm_id) ON UPDATE CASCADE ON DELETE SET NULL;
ALTER TABLE ONLY eventtemplate ADD CONSTRAINT eventtemplate_tag_id_eventtag_id_fkey FOREIGN KEY (eventtemplate_tag_id) REFERENCES eventtag(eventtag_id) ON UPDATE CASCADE ON DELETE SET NULL;

-- Event's document attachments
ALTER TABLE event ADD COLUMN event_allow_documents boolean default false;
ALTER TABLE documentlink ADD COLUMN documentlink_usercreate integer DEFAULT NULL;
CREATE INDEX documentlink_usercreate_fkey ON documentlink (documentlink_usercreate);
ALTER TABLE ONLY documentlink ADD CONSTRAINT documentlink_usercreate_userobm_id_fkey FOREIGN KEY (documentlink_usercreate) REFERENCES userobm(userobm_id) ON UPDATE CASCADE ON DELETE SET NULL;
-- -----------------------------------------------------------------------------


-- Write that the 2.2->2.3 is completed
UPDATE ObmInfo SET obminfo_value='2.3.0' WHERE obminfo_name='db_version';

INSERT INTO DisplayPref (display_user_id,display_entity,display_fieldname,display_fieldorder,display_display) VALUES (NULL,'people', 'userobm_direction', 11, 1);
INSERT INTO DisplayPref (display_user_id,display_entity,display_fieldname,display_fieldorder,display_display) VALUES (NULL,'people', 'userobm_service', 12, 1);
INSERT INTO DisplayPref (display_user_id,display_entity,display_fieldname,display_fieldorder,display_display) VALUES (NULL,'people', 'userobm_address', 13, 1);
INSERT INTO DisplayPref (display_user_id,display_entity,display_fieldname,display_fieldorder,display_display) VALUES (NULL,'people', 'userobm_town', 14, 1);
INSERT INTO DisplayPref (display_user_id,display_entity,display_fieldname,display_fieldorder,display_display) VALUES (NULL,'people', 'userobm_zipcode', 15, 1);
INSERT INTO DisplayPref (display_user_id,display_entity,display_fieldname,display_fieldorder,display_display) VALUES (NULL,'user', 'userobm_direction', 26, 1);
INSERT INTO DisplayPref (display_user_id,display_entity,display_fieldname,display_fieldorder,display_display) VALUES (NULL,'user', 'userobm_service', 27, 1);
