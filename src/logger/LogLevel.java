package logger;

public class LogLevel {
	String prefix;
	boolean botherPrinting=true;
	boolean inErrStream=false;
	boolean exitOnRecv=false;
	int exitCode=1;
	public LogLevel(String p, boolean b, boolean e, boolean ex) {
		this.prefix=p;
		this.botherPrinting=b;
		this.inErrStream=e;
		this.exitOnRecv=ex;
	}
	public LogLevel setExitCode(int nCode) {
		this.exitCode=nCode;
		return this;
	}
	public void tryExit() {
		if(exitOnRecv) {
			Logger.log(Logger.no_prefix,"This isn't a fake, unimportant surprise like last time.");
			Logger.log(Logger.no_prefix,"It's a *fatal* surprise, with tragic consequences.");
			Logger.log(Logger.no_prefix,"And a real emergency exit this time.");
			System.exit(exitCode);
		}
	}
}
