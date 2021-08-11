package console;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.python.util.PythonInterpreter;

import lepton.util.LeptonUtil;
import lepton.util.advancedLogger.Logger;
import lepton.util.console.ConsoleWindow;

public class JythonConsoleManager {
	private interface OnJythonOutput {
		void recv(String in);
	}
	private class JythonOutputStreamPoller implements Runnable {
		public volatile boolean run=true;
		private volatile BufferedReader reader;
		private PipedOutputStream out;
		private OnJythonOutput onOutput;
		public JythonOutputStreamPoller(PipedOutputStream out, OnJythonOutput ojo) {
			this.out=out;
			this.onOutput=ojo;
			try {
				PipedInputStream input=new PipedInputStream(out);
				reader=new BufferedReader(new InputStreamReader(input));
			} catch (IOException e) {
				Logger.log(4,e.toString());
			}
		}
		public void stop() {
			run=false;
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				Logger.log(3,e.toString());
			}
			try {
				out.write((byte)('\n'));
			} catch (IOException e) {
				Logger.log(4,e.toString());
			}
		}
		public void start() {
			(new Thread(this)).start();
		}
		@Override
		public void run() {
			try {
				while(run) {
					String line=reader.readLine();
					if(run) {
						onOutput.recv(line);
					}
				}
			} catch (IOException e) {
				Logger.log(4,e.toString());
			}
		}
	}
	private ConsoleWindow consoleWindow;
	private boolean pollOutputStream=false;
	private PythonInterpreter jython;
	public JythonConsoleManager(ConsoleWindow cw) {
		consoleWindow=cw;
	}
	
	private JythonOutputStreamPoller osp;
//	private StringWriter sw=new StringWriter();
	public void initJython() {
		jython=new PythonInterpreter();
		pollOutputStream=true;
		println("JYTHON output stream initialized.");
		PipedOutputStream jython_out=new PipedOutputStream();
		jython.setOut(new PrintWriter(jython_out));
		jython.setErr(new PrintWriter(jython_out));
//		jython.setOut(sw);
//		jython.setErr(sw);
		osp=new JythonOutputStreamPoller(jython_out,this::println);
		osp.start();
		jython.exec("import sys");
		jython.exec("sys.path.append(\""+LeptonUtil.getJarPath()+"\")");
		jython.exec("from game import Main");
		jython.exec("from console.Commands import *");
	}
	public void stopJython() {
		pollOutputStream=false;
		osp.stop();
		jython.close();
		jython=null;
	}
	
	public synchronized void println(String s) {
		consoleWindow.println(s);
	}
	
	public void recv(String cmd) {
		if(pollOutputStream) {
			jython.exec(cmd);
		} else {
			Logger.log(1,"Jython is not polling the console input.");
		}
//		System.out.println("GOT: "+sw.toString());
	}
	public boolean isPollingOutputStream() {
		return pollOutputStream;
	}
}
