package com.graphbenchmark.common;

import com.graphbenchmark.settings.Venv;
import org.checkerframework.dataflow.qual.TerminatesExecution;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;

public class GdbLogger {
	public static boolean lvl_debug = false;

	private static GdbLogger self;
	private String log_path;
	private FileOutputStream out = null;

	DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
	LocalDateTime now = LocalDateTime.now();

	private GdbLogger() {
		GdbLogger.self = this;
	}

	private Deque<String> buffer = new ArrayDeque<>();

	private String prepare(String lvl, String msg, Object ... args) {
		return String.format("%s| %-8s - %s\n", dtf.format(LocalDateTime.now()), lvl, args.length == 0 ? msg : String.format(msg, args));
	}

	public void delayed(String lvl, String msg, Object ... args) {
		buffer.add(prepare(lvl, msg, args));
	}

	private synchronized void write(String lvl, String msg, Object ... args) {
		// Create log file if needed
		if (out == null) {
			try {
				out = new FileOutputStream(log_path);
			} catch (FileNotFoundException ex) {
				System.err.printf("Cannot init log file at %s\n", log_path);
				ex.printStackTrace();
			}
		}

		// Add message to buffer
		buffer.offer(prepare(lvl, msg, args));

		// Flush buffer if needed
		while (!buffer.isEmpty()) {
			String l = 	buffer.poll();
			try {
				out.write(l.getBytes());
			} catch (IOException e) {
				System.err.printf("[LOG ERROR] Cannot log to %s\n", log_path);
				System.err.println(l);
			}
		};
	}

	public void fatal(String msg, Object ... args) {
		this.write("FATAL", msg, args);
		this.close();
		System.exit(1);
	}

	public void warning(String msg, Object ... args) {
		this.write("WARNING", msg, args);
	}

	public void info(String msg, Object ... args) {
		if (lvl_debug)
			this.write("INFO", msg, args);
	}

	public void debug(String msg, Object ... args) {
		if (lvl_debug)
			this.write("DEBUG", msg, args);
	}

	public void ensure(boolean res, String msg, Object... args) {
		if (res)
			return;
		this.fatal(msg, args);
	}


	/*
	 * SEVERE
	 * WARNING
	 * INFO
	 * FIN*
	 */

	public void close() {
		if (this.out == null)
			return;
		try {
			this.out.flush();
			this.out.close();
		} catch (IOException e) {
			System.err.println("Error closing log file");
			e.printStackTrace();
		}
	}

	static public void setup(boolean debug, String shell) throws IOException {
		GdbLogger log = new GdbLogger();
		log.log_path = String.format("%s%s_%d.log", Venv.LOGS_DIR, shell, System.currentTimeMillis() / 1000);
		lvl_debug = debug;
	}

	static public GdbLogger getLogger() {
		return self;
	}
}

