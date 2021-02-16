package logger;

public class ConsoleHandler implements LogHandler {
	@Override
	public void handle(LogEntry entry) {
		if(entry.level.botherPrinting) {
			boolean isPrefixEmpty=entry.level.prefix.equals("");
			if(!entry.level.inErrStream) {
				System.out.println((isPrefixEmpty?"":"[")+entry.level.prefix+(isPrefixEmpty?"":"]: ")+entry.message);
			} else {
				System.err.println((isPrefixEmpty?"":"[")+entry.level.prefix+(isPrefixEmpty?"":"]: ")+entry.message);
			}
		}
	}
}
