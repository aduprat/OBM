--/////////////////////////////////////////////////////////////////////////////
--// OBM - File : create_obmdb_0.9.pgsql.sql                                 //
--//     - Desc : PostGreSQL Database 0.9 creation script                    //
--// 2004-12-30 Pierre Baudracco                                             //
--/////////////////////////////////////////////////////////////////////////////
-- $Id$
--/////////////////////////////////////////////////////////////////////////////

BEGIN;

-------------------------------------------------------------------------------
-- Database creation (old one is deleted !)
-------------------------------------------------------------------------------

-- drop database IF EXISTS obm;

-- create database obm;

-- use obm;


-------------------------------------------------------------------------------
-- Global Information table
-------------------------------------------------------------------------------
--
-- Table structure for table 'ObmInfo'
--
CREATE TABLE ObmInfo (
  obminfo_name   varchar(32) NOT NULL default '',
  obminfo_value  varchar(255) default '',
  PRIMARY KEY (obminfo_name)
);


-------------------------------------------------------------------------------
-- User, Preferences tables
-------------------------------------------------------------------------------
--
-- Table structure for table 'ObmSession'
--
CREATE TABLE ObmSession (
  obmsession_sid         varchar(32) NOT NULL default '',
  obmsession_timeupdate  timestamp,
  obmsession_name        varchar(32) NOT NULL default '',
  obmsession_data        text,
  PRIMARY KEY (obmsession_sid, obmsession_name)
);

--
-- Table structure for table 'ActiveUserObm'
--
CREATE TABLE ActiveUserObm (
  activeuserobm_sid            varchar(32) NOT NULL DEFAULT '',
  activeuserobm_session_name   varchar(32) NOT NULL DEFAULT '',
  activeuserobm_userobm_id     integer DEFAULT NULL,
  activeuserobm_timeupdate     timestamp,
  activeuserobm_timecreate     timestamp,
  activeuserobm_nb_connexions  integer NOT NULL DEFAULT 0,
  activeuserobm_lastpage       varchar(64) NOT NULL DEFAULT '0',
  activeuserobm_ip             varchar(32) NOT NULL DEFAULT '0',
  PRIMARY KEY (activeuserobm_sid)
);


--
-- Table structure for table 'UserObm_SessionLog'
--
CREATE TABLE UserObm_SessionLog (
  userobm_sessionlog_sid            varchar(32) NOT NULL DEFAULT '',
  userobm_sessionlog_session_name   varchar(32) NOT NULL DEFAULT '',
  userobm_sessionlog_userobm_id     integer DEFAULT NULL,
  userobm_sessionlog_timeupdate     timestamp,
  userobm_sessionlog_timecreate     timestamp,
  userobm_sessionlog_nb_connexions  integer NOT NULL DEFAULT 0,
  userobm_sessionlog_lastpage       varchar(32) NOT NULL DEFAULT '0',
  userobm_sessionlog_ip             varchar(32) NOT NULL DEFAULT '0',
  PRIMARY KEY (userobm_sessionlog_sid)
);


--
-- Table structure for table 'UserObm'
--
CREATE TABLE UserObm (
  userobm_id              serial,
  userobm_timeupdate      TIMESTAMP,
  userobm_timecreate      TIMESTAMP,
  userobm_userupdate      integer,
  userobm_usercreate      integer,
  userobm_local           integer DEFAULT 1,
  userobm_ext_id          varchar(16),
  userobm_login           varchar(32) DEFAULT '' NOT NULL,
  userobm_password        varchar(64) DEFAULT '' NOT NULL,
  userobm_perms           varchar(254),
  userobm_calendar_version  timestamp,
  userobm_archive         char(1) not null DEFAULT '0',
  userobm_datebegin       date,
  userobm_lastname        varchar(32),
  userobm_firstname       varchar(48),
  userobm_phone           varchar(32),
  userobm_phone2          varchar(32),
  userobm_fax             varchar(32),
  userobm_fax2            varchar(32),
  userobm_email           varchar(60),
  userobm_description     varchar(255),
  userobm_timelastaccess  TIMESTAMP,
  PRIMARY KEY (userobm_id),
  UNIQUE (userobm_login)
);
CREATE UNIQUE INDEX k_login_user_UserObm_index ON UserObm (userobm_login);


--
-- Table structure for table 'UserObmPref'
--
CREATE TABLE UserObmPref (
  userobmpref_user_id  integer DEFAULT 0 NOT NULL,
  userobmpref_option   varchar(50) NOT NULL,
  userobmpref_value    varchar(50) NOT NULL
);


--
-- New table 'DisplayPref'
--
CREATE TABLE DisplayPref (
  display_user_id     integer NOT NULL DEFAULT 0,
  display_entity      varchar(32) NOT NULL DEFAULT '',
  display_fieldname   varchar(64) NOT NULL DEFAULT '',
  display_fieldorder  integer DEFAULT NULL,
  display_display     integer NOT NULL DEFAULT 1,
  PRIMARY KEY(display_user_id, display_entity, display_fieldname)
);
create INDEX DisplayPref_user_id_index ON DisplayPref (display_user_id);
create INDEX DisplayPref_entity_index ON DisplayPref (display_entity);


-------------------------------------------------------------------------------
-- References Tables
-------------------------------------------------------------------------------
--
-- Table structure for the table  'DataSource'
--
CREATE TABLE DataSource (
  datasource_id          serial,
  datasource_timeupdate  timestamp,
  datasource_timecreate  timestamp,
  datasource_userupdate  integer,
  datasource_usercreate  integer,
  datasource_name        varchar(64),
  PRIMARY KEY (datasource_id)
);


--
-- Table structure for the table  'Country'
--
CREATE TABLE Country (
  country_timeupdate  timestamp,
  country_timecreate  timestamp,
  country_userupdate  integer,
  country_usercreate  integer,
  country_iso3166     char(2) NOT NULL,
  country_name        varchar(64),
  country_lang        char(2) NOT NULL,
  country_phone       varchar(4),
  PRIMARY KEY (country_iso3166, country_lang)
);


-------------------------------------------------------------------------------
-- Company module tables
-------------------------------------------------------------------------------
-- 
-- Table structure for table 'CompanyType'
--
CREATE TABLE CompanyType (
  companytype_id          serial,
  companytype_timeupdate  TIMESTAMP,
  companytype_timecreate  TIMESTAMP,
  companytype_userupdate  integer,
  companytype_usercreate  integer,
  companytype_label       char(12),
  PRIMARY KEY (companytype_id)
);


-- 
-- Table structure for table 'CompanyActivity'
--
CREATE TABLE CompanyActivity (
  companyactivity_id          serial,
  companyactivity_timeupdate  TIMESTAMP,
  companyactivity_timecreate  TIMESTAMP,
  companyactivity_userupdate  integer,
  companyactivity_usercreate  integer,
  companyactivity_label       varchar(64),
  PRIMARY KEY (companyactivity_id)
);


-- 
-- Table structure for table 'CompanyNafCode'
--
CREATE TABLE CompanyNafCode (
  companynafcode_id          serial,
  companynafcode_timeupdate  timestamp,
  companynafcode_timecreate  timestamp,
  companynafcode_userupdate  integer,
  companynafcode_usercreate  integer,
  companynafcode_title       integer NOT NULL DEFAULT 0,
  companynafcode_code        varchar(4),
  companynafcode_label       varchar(128),
  PRIMARY KEY (companynafcode_id)
);


--
-- Table structure for table 'Company'
--
CREATE TABLE Company (
  company_id                   serial,
  company_timeupdate           TIMESTAMP,
  company_timecreate           TIMESTAMP,
  company_userupdate           integer,
  company_usercreate           integer,
  company_datasource_id        integer,
  company_number               varchar(32),
  company_vat                  varchar(20),
  company_archive              char(1) DEFAULT '0' NOT NULL,
  company_name                 varchar(96) DEFAULT '' NOT NULL,
  company_aka                  varchar(255),
  company_sound                varchar(48),
  company_type_id              integer,
  company_activity_id          integer, 
  company_nafcode_id           integer, 
  company_marketingmanager_id  integer,
  company_address1             varchar(64),
  company_address2             varchar(64),
  company_address3             varchar(64),
  company_zipcode              varchar(14),
  company_town                 varchar(64),
  company_expresspostal        varchar(16),
  company_country_iso3166      char(2) DEFAULT '',
  company_phone                varchar(32),
  company_fax                  varchar(32),
  company_web                  varchar(64),
  company_email                varchar(64),
  company_contact_number       integer DEFAULT 0 NOT NULL,
  company_deal_number          integer DEFAULT 0 NOT NULL,
  company_deal_total           integer DEFAULT 0 NOT NULL,
  company_comment              text,
  PRIMARY KEY (company_id)
);


--
-- Table structure for table 'CompanyCategory'
--
CREATE TABLE CompanyCategory (
  companycategory_id          serial,
  companycategory_timeupdate  TIMESTAMP,
  companycategory_timecreate  TIMESTAMP,
  companycategory_userupdate  integer,
  companycategory_usercreate  integer NOT NULL default 0,
  companycategory_code        varchar(10) NOT NULL default '',
  companycategory_label       varchar(100) NOT NULL default '',
  PRIMARY KEY (companycategory_id)
);


--
-- Table structure for table 'CompanyCategoryLink'
--
CREATE TABLE CompanyCategoryLink (
  companycategorylink_category_id  integer NOT NULL default 0,
  companycategorylink_company_id   integer NOT NULL default 0,
  PRIMARY KEY (companycategorylink_category_id,companycategorylink_company_id)
);


-------------------------------------------------------------------------------
-- Contact module tables
-------------------------------------------------------------------------------
--
-- Table structure for table 'Contact'
--
CREATE TABLE Contact (
  contact_id                   serial,
  contact_timeupdate           TIMESTAMP,
  contact_timecreate           TIMESTAMP,
  contact_userupdate           integer,
  contact_usercreate           integer,
  contact_datasource_id        integer,
  contact_company_id           integer,
  contact_company              varchar(64),
  contact_kind_id              integer,
  contact_marketingmanager_id  integer,
  contact_lastname             varchar(64) DEFAULT '' NOT NULL,
  contact_firstname            varchar(64),
  contact_service              varchar(64),
  contact_address1             varchar(64),
  contact_address2             varchar(64),
  contact_address3             varchar(64),
  contact_zipcode              varchar(14),
  contact_town                 varchar(64),
  contact_expresspostal        varchar(16),
  contact_country_iso3166      char(2) DEFAULT '',
  contact_function_id          integer,
  contact_title                varchar(64),
  contact_phone                varchar(32),
  contact_homephone            varchar(32),
  contact_mobilephone          varchar(32),
  contact_fax                  varchar(32),
  contact_email                varchar(128),
  contact_email2               varchar(128),
  contact_mailing_ok           char(1) DEFAULT '0',
  contact_archive              char(1) DEFAULT '0',
  contact_privacy              integer DEFAULT 0,
  contact_comment              text,
  PRIMARY KEY (contact_id)
);


--
-- Table structure for table 'Kind'
--
CREATE TABLE Kind (
  kind_id          serial,
  kind_timeupdate  TIMESTAMP,
  kind_timecreate  TIMESTAMP,
  kind_userupdate  integer,
  kind_usercreate  integer,
  kind_minilabel   varchar(64),
  kind_header      varchar(64),
  kind_lang        char(2),
  kind_default     integer NOT NULL DEFAULT 0,
  PRIMARY KEY (kind_id)
);


--
-- Table structure for the table 'Function'
--
CREATE TABLE Function (
  function_id          serial,
  function_timeupdate  TIMESTAMP,
  function_timecreate  TIMESTAMP,
  function_userupdate  integer,
  function_usercreate  integer,
  function_label       varchar(64),
  PRIMARY KEY (function_id)
);


--
-- Table structure for table 'ContactCategory1'
--
CREATE TABLE ContactCategory1 (
  contactcategory1_id          serial,
  contactcategory1_timeupdate  timestamp,
  contactcategory1_timecreate  timestamp,
  contactcategory1_userupdate  integer default 0,
  contactcategory1_usercreate  integer default 0,
  contactcategory1_code        integer default 0,
  contactcategory1_label       varchar(100) NOT NULL default '',
  PRIMARY KEY (contactcategory1_id)
);


--
-- Table structure for table 'ContactCategory1Link'
--
CREATE TABLE ContactCategory1Link (
  contactcategory1link_category_id  integer NOT NULL default 0,
  contactcategory1link_contact_id   integer NOT NULL default 0,
  PRIMARY KEY (contactcategory1link_category_id,contactcategory1link_contact_id)
);


--
-- Table structure for table 'ContactCategory2'
--
CREATE TABLE ContactCategory2 (
  contactcategory2_id          serial,
  contactcategory2_timeupdate  timestamp,
  contactcategory2_timecreate  timestamp,
  contactcategory2_userupdate  integer default 0,
  contactcategory2_usercreate  integer default 0,
  contactcategory2_code        integer  default 0,
  contactcategory2_label       varchar(100) NOT NULL default '',
  PRIMARY KEY (contactcategory2_id)
);


--
-- Table structure for table 'ContactCategory2Link'
--
CREATE TABLE ContactCategory2Link (
  contactcategory2link_category_id  integer NOT NULL default 0,
  contactcategory2link_contact_id   integer NOT NULL default 0,
  PRIMARY KEY (contactcategory2link_category_id,contactcategory2link_contact_id)
);


-------------------------------------------------------------------------------
-- Deal module tables
-------------------------------------------------------------------------------
--
-- Table structure for table 'ParentDeal'
--
CREATE TABLE ParentDeal (
  parentdeal_id                   serial,
  parentdeal_timeupdate           TIMESTAMP,
  parentdeal_timecreate           TIMESTAMP,
  parentdeal_userupdate           integer,
  parentdeal_usercreate           integer,
  parentdeal_label                varchar(128) NOT NULL,
  parentdeal_marketingmanager_id  integer,
  parentdeal_technicalmanager_id  integer,
  parentdeal_archive              char(1) DEFAULT '0',
  parentdeal_comment              text,
  PRIMARY KEY (parentdeal_id)
);


--
-- Table structure for table 'Deal'
--
CREATE TABLE Deal (
  deal_id                   serial,
  deal_timeupdate           TIMESTAMP,
  deal_timecreate           TIMESTAMP,
  deal_userupdate           integer,
  deal_usercreate           integer,
  deal_number               varchar(32),
  deal_label                varchar(128),
  deal_datebegin            date,
  deal_parentdeal_id        integer,
  deal_type_id              integer,
  deal_tasktype_id          integer,
  deal_company_id           integer DEFAULT 0 NOT NULL,
  deal_contact1_id          integer,
  deal_contact2_id          integer,
  deal_marketingmanager_id  integer,
  deal_technicalmanager_id  integer,
  deal_dateproposal         date,
  deal_amount               decimal(12,2),
  deal_hitrate              integer DEFAULT 0,
  deal_status_id            integer,
  deal_datealarm            date,
  deal_archive              char(1) DEFAULT '0',
  deal_todo                 varchar(128),
  deal_privacy              integer DEFAULT 0,
  deal_comment              text,
  PRIMARY KEY (deal_id)
);


--
-- Table structure for table 'DealStatus'
--
CREATE TABLE DealStatus (
  dealstatus_id          serial,
  dealstatus_timeupdate  TIMESTAMP,
  dealstatus_timecreate  TIMESTAMP,
  dealstatus_userupdate  integer,
  dealstatus_usercreate  integer,
  dealstatus_label       varchar(24),
  dealstatus_order       integer,
  dealstatus_hitrate     char(3),
  PRIMARY KEY (dealstatus_id)
);


--
-- Table structure for table 'DealType'
--
CREATE TABLE DealType (
  dealtype_id          serial,
  dealtype_timeupdate  TIMESTAMP,
  dealtype_timecreate  TIMESTAMP,
  dealtype_userupdate  integer,
  dealtype_usercreate  integer,
  dealtype_label       varchar(16),
  dealtype_inout       varchar(1) DEFAULT '-',
  PRIMARY KEY (dealtype_id)
);


--
-- Table structure for table 'DealCategory'
--
CREATE TABLE DealCategory (
  dealcategory_id          serial,
  dealcategory_timeupdate  timestamp,
  dealcategory_timecreate  timestamp,
  dealcategory_userupdate  integer default 0,
  dealcategory_usercreate  integer default 0,
  dealcategory_code        integer default 0,
  dealcategory_label       varchar(100) NOT NULL default '',
  PRIMARY KEY (dealcategory_id)
);

-- Table structure for table 'DealCategoryLink'
--
CREATE TABLE DealCategoryLink (
  dealcategorylink_category_id  integer NOT NULL default 0,
  dealcategorylink_deal_id      integer NOT NULL default 0,
  PRIMARY KEY (dealcategorylink_category_id,dealcategorylink_deal_id)
);


-------------------------------------------------------------------------------
-- List module tables
-------------------------------------------------------------------------------
--
-- Table structure for table 'List'
--
CREATE TABLE List (
  list_id          	 serial,
  list_timeupdate  	 TIMESTAMP,
  list_timecreate  	 TIMESTAMP,
  list_userupdate  	 integer,
  list_usercreate  	 integer,
  list_privacy     	 integer DEFAULT 0,
  list_name        	 varchar(64) NOT NULL,
  list_subject     	 varchar(128),
  list_email       	 varchar(128),
  list_mailing_ok  	 integer DEFAULT 0,
  list_contact_archive	 integer DEFAULT 0,
  list_info_publication  integer DEFAULT 0,
  list_static_nb   	 integer DEFAULT 0,
  list_query_nb    	 integer DEFAULT 0,
  list_query       	 text,
  list_structure   	 text, 
  PRIMARY KEY (list_id),
  UNIQUE (list_name)
);


--
-- Table structure for table 'ContactList'
--
CREATE TABLE ContactList (
  contactlist_list_id     integer DEFAULT 0 NOT NULL,
  contactlist_contact_id  integer DEFAULT 0 NOT NULL
);


-------------------------------------------------------------------------------
-- Calendar module tables
-------------------------------------------------------------------------------
--
-- Table structure for the table  'CalendarEvent'
--
CREATE TABLE CalendarEvent (
  calendarevent_id           serial,
  calendarevent_timeupdate   timestamp,
  calendarevent_timecreate   timestamp,
  calendarevent_userupdate   integer,
  calendarevent_usercreate   integer,
  calendarevent_owner        integer default NULL,    
  calendarevent_title        varchar(255) default NULL,
  calendarevent_description  text,
  calendarevent_category_id  integer,
  calendarevent_priority     integer,
  calendarevent_privacy      integer,
  calendarevent_date         timestamp NOT NULL,
  calendarevent_duration     integer NOT NULL default 0,
  calendarevent_allday       integer NOT NULL default 0,
  calendarevent_repeatkind   varchar(20) default NULL,
  calendarevent_repeatfrequence  integer default NULL,
  calendarevent_repeatdays   varchar(7) default NULL,
  calendarevent_endrepeat    timestamp NOT NULL,
  PRIMARY KEY (calendarevent_id)
);


--
-- Table structure for the table  'CalendarUser'
--
CREATE TABLE CalendarUser (
  calendaruser_timeupdate   timestamp,
  calendaruser_timecreate   timestamp,
  calendaruser_userupdate   integer default NULL,
  calendaruser_usercreate   integer default NULL,
  calendaruser_user_id      integer NOT NULL default 0,
  calendaruser_event_id     integer NOT NULL default 0,
  calendaruser_state        char(1) NOT NULL default '',
  calendaruser_required     integer NOT NULL default 0,
  PRIMARY KEY (calendaruser_user_id,calendaruser_event_id)
);


--
-- Table structure for the table  'CalendarException'
--
CREATE TABLE CalendarException (
  calendarexception_timeupdate   timestamp,
  calendarexception_timecreate   timestamp,
  calendarexception_userupdate   integer default NULL,
  calendarexception_usercreate   integer default NULL,
  calendarexception_event_id     serial,
  calendarexception_date         timestamp NOT NULL,
  PRIMARY KEY (calendarexception_event_id,calendarexception_date)
);


--
-- Table structure for table 'CalendarCategory'
--
CREATE TABLE CalendarCategory (
  calendarcategory_id          serial,
  calendarcategory_timeupdate  TIMESTAMP,
  calendarcategory_timecreate  TIMESTAMP,
  calendarcategory_userupdate  integer DEFAULT NULL,
  calendarcategory_usercreate  integer DEFAULT NULL,
  calendarcategory_label       varchar(128) DEFAULT NULL,
  PRIMARY KEY (calendarcategory_id)
);


--
-- Table structure for table 'CalendarRight'
--
CREATE TABLE CalendarRight (
  calendarright_ownerid     integer NOT NULL DEFAULT 0,
  calendarright_customerid  integer NOT NULL DEFAULT 0,
  calendarright_write       integer NOT NULL DEFAULT 0,
  calendarright_read        integer NOT NULL DEFAULT 0,
  PRIMARY KEY (calendarright_ownerid,calendarright_customerid)
);


--
-- structure fot table 'RepeatKind'
--
CREATE TABLE RepeatKind (
  repeatkind_id          serial,
  repeatkind_timeupdate  TIMESTAMP,
  repeatkind_timecreate  TIMESTAMP,
  repeatkind_userupdate  integer,
  repeatkind_usercreate  integer,
  repeatkind_label       varchar(128),
  PRIMARY KEY(repeatkind_id)	
);


-------------------------------------------------------------------------------
-- Todo
-------------------------------------------------------------------------------
--
-- New table 'Todo'
--
CREATE TABLE Todo (
  todo_id          serial,
  todo_timeupdate  timestamp,
  todo_timecreate  timestamp,
  todo_userupdate  integer,
  todo_usercreate  integer,
  todo_user        integer,
  todo_date        timestamp,
  todo_deadline    timestamp,
  todo_priority    integer DEFAULT NULL,
  todo_title       varchar(80) DEFAULT NULL,
  todo_content     text DEFAULT NULL,
  PRIMARY KEY (todo_id)
);


-------------------------------------------------------------------------------
-- Publication module tables
-------------------------------------------------------------------------------
--
-- Table structure for table 'Publication'
--

CREATE TABLE Publication (
  publication_id             serial,
  publication_timeupdate     timestamp,
  publication_timecreate     timestamp,
  publication_userupdate     integer,
  publication_usercreate     integer,
  publication_title          varchar(64) NOT NULL,
  publication_type_id        integer,
  publication_year           integer,
  publication_lang           varchar(30),
  publication_desc           text,
  PRIMARY KEY (publication_id)
);

--
-- Table structure for table 'PublicationType'
--
CREATE TABLE PublicationType (
  publicationtype_id          serial,
  publicationtype_timeupdate  timestamp,
  publicationtype_timecreate  timestamp,
  publicationtype_userupdate  integer,
  publicationtype_usercreate  integer,
  publicationtype_label       varchar(64),
  PRIMARY KEY (publicationtype_id)
);


--
-- Table structure for table 'Subscription'
--
CREATE TABLE Subscription (
  subscription_id		serial,
  subscription_publication_id 	integer NOT NULL,
  subscription_contact_id       integer NOT NULL,
  subscription_timeupdate       timestamp,
  subscription_timecreate       timestamp,
  subscription_userupdate       integer,
  subscription_usercreate       integer,
  subscription_quantity       	integer,
  subscription_renewal          integer NOT NULL,
  subscription_reception_id     integer NOT NULL,
  subscription_date_begin       timestamp,
  subscription_date_end         timestamp,
  PRIMARY KEY (subscription_id)
);


--
-- Table structure for table 'SubscriptionReception'
--
CREATE TABLE SubscriptionReception ( 
  subscriptionreception_id          serial,
  subscriptionreception_timeupdate  timestamp,
  subscriptionreception_timecreate  timestamp,
  subscriptionreception_userupdate  integer,
  subscriptionreception_usercreate  integer,
  subscriptionreception_label       char(12),
  PRIMARY KEY (subscriptionreception_id)
);


-------------------------------------------------------------------------------
-- Document module tables
-------------------------------------------------------------------------------
--
-- Table structure for table 'Document'
--
CREATE TABLE Document (
  document_id            serial,
  document_timeupdate    TIMESTAMP,
  document_timecreate    TIMESTAMP,
  document_userupdate  	 integer DEFAULT NULL,
  document_usercreate  	 integer DEFAULT NULL,
  document_title       	 varchar(255) DEFAULT NULL,
  document_name        	 varchar(255) DEFAULT NULL,
  document_kind        	 integer DEFAULT NULL,
  document_mimetype    	 varchar(255) DEFAULT NULL,
  document_category1_id  integer NOT NULL DEFAULT 0,
  document_category2_id  integer NOT NULL DEFAULT 0,
  document_author      	 varchar(255) DEFAULT NULL,
  document_privacy     	 integer NOT NULL DEFAULT 0,
  document_path        	 text DEFAULT NULL,
  document_size        	 integer DEFAULT NULL,
  PRIMARY KEY (document_id)
);


--
-- Table structure for table 'DocumentCategory1'
--
CREATE TABLE DocumentCategory1 (
  documentcategory1_id          serial,
  documentcategory1_timeupdate  TIMESTAMP,
  documentcategory1_timecreate  TIMESTAMP,
  documentcategory1_userupdate  integer DEFAULT NULL,
  documentcategory1_usercreate  integer DEFAULT NULL,
  documentcategory1_label       varchar(255) DEFAULT NULL,
  PRIMARY KEY (documentcategory1_id)
);


--
-- Table structure for table 'DocumentCategory2'
--
CREATE TABLE DocumentCategory2 (
  documentcategory2_id          serial,
  documentcategory2_timeupdate  TIMESTAMP,
  documentcategory2_timecreate  TIMESTAMP,
  documentcategory2_userupdate  integer DEFAULT NULL,
  documentcategory2_usercreate  integer DEFAULT NULL,
  documentcategory2_label       varchar(255) DEFAULT NULL,
  PRIMARY KEY (documentcategory2_id)
);


--
-- Table structure for table 'DocumentMimeType'
--
CREATE TABLE DocumentMimeType (
  documentmimetype_id          serial,
  documentmimetype_timeupdate  TIMESTAMP,
  documentmimetype_timecreate  TIMESTAMP,
  documentmimetype_userupdate  integer DEFAULT NULL,
  documentmimetype_usercreate  integer DEFAULT NULL,
  documentmimetype_label       varchar(255) DEFAULT NULL,
  documentmimetype_extension   varchar(10) DEFAULT NULL,
  documentmimetype_mime        varchar(255) DEFAULT NULL,
  PRIMARY KEY (documentmimetype_id)
);


--
-- Table structure for table 'DocumentEntity'
--
CREATE TABLE DocumentEntity (
  documententity_document_id  integer NOT NULL,
  documententity_entity_id    integer NOT NULL,
  documententity_entity       varchar(255) NOT NULL,
  PRIMARY KEY (documententity_document_id, documententity_entity_id, documententity_entity)
);


-------------------------------------------------------------------------------
-- Project module tables
-------------------------------------------------------------------------------
--
-- Table structure for table 'Project'
--
CREATE TABLE Project (
  project_id             serial,
  project_timeupdate     timestamp,
  project_timecreate     timestamp,
  project_userupdate     integer,
  project_usercreate     integer,
  project_name           varchar(128),
  project_tasktype_id    integer,
  project_company_id     integer,
  project_deal_id        integer,
  project_soldtime       integer DEFAULT NULL,
  project_estimatedtime  integer DEFAULT NULL,
  project_datebegin      date,
  project_dateend        date,
  project_archive        char(1) DEFAULT '0',
  project_comment        text,
  PRIMARY KEY (project_id)
);
create INDEX project_idx_comp ON Project (project_company_id);
create INDEX project_idx_deal ON Project (project_deal_id);


--
-- Table structure for table 'ProjectTask'
--
CREATE TABLE ProjectTask (
  projecttask_id             serial,
  projecttask_project_id     integer NOT NULL,
  projecttask_timeupdate     timestamp,
  projecttask_timecreate     timestamp,
  projecttask_userupdate     integer default NULL,
  projecttask_usercreate     integer default NULL,
  projecttask_label          varchar(128) default NULL,
  projecttask_parenttask_id  integer default 0,
  projecttask_rank           integer default NULL,
  PRIMARY KEY (projecttask_id)
);
create INDEX pt_idx_pro ON ProjectTask (projecttask_project_id);


--
-- Table structure for table 'ProjectUser'
--
CREATE TABLE ProjectUser (
  projectuser_id              serial,
  projectuser_project_id      integer NOT NULL,
  projectuser_user_id         integer NOT NULL,
  projectuser_projecttask_id  integer,
  projectuser_timeupdate      timestamp,
  projectuser_timecreate      timestamp,
  projectuser_userupdate      integer default NULL,
  projectuser_usercreate      integer default NULL,
  projectuser_projectedtime   integer default NULL,
  projectuser_missingtime     integer default NULL,
  projectuser_validity        timestamp,
  projectuser_soldprice       integer default NULL,
  projectuser_manager         integer default NULL,
  PRIMARY KEY (projectuser_id)
);
create INDEX pu_idx_pro ON ProjectUser (projectuser_project_id);
create INDEX pu_idx_user ON ProjectUser (projectuser_user_id);
create INDEX pu_idx_pt ON ProjectUser (projectuser_projecttask_id);


--
-- Table structure for table 'ProjectStat'
--
CREATE TABLE ProjectStat (
  projectstat_project_id     integer NOT NULL,
  projectstat_usercreate     integer NOT NULL,
  projectstat_date           timestamp NOT NULL,
  projectstat_useddays       integer default NULL,
  projectstat_remainingdays  integer default NULL,
  PRIMARY KEY (projectstat_project_id, projectstat_usercreate, projectstat_date)
);


-------------------------------------------------------------------------------
-- Timemanagement tables
-------------------------------------------------------------------------------
--
-- Task table
--
CREATE TABLE TimeTask (
  timetask_id              serial,
  timetask_timeupdate      timestamp,
  timetask_timecreate      timestamp,
  timetask_userupdate      integer DEFAULT NULL,
  timetask_usercreate      integer DEFAULT NULL,
  timetask_user_id         integer DEFAULT NULL,
  timetask_date            timestamp NOT NULL,
  timetask_projecttask_id  integer DEFAULT NULL,
  timetask_length          integer DEFAULT NULL,
  timetask_tasktype_id     integer DEFAULT NULL,
  timetask_label           varchar(255) DEFAULT NULL,
  timetask_status          integer DEFAULT NULL,
  PRIMARY KEY (timetask_id)
);
create INDEX tt_idx_pt ON TimeTask (timetask_projecttask_id);


--
-- TaskType table
--
CREATE TABLE TaskType (
  tasktype_id          serial,
  tasktype_timeupdate  timestamp,
  tasktype_timecreate  timestamp,
  tasktype_userupdate  integer DEFAULT NULL,
  tasktype_usercreate  integer DEFAULT NULL,
  tasktype_internal    integer NOT NULL,
  tasktype_label       varchar(32) DEFAULT NULL,
  PRIMARY KEY (tasktype_id)
);


-------------------------------------------------------------------------------
-- Support tables
-------------------------------------------------------------------------------
--
-- New table 'Contract'
--
CREATE TABLE Contract (
  contract_id                serial,
  contract_timeupdate        timestamp,
  contract_timecreate        timestamp,
  contract_userupdate        integer DEFAULT NULL,
  contract_usercreate        integer DEFAULT NULL,
  contract_deal_id           integer DEFAULT NULL,
  contract_company_id        integer DEFAULT NULL,
  contract_label             varchar(128) DEFAULT NULL,
  contract_number            varchar(20) DEFAULT NULL,
  contract_datesignature     date DEFAULT NULL,
  contract_datebegin         date DEFAULT NULL,
  contract_dateexp           date DEFAULT NULL,
  contract_daterenew         date DEFAULT NULL,
  contract_datecancel        date DEFAULT NULL,
  contract_type_id           integer DEFAULT NULL,
  contract_priority_id       integer DEFAULT 0 NOT NULL,
  contract_status_id         integer DEFAULT 0 NOT NULL,
  contract_kind              integer DEFAULT 0 NULL,
  contract_format            integer DEFAULT 0 NULL,
  contract_ticketnumber      integer DEFAULT 0 NULL,
  contract_duration          integer DEFAULT 0 NULL,
  contract_autorenewal       integer DEFAULT 0 NULL,
  contract_contact1_id       integer DEFAULT NULL,
  contract_contact2_id       integer DEFAULT NULL,
  contract_techmanager_id    integer DEFAULT NULL,
  contract_marketmanager_id  integer DEFAULT NULL,
  contract_privacy           integer DEFAULT 0 NULL,
  contract_archive           integer DEFAULT 0,
  contract_clause            text,
  contract_comment           text,
  PRIMARY KEY (contract_id)
);


--
-- New table 'ContractType'
--
CREATE TABLE ContractType (
  contracttype_id          serial,
  contracttype_timeupdate  timestamp,
  contracttype_timecreate  timestamp,
  contracttype_userupdate  integer DEFAULT NULL,
  contracttype_usercreate  integer DEFAULT NULL,
  contracttype_label       varchar(64) DEFAULT NULL,
  PRIMARY KEY (contracttype_id)
);


--
-- New table 'ContractPriority'
--
CREATE TABLE ContractPriority (
  contractpriority_id          serial,
  contractpriority_timeupdate  timestamp,
  contractpriority_timecreate  timestamp,
  contractpriority_userupdate  integer DEFAULT NULL,
  contractpriority_usercreate  integer DEFAULT NULL,
  contractpriority_color       varchar(6) DEFAULT NULL,
  contractpriority_order       integer DEFAULT NULL,
  contractpriority_label       varchar(64) DEFAULT NULL,
  PRIMARY KEY (contractpriority_id)
);


--
-- New table 'ContractStatus'
--
CREATE TABLE ContractStatus (
  contractstatus_id     	serial,
  contractstatus_timeupdate  	timestamp,
  contractstatus_timecreate  	timestamp,
  contractstatus_userupdate  	integer DEFAULT	NULL,
  contractstatus_usercreate  	integer DEFAULT	NULL,
  contractstatus_order  	integer DEFAULT	NULL,
  contractstatus_label  	varchar(64) DEFAULT NULL,
PRIMARY KEY (contractstatus_id)
);


--
-- New table 'Incident'
--
CREATE TABLE Incident (
  incident_id               serial,
  incident_timeupdate       timestamp,
  incident_timecreate       timestamp,
  incident_userupdate       integer DEFAULT NULL,
  incident_usercreate       integer DEFAULT NULL,
  incident_contract_id      integer NOT NULL,
  incident_label            varchar(100) DEFAULT NULL,
  incident_date             timestamp,
  incident_priority_id      integer DEFAULT NULL,
  incident_status_id        integer DEFAULT NULL,
  incident_cat1_id          integer DEFAULT NULL,
  incident_logger           integer DEFAULT NULL,
  incident_owner            integer DEFAULT NULL,
  incident_duration         char(4) DEFAULT '0',
  incident_archive          char(1) NOT NULL DEFAULT '0',
  incident_comment          text,
  incident_resolution       text,
  PRIMARY KEY (incident_id)
);


--
-- New table 'IncidentPriority'
--
CREATE TABLE IncidentPriority (
  incidentpriority_id          serial,
  incidentpriority_timeupdate  timestamp,
  incidentpriority_timecreate  timestamp,
  incidentpriority_userupdate  integer DEFAULT NULL,
  incidentpriority_usercreate  integer DEFAULT NULL,
  incidentpriority_order       integer,
  incidentpriority_color       char(6),
  incidentpriority_label       varchar(32) DEFAULT NULL,
  PRIMARY KEY (incidentpriority_id)
);


--
-- New table 'IncidentStatus'
--
CREATE TABLE IncidentStatus (
  incidentstatus_id          serial,
  incidentstatus_timeupdate  timestamp,
  incidentstatus_timecreate  timestamp,
  incidentstatus_userupdate  integer DEFAULT NULL,
  incidentstatus_usercreate  integer DEFAULT NULL,
  incidentstatus_order       integer,
  incidentstatus_label       varchar(32) DEFAULT NULL,
  PRIMARY KEY (incidentstatus_id)
);


--
-- New table 'IncidentCategory1'
--
CREATE TABLE IncidentCategory1 (
  incidentcategory1_id          serial,
  incidentcategory1_timeupdate  timestamp,
  incidentcategory1_timecreate  timestamp,
  incidentcategory1_userupdate  integer DEFAULT NULL,
  incidentcategory1_usercreate  integer DEFAULT NULL,
  incidentcategory1_order       integer,
  incidentcategory1_label       varchar(32) DEFAULT NULL,
  PRIMARY KEY (incidentcategory1_id)
);


-------------------------------------------------------------------------------
-- Accounting Section tables
-------------------------------------------------------------------------------
--
-- New table 'Invoice'
--
CREATE TABLE Invoice ( 
  invoice_id               serial,
  invoice_timeupdate       timestamp,
  invoice_timecreate       timestamp,
  invoice_userupdate       integer,
  invoice_usercreate       integer,
  invoice_company_id       integer NOT NULL,
  invoice_deal_id          integer default NULL,
  invoice_project_id       integer default NULL,
  invoice_number           varchar(10) DEFAULT '0',
  invoice_label            varchar(40) NOT NULL DEFAULT '',
  invoice_amount_ht        DECIMAL(10,2),
  invoice_amount_ttc       DECIMAL(10,2),
  invoice_status_id        integer DEFAULT 0 NOT NULL,
  invoice_date             date,
  invoice_expiration_date  date,
  invoice_payment_date     date,
  invoice_inout            char(1),
  invoice_archive          char(1) NOT NULL DEFAULT '0',
  invoice_comment          text,
  PRIMARY KEY (invoice_id)
);


--
-- New table 'InvoiceStatus'
--
CREATE TABLE InvoiceStatus (
  invoicestatus_id       serial,
  invoicestatus_payment  integer DEFAULT 0 NOT NULL,
  invoicestatus_archive  integer DEFAULT 0 NOT NULL,
  invoicestatus_label    varchar(24) DEFAULT '' NOT NULL,
  PRIMARY KEY (invoicestatus_id)
);


--
-- New table 'Payment'
--
CREATE TABLE  Payment (
  payment_id              serial,
  payment_timeupdate      timestamp,
  payment_timecreate      timestamp,
  payment_userupdate      integer,
  payment_usercreate      integer,
  payment_company_id      integer NOT NULL,
  payment_number          integer default null,
  payment_date            date,
  payment_expected_date   date,		
  payment_amount          decimal(10,2) DEFAULT '0.0' NOT NULL,
  payment_label           varchar(128) NOT NULL DEFAULT '',
  payment_paymentkind_id  integer,
  payment_account_id      integer,
  payment_inout           char(1) NOT NULL,
  payment_paid            char(1) NOT NULL DEFAULT '0',
  payment_checked         char(1) NOT NULL DEFAULT '0',
  payment_comment         text,
  PRIMARY KEY (payment_id)
);


--
-- New table 'PaymentKind'
--
CREATE TABLE PaymentKind (
  paymentkind_id          serial,
  paymentkind_shortlabel  varchar(3) NOT NULL DEFAULT '',
  paymentkind_longlabel   varchar(40) NOT NULL DEFAULT '',
  PRIMARY KEY (paymentkind_id)
);


--
-- New table 'PaymentInvoice'
--
CREATE TABLE PaymentInvoice (
  paymentinvoice_invoice_id  integer NOT NULL,
  paymentinvoice_payment_id  integer NOT NULL,
  paymentinvoice_timeupdate  timestamp,
  paymentinvoice_timecreate  timestamp,
  paymentinvoice_userupdate  integer,
  paymentinvoice_usercreate  integer,
  paymentinvoice_amount      decimal (10,2) NOT NULL DEFAULT '0',
  PRIMARY KEY (paymentinvoice_invoice_id,paymentinvoice_payment_id)
);


--
-- New table 'Account'
--
CREATE TABLE Account (
  account_id          serial,
  account_timeupdate  timestamp,
  account_timecreate  timestamp,
  account_userupdate  integer,
  account_usercreate  integer,
  account_bank	      varchar(60) DEFAULT '' NOT NULL,
  account_number      varchar(11) DEFAULT '0' NOT NULL,
  account_balance     DECIMAL(15,2) DEFAULT '0.00' NOT NULL,
  account_today	      DECIMAL(15,2) DEFAULT '0.00' NOT NULL,
  account_comment     varchar(100),
  account_label	      varchar(40) NOT NULL DEFAULT '',
  PRIMARY KEY (account_id)
);


--
-- EntryTemp and PaymentTemp are used when importing data from the bank files
--

--
-- New table 'EntryTemp'
--
CREATE TABLE EntryTemp (
  entrytemp_id        serial,
  entrytemp_label     varchar(40),
  entrytemp_amount    DECIMAL(10,2) not null DEFAULT '0.00',
  entrytemp_type      varchar(100),
  entrytemp_date      date not null DEFAULT '0001-01-01',
  entrytemp_realdate  date not null DEFAULT '0001-01-01',
  entrytemp_comment   varchar(100),
  entrytemp_checked   char(1) not null DEFAULT '0',
  PRIMARY KEY (entrytemp_id)
);


--
-- New table 'PaymentTemp'
--
CREATE TABLE  PaymentTemp (
  paymenttemp_id              serial,
  paymenttemp_timeupdate      timestamp,
  paymenttemp_timecreate      timestamp,
  paymenttemp_usercreate      integer,
  paymenttemp_userupdate      integer,
  paymenttemp_number          integer default null,
  paymenttemp_date            date,
  paymenttemp_expected_date   date,		
  paymenttemp_amount          decimal(10,2) DEFAULT '0.0' NOT NULL,
  paymenttemp_label           varchar(40) NOT NULL DEFAULT '',
  paymenttemp_paymentkind_id  integer,
  paymenttemp_account_id      integer,
  paymenttemp_comment         text,
  paymenttemp_inout           char(1) NOT NULL,
  paymenttemp_paid            char(1) NOT NULL DEFAULT '0',
  paymenttemp_checked         char(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (paymenttemp_id)
);


-------------------------------------------------------------------------------
-- Group module tables
-------------------------------------------------------------------------------
--
-- Table structure for table 'UGroup' (cause Group is a reserved keyword)
--
CREATE TABLE UGroup (
  group_id          serial,
  group_timeupdate  timestamp,
  group_timecreate  timestamp,
  group_userupdate  integer,
  group_usercreate  integer,
  group_local       integer DEFAULT 1,
  group_ext_id      integer,
  group_system      integer DEFAULT 0,
  group_privacy     integer DEFAULT 0,
  group_name        varchar(32) NOT NULL,
  group_desc        varchar(128),
  group_email       varchar(128),
  PRIMARY KEY (group_id)
);


--
-- Table structure for table 'UserObmGroup'
--
CREATE TABLE UserObmGroup (
  userobmgroup_group_id    integer DEFAULT 0 NOT NULL,
  userobmgroup_userobm_id  integer DEFAULT 0 NOT NULL
);


--
-- Table structure for table 'GroupGroup'
--
CREATE TABLE GroupGroup (
  groupgroup_parent_id  integer DEFAULT 0 NOT NULL,
  groupgroup_child_id   integer DEFAULT 0 NOT NULL
);


-------------------------------------------------------------------------------
-- Import module tables
-------------------------------------------------------------------------------
--
-- Table structure for table 'Import'
--
CREATE TABLE Import (
  import_id                   serial,
  import_timeupdate           timestamp,
  import_timecreate           timestamp,
  import_userupdate           integer,
  import_usercreate           integer,
  import_name                 varchar(64) NOT NULL,
  import_datasource_id        integer,
  import_marketingmanager_id  integer,
  import_separator            varchar(3),
  import_enclosed             char(1),
  import_desc                 text,
  PRIMARY KEY (import_id),
  UNIQUE (import_name)
);


COMMIT;

