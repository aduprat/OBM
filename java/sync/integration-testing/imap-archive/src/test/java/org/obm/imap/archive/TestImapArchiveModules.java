/* ***** BEGIN LICENSE BLOCK *****
 * 
 * Copyright (C) 2014  Linagora
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version, provided you comply with the Additional Terms applicable for OBM
 * software by Linagora pursuant to Section 7 of the GNU Affero General Public
 * License, subsections (b), (c), and (e), pursuant to which you must notably (i)
 * retain the displaying by the interactive user interfaces of the “OBM, Free
 * Communication by Linagora” Logo with the “You are using the Open Source and
 * free version of OBM developed and supported by Linagora. Contribute to OBM R&D
 * by subscribing to an Enterprise offer !” infobox, (ii) retain all hypertext
 * links between OBM and obm.org, between Linagora and linagora.com, as well as
 * between the expression “Enterprise offer” and pro.obm.org, and (iii) refrain
 * from infringing Linagora intellectual property rights over its trademarks and
 * commercial brands. Other Additional Terms apply, see
 * <http://www.linagora.com/licenses/> for more details.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License and
 * its applicable Additional Terms for OBM along with this program. If not, see
 * <http://www.gnu.org/licenses/> for the GNU Affero General   Public License
 * version 3 and <http://www.linagora.com/licenses/> for the Additional Terms
 * applicable to the OBM software.
 * ***** END LICENSE BLOCK ***** */

package org.obm.imap.archive;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.rules.TemporaryFolder;
import org.obm.Configuration;
import org.obm.StaticConfigurationService;
import org.obm.configuration.DatabaseConfiguration;
import org.obm.configuration.DatabaseFlavour;
import org.obm.configuration.TransactionConfiguration;
import org.obm.dao.utils.DaoTestModule;
import org.obm.imap.archive.beans.ArchiveTreatmentRunId;
import org.obm.imap.archive.beans.Mailing;
import org.obm.imap.archive.logging.LoggerFileNameService;
import org.obm.imap.archive.scheduling.ArchiveDomainTask;
import org.obm.imap.archive.scheduling.ArchiveSchedulerBus;
import org.obm.imap.archive.scheduling.OnlyOnePerDomainMonitorFactory;
import org.obm.imap.archive.scheduling.OnlyOnePerDomainMonitorFactory.OnlyOnePerDomainMonitorFactoryImpl;
import org.obm.imap.archive.services.Mailer;
import org.obm.imap.archive.services.TestingDateProvider;
import org.obm.imap.archive.services.TestingDateProviderImpl;
import org.obm.locator.LocatorClientException;
import org.obm.locator.store.LocatorService;
import org.obm.push.mail.greenmail.GreenMailProviderModule;
import org.obm.push.utils.UUIDFactory;
import org.obm.push.utils.jvm.VMArgumentsUtils;
import org.obm.server.ServerConfiguration;
import org.obm.sync.date.DateProvider;
import org.obm.sync.locators.Locator;
import org.obm.utils.ObmHelper;

import com.github.restdriver.clientdriver.ClientDriverRule;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import com.google.inject.util.Modules;
import com.linagora.scheduling.DateTimeProvider;
import com.linagora.scheduling.Monitor;
import com.linagora.scheduling.ScheduledTask.State;

import fr.aliacom.obm.common.domain.ObmDomain;

public class TestImapArchiveModules {
	
	public static final UUID uuid = UUID.fromString("08c00ba3-fb00-48ac-a077-24b47c123692");
	public static final UUID uuid2 = UUID.fromString("e72906d1-4b6f-4727-8be8-3e78441623ea");
	
	public static final ZonedDateTime LOCAL_DATE_TIME = ZonedDateTime.parse("2014-06-18T00:00:00Z");
	
	public static class Simple extends AbstractModule {
	
		private final ClientDriverRule obmSyncHttpMock;
		private final ServerConfiguration config;
		private final Provider<TemporaryFolder> temporaryFolder;

		public Simple(ClientDriverRule obmSyncHttpMock, Provider<TemporaryFolder> temporaryFolder) {
			this.obmSyncHttpMock = obmSyncHttpMock;
			this.temporaryFolder = temporaryFolder;
			this.config = ServerConfiguration.builder()
					.lifeCycleHandler(ImapArchiveModule.STARTUP_HANDLER_CLASS)
					.build();
		}
		
		@Override
		protected void configure() {
			install(Modules.override(new ImapArchiveModule(config)).with(
				new DaoTestModule(),
				new TransactionalModule(),
				new TimeBasedModule(),
				new StaticUUIDModule(),
				logFileModule(),
				new SchedulerModule(),
				new LocalLocatorModule(obmSyncHttpMock.getBaseUrl() + "/obm-sync"),
				new AbstractModule() {
					
					@Override
					protected void configure() {
						Multibinder<ArchiveSchedulerBus.Client> busClients = Multibinder.newSetBinder(binder(), ArchiveSchedulerBus.Client.class);
						busClients.addBinding().to(FutureSchedulerBusClient.class);
						
						Multibinder<DatabaseFlavour> supportedDatabases = Multibinder.newSetBinder(binder(), DatabaseFlavour.class);
						supportedDatabases.addBinding().toInstance(DatabaseFlavour.H2);
					}
				}
			));
		}

		protected AbstractModule logFileModule() {
			return new AbstractModule() {
				
				@Override
				protected void configure() {
					bind(LoggerFileNameService.class).toInstance(new TemporaryLoggerFileNameService(temporaryFolder));
				}
			};
		}
	}
	
	public static class Mysql extends AbstractModule {

		private ClientDriverRule obmSyncHttpMock;
		private Provider<TemporaryFolder> temporaryFolder;

		public Mysql(ClientDriverRule obmSyncHttpMock, Provider<TemporaryFolder> temporaryFolder) {
			this.obmSyncHttpMock = obmSyncHttpMock;
			this.temporaryFolder = temporaryFolder;
		}
		
		@Override
		protected void configure() {
			install(Modules.override(new Simple(obmSyncHttpMock, temporaryFolder)).with(new AbstractModule() {
				
				@Override
				protected void configure() {
					bind(DatabaseConfiguration.class).to(DatabaseConfigurationFixtureMySQL.class);
				}
			}));
		}
	}
	
	public static class WithGreenmail extends AbstractModule {

		private ClientDriverRule obmSyncHttpMock;
		private Provider<TemporaryFolder> temporaryFolder;

		public WithGreenmail(ClientDriverRule obmSyncHttpMock, Provider<TemporaryFolder> temporaryFolder) {
			this.obmSyncHttpMock = obmSyncHttpMock;
			this.temporaryFolder = temporaryFolder;
		}
		
		@Override
		protected void configure() {
			install(Modules.override(new Simple(obmSyncHttpMock, temporaryFolder)).with(new AbstractModule() {

				@Override
				protected void configure() {
					install(new GreenMailProviderModule());
					bind(Integer.class).annotatedWith(Names.named("imapTimeout")).toInstance(3600000);
					bind(Mailer.class).to(NoopMailer.class);
				}})
			);
		}
	}
	
	private static class NoopMailer implements Mailer {
		
		@Override
		public void send(ObmDomain domain, ArchiveTreatmentRunId runId, State state, Mailing mailing) {
		}
	}

	public static class WithTestingMonitor extends AbstractModule {

		private ClientDriverRule obmSyncHttpMock;
		private Provider<TemporaryFolder> temporaryFolder;

		public WithTestingMonitor(ClientDriverRule obmSyncHttpMock, Provider<TemporaryFolder> temporaryFolder) {
			this.obmSyncHttpMock = obmSyncHttpMock;
			this.temporaryFolder = temporaryFolder;
		}
		
		@Override
		protected void configure() {
			install(Modules.override(new Simple(obmSyncHttpMock, temporaryFolder)).with(new AbstractModule() {

				@Override
				protected void configure() {
					TestingOnlyOnePerDomainMonitorFactory factory = new TestingOnlyOnePerDomainMonitorFactory();
					bind(OnlyOnePerDomainMonitorFactory.class).toInstance(factory);
					bind(TestingOnlyOnePerDomainMonitorFactory.class).toInstance(factory);
				}})
			);
		}

		public static class TestingOnlyOnePerDomainMonitorFactory extends OnlyOnePerDomainMonitorFactoryImpl {
			
			Monitor<ArchiveDomainTask> monitor;

			@Override
			public Monitor<ArchiveDomainTask> create() {
				monitor = super.create();
				return monitor;
			}
			
			public Monitor<ArchiveDomainTask> get() {
				return monitor;
			}
		}

	}
	
	public static class LocalLocatorModule extends AbstractModule {

		private String obmSyncBaseUrl;

		public LocalLocatorModule(String obmSyncBaseUrl) {
			this.obmSyncBaseUrl = obmSyncBaseUrl;
		}
		
		@Override
		protected void configure() {
			bind(LocatorService.class).toInstance(new LocatorService() {
				
				@Override
				public String getServiceLocation(String serviceSlashProperty,
						String loginAtDomain) throws LocatorClientException {
					if (serviceSlashProperty.equals("mail/imap_frontend")) {
						return "localhost";
					}
					throw new IllegalStateException();
				}
			});
			bind(Locator.class).toInstance(new TestLocator(obmSyncBaseUrl));
		}
	}

	public static class TestLocator extends Locator {
		
		private String obmSyncBaseUrl;

		public TestLocator(String obmSyncBaseUrl) {
			super(null, null);
			this.obmSyncBaseUrl = obmSyncBaseUrl;
		}
		
		@Override
		public String backendUrl(String loginAtDomain) throws LocatorClientException {
			return obmSyncBaseUrl;
		}
		
		@Override
		public String backendBaseUrl(String loginAtDomain) throws LocatorClientException {
			return obmSyncBaseUrl;
		}
	}
	
	public static class TransactionalModule extends AbstractModule {

		@Override
		protected void configure() {
			bind(TransactionConfiguration.class).toInstance(new StaticConfigurationService.Transaction(new Configuration.Transaction()));
		}
	}
	
	public static class TimeBasedModule extends AbstractModule {

		@Override
		protected void configure() {
			if (!isInTestingMode()) {
				bind(DateProvider.class).to(TestDateProvider.class);
				bind(DateTimeProvider.class).to(TestDateProvider.class);
			} else {
				bind(TestingDateProvider.class).to(TestTestingDateProvider.class);
			}
			bind(TimeUnit.class).annotatedWith(Names.named("schedulerResolution")).toInstance(TimeUnit.MILLISECONDS);
		}

		private boolean isInTestingMode() {
			return VMArgumentsUtils.booleanArgumentValue(ImapArchiveModule.TESTING_MODE);
		}
		
		@Singleton
		public static class TestDateProvider implements DateTimeProvider, DateProvider {

			private ZonedDateTime current;

			public TestDateProvider() {
				this.current = LOCAL_DATE_TIME;
			}
			
			public void setCurrent(ZonedDateTime current) {
				this.current = current;
			}
			
			@Override
			public Date getDate() {
				return Date.from(current.toInstant());
			}

			@Override
			public ZonedDateTime now() {
				return current;
			}
			
		}
		
		@Singleton
		public static class TestTestingDateProvider extends TestingDateProviderImpl {

			@Inject
			public TestTestingDateProvider(ObmHelper obmHelper) {
				super(obmHelper);
			}


			@Override
			protected Date currentDate() {
				return Date.from(LOCAL_DATE_TIME.toInstant());
			}
		}
	}
	
	public static class StaticUUIDModule extends AbstractModule {

		@Override
		protected void configure() {
			bind(UUIDFactory.class).toInstance(new UUIDFactory() {

				boolean use1 = true;
				
				@Override
				public synchronized UUID randomUUID() {
					if (use1) {
						use1 = !use1;
						return uuid;
					} else {
						use1 = !use1;
						return uuid2;
					}
				}
			});
		}
	}
	
	public static class SchedulerModule extends AbstractModule {

		@Override
		protected void configure() {
			bind(Boolean.class).annotatedWith(Names.named("endlessTask")).toInstance(Boolean.FALSE);
		}
	}
	
	public static class TemporaryLoggerFileNameService implements LoggerFileNameService {

		private final Provider<TemporaryFolder> temporaryFolderProvider;
		private String loggerFileName;

		public TemporaryLoggerFileNameService(Provider<TemporaryFolder> temporaryFolderProvider) {
			this.temporaryFolderProvider = temporaryFolderProvider;
		}
		
		@Override
		public String loggerFileName(ArchiveTreatmentRunId runId) {
			try {
				TemporaryFolder temporaryFolder = temporaryFolderProvider.get();
				createIfNone(temporaryFolder);
				
				loggerFileName = temporaryFolder.getRoot().getAbsolutePath() + "/" + runId.serialize() + ".log";
				return loggerFileName;
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		private void createIfNone(TemporaryFolder temporaryFolder) throws IOException {
			try {
				temporaryFolder.getRoot();
			} catch (IllegalStateException e) {
				temporaryFolder.create();
			}
		}

	}
}