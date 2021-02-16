package logger;

import java.util.HashSet;

public class Logger {
	public static final LogLevel logger_internal=new LogLevel("LOGGER_INTERNAL",true,false,false);
	public static final LogLevel no_prefix=new LogLevel("",true,false,false);
	public static final LogLevel[] levels=new LogLevel[] {
			new LogLevel("DEBUG",true,false,false), //0
			new LogLevel("INFO",true,false,false), //1
			new LogLevel("WARN",true,false,false), //2
			new LogLevel("ERROR",true,true,false), //3
			new LogLevel("FATAL",true,true,true) //4
	};
	public static HashSet<LogEntry> local=new HashSet<LogEntry>();
	public static LogHandler[] handlers=new LogHandler[] {new ConsoleHandler()};
	public static boolean localLog=true;
	public static void log(LogEntry entry) {
		if(localLog) {local.add(entry);}
		for(LogHandler handler : handlers) {
			handler.handle(entry);
		}
		entry.level.tryExit();
	}
	public static void log(int level, String message) {
		log(new LogEntry(level,message));
	}
	public static void log(LogLevel level, String message) {
		log(new LogEntry(level,message));
	}
	public static void log(int level, String message, Exception attachment) {
		log(new LogEntry(level,message,attachment));
	}
	public static void clearLocal() {
		log(new LogEntry(logger_internal,"Clearing local logs"));
	}
}
