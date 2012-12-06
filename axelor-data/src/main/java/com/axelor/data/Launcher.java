package com.axelor.data;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.name.Names;

public abstract class Launcher {
	
	protected final Logger LOG = LoggerFactory.getLogger(getClass());
	
	/**
	 * Create additional guice module that configures other stuffs like
	 * persistence etc.
	 * 
	 */
	protected abstract AbstractModule createModule();
	
	public void run(String... args) throws IOException {
		
		Commander cmd = new Commander();
		try {
			if (args == null || args.length == 0)
				throw new Exception();
			cmd.parse(args);
			if (!cmd.getDataDir().isDirectory())
				throw new Exception("invalid data directory");
			if (!cmd.getConfig().isFile())
				throw new Exception("invalid config file");
		} catch (Exception e) {
			String message = e.getMessage();
			if (!Strings.isNullOrEmpty(message))
				System.err.println(e.getMessage());
			Commander.usage();
			return;
		}
		
		if (cmd.getShowHelp() == Boolean.TRUE) {
			Commander.usage();
			return;
		}
		
		final String config = cmd.getConfig().getPath();
		final String dataDir = cmd.getDataDir().getPath();
		
		Injector injector = Guice.createInjector(new AbstractModule() {
			
			@Override
			protected void configure() {
				install(createModule());
				bindConstant().annotatedWith(Names.named("axelor.data.config")).to(config);
				bindConstant().annotatedWith(Names.named("axelor.data.dir")).to(dataDir);
			}
		});

		if (LOG.isInfoEnabled())
			LOG.info("Importing data. Please wait...");
		
		Importer importer = injector.getInstance(Importer.class);
		Map<String, String[]> mappings = new HashMap<String, String[]>();

		for (Map.Entry<Object, Object> entry : cmd.getFiles().entrySet()) {
			String name = (String) entry.getKey();
			String[] files = ((String) entry.getValue()).split(",");
			mappings.put(name, files);
		}
		
		importer.run(mappings);
		
		if (LOG.isInfoEnabled())
			LOG.info("Import done!");
	}
}
